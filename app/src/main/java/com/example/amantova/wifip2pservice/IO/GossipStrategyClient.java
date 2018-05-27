
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GossipStrategyClient implements IOStrategy {

    Routing_table routing_table;
    Waiting_table waiting_table;
    Packet_table  packet_table;

    public GossipStrategyClient(Routing_table rout, Waiting_table wait, Packet_table packet){
        routing_table = rout;
        waiting_table = wait;
        packet_table = packet;
    }

    private class Exchange_item{
        public String eid;
        public long avg;

        public Exchange_item(String eid, long avg){
            eid = eid;
            avg = avg;
        }
    }

    public void run(Socket socket){
        try {

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(socket.getInputStream()));

            // get eids
            List<String> eids = packet_table.get_eids_list();
            List<Exchange_item> exchange_items = new LinkedList<Exchange_item>();

            out.println("start_exchange_avgs");

            Iterator<String> itera =  eids.iterator();
            while(itera.hasNext()){
                String eid = itera.next();
                // send eid
                out.println(eid);
                // get avg
                String avg = in.readLine();
                // store avg-eid
                exchange_items.add(new Exchange_item( eid, Long.parseLong(avg)));
            }
            // end sending end receiveing eids-avgs
            out.println("end_exchange_avgs");

            List<String> packets_list = new LinkedList<>();

            // check which packets must be sent to the server and store them in a List<String>
            Iterator<Exchange_item> it = exchange_items.iterator();
            while(it.hasNext()){
                Exchange_item item = it.next();
                long other_avg_time = item.avg;
                long my_time = routing_table.get_avg_time(item.eid);
                if(other_avg_time <= my_time){
                    // get packet
                    Packet_table_item packet_item = packet_table.get(item.eid);
                    String packet_string = Packet_table_item.serialize(packet_item);
                    // put in the list
                    packets_list.add(packet_string);
                    // delete from packet_table
                    packet_table.remove_packet(item.eid);
                }
            }

            // start_receiving_packets
            out.println("start_receiving_packets");

            // send packets
            Iterator<String> itera_packet =  packets_list.iterator();
            while(itera_packet.hasNext()) {
                String packet = itera.next();
                // send packet
                out.println(packet);
            }
            // end_receiving_packets
            out.println("end_receiving_packets");

            // now simmetrically client and server invert they're roles


        }catch (IOException exc){
            Log.d("IOEXCEPTION",String.valueOf(exc.getCause()));
        }

    }

}