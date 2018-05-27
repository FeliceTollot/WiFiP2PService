package com.example.amantova.wifip2pservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private final IntentFilter _IntentFilter = new IntentFilter();

    private Channel         mChannel;
    private WifiP2pManager  mManager;

    private boolean         mAllowServiceRequest = true;
    // Will be set when a needed service is discovered (into onDnsSdTxtRecordAvailable callback)
    private WaitingService  mWaitingService;

    private HashMap<String, IOStrategy> mServicesClient = new HashMap<>();

    private Waiting_table   mWaitingTable = new Waiting_table();
    private Packet_table    mPacketTable = new Packet_table();
    private Routing_table   mRoutingTable = new Routing_table(Format.gen_eid());

    private WifiP2pManager.ConnectionInfoListener connectionListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            // This is the client and it's waiting to satisfy its service request
            if (mWaitingService != null) {
                Log.d("Connection Information", info.groupOwnerAddress.getHostAddress() + " " + mWaitingService.name + ":" + mWaitingService.port);
                InetSocketAddress target = new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), mWaitingService.port);

                AsyncTask clientTask = new ServiceClientTask(target, mServicesClient.get(mWaitingService.name)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            // No peers found nearby
            if (peers.getDeviceList().size() == 0) {
                Log.d("Peers Discovery", "No peers found! Rescanning.");
                // Reschedule a new peers discovery
                discoverPeers();
            } else if (mAllowServiceRequest) {
                Log.d("Peers Discovery", "New peers found! Discover the provided services.");
                // Discover the services provided by the close peers
                discoverService();
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

        setDiscoveryServiceListener();
        registerAvailableServices();

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

        discoverPeers();
    }

    private void disconnectToWifiP2PDevice() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
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

    private int initializeServerSocketForService(IOStrategy serverStrategy) throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);

        new ServiceServerTask(serverSocket, serverStrategy).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Log.d("ServiceRegistration", "Listening service request on port " + serverSocket.getLocalPort());

        return serverSocket.getLocalPort();
    }

    private void registerAvailableServices() {
        ArrayList<ServiceInfo> providedService = new ArrayList<>();
/*
        final GpsStrategyServer gps = new GpsStrategyServer(this);
        providedService.add(new ServiceInfo() {{
            name = "_gps";
            type = "_presence._tpc";
            server = gps;
        }});
        mServicesClient.put("_gps", new GpsStrategyClient());
*/
        final GossipStrategyServer gossipStrategy = new GossipStrategyServer(mRoutingTable, mWaitingTable, mPacketTable);
        ServiceInfo gossip = new ServiceInfo() {{
            name = "_gossip";
            type = "_presence._tpc";
            server = gossipStrategy;
        }};
        mServicesClient.put("_gossip", new GossipStrategyClient(mRoutingTable, mWaitingTable, mPacketTable));

        HashMap<String, String> record = new HashMap<>();

        try {
            // Register custom services
            for (ServiceInfo service : providedService) {
                record.put("port",  String.valueOf(initializeServerSocketForService(service.server)));
                registerService(service, record);

                record.clear();
            }

            // Register Gossip service
            record.put("port", String.valueOf(initializeServerSocketForService(gossip.server)));
            record.put("identity", mRoutingTable.get_my_eid());

            registerService(gossip, record);
        } catch (IOException e) {
            Log.e("Service Registration", "Failed to register service");
        } catch (Exception e) {
            Log.e("Service registration", e.getMessage());
        }
    }

    private void setDiscoveryServiceListener() {
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, final Map<String, String> txtRecordMap, WifiP2pDevice provider) {
                final String serviceName = fullDomainName.split("\\.")[0];
                Log.d("Service Discovery", "Service found: " + serviceName);

                // Register the device met
                if (serviceName.equals("_gossip")) {
                    Log.d("Service Discovery", "Met the user " + txtRecordMap.get("identity"));
                    mRoutingTable.meet(txtRecordMap.get("identity"));
                }

                if (mAllowServiceRequest /*&& mWaitingTable.check_service_need(serviceName)*/) {
                    mAllowServiceRequest = false;
                    // Deny other attempts to connect with other service discovered previously
                    mWaitingService = new WaitingService() {{
                       name = serviceName;
                       port = Integer.parseInt(txtRecordMap.get("port"));
                    }};

                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = provider.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;

                    Log.d("Connection","Attempting to connect with: " + provider.deviceName + " (" + provider.deviceAddress + ")");

                    mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d("Connection", "Launched!");
                        }

                        @Override
                        public void onFailure(int reason) {
                            mAllowServiceRequest = true;
                            Log.d("Connection", "Connect failed");
                        }
                    });
                }
            }
        };

        WifiP2pManager.DnsSdServiceResponseListener serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {
            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice provider) {
                // Log.d("ServiceDiscovering", instanceName + " " + registrationType + " " + provider.deviceName);
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, serviceListener, txtListener);
    }

    private void registerService(ServiceInfo service, HashMap<String, String> record) {
        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(service.name, service.type, record);

        // Add the local service, sending the service info, network channel,
        // and listener that will be used to indicate success or failure of
        // the request.
        mManager.addLocalService(mChannel, serviceInfo, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("ServiceRegistration","Success!!");
            }

            @Override
            public void onFailure(int reason) {
                Log.e("ServiceRegistration", String.valueOf(reason));
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
