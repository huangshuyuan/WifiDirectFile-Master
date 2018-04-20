package com.example.syhuang.wifidirectdemo.oldthread;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.syhuang.wifidirectdemo.utils.Constant;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */
public class ClientSendThread extends Thread {

    private Handler handler;
    private Socket  socket;
    private String  host;
    private String  message;
    OutputStream mOutputStream;

    public ClientSendThread(Handler handler, String host, String message) {
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
            socket.connect((new InetSocketAddress(host, Constant.PORT)), 5000);
            getMessage(socket.getInputStream());
            mOutputStream = socket.getOutputStream();
            mOutputStream.write(message.getBytes());
            //            os.close();
            Message msg = new Message();
            msg.what = 2;
            handler.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    public void sendMessage(String str) {
        try {
            if (mOutputStream != null) {
                mOutputStream.write(str.getBytes());
                Log.i("ServerThread:send", str);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
