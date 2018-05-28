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

    private boolean         mAllowServiceRequest = true;

    private HashMap<String, IOStrategy> mServicesClient = new HashMap<>();

    private List<String>    mServices = new LinkedList<>();

    private Waiting_table   mWaitingTable = new Waiting_table();
    private Packet_table    mPacketTable = new Packet_table();
    private Routing_table   mRoutingTable = new Routing_table(Format.gen_eid());

    private Queue<WifiP2pDevice>    mMetDevices = new LinkedList<>();
    private WifiP2pDevice           mWaitingDevice = null;

    private Timer mSchedulerTimer = new Timer();

    private final int MAX_MET_DEVICES = 5;
    private final int PERIOD_PEERS_CHECK = 10000;
    private final int MAX_ATTEMPT_CONNECTION_TIME = 10000;

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
                WifiP2pDevice potentialTarget = itr.next();

                while (mMetDevices.contains(potentialTarget.deviceAddress)) {
                    Log.d("Peers Discovery", potentialTarget.deviceName + " ( " + potentialTarget.deviceAddress + " )");
                    if (itr.hasNext()) { potentialTarget = itr.next(); }
                    else { potentialTarget = null; break; }
                }

                if (potentialTarget != null && mPacketTable.is_empty()) {
                    final WifiP2pDevice target = potentialTarget;
                    mWaitingDevice = target;

                    Log.d("Connection","Attempting to connect with: " + target.deviceName + " (" + target.deviceAddress + ")");

                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = target.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;

                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
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
                    mAllowServiceRequest = true;
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

        // List of available services
        mServices.add("computation");
        mServices.add("gps");

        startGossipServer();

        TextView txtID = findViewById(R.id.txtID);
        txtID.setText(mRoutingTable.get_my_eid());

        Button btnSendMessage = findViewById(R.id.btnSendMessage);
        btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = ((TextView) findViewById(R.id.txtMessage)).getText().toString();
                String recipient = ((TextView) findViewById(R.id.txtRecipient)).getText().toString();

                String dest_eid = recipient;
                long timestamp = System.currentTimeMillis()/1000;
                long ttl = 10000;
                String service = "_text";
                byte[] payload = message.getBytes(Charset.forName("UTF-8"));

                Packet_table_item packet = new Packet_table_item(recipient,timestamp,ttl,service,payload);
                mPacketTable.add_packet(packet);
            }
        });

        TimerTask peersCheck = new TimerTask() {
            @Override
            public void run() {
                Log.d("Peers Discovering", "Rescheduled Peer Discovery");
                discoverPeers();
            }
        };
        mSchedulerTimer.schedule(peersCheck, 1000, PERIOD_PEERS_CHECK);
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

    private void startGossipServer() {
        IOStrategy gossipServer = new GossipStrategyServer(mRoutingTable, mWaitingTable, mPacketTable);

        try {
            ServerSocket serverSocket = new ServerSocket(2702);
            Log.d("Start Gossip", "Starting Gossip service on port " + serverSocket.getLocalPort());
            new ServiceServerTask(serverSocket, gossipServer).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } catch (IOException e) {
            Log.d("Start Gossip", e.getMessage());
        }
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
