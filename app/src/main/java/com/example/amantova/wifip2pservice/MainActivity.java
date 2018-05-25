package com.example.amantova.wifip2pservice;

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
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Provider;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final IntentFilter _IntentFilter = new IntentFilter();

    private Channel         mChannel;
    private WifiP2pManager  mManager;

    private Service_table   mServices = new Service_table();

    private boolean         mAllowServiceRequest = true;

    private LinkedList<WaitingService> mWaitingService = new LinkedList<>();

    private WifiP2pManager.ConnectionInfoListener connectionListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            if (!mWaitingService.isEmpty()) {
                Log.d("Connection Information", info.groupOwnerAddress.getHostAddress());
                WaitingService waitingService = mWaitingService.remove();

                new ServiceClientTask(new InetSocketAddress(info.groupOwnerAddress.getHostAddress(), waitingService.port))
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
                    // m_txtWifiP2PStatus.setText("On");
                } else {
                    // m_txtWifiP2PStatus.setText("Off");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // if the device is not connected to an other
                discoverService();
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (mManager == null) {
                    Log.e("BroadcastReceiver Wifi", "WifiP2pManager is set to null");
                    return;
                }

                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {
                    Log.d("Connection", "Success! The device is connected with an other one");
                    mManager.requestConnectionInfo(mChannel, connectionListener);
                }

                // Connection state changed! We should probably do something about
                // that.

            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                /*
                DeviceListFragment fragment = (DeviceListFragment) activity.getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.updateThisDevice((WifiP2pDevice) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));
                        */
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
/*
        final Button btnSearchService = findViewById(R.id.btnSearchServie);
        btnSearchService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { discoverService(); }
        });
*/
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

    private int initializeServerSocket() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);

        new ServiceServerTask(serverSocket).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        Log.d("ServiceRegistration", "Listening service request on port " + serverSocket.getLocalPort());

        return serverSocket.getLocalPort();
    }

    private void registerAvailableServices() {
        HashMap<String, String> record = new HashMap<>();

        mServices.add(new ServiceInfo("_gps", "_presence._tcp"));

        try {
            // Register custom services
            for (ServiceInfo service : mServices.get_all()) {
                record.put("port", String.valueOf(initializeServerSocket()));
                register(service, record);

                record.clear();
            }

            record.put("port", String.valueOf(initializeServerSocket()));
            record.put("identity", Format.gen_eid());

            register(new ServiceInfo("_gossip", "presence._tcp"), record);
        } catch (IOException e) {
            Log.e("Service Registration", "Failed to register service");
        } catch (Exception e) {
            Log.e("Service registration", e.getMessage());
        }
    }

    private void setDiscoveryServiceListener() {
        WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice provider) {
                String serviceName = fullDomainName.split(".")[0];

                if (mAllowServiceRequest && waitingTable.isRequiredService(serviceName)) {
                    // Deny other attempts to connect with other service discovered previously
                    mAllowServiceRequest = false;

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
                Log.d("ServiceDiscovering", instanceName + " " + registrationType + " " + provider.deviceName);
            }
        };

        mManager.setDnsSdResponseListeners(mChannel, serviceListener, txtListener);
    }

    private void register(ServiceInfo service, HashMap<String, String> record) {
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
