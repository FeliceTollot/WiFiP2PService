package com.example.amantova.wifip2pservice;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServiceServerTask extends AsyncTask<Void, Void, Void> {
    private ServerSocket mServerSocket;

    public ServiceServerTask(ServerSocket socket) {
        mServerSocket = socket;
    }

    @Override
    public Void doInBackground(Void... params) {
        Log.d("Server Socket", "Running in background!");
        try {
            while (true) {
                Log.d("Server Socket", "Waiting new client connections...");
                Socket client = mServerSocket.accept();
                Log.d("Server Socket", "A new connection was accepted (port: " + client.getLocalPort() + ").");

                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

                String request = in.readLine();
                Log.d("Server Socket", "Received: " + request);
                out.println("... world!");
                Log.d("Server Socket", "Send: ... world!");

                out.close();
                in.close();
            }
        } catch (IOException e) {
            Log.e("Server Socket", e.getMessage());
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
