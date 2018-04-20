package com.example.syhuang.wifidirectdemo.service;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.syhuang.wifidirectdemo.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */

public class FileTransferService extends IntentService {
    String TAG = "FileTransferService";

    private static final int    SOCKET_TIMEOUT             = 3600;
    public static        String ACTION_SEND_FILE           = "android.intent.action.SEND_FILE";
    public static        String EXTRAS_FILE_PATH           = "extras_file_path";
    public static        String EXTRAS_GROUP_OWNER_ADDRESS = "extras_group_owner_address";
    public static        String EXTRAS_GROUP_OWNER_PORT    = "extras_group_owner_port";


    public FileTransferService(String name) {
        super(name);
    }

    public FileTransferService() {
        super("FileTransferService");

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
            Socket socket = new Socket();
            int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

            try {
                if (host != null) {
                    Log.i(TAG, "Opening client socket - ");
                    socket.bind(null);
                    socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

                    Log.i(TAG, "Client socket - " + socket.isConnected());
                    OutputStream stream = socket.getOutputStream();
                    ContentResolver cr = context.getContentResolver();
                    InputStream is = null;
                    try {
                        is = cr.openInputStream(Uri.parse(fileUri));
                    } catch (Exception e) {
                        Log.i(TAG, e.toString());
                    }
                    Utils.copyFile(is, stream);
                }
                Log.i(TAG, "Client: Data written");

            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
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