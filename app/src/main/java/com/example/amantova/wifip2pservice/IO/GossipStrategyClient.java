
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

        public Exchange_item(String eid_arg, long avg_arg){
            eid = eid_arg;
            avg = avg_arg;
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

            // exchange services TODO
            // request services provided
            out.println("ASK_FOR_SERVICES");
            String recv = in.readLine();
            while(recv.equals("END_OF_SERVICES") == false){
                boolean needed = waiting_table.check_service_need(recv);
                if(needed){
                    out.println("YES");
                    String service_info = in.readLine();
                    // deal with waiting table
                    // store in packet table
                }
                recv = in.readLine();
            }

            /*
            public String dest_eid;
            public long timestamp;
            public long ttl;
            public String service;
            public byte[] payload;
             */

            // if some neede request them
            // store in packege table

            out.println("start_exchange_avgs");

            Iterator<String> itera =  eids.iterator();
            while(itera.hasNext()){
                String eid = itera.next();
                // send eid
                out.println(eid);
                // get avg
                String avg = in.readLine();
                Log.d("CLIENT_RECEIVED: ", avg);
                Log.d("CLIENT_EID_SENT: ", eid);
                // store avg-eid
                if(avg.equals("NULL")){
                    Log.d("CLIENT_RECEIVED: ", "ENTERED NULL RECEIVED IF");
                    exchange_items.add(new Exchange_item( eid, -1l));
                }else{
                    exchange_items.add(new Exchange_item( eid, Long.parseLong(avg)));
                }
            }
            // end sending end receiveing eids-avgs
            out.println("end_exchange_avgs");

            List<String> packets_list = new LinkedList<>();

            // check which packets must be sent to the server and store them in a List<String>
            Iterator<Exchange_item> it = exchange_items.iterator();
            while(it.hasNext()){
                Exchange_item item = it.next();
                long other_avg_time = item.avg;
                Log.d("CLIENT_EID: ", String.valueOf(item.avg));
                Log.d("CLIENT_EID: ", String.valueOf(item.eid==null));
                long my_time = routing_table.get_avg_time(item.eid);

                if(other_avg_time <= my_time ){
                    // get packet
                    Packet_table_item packet_item = packet_table.get(item.eid);
                    String packet_string = Packet_table_item.serialize(packet_item);
                    // put in the list
                    Log.d("PACKET_ON_CLIENT ", packet_string);
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
                String packet = itera_packet.next();
                // send packet
                out.println(packet);
            }
            // end_receiving_packets
            out.println("end_receiving_packets");

            out.close();
            in.close();
            // now simmetrically client and server invert they're roles


        }catch (IOException exc){
            Log.d("IOEXCEPTION",String.valueOf(exc.getCause()));
        }

    }

}