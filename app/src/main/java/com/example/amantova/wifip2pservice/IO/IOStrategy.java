package com.example.amantova.wifip2pservice.IO;

import java.net.Socket;

public interface IOStrategy {
    void run(Socket socket, Object sync);
}
