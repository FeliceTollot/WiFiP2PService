package com.example.amantova.wifip2pservice.IO;

import android.app.Activity;
import android.content.Context;

import java.io.InputStream;
import java.io.OutputStream;

public interface IOStrategy {
    void run(final InputStream in, final OutputStream out, Activity main_act);
}
