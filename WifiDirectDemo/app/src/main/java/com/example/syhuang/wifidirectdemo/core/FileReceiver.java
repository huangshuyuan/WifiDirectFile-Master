package com.example.syhuang.wifidirectdemo.core;

import com.example.syhuang.wifidirectdemo.core.entity.FileInfo;
import com.example.syhuang.wifidirectdemo.utils.AppConfig;
import com.example.syhuang.wifidirectdemo.utils.FileUtils;
import com.example.syhuang.wifidirectdemo.utils.MLog;
import com.example.syhuang.wifidirectdemo.utils.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;


/**
 * @author syhuang
 * @date 2018/4/13
 */
public class FileReceiver extends BaseTransfer implements Runnable {

    private static final String TAG = FileReceiver.class.getSimpleName();

    /**
     * Socket的输入输出流
     */
    private Socket      mSocket;
    private InputStream mInputStream;

    /**
     * 传送文件的信息
     */
    private FileInfo mFileInfo;

    /**
     * 控制线程暂停 恢复
     */
    private final Object LOCK = new Object();
    boolean mIsPaused = false;

    /**
     * 文件接收的监听
     */
    OnReceiveListener mOnReceiveListener;


    public FileReceiver(Socket mSocket) {
        this.mSocket = mSocket;
    }

    public void setOnReceiveListener(OnReceiveListener mOnReceiveListener) {
        this.mOnReceiveListener = mOnReceiveListener;
    }

    @Override
    public void run() {
        //初始化
        try {
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onStart();
            }
            init();
        } catch (Exception e) {
            e.printStackTrace();
            MLog.i(TAG, "FileReceiver init() -->> occur expection");
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onFailure(e, mFileInfo);
            }
        }

        //解析头部
        try {
            parseHeader();
        } catch (Exception e) {
            e.printStackTrace();
            MLog.i(TAG, "FileReceiver parseHeader() -->> occur expection");
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onFailure(e, mFileInfo);
            }
        }


        //解析主体
        try {
            parseBody();
        } catch (Exception e) {
            e.printStackTrace();
            MLog.i(TAG, "FileReceiver parseBody() -->> occur expection");
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onFailure(e, mFileInfo);
            }
        }

        //结束
        try {
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            MLog.i(TAG, "FileReceiver finish() -->> occur expection");
            if (mOnReceiveListener != null) {
                mOnReceiveListener.onFailure(e, mFileInfo);
            }
        }


    }

    @Override
    public void init() throws Exception {
        if (this.mSocket != null) {
            this.mInputStream = mSocket.getInputStream();
        }
    }

    @Override
    public void parseHeader() throws IOException {
        MLog.i(TAG, "parseHeader-->>start");

        //Are you sure can read the 1024 byte accurately?
        //读取header部分
        byte[] headerBytes = new byte[BYTE_SIZE_HEADER];
        int headTotal = 0;
        int readByte = -1;
        //开始读取header
        while ((readByte = mInputStream.read()) != -1) {
            headerBytes[headTotal] = (byte) readByte;

            headTotal++;
            if (headTotal == headerBytes.length) {
                break;
            }
        }
        MLog.i(TAG, "FileReceiver receive header size-->>>" + headTotal);
        MLog.i(TAG, "FileReceiver receive header-->>>" + new String(headerBytes, UTF_8));


        //解析header
        String jsonStr = new String(headerBytes, UTF_8);
        mFileInfo = FileInfo.toObject(jsonStr);
        if (mOnReceiveListener != null) {
            mOnReceiveListener.onGetFileInfo(mFileInfo);
        }
        MLog.i(TAG, "parseHeader-->>end");
    }

    @Override
    public void parseBody() throws Exception {
        MLog.i(TAG, "parseBody-->>start");

        //写入文件
        long fileSize = mFileInfo.getSize();


        //        OutputStream bos = new FileOutputStream(FileUtils.getLocalFile(mFileInfo.getName()));

        //记录文件开始写入时间
        long startTime = System.currentTimeMillis();


        File file = FileUtils.getLocalFile(mFileInfo.getName());
        //获取文件缓存
        long position = AppConfig.getInstance().getLong(mFileInfo.getId(), 0);
        OutputStream outStream = mSocket.getOutputStream();

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("fileId", mFileInfo.getId());
            jsonObject.put("fileLength", position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String response = jsonObject.toString() + "\r\n";
        //服务器收到客户端的请求信息后，给客户端返回响应信息：sourceid=1274773833264;position=0
        //sourceid由服务器端生成，唯一标识上传的文件，position指示客户端从文件的什么位置开始上传
        outStream.write(response.getBytes());

        RandomAccessFile bos = new RandomAccessFile(file, "rwd");
        if (position == 0) {
            bos.setLength(Integer.valueOf(mFileInfo.getSize() + ""));//设置文件长度
        }
        bos.seek(position);//指定从文件的特定位置开始写入数据


        byte[] bytes = new byte[BYTE_SIZE_DATA];
        long total = position;
        int len = -1;

        long sTime = System.currentTimeMillis();
        long eTime = 0;
        while ((len = mInputStream.read(bytes)) != -1) {
            synchronized (LOCK) {
                if (mIsPaused) {
                    try {
                        LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                bos.write(bytes, 0, len);
                total = total + len;
                //记录文件日志长度
                AppConfig.getInstance().putLong(mFileInfo.getId(), total);

                eTime = System.currentTimeMillis();
                if (eTime - sTime > 200) {
                    //大于500ms 才进行一次监听
                    sTime = eTime;
                    if (mOnReceiveListener != null) {
                        mOnReceiveListener.onProgress(total, fileSize);
                    }
                }
            }

        }

        if (total == bos.length()) {
            //            delete(id);
            AppConfig.getInstance().putLong(mFileInfo.getId(), 0);
        }

        long endTime = System.currentTimeMillis();
        //记录文件结束写入时间
        MLog.i(TAG, "FileReceiver body receive-->>" + (TimeUtils.formatTime(endTime - startTime)));
        MLog.i(TAG, "FileReceiver body receive-->>" + total);

        MLog.i(TAG, "parseBody-->>end");

        if (mOnReceiveListener != null) {
            mOnReceiveListener.onSuccess(mFileInfo);
        }
    }


    @Override
    public void finish() {
        //TODO 实现一些资源的关闭

        if (mInputStream != null) {
            try {
                mInputStream.close();
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

        MLog.i(TAG, "FileReceiver close socket-->>");
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
     * 文件接收的监听
     */
    public interface OnReceiveListener {
        /**
         * 启动
         */
        void onStart();

        /**
         * 获取文件
         *
         * @param fileInfo
         */

        void onGetFileInfo(FileInfo fileInfo);


        /**
         * 获取进度
         *
         * @param progress
         * @param total
         */

        void onProgress(long progress, long total);

        /**
         * 成功
         *
         * @param fileInfo
         */

        void onSuccess(FileInfo fileInfo);

        /**
         * 失败
         *
         * @param t
         * @param fileInfo
         */
        void onFailure(Throwable t, FileInfo fileInfo);
    }

}
