package com.example.syhuang.wifidirectdemo.oldthread;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.syhuang.wifidirectdemo.utils.Constant;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */
public class ServerSendThread extends Thread {

    private Handler handler;
    private Socket  socket;
    private String  host;
    private String  message;

    public ServerSendThread(Handler handler, String host, String message) {
        this.handler = handler;
        this.host = host;
        socket = new Socket();
        this.message = message;
    }


    @Override
    public void run() {
        super.run();
        try {
            Log.i("ClentThread:", host);
            socket.bind(null);
            socket.connect((new InetSocketAddress(host, Constant.PORT)), 500);
            OutputStream os = socket.getOutputStream();
            os.write(message.getBytes());
            //            os.close();
            Message msg = new Message();
            msg.what = 2;
            handler.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
