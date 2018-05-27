// routing for DTnets based on partial knowledge

package com.example.amantova.wifip2pservice.routing;

import com.example.amantova.wifip2pservice.format.Format;

import java.nio.charset.Charset;
import java.util.*;

public class Packet_table_item{
  public String dest_eid;
  public long timestamp;
  public long ttl;
  public String service;
  public byte[] payload;

  public Packet_table_item(String arg1, long arg2, long arg3, String service_arg, byte[] load){
    dest_eid = arg1;
    timestamp = arg2;
    ttl = arg3;
    service = service_arg;
    payload = load;
  }

  public static String serialize(Packet_table_item item){
    String result = "";
    result = result + item.dest_eid + ";";
    result = result + String.valueOf(item.timestamp) + ";";
    result = result + String.valueOf(item.ttl) + ";";
    result = result + item.service + ";";


    String payload = new String(item.payload, Charset.forName("UTF-8"));

    result = result + payload;
    return result;
  }

  public static Packet_table_item deserialize(String str){
    // extract info from str
    String[] parts = str.split(";");
    String dest_eid = parts[0];
    long timestamp = Long.parseLong( parts[1] );
    long ttl = Long.parseLong( parts[2] );
    String service = parts[3];
    byte[] payload = parts[4].getBytes(Charset.forName("UTF-8"));
    Packet_table_item item = new Packet_table_item(dest_eid, timestamp, ttl, service, payload);
    return item;
  }

}
