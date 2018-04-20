package com.example.syhuang.wifidirectdemo.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */

public class StringTransferService extends IntentService {
    String TAG = "StringTransferService";

    private static final int    SOCKET_TIMEOUT             = 3600;
    public static        String ACTION_SEND_STRING         = "android.intent.action.SEND_STRING";
    public static        String EXTRAS_STRING              = "extras_string";
    public static        String EXTRAS_GROUP_OWNER_ADDRESS = "extras_group_owner_address";
    public static        String EXTRAS_GROUP_OWNER_PORT    = "extras_group_owner_port";


    public StringTransferService(String name) {
        super(name);
    }

    public StringTransferService() {
        super("StringTransferService");

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_STRING)) {
            String str = intent.getExtras().getString(EXTRAS_STRING);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                Log.i(TAG, "Opening client socket - ");
                if (host != null) {
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                    Log.i(TAG, "Client socket - " + socket.isConnected());
                    OutputStream stream = socket.getOutputStream();
                    stream.write(str.getBytes());
                    Log.i(TAG, "Client: Data written");
                }

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                            Log.i(TAG, "Close " + host);
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}