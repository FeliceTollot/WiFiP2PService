// routing for DTnets based on partial knowledge

package com.example.amantova.wifip2pservice.routing;

import java.util.*;


public class Routing_table {

  private class table_item{
    public long first_time_seen;
    public long count_times_seen;
    public long last_seen;

    public table_item(long arg1, int arg2){
      first_time_seen = arg1;
      count_times_seen = arg2;
    }
  }

  private String my_eid;
  private long check_period;

  public Routing_table(String eid, long check_period){
    my_eid = eid;
    this.check_period = check_period;
  }

  public String get_my_eid(){
    return my_eid;
  }

  private Hashtable<String, table_item> table = new Hashtable<String, table_item>();

  public void register_eid(String eid_arg){
    table.put(eid_arg, new table_item(System.currentTimeMillis()/1000,1));
  }

  public void clean(){
    List<String> keys = this.get_eids();
    Iterator<String> it = keys.iterator();
    while(it.hasNext()){
      String item = it.next();
      table_item entry = table.get(item);
      long now = System.currentTimeMillis()/1000;
      if(now - entry.last_seen > check_period){
        table.remove(item);
      }
    }
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
    item.last_seen = System.currentTimeMillis()/1000;
  }

  public void delete_eid(String eid_arg){
    table.remove(eid_arg);
  }

  public Long get_avg_time(String eid_arg){
    if(eid_arg.equals(my_eid)){
      return 0l;
    }
    table_item item =  table.get(eid_arg);
    if(item != null){
      return ( (System.currentTimeMillis()/1000) - item.first_time_seen) / (item.count_times_seen);
    }
    return Long.MAX_VALUE;
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
      // check time
      long now = System.currentTimeMillis()/1000;
      long last_seen = table.get(eid).last_seen;
      if(now - last_seen > 1000*60*2){
        add_seen(eid);
      }
    }
  }

  public long last_time_seen(String dest_eid){
    table_item item = table.get(dest_eid);
    if(item == null){
      return 0;
    }
    return item.last_seen;
  }

}
