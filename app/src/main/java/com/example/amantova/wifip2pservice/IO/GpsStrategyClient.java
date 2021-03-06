package com.example.amantova.wifip2pservice.IO;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class GpsStrategyClient implements IOStrategy {

    private static byte[] convertToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putDouble(value);
        return buffer.array();
    }
    private static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public void run(Socket socket){
        //byte[] b1 = convertToByteArray(1);
        //byte[] b2 = convertToByteArray(2);

        try {

            //PrintWriter out =
            //        new PrintWriter(echoSocket.getOutputStream(), true);
            // out.println(userInput);
            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));


            String longitude = in.readLine();
            String latitude = in.readLine();

            Log.d("Longitude",longitude);
            Log.d("Latitude", latitude);
            //Log.d("GpsClient", "Received: " + Arrays.toString(b1));

            //in.read(b2);
            //Log.d("GpsClient", "Received: " + Arrays.toString(b2));

            //Log.d("Longitude",String.valueOf(toDouble(b1)));
            //Log.d("Latitude", String.valueOf(toDouble(b2)));

            in.close();
            socket.close();
        }catch(IOException exc){
            Log.d("IOEXCEPTION:", String.valueOf(exc.getCause()));
        }


    };
}
