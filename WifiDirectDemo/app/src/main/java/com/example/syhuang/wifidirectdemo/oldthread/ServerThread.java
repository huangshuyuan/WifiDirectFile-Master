
package com.example.syhuang.wifidirectdemo.oldthread;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */
public class ServerThread extends Thread {

    private boolean running = true;
    private Handler handler;
    private Context ctx;
    private int     port;

    public ServerThread(Handler handler, Context ctx, int port) {
        this.handler = handler;
        this.ctx = ctx;
        this.port = port;
    }

    OutputStream mOutputStream;

    @Override
    public void run() {
        super.run();
        try {
            while (running) {
                ServerSocket serverSocket = new ServerSocket(port);
                Socket client = serverSocket.accept();
                getMessage(client.getInputStream());
                mOutputStream = client.getOutputStream();

                //                serverSocket.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String str) {
        try {
            if (mOutputStream != null) {
                mOutputStream.write(str.getBytes(), 0, str.getBytes().length);
                Log.i("ServerThread:send", str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        this.running = false;
    }

    private void getMessage(InputStream in) {
        try {
            int BUFFER_SIZE = 1024;

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] data = new byte[BUFFER_SIZE];
            int count;
            while ((count = in.read(data, 0, BUFFER_SIZE)) != -1)
                outStream.write(data, 0, count);

            String str = new String(outStream.toByteArray(), "UTF-8");

            Log.i("ServerThread:", str);
            Message msg = new Message();
            if (str.startsWith("ip:")) {
                str = str.replace("ip:", "");
                //                [0] 是ip [1] device name
                String[] deviceInfo = str.split("name:");

                msg.what = 3;
                msg.obj = deviceInfo;

            } else {
                msg.what = 1;
                msg.obj = str;
            }
            handler.sendMessage(msg);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null) {
//                    in.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

}
