package com.example.amantova.wifip2pservice;

import java.io.InputStream;
import java.io.OutputStream;

public interface IOStrategy {
    void run(InputStream in, OutputStream out);
}
