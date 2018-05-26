
package com.example.amantova.wifip2pservice.IO;

import com.example.amantova.wifip2pservice.IO.IOStrategy;
import com.example.amantova.wifip2pservice.routing.Packet_table;
import com.example.amantova.wifip2pservice.routing.Routing_table;
import com.example.amantova.wifip2pservice.routing.Waiting_table;

import java.net.Socket;

public class GossipStrategyClient implements IOStrategy {

    Routing_table routing_table;
    Waiting_table waiting_table;
    Packet_table  packet_table;

    public GossipStrategyClient(Routing_table rout, Waiting_table wait, Packet_table packet){
        routing_table = rout;
        waiting_table = wait;
        packet_table = packet;
    }

    public void run(Socket socket){

        

    }

}