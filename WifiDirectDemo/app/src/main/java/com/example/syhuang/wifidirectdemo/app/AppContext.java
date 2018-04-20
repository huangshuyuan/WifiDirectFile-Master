package com.example.syhuang.wifidirectdemo.app;

import android.app.Application;

import com.example.syhuang.wifidirectdemo.core.entity.FileInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author syhuang
 * @date 2018/4/13
 */
public class AppContext extends Application {

    //
    ////    复制
    //    private static String               connString = "HostName={youriothubname}.azure-devices.net;DeviceId=myDeviceID;SharedAccessKey={yourdevicekey}";
    //    private static IotHubClientProtocol protocol   = IotHubClientProtocol.MQTT;
    //    private static String               deviceId   = "myDeviceId";

    /**
     * 全局应用的上下文
     */
    public static AppContext mAppContext;
    /**
     * 文件发送方
     */
    Map<String, FileInfo> mFileInfoMap = new HashMap<String, FileInfo>();
    //采用HashMap结构，文件地址--->>>FileInfo 映射结构，重复加入FileInfo

    Map<String, FileInfo> mReceiverFileInfoMap = new HashMap<String, FileInfo>();

    /**
     * 主要的线程池
     */
    public static Executor MAIN_EXECUTOR = Executors.newFixedThreadPool(5);


    /**
     * 文件发送单线程
     */
    public static Executor FILE_SENDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = this;


    }

    public static AppContext getAppContext() {
        return mAppContext;
    }


    /**
     * 获取即将发送文件列表的总长度
     *
     * @return
     */
    public long getAllSendFileInfoSize() {
        long total = 0;
        for (FileInfo fileInfo : mFileInfoMap.values()) {
            if (fileInfo != null) {
                total = total + fileInfo.getSize();
            }
        }
        return total;
    }

    public Map<String, FileInfo> getFileInfoMap() {
        return mFileInfoMap;
    }

    public void setFileInfoMap(Map<String, FileInfo> fileInfoMap) {
        mFileInfoMap = fileInfoMap;
    }
    //发送方

    /**
     * 添加一个FileInfo
     *
     * @param fileInfo
     */
    public void addFileInfo(FileInfo fileInfo) {
        //        if(!mFileInfoSet.contains(fileInfo)){
        //            mFileInfoSet.add(fileInfo);
        //        }

        if (!mFileInfoMap.containsKey(fileInfo.getFilePath())) {
            mFileInfoMap.put(fileInfo.getFilePath(), fileInfo);
        }
    }

    /**
     * 更新FileInfo
     *
     * @param fileInfo
     */
    public void updateFileInfo(FileInfo fileInfo) {
        mFileInfoMap.put(fileInfo.getFilePath(), fileInfo);
    }

    /**
     * 删除一个FileInfo
     *
     * @param fileInfo
     */
    public void delFileInfo(FileInfo fileInfo) {
        //        if(mFileInfoSet.contains(fileInfo)){
        //            mFileInfoSet.remove(fileInfo);
        //        }
        if (mFileInfoMap.containsKey(fileInfo.getFilePath())) {
            mFileInfoMap.remove(fileInfo.getFilePath());
        }
    }
    //==========================================================================
    //==========================================================================
    //发送方

    /**
     * 获取全局变量中的FileInfoMap
     *
     * @return
     */
    public Map<String, FileInfo> getReceiverFileInfoMap() {
        return mReceiverFileInfoMap;
    }

    /**
     * 获取即将接收文件列表的总长度
     *
     * @return
     */
    public long getAllReceiverFileInfoSize() {
        long total = 0;
        for (FileInfo fileInfo : mReceiverFileInfoMap.values()) {
            if (fileInfo != null) {
                total = total + fileInfo.getSize();
            }
        }
        return total;
    }

    /**
     * 添加一个FileInfo
     *
     * @param fileInfo
     */
    public void addReceiverFileInfo(FileInfo fileInfo) {
        if (!mReceiverFileInfoMap.containsKey(fileInfo.getFilePath())) {
            mReceiverFileInfoMap.put(fileInfo.getFilePath(), fileInfo);
        }
    }

    /**
     * 更新FileInfo
     *
     * @param fileInfo
     */
    public void updateReceiverFileInfo(FileInfo fileInfo) {
        mReceiverFileInfoMap.put(fileInfo.getFilePath(), fileInfo);
    }

}
