package com.example.amantova.wifip2pservice;

import java.util.HashMap;

public class ServiceInfo {
    public String                   name;
    public String                   type;
    public HashMap<String, String>  record = new HashMap<>();

    public ServiceInfo(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
