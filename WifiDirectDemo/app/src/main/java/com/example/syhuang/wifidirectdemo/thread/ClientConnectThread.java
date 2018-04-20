package com.example.syhuang.wifidirectdemo.thread;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.syhuang.wifidirectdemo.utils.Constant;
import com.example.syhuang.wifidirectdemo.utils.Utils;
import com.example.syhuang.wifidirectdemo.utils.WifiDirectMgr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */
public class ClientConnectThread extends Thread {

    private final Socket       socket;
    private       Handler      handler;
    private       InputStream  inputStream;
    private       OutputStream outputStream;

    public ClientConnectThread(Socket socket, Handler handler) {
        setName("ClientConnectThread");
        Log.i("ClientConnectThread", "ClientConnectThread");
        this.socket = socket;
        this.handler = handler;
    }

    @Override
    public void run() {
        if (socket == null) {
            return;
        }
        handler.sendEmptyMessage(WifiDirectMgr.DEVICE_CONNECTED);
        try {
            //获取数据流
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            getMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getMessage() {
        Log.i("ClientConnectThread", "发送数据:" + (inputStream != null));
        if (inputStream != null) {
            try {
                byte[] buffer = new byte[1024];
                int bytes;
                while (true) {
                    DataInputStream dataInputStream = null;
                    dataInputStream = new DataInputStream(inputStream);
                    int type = dataInputStream.readInt();
                    switch (type) {
                        case Constant.TYPE_STRING: {
                            //读取数据
                            bytes = inputStream.read(buffer);
                            if (bytes > 0) {
                                String str = new String(buffer, 0, bytes);
                                Message message = Message.obtain();
                                message.what = WifiDirectMgr.GET_MSG;
                                Bundle bundle = new Bundle();
                                bundle.putString("MSG", str);
                                message.setData(bundle);
                                handler.sendMessage(message);

                                Log.i("ClientConnectThread", "读取到数据:" + str);
                            }
                        }
                        break;
                        case Constant.TYPE_FILE: {
                            FileOutputStream fileOutputStream = null;
                            // 文件名和长度
                            String fileName = dataInputStream.readUTF();
                            long fileLength = dataInputStream.readLong();
                            Log.i("ClientConnectThread", "======== 文件接收 [File Name：" + fileName + "] " +
                                    "[Size：" + Utils.getFormatFileSize(fileLength) + "] ========");
                            File directory = new File(Environment.getExternalStorageDirectory() + "/");
                            if (!directory.exists()) {
                                directory.mkdir();
                            } else {
                            }
                            File file = new File(directory.getAbsolutePath() + File.separatorChar + fileName);
                            fileOutputStream = new FileOutputStream(file);
                            int length = 0;
                            int progress = 0;
                            while ((length = dataInputStream.read(buffer, 0, buffer.length)) != -1) {
                                Log.i("ClientConnectThread", length + "...");
                                fileOutputStream.write(buffer, 0, length);
                                fileOutputStream.flush();
                                progress += length;
                                Log.i("ClientConnectThread", "| " + (100 * progress / file.length()) + "% |");
                            }
                            Log.i("ClientConnectThread", "文件传输完成");

                            Message message = Message.obtain();
                            message.what = WifiDirectMgr.GET_MSG;
                            Bundle bundle = new Bundle();
                            bundle.putString("MSG", new String("接收到文件：" + file.getAbsolutePath()));
                            message.setData(bundle);
                            handler.sendMessage(message);

                        }
                        break;
                    }


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

//    public void sendData(File file) {
//        Log.i("ClientConnectThread", "发送数据:" + (outputStream == null));
//        if (outputStream != null) {
//            try {
//                FileInputStream fileInputStream = null;
//                DataOutputStream dataOutputStream = null;
//                if (file.exists()) {
//                    fileInputStream = new FileInputStream(file);
//                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
//
//                    // 文件名和长度
//                    dataOutputStream.writeInt(Constant.TYPE_FILE);
//                    dataOutputStream.writeUTF(file.getName());
//                    dataOutputStream.flush();
//                    dataOutputStream.writeLong(file.length());
//                    dataOutputStream.flush();
//
//                    // 开始传输文件
//                    Log.i("ClientConnectThread", "======== 开始传输文件 ========" + file.getName());
//                    Log.i("ClientConnectThread", "fileName:" + file.getName() + "");
//                    Log.i("ClientConnectThread", "fileLength:" + file.length() + "");
//                    byte[] bytes = new byte[1024];
//                    int length = 0;
//                    long progress = 0;
//                    while ((length = fileInputStream.read(bytes, 0, bytes.length)) != -1) {
//                        dataOutputStream.write(bytes, 0, length);
//                        dataOutputStream.flush();
//                        progress += length;
//                        Log.i("ClientConnectThread", "| " + (100 * progress / file.length()) + "% |");
//                    }
//                    Log.i("ClientConnectThread", "======== 文件传输成功 ========");
//                    Message message = Message.obtain();
//                    message.what = WifiDirectMgr.SEND_MSG_SUCCSEE;
//                    Bundle bundle = new Bundle();
//                    bundle.putString("MSG", new String("文件传输成功"));
//                    message.setData(bundle);
//                    handler.sendMessage(message);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                Message message = Message.obtain();
//                message.what = WifiDirectMgr.SEND_MSG_ERROR;
//                Bundle bundle = new Bundle();
//                bundle.putString("MSG", new String("发送文件失败"));
//                message.setData(bundle);
//                handler.sendMessage(message);
//            }
//        }
//    }


    /**
     * 发送数据
     */
    public void sendData(String msg) {
        Log.i("ServerConnectThread", "发送数据:" + (outputStream == null));
        if (outputStream != null) {
            DataOutputStream dataOutputStream = null;
            try {
                dataOutputStream = new DataOutputStream(outputStream);
                // 发送类型
                dataOutputStream.writeInt(Constant.TYPE_STRING);
                dataOutputStream.write(msg.getBytes());
                Log.i("ServerConnectThread", "发送消息：" + msg);
                Message message = Message.obtain();
                message.what = WifiDirectMgr.SEND_MSG_SUCCSEE;
                Bundle bundle = new Bundle();
                bundle.putString("MSG", new String(msg));
                message.setData(bundle);
                handler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = WifiDirectMgr.SEND_MSG_ERROR;
                Bundle bundle = new Bundle();
                bundle.putString("MSG", new String(msg));
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }
    }


    public void close() {
        this.close();
        try {
            if (socket != null) {
                socket.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
