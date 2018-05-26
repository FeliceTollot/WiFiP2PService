package com.example.amantova.wifip2pservice;

import android.os.AsyncTask;
import android.util.Log;

import com.example.amantova.wifip2pservice.IO.IOStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ServiceClientTask extends AsyncTask<Void, Void, Void> {
    private InetSocketAddress   mTarget;
    private IOStrategy          mStrategy;

    public ServiceClientTask(InetSocketAddress target, IOStrategy strategy) {
        mTarget = target;
        mStrategy = strategy;
    }

    @Override
    public Void doInBackground(Void... params) {
        Log.d("Socket Client", "Running in background!");
        Socket socket = new Socket();

        try {
            socket.bind(null);
            Log.d("Socket Client", "Wait to establish a connection...");
            socket.connect(mTarget, 500);
            Log.d("Socket Client", "Connected with the server!");

            mStrategy.run(socket, null);
            /*
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Hello...");
            Log.d("Socket Client", "Write: Hello... ");
            String result = in.readLine();
            Log.d("Socket Client", "Received: " + result);
            */
        } catch (IOException e) {
            Log.e("Socket Client", e.getMessage());
        } finally {
            if (socket != null && socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e("Socket Client", e.getMessage());
                }
            }
        }

        return null;
    }
}
