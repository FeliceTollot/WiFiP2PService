// routing for DTnets based on partial knowledge

import java.util.*;

public class Packet_table_item{
  public String dest_eid;
  public long timestamp;
  public long ttl;
  public String sql_id;

  public Packet_table_item(String arg1, long arg2, long arg3, String arg4){
    dest_eid = arg1;
    timestamp = arg2;
    ttl = arg3;
    sql_id = arg4;
  }
}
