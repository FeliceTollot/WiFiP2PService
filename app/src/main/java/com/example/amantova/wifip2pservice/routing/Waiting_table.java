// routing for DTnets based on partial knowledge

import java.util.*;

public class Waiting_table{

  private Hashtable<String, Long> table = new Hashtable<String, Long>();

  public Boolean check_waiting(String eid_arg){
    Long item = table.get(eid_arg);
    if(item == null) {return false;}
    if(item > System.currentTimeMillis()/1000){return true;}
    table.remove(eid_arg);
    return false;
  }

  public void clean_table(){
    Set<String> set_eids = table.keySet();
    Iterator<String> it = set_eids.iterator();
    while(it.hasNext()){
      String eid = it.next();
      Long extintion = table.get(eid);
      if(extintion < System.currentTimeMillis()/1000){
        remove_eid(eid);
      }
    }
  }

  public int size(){
    return table.size();
  }

  public void remove_eid(String eid_arg){
    table.remove(eid_arg);
  }

  public void register_eid(String eid_arg, long expiration_time){
    table.put(eid_arg, expiration_time);
  }

}
