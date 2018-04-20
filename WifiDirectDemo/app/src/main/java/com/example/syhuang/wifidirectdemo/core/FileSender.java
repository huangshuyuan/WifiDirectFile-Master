package com.example.syhuang.wifidirectdemo.core;

import android.content.Context;
import android.util.Log;

import com.example.syhuang.wifidirectdemo.core.entity.FileInfo;
import com.example.syhuang.wifidirectdemo.db.StreamTool;
import com.example.syhuang.wifidirectdemo.db.UploadHelper;
import com.example.syhuang.wifidirectdemo.utils.MLog;
import com.example.syhuang.wifidirectdemo.utils.TimeUtils;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.RandomAccessFile;
import java.net.Socket;


/**
 * Author: syhuang
 * Date:  2018/4/11
 */
public class FileSender extends BaseTransfer implements Runnable {

    private static final String TAG = FileSender.class.getSimpleName();

    Context mContext;
    private UploadHelper upHelper;
    /**
     * 传送文件目标的地址以及端口
     */
    private String       mServerIpAddress;
    private int          mPort;

    /**
     * 传送文件的信息
     */
    private FileInfo mFileInfo;

    /**
     * Socket的输入输出流
     */
    private Socket       mSocket;
    private OutputStream mOutputStream;

    /**
     * 控制线程暂停 恢复
     */
    private final Object LOCK = new Object();
    boolean mIsPaused = false;

    /**
     * 判断此线程是否完毕
     */
    boolean mIsFinished = false;

    /**
     * 设置未执行的线程不执行的标识
     */
    boolean mIsStop = false;

    /**
     * 文件传送的监听
     */
    OnSendListener mOnSendListener;

    public FileSender(Context context, FileInfo mFileInfo, String mServerIpAddress, int mPort) {
        this.mContext = context;
        this.mFileInfo = mFileInfo;
        this.mServerIpAddress = mServerIpAddress;
        this.mPort = mPort;
        upHelper = new UploadHelper(mContext);
    }

    public void setOnSendListener(OnSendListener mOnSendListener) {
        this.mOnSendListener = mOnSendListener;
    }

