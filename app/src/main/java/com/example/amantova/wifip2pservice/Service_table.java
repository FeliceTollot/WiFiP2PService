// routing for DTnets based on partial knowledge
package com.example.amantova.wifip2pservice;
import java.util.*;

public class Service_table{

    private List<ServiceInfo> table = new LinkedList<ServiceInfo>();

    public void add(ServiceInfo service){
        table.add(service);
    }
    public List<ServiceInfo> get_all(){
        return table;
    }

    public String serialize(){
        String result = "";
        for (ServiceInfo service : table) {
            result = result + service.name.toString() + "," + service.type.toString() + "," + service.port.toString() + ";";
        }
        return result;
    }
}
