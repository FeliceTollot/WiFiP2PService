package com.example.amantova.wifip2pservice.io;

import java.io.InputStream;
import java.io.OutputStream;

public class GpsStrategyServer implements IOStrategy {
    public void run(InputStream in, OutputStream out);

    // recupare valore gps
    if( PackageManager.hasSystemFeature("FEATURE_LOCATION") == false ){
      // some problem
    }else{
      LocationManager manager = Context.getSystemService("Context.LOCATION_SERVICE");

      Location value = manager.getLastKnownLocation(String provider)
    }

    // mandalo nell'outputstream
}
