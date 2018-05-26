// routing for DTnets based on partial knowledge

package com.example.amantova.wifip2pservice.routing;

import java.util.*;

public class Waiting_table_entry{
  public String eid_dest;
  public String service_name;
  public long   extintion;

  public Waiting_table_entry(String eid, String service, long time){
    eid_dest = eid;
    service_name = service;
    extintion = time;
  }
}
