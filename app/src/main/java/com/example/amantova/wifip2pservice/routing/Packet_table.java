// routing for DTnets based on partial knowledge

import java.util.*;

public class Packet_table {

  private Hashtable<String, Packet_table_item> table = new Hashtable<String, Packet_table_item>();

  public void add_packet(Packet_table_item packet_arg){
    table.put(packet_arg.dest_eid, packet_arg);
  }
  public void remove_packet(String eid_arg){
    table.remove(eid_arg);
  }
  public Boolean check_packet(String eid_arg){
    Packet_table_item item = table.get(eid_arg);
    if(item == null){return false;}
    Long deadline = item.timestamp + item.ttl;
    if(System.currentTimeMillis()/1000 > deadline){
      remove_packet(eid_arg);
      return false;
    }
    return true;
  }
  public Packet_table_item get(String eid_arg){
    return table.get(eid_arg);
  }
  public void clean(){
    Set<String> set_eids = table.keySet();
    Iterator<String> it = set_eids.iterator();
    while(it.hasNext()){
      String key = it.next();
      Packet_table_item item = table.get(key);
      Long extintion = item.timestamp + item.ttl;
      if(extintion < System.currentTimeMillis()/1000){
        remove_packet(key);
      }
    }
  }
}
