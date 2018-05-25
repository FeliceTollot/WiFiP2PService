// routing for DTnets based on partial knowledge

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

  public void add_seen(String eid){
    table_item item =table.get(eid);
    item.count_times_seen = item.count_times_seen + 1;
  }

  public void delete_eid(String eid_arg){
    table.remove(eid_arg);
  }

  public long get_avg_time(String eid_arg){
    table_item item =  table.get(eid_arg);
    return ( (System.currentTimeMillis()/1000) - item.first_time_seen) / (item.count_times_seen);
  }

  public Boolean to_send(String eid_arg, long avg_time_arg){
    long actual_avg = get_avg_time(eid_arg);
    if(avg_time_arg < actual_avg){return true;}
    else{return false;}
  }

}
