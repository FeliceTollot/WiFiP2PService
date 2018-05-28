package com.example.amantova.wifip2pservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.os.Debug;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.amantova.wifip2pservice.IO.GossipStrategyClient;
import com.example.amantova.wifip2pservice.IO.GossipStrategyServer;
import com.example.amantova.wifip2pservice.IO.GpsStrategyClient;
import com.example.amantova.wifip2pservice.IO.GpsStrategyServer;
import com.example.amantova.wifip2pservice.format.Format;
import com.example.amantova.wifip2pservice.IO.IOStrategy;
import com.example.amantova.wifip2pservice.routing.Packet_table;
import com.example.amantova.wifip2pservice.routing.Packet_table_item;
import com.example.amantova.wifip2pservice.routing.Routing_table;
import com.example.amantova.wifip2pservice.routing.Waiting_table;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private final IntentFilter _IntentFilter = new IntentFilter();

    private Channel         mChannel;
    private WifiP2pManager  mManager;

    private HashMap<String, IOStrategy> mServicesClient = new HashMap<>();

    private List<String>    mServices = new LinkedList<>();

    private Waiting_table   mWaitingTable = new Waiting_table();
    private Packet_table    mPacketTable = new Packet_table();
    private Routing_table   mRoutingTable = new Routing_table(Format.gen_eid());

    private Queue<WifiP2pDevice>    mMetDevices = new LinkedList<>();
    private WifiP2pDevice           mWaitingDevice = null;

    private Timer mSchedulerTimer = new Timer();

    private static final int MAX_MET_DEVICES = 5;
    private static final int PEERS_CHECK_PERIOD = 30000;
    private static final int PACKAGE_CHECK_PERIOD = 5000;
    private static final int CLEAN_MET_TABLE_PERIOD = 10 * 60 * 1000;
    private static final int TIME_FROM_LAST_MET = 60;

    private final List<String>      mDeviceWhiteList = new LinkedList<>();
    private HashMap<String, Long>   mMetTimeTable = new HashMap<>();

    private WifiP2pManager.ConnectionInfoListener connectionListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            // This is the client and it's waiting to satisfy its service request
            if (mWaitingDevice != null) {
                InetSocketAddress target = new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), 2702);

                GossipStrategyClient gossipClient = new GossipStrategyClient(mRoutingTable, mWaitingTable, mPacketTable);
                AsyncTask clientTask = new ServiceClientTask(target, gossipClient).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                try {
                    Log.d("Client Task", "Running ...");
                    clientTask.get();
                    Log.d("Client Task", "... completed!");
                    disconnectToWifiP2PDevice();
                } catch (Exception e) {
                    Log.d("Client Task", "Error during the task execution.");
                    disconnectToWifiP2PDevice();
                }
            }
        }
    };

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            Collection<WifiP2pDevice> devices = peerList.getDeviceList();

            // Some peers found nearby
            if (devices.size() > 0 && mWaitingDevice == null) {
                Iterator<WifiP2pDevice> itr = devices.iterator();
                WifiP2pDevice potentialTarget = null; // itr.next();

                while (itr.hasNext()) {
                    WifiP2pDevice actual = itr.next();

                    Long time = mMetTimeTable.get(potentialTarget.deviceAddress);
                    long now = System.currentTimeMillis() / 1000;

                    if (time == null || TIME_FROM_LAST_MET < now - time) {
                        potentialTarget = actual;
                    }

                    mMetTimeTable.put(potentialTarget.deviceAddress, now);
                }
/*
                // Ignore all devices was met recently or does not provide the Gossip service
                while (!mDeviceWhiteList.contains(potentialTarget.deviceAddress)) {
                    Log.d("Peers Discovery", potentialTarget.deviceName + " (" + potentialTarget.deviceAddress + ")");
                    if (itr.hasNext()) { potentialTarget = itr.next(); }
                    else { potentialTarget = null; break; }
                }
*/
                if (potentialTarget != null && !mPacketTable.is_empty()) {
                    final WifiP2pDevice target = potentialTarget;
                    mWaitingDevice = target;

                    Log.d("Connection","Attempting to connect with: " + target.deviceName + " (" + target.deviceAddress + ")");

                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = target.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;

                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(getApplicationContext(), "Connecting to " + target.deviceName, Toast.LENGTH_LONG).show();
                            Log.d("Connection", "Launched!");
                        }

                        @Override
                        public void onFailure(int reason) {
                            mWaitingDevice = null;
                            Log.d("Connection", "Connect failed");
                        }
                    });
                }
            }
        }
    };

    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Log.d("WiFi P2P Status", "On");
                } else {
                    Log.d("WiFi P2P Status", "Off");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if (mManager != null) {
                    mManager.requestPeers(mChannel, peerListListener);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (mManager == null) {
                    Log.e("BroadcastReceiver Wifi", "WifiP2pManager is set to null");
                    return;
                }

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    // Remove the oldest device met
                    mMetDevices.add(mWaitingDevice);
                    if (mMetDevices.size() > MAX_MET_DEVICES) { mMetDevices.poll(); }

                    Log.d("Connection", "Success! The device is connected with an other one");
                    mManager.requestConnectionInfo(mChannel, connectionListener);
                } else { // The devises are disconnected
                    mWaitingDevice = null;
                    discoverPeers();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Indicates a change in the Wi-Fi P2P status.
        _IntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);

        // Indicates a change in the list of available peers.
        _IntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        _IntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);

        // Indicates this device's details have changed.
        _IntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        mManager = (WifiP2pManager) getSystemService(getApplicationContext().WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        // setDiscoveryServiceListener();

        // List of available services
        mServices.add("computation");
        mServices.add("gps");
/*
        // PhonePad MAC Address
        mDeviceWhiteList.add("02:08:22:0d:4f:a3");
        // Samsung MAC Address
        mDeviceWhiteList.add("82:57:19:50:a5:59");
        // Samsung Galaxy MAC Address
        mDeviceWhiteList.add("f2:5b:7b:03:d3:85");
*/
        startGossipService();

        TextView txtID = findViewById(R.id.txtID);
        txtID.setText(mRoutingTable.get_my_eid());

        Button btnSendMessage = findViewById(R.id.btnSendMessage);
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView txtMessage = findViewById(R.id.txtMessage);
                TextView txtRecipient = findViewById(R.id.txtRecipient);

                String message = txtMessage.getText().toString();
                String recipient = txtRecipient.getText().toString();

                String dest_eid = recipient;
                long timestamp = System.currentTimeMillis()/1000;
                long ttl = 10000;
                String service = "_text";
                byte[] payload = message.getBytes(Charset.forName("UTF-8"));

                Packet_table_item packet = new Packet_table_item(recipient,timestamp,ttl,service,payload);
                mPacketTable.add_packet(packet);

                txtMessage.setText("");
                txtRecipient.setText("");

                Toast toast = Toast.makeText(getApplicationContext(), "Message sent!", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        TimerTask peersCheck = new TimerTask() {
            @Override
            public void run() {
                // Log.d("Peers Discovering", "Rescheduled Peer Discovery");
                // discoverService();
                discoverPeers();
            }
        };

        TimerTask packageTableCheck = new TimerTask() {
            @Override
            public void run() {
                for (String eid : mPacketTable.get_eids_list()) {
                    Packet_table_item myPacket = mPacketTable.get(eid);

                    final String payload = new String(myPacket.payload, Charset.forName("UTF-8"));
                    final String recipient = new String(myPacket.dest_eid);

                    if (eid.equals(mRoutingTable.get_my_eid())) {
                        mPacketTable.remove_packet(mRoutingTable.get_my_eid());
                        Log.d("Check Packet Table", "Found my packet! " + payload);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), payload + " by " , Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Hold '" + payload + "' for " + recipient, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }
        };

        TimerTask cleanMetTableTask = new TimerTask() {
            @Override
            public void run() {
                for (String device : mMetTimeTable.keySet()) {
                    if (mMetTimeTable.get(device) > 10 * 60 * 1000) {
                        mMetTimeTable.remove(device);
                    }
                }
            }
        };

        mSchedulerTimer.schedule(packageTableCheck, 1, PACKAGE_CHECK_PERIOD);
        mSchedulerTimer.schedule(peersCheck, 1, PEERS_CHECK_PERIOD);
        mSchedulerTimer.schedule(cleanMetTableTask, 1, CLEAN_MET_TABLE_PERIOD);
    }

    private void disconnectToWifiP2PDevice() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                mWaitingDevice = null;
                Log.d("Disconnection", "Success!");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("Disconnect", "Error: " + reason);
            }
        });
    }

    private void discoverPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("DiscoverPeers", "Success!");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("DiscoverPeers", "Error: " + reason);
            }
        });
    }

    private void discoverService() {
        setDiscoveryServiceListener();

        mManager.addServiceRequest(mChannel, WifiP2pDnsSdServiceRequest.newInstance(), new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("AddServiceRequest", "Success!");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("AddServiceRequest", "Service Request Error: " + reason);
            }
        });

        mManager.discoverServices(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("ServiceDiscovering", "Launched!");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("ServiceDiscovering", "Service Discover Error: " + reason);
            }
        });
    }

    private void setDiscoveryServiceListener() {
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, final Map<String, String> txtRecordMap, WifiP2pDevice provider) {
                final String serviceName = fullDomainName.split("\\.")[0];
                Log.d("Service Discovery", "Service found: " + serviceName);
                Log.d("Service Discovery", "Provided by: " + provider.deviceAddress);

                if (serviceName.equals("_gossip")) {
                    // mRoutingTable.meet(txtRecordMap.get("identity"));
                    Log.d("Service Discovery", "Met " + txtRecordMap.get("identity"));

                    if (!mDeviceWhiteList.contains(provider.deviceAddress)) {
                        mDeviceWhiteList.add(provider.deviceAddress);
                        Log.d("Service Discovery", "The peer " + txtRecordMap.get("identity") + " (" + provider.deviceAddress + ") was added to the white list.");
                    }
                }
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice provider) {
                Log.d("Service Discovery", "Message: " + instanceName + " " + registrationType);
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, serviceListener, txtListener);
    }

    private void startGossipService() {
        IOStrategy gossipServer = new GossipStrategyServer(mRoutingTable, mWaitingTable, mPacketTable);

        try {
            ServerSocket serverSocket = new ServerSocket(2702);
            Log.d("Start Gossip", "Starting Gossip service on port " + serverSocket.getLocalPort());
            new ServiceServerTask(serverSocket, gossipServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            HashMap<String, String> record = new HashMap<>();
            record.put("port", String.valueOf(serverSocket.getLocalPort()));
            record.put("identity", mRoutingTable.get_my_eid());

            registerGossipService(record);
        } catch (IOException e) {
            Log.d("Start Gossip", e.getMessage());
        }
    }

    private void registerGossipService(HashMap<String, String> record) {
        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance("_gossip" + mRoutingTable.get_my_eid(), "_presence._tcp", record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("Gossip Registration","Success!!");
            }

            @Override
            public void onFailure(int reason) {
                Log.e("Gossip Registration", String.valueOf(reason));
                // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mWifiReceiver, _IntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mWifiReceiver);
    }
}
