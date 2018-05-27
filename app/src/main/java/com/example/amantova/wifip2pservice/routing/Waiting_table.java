// routing for DTnets based on partial knowledge

package com.example.amantova.wifip2pservice.routing;

import java.util.*;

public class Waiting_table{

  private Hashtable<String, Waiting_table_entry> table = new Hashtable<String, Waiting_table_entry>();

  public Boolean check_waiting(String eid_arg){
    Waiting_table_entry item = table.get(eid_arg);
    if(item == null) {return false;}
    if(item.extintion > System.currentTimeMillis()/1000){return true;}
    table.remove(eid_arg);
    return false;
  }

  public void clean_table(){
    Set<String> set_eids = table.keySet();
    Iterator<String> it = set_eids.iterator();
    while(it.hasNext()){
      Waiting_table_entry entry = table.get( it.next() );
      Long extintion = entry.extintion;
      if(extintion < System.currentTimeMillis()/1000){
        remove_eid(entry.eid_dest);
      }
    }
  }

  public List<String> get_eids_list(){
    Set<String> set_eids = table.keySet();
    List<String> list = new LinkedList<String>();
    list.addAll(set_eids);
    return list;
  }

  public Waiting_table_entry get_eid(String eid){
    return table.get(eid);
  }

  public int size(){
    return table.size();
  }

  public void remove_eid(String eid_arg){
    table.remove(eid_arg);
  }

  public void register_eid(String eid_dest, String service, long expiration_time){
    table.put(eid_dest, new Waiting_table_entry(eid_dest,service,expiration_time));
  }

  public Boolean check_service_need(String service_name){
    Set<String> set_eids = table.keySet();
    Iterator<String> it = set_eids.iterator();
    while(it.hasNext()){
      Waiting_table_entry entry = table.get( it.next() );
      if( entry.service_name.equals(service_name) && entry.extintion > System.currentTimeMillis()/1000){
        return true;
      }
    }
    return false;
  }

}