    @Override
    public void run() {
        if (mIsStop) {
            return; //设置当前的任务不执行， 只能在线程未执行之前有效
        }

        //初始化
        try {
            if (mOnSendListener != null) {
                mOnSendListener.onStart();
            }
            init();
        } catch (Exception e) {
            e.printStackTrace();
            MLog.i(TAG, "FileSender init() -->> occur expection");
            if (mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }

        //解析头部
        try {
            parseHeader();
        } catch (Exception e) {
            e.printStackTrace();
            MLog.i(TAG, "FileSender init() -->> occur expection");
            if (mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }

        //解析主体
        try {
            parseBody();
        } catch (Exception e) {
            e.printStackTrace();
            MLog.i(TAG, "FileSender init() -->> occur expection");
            if (mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }

        //结束
        try {
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            MLog.i(TAG, "FileSender finish() -->> occur expection");
            if (mOnSendListener != null) {
                mOnSendListener.onFailure(e, mFileInfo);
            }
        }


    }

    @Override
    public void init() throws Exception {
        this.mSocket = new Socket(mServerIpAddress, mPort);
        OutputStream os = this.mSocket.getOutputStream();
        mOutputStream = new BufferedOutputStream(os);
    }

    @Override
    public void parseHeader() throws Exception {
        MLog.i(TAG, "parseHeader-->>start");
        //拼接header
        StringBuilder headerSb = new StringBuilder();
        mFileInfo.setId("12345678");
        Log.i("fileId", mFileInfo.getId());
        String jsonStr = FileInfo.toJsonStr(mFileInfo);
        //        jsonStr = TYPE_FILE + SPERATOR + jsonStr;
        headerSb.append(jsonStr);
        int leftLen = BYTE_SIZE_HEADER - jsonStr.getBytes(UTF_8).length;
        //对于英文是一个字母对应一个字节，中文的情况下对应两个字节。剩余字节数不应该是字节数
        for (int i = 0; i < leftLen; i++) {
            headerSb.append(" ");
        }
        byte[] headbytes = headerSb.toString().getBytes(UTF_8);

        //写入header
        mOutputStream.write(headbytes);

        MLog.i(TAG, "FileSender header write-->>" + new String(headbytes, UTF_8));

        MLog.i(TAG, "parseHeader-->>end");
    }

    @Override
    public void parseBody() throws Exception {
        MLog.i(TAG, "parseBody-->>start");

        //写入文件
        long fileSize = mFileInfo.getSize();
        File file = new File(mFileInfo.getFilePath());
        String sourceid = upHelper.getBindId(file);
        //获取服务器反馈信息
        PushbackInputStream inStream = new PushbackInputStream(mSocket.getInputStream());
        String response = StreamTool.readLine(inStream);
        Log.i(TAG, "last length:" + response);
        JSONObject jsonObject = new JSONObject(response);
        String responseSourceid = jsonObject.getString("fileId");
        String position = jsonObject.getLong("fileLength") + "";
        if (sourceid == null) {
            //如果是第一次上传文件，在数据库中不存在该文件所绑定的资源id
            upHelper.save(responseSourceid, file);
        }
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rwd");
        //从指定位置读取文件
        randomAccessFile.seek(Integer.valueOf(position));


        //记录文件开始写入时间
        long startTime = System.currentTimeMillis();

        byte[] bytes = new byte[BYTE_SIZE_DATA];
        long total = Integer.valueOf(position);
        int len = -1;
        long sTime = System.currentTimeMillis();
        long eTime = 0;
        while ((len = randomAccessFile.read(bytes)) != -1) {
            synchronized (LOCK) {
                if (mIsPaused) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                mOutputStream.write(bytes, 0, len);
                total = total + len;
                //累加已经上传的数据长度
                eTime = System.currentTimeMillis();
                if (eTime - sTime > 200) {
                    //大于500ms 才进行一次监听
                    sTime = eTime;
                    if (mOnSendListener != null) {
                        mOnSendListener.onProgress(total, fileSize);
                    }
                }
            }


        }


        if (total == file.length()) {
            upHelper.delete(file);
        }


        //记录文件结束写入时间
        long endTime = System.currentTimeMillis();
        MLog.i(TAG, "FileSender body write-->>" + (TimeUtils.formatTime(endTime - startTime)));
        MLog.i(TAG, "FileSender body write-->>" + total);

        mOutputStream.flush();
        //每一次socket连接就是一个通信，如果当前OutputStream不关闭的话。FileReceiver端会阻塞在那里
        mOutputStream.close();
        MLog.i(TAG, "parseBody-->>end");

        if (mOnSendListener != null) {
            mOnSendListener.onSuccess(mFileInfo);
        }

        mIsFinished = true;
    }

    @Override
    public void finish() {

        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {

            }
        }

        if (mSocket != null && mSocket.isConnected()) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        MLog.i(TAG, "FileSender close socket-->>");
    }

    /**
     * 停止线程下载
     */
    public void pause() {
        synchronized (LOCK) {
            mIsPaused = true;
            LOCK.notifyAll();
        }
    }

    /**
     * 重新开始线程下载
     */
    public void resume() {
        synchronized (LOCK) {
            mIsPaused = false;
            LOCK.notifyAll();
        }
    }

    /**
     * 设置当前的发送任务不执行
     */
    public void stop() {
        mIsStop = true;
    }

    /**
     * 文件是否在传送中？
     *
     * @return
     */
    public boolean isRunning() {
        return !mIsFinished;
    }


    /**
     * 文件传送的监听
     */
    public interface OnSendListener {
        /**
         * 启动
         */
        void onStart();

        /**
         * 接收文件进度
         *
         * @param progress
         * @param total
         */
        void onProgress(long progress, long total);

        /**
         * 成功接收
         *
         * @param fileInfo
         */
        void onSuccess(FileInfo fileInfo);

        /**
         * 失败接收
         *
         * @param t
         * @param fileInfo
         */
        void onFailure(Throwable t, FileInfo fileInfo);
    }

}
