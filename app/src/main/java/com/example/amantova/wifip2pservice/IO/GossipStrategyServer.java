
package com.example.amantova.wifip2pservice.IO;

import android.util.Log;

import com.example.amantova.wifip2pservice.IO.IOStrategy;
import com.example.amantova.wifip2pservice.routing.Packet_table;
import com.example.amantova.wifip2pservice.routing.Packet_table_item;
import com.example.amantova.wifip2pservice.routing.Routing_table;
import com.example.amantova.wifip2pservice.routing.Waiting_table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GossipStrategyServer implements IOStrategy {

    Routing_table routing_table;
    Waiting_table waiting_table;
    Packet_table  packet_table;

    public GossipStrategyServer(Routing_table rout, Waiting_table wait, Packet_table packet){
        routing_table = rout;
        waiting_table = wait;
        packet_table = packet;
    }

    public void run(Socket socket){
        try {

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

            String input = in.readLine();
            Log.d("SERVER_RECEIVED: ", input);
            if(input.equals("start_exchange_avgs")){
                input = in.readLine();
                while(input.equals("end_exchange_avgs")==false){
                    // recupera il avg eid
                    long avg = routing_table.get_avg_time(input);
                    // trasmetti avg eid
                    out.println(avg);
                    Log.d("SERVER_PACKET: ", input);
                    input = in.readLine();
                }
            }
            Log.d("SERVER_RECEIVED: ", input);

            input = in.readLine();
            Log.d("SERVER_RECEIVED: ", input);

            if(input.equals("start_receiving_packets")){
                input = in.readLine();
                while(input.equals("end_receiving_packets")==false){
                    // get the packet
                    Packet_table_item packet_item = Packet_table_item.deserialize(input);
                    // store the packet in packet_table
                    packet_table.add_packet(packet_item);
                    Log.d("SERVER_PACKET: ", input);
                    input = in.readLine();
                }
            }
            Log.d("SERVER_PACKET: ", input);

            // now simmetrically client and server invert they're roles



        }catch (IOException exc){
            Log.d("IOEXCEPTION",String.valueOf(exc.getCause()));
        }

    }

}