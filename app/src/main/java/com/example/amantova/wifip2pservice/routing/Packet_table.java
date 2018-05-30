// routing for DTnets based on partial knowledge

package com.example.amantova.wifip2pservice.routing;

import java.util.*;

public class Packet_table {

  private Hashtable<String, List<Packet_table_item>> table = new Hashtable<String, List<Packet_table_item>>();

  public void add_packet(Packet_table_item packet_arg) {
    List<Packet_table_item> list = table.get(packet_arg.dest_eid);
    if (list == null) {
      list = new LinkedList<Packet_table_item>();
      list.add(packet_arg);
    } else {
      list.add(packet_arg);
    }
  }
  public void remove_packet_list(String eid_arg){
    table.remove(eid_arg);
  }

  public void check_packet_list(String eid_arg){
    List<Packet_table_item> item_list = table.get(eid_arg);
    if(item_list != null){
      long now = System.currentTimeMillis()/1000;
      Iterator<Packet_table_item> it = item_list.iterator();
      while(it.hasNext()){
        Packet_table_item item = it.next();
        long deadline = item.timestamp + item.ttl;
        if(now > deadline){
          it.remove();
        }
      }
    }
  }
  public List<Packet_table_item> get_packet_list(String eid_arg){
    return table.get(eid_arg);
  }
  public List<Packet_table_item> get_list_for_service(String eid_arg, String service){
    List<Packet_table_item> list_items = get_packet_list(eid_arg);
    List<Packet_table_item> result_items = new LinkedList<>();
    for(Packet_table_item item : list_items){
      if(item.service.equals(service)){
        result_items.add(item);
      }
    }
    return result_items;
  }

  public List<String> get_eids_list(){
    Set<String> eids = table.keySet();
    List<String> list_eids = new LinkedList<>();
    list_eids.addAll(eids);
    return list_eids;
   }

  public void clean(){
    List<String> eids = get_eids_list();
    for(String key : eids){
      check_packet_list(key);
    }
  }

   public int number_of_packets_for(String eid_dest){
      List<Packet_table_item> packets_list = get_packet_list(eid_dest);
      if(packets_list == null){
        return 0;
      }
      return packets_list.size();
   }

   public boolean is_empty(){
    return table.size()==0;
   }

}
