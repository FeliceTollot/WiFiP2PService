package com.example.amantova.wifip2pservice;

import android.os.AsyncTask;
import android.util.Log;

import com.example.amantova.wifip2pservice.IO.IOStrategy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServiceServerTask extends AsyncTask<Void, Void, Void> {
    private ServerSocket    mServerSocket;
    private IOStrategy      mStrategy;

    public ServiceServerTask(ServerSocket socket, IOStrategy strategy) {
        mServerSocket = socket;
        mStrategy = strategy;
    }

    @Override
    public Void doInBackground(Void... params) {
        Log.d("Server Socket", "Running in background!");
        try {
            while (!isCancelled()) {
                Log.d("Server Socket", "Waiting new client connections...");
                Socket client = mServerSocket.accept();
                Log.d("Server Socket", "A new connection was accepted (port: " + client.getLocalPort() + ").");

                Log.d("ServiceServerTask", "Task is running!");
                mStrategy.run(client);
                Log.d("ServiceServerTask", "Finished!");
            }
        } catch (IOException e) {
            Log.d("ServiceServerSocket", "IOException occurred");
            if (e.getMessage() != null) {
                Log.d("ServiceServerSocket", e.getMessage());
            }
        } finally {
            if (mServerSocket != null) {
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    Log.e("Server socket", e.getMessage());
                }
            }
        }

        return null;
    }
}
