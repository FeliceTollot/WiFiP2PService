package com.example.amantova.wifip2pservice.IO;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

public class GpsStrategyServer implements IOStrategy {

    Activity main_activity;

    public GpsStrategyServer(Activity arg){
        main_activity = arg;
    }

    private static byte[] convertToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putDouble(value);
        return buffer.array();

    }

    @Override
    public void run(final Socket socket, final Object sync){
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(main_activity);

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(main_activity, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object
                            Log.d("Longitude: ",String.valueOf(location.getLongitude()));
                            Log.d("Latitude: ",String.valueOf(location.getLatitude()));

                            try {
                                OutputStream out = socket.getOutputStream();

                                // out.write(convertToByteArray(location.getLongitude()));
                                out.write(convertToByteArray(location.getLatitude()));

                                out.close();
                                socket.close();

                                sync.notifyAll();
                            }catch(IOException exc){
                                Log.d("IOException", String.valueOf(exc.getCause()));
                            }

                        }else{
                            Log.d("Error: ", "null value location object");
                        }
                    }
                });

    }

}
