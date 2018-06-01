package com.example.amantova.wifip2pservice;

import java.util.concurrent.Callable;

public class Service{
    public String   service_name;
    public Computable service_handler;

    public Service(String name, Computable handler){
        service_name = name;
        service_handler = handler;
    }
}