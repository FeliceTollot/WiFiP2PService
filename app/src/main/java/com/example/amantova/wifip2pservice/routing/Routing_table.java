// routing for DTnets based on partial knowledge

package com.example.amantova.wifip2pservice.routing;

import java.util.*;


public class Routing_table {

  private class table_item{
    public long first_time_seen;
    public long count_times_seen;

    public table_item(long arg1, int arg2){
      first_time_seen = arg1;
      count_times_seen = arg2;
    }
  }

  private Hashtable<String, table_item> table = new Hashtable<String, table_item>();

  public void register_eid(String eid_arg){
    table.put(eid_arg, new table_item(System.currentTimeMillis()/1000,1));
  }

  public List<String> get_eids(){
    Set<String> eids = table.keySet();
    List<String> list_eids = new LinkedList<>();
    list_eids.addAll(eids);
    return list_eids;
  }

  public void add_seen(String eid){
    table_item item = table.get(eid);
    item.count_times_seen = item.count_times_seen + 1;
  }

  public void delete_eid(String eid_arg){
    table.remove(eid_arg);
  }

  public Long get_avg_time(String eid_arg){
    table_item item =  table.get(eid_arg);
    if(item != null){
      return ( (System.currentTimeMillis()/1000) - item.first_time_seen) / (item.count_times_seen);
    }
    return null;
  }

  public Boolean to_send(String eid_arg, long avg_time_arg){
    long actual_avg = get_avg_time(eid_arg);
    if(avg_time_arg < actual_avg){return true;}
    else{return false;}
  }

  public void meet(String eid){
    List<String> registered_eids = get_eids();
    boolean registered = false;
    Iterator<String> it = registered_eids.iterator();
    while(it.hasNext()){
      String item = it.next();
      if(item.equals(eid)){
        registered = true;
      }
    }

    if(registered==false){
      register_eid(eid);
    }else{
      add_seen(eid);
    }

  }

}
