//  Format of Boundle_lite packets

//  EID destination             24 Bit
//  EID source                  24 Bit
//  Time to live                32 Bit  (in seconds)
//  Timestamp                   64 Bit  (in seconds)
//  Fragmented                  8  Bit
//  Number of fragmentation     24 Bit  (12 Bit / 12 Bit)
//  Size payload                32 Bit  (in bytes)
//  Payload                     variable

package com.example.amantova.wifip2pservice.format;

import java.util.*;
import java.nio.ByteBuffer;

public class Format{

  private byte[] eid_destination;
  private byte[] eid_source;
  private byte[] time_to_live;
  private byte[] timestamp;
  private byte fragmented;
  private byte[] num_fragmentation;
  private byte[] size_payload;
  private byte[] payload;

  public static byte[] longToBytes(long x) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(x);
    return buffer.array();
  }
  public static byte[] intToBytes(int x) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.putInt(x);
    return buffer.array();
  }
  public static long bytesToLong(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(bytes);
    buffer.flip();//need flip
    return buffer.getLong();
  }
  public static int bytesToInt(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    buffer.put(bytes);
    buffer.flip();//need flip
    return buffer.getInt();
  }

  public void set_eid_dest(byte[] eid){
    eid_destination = eid;
  }
  public void set_eid_source(byte[] eid){
    eid_source = eid;
  }
  public void set_ttl(byte[] ttl){
    time_to_live = ttl;
  }
  public void set_timestampt(byte[] time){
    timestamp = time;
  }
  public void set_fragmented(byte fragm){
    fragmented = fragm;
  }
  public void set_frag_num(byte[] frag_num){
    num_fragmentation = frag_num;
  }
  public void set_size_load(byte[] size_load){
    size_payload = size_load;
  }
  public void set_payload(byte[] load){
    payload = load;
  }

  public byte[] get_eid_dest(){
    return eid_destination;
  }
  public byte[] get_eid_source(){
    return eid_source;
  }
  public byte[] get_ttl(){
    return time_to_live;
  }
  public byte[] get_timestampt(){
    return timestamp;
  }
  public byte get_fragmented(){
    return fragmented;
  }
  public byte[] get_frag_num(){
    return num_fragmentation;
  }
  public byte[] get_size_load(){
    return size_payload;
  }
  public byte[] get_payload(){
    return payload;
  }

  public void set_boundle(byte[] packet){
    eid_destination = Arrays.copyOfRange(packet, 0, 3);
    eid_source      = Arrays.copyOfRange(packet, 3, 6);
    time_to_live    = Arrays.copyOfRange(packet, 6, 10);
    timestamp       = Arrays.copyOfRange(packet, 10, 18);
    fragmented      = packet[18];
    num_fragmentation = Arrays.copyOfRange(packet, 19, 22);
    size_payload    = Arrays.copyOfRange(packet, 22, 26);
    payload         = Arrays.copyOfRange(packet, 26, 28);
  }

  public byte[] get_boundle(){

      int size = 3 + 3 + 4 + 8 + 1 + 3 + 4 + Format.bytesToInt(size_payload);
      byte[] packet = new byte[size];

      // concatenation of fields
      packet[0] = eid_destination[0];
      packet[1] = eid_destination[1];
      packet[2] = eid_destination[2];

      packet[3] = eid_source[0];
      packet[4] = eid_source[1];
      packet[5] = eid_source[2];

      packet[6] = time_to_live[0];
      packet[7] = time_to_live[1];
      packet[8] = time_to_live[2];
      packet[9] = time_to_live[3];

      packet[10] = timestamp[0];
      packet[11] = timestamp[1];
      packet[12] = timestamp[2];
      packet[13] = timestamp[3];
      packet[14] = timestamp[4];
      packet[15] = timestamp[5];
      packet[16] = timestamp[6];
      packet[17] = timestamp[7];

      packet[18] = fragmented;

      packet[19] = num_fragmentation[0];
      packet[20] = num_fragmentation[1];
      packet[21] = num_fragmentation[2];

      packet[22] = size_payload[0];
      packet[23] = size_payload[1];
      packet[24] = size_payload[2];
      packet[25] = size_payload[3];

      for(int i=0; i<Format.bytesToInt(size_payload); i++){
        packet[26+i]= payload[i];
      }
      return packet;
}

  public static String gen_eid(){
    byte[] r = new byte[3];
    Random rnd = new Random();
    rnd.nextBytes(r);
    String s = "";
    for(int i=0; i<3; i++){
      String tmp = Integer.toBinaryString(r[i] & 0xFF);
      int times = 8 - tmp.length();
      String prefix = "";
      for(int j=0;j<times;j++){
        prefix = prefix + "0";
      }
      tmp = prefix + tmp;
      s = s + tmp;
    }
    return s;
  }

  //public static void main(String[] args) {
/*
    byte[] b1 = new byte[]{(byte) Integer.parseInt("11111111", 2),(byte) Integer.parseInt("11111111", 2),(byte) Integer.parseInt("11111111", 2)};
    byte[] b2 = new byte[]{(byte) Integer.parseInt("00000000", 2),(byte) Integer.parseInt("00000000", 2),(byte) Integer.parseInt("00000000", 2)};
    byte[] b3 = Format.intToBytes(1024);
    byte[] b4 = Format.longToBytes(System.currentTimeMillis()/1000);
    byte b5 = 0x00000000;
    byte[] b6 = new byte[]{(byte) Integer.parseInt("00000000", 2),(byte) Integer.parseInt("00000000", 2),(byte) Integer.parseInt("00000000", 2)};
    byte[] b7 = Format.intToBytes(2);
    byte[] b8 = new byte[]{(byte) Integer.parseInt("10101010", 2),(byte) Integer.parseInt("10101010", 2)};

    Format test = new Format();
    test.set_eid_dest(b1);
    test.set_eid_source(b2);
    test.set_ttl(b3);
    test.set_timestampt(b4);
    test.set_fragmented(b5);
    test.set_frag_num(b6);
    test.set_size_load(b7);
    test.set_payload(b8);

    byte[] packet = test.get_boundle();

    for(int i=0; i<28;i++){
      System.out.println("Byte: "+Integer.toBinaryString(packet[i] & 0xFF)+"\n");
    }
  }*/

//  System.out.println(gen_eid());
  //}
}
