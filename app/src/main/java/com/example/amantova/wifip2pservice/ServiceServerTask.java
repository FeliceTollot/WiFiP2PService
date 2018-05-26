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
            while (true) {
                Log.d("Server Socket", "Waiting new client connections...");
                Socket client = mServerSocket.accept();
                Log.d("Server Socket", "A new connection was accepted (port: " + client.getLocalPort() + ").");

                Object sync = new Object();

                mStrategy.run(client, sync);
                Log.d("ServiceServerTask", "Task is running!");
/*
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                String request = in.readLine();
                Log.d("Server Socket", "Received: " + request);
                out.println("... world!");
                Log.d("Server Socket", "Send: ... world!");
*/
                Log.d("ServiceServerTask", "Waiting task termination");
                synchronized (sync) {
                    while (!mServerSocket.isClosed()) { sync.wait(); }
                }

                Log.d("ServiceServerTask", "Finished!");
            }
        } catch (IOException e) {
            Log.d("ServiceServerSocket", "IOException occurred");
            if (e.getMessage() != null) {
                Log.d("ServiceServerSocket", e.getMessage());
            }
        } catch (InterruptedException e) {
            Log.d("ServiceServerSocket", "InterruptedException occurred");
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
