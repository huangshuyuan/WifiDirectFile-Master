package com.example.syhuang.wifidirectdemo.ui;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.syhuang.wifidirectdemo.R;
import com.example.syhuang.wifidirectdemo.app.AppContext;
import com.example.syhuang.wifidirectdemo.core.FileReceiver;
import com.example.syhuang.wifidirectdemo.core.FileSender;
import com.example.syhuang.wifidirectdemo.core.entity.FileInfo;
import com.example.syhuang.wifidirectdemo.db.UploadHelper;
import com.example.syhuang.wifidirectdemo.utils.FileUtils;
import com.example.syhuang.wifidirectdemo.utils.ToastUtils;
import com.example.syhuang.wifidirectdemo.utils.WifiDirectMgr;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * Author: syhuang
 * Date:  2018/4/11
 */
@RuntimePermissions
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    Button button, disConnect;
    TextView textView, infoTextView;
    private RecyclerView mRecyclerView;
    WifiAdapter mWifiAdapter;

    /**
     * 发送进度
     */
    ProgressBar mProgressBar;
    TextView    storageText, timeText;

    private UploadHelper upHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView = findViewById(R.id.recylerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mProgressBar = findViewById(R.id.progress);
        storageText = findViewById(R.id.storage_text);
        timeText = findViewById(R.id.time_text);
        findViewById(R.id.scan_button).setOnClickListener(this);
        findViewById(R.id.sendMessage).setOnClickListener(this);
        findViewById(R.id.sendFileButton).setOnClickListener(this);
        findViewById(R.id.open).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
        findViewById(R.id.client_init).setOnClickListener(this);
        disConnect = findViewById(R.id.disConnect);
        disConnect.setOnClickListener(this);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
        textView = (TextView) findViewById(R.id.text);
        infoTextView = findViewById(R.id.info);

        /**
         * 初始化Wifi  Direct
         */


        WifiDirectMgr.getInstance(this).init();

        WifiDirectMgr.getInstance(this).initHandler(handler);

        WifiDirectMgr.getInstance(this).setOnWifiP2pDeviceListListener(new WifiDirectMgr.OnWifiP2pDeviceListListener() {
            @Override
            public void onWifiP2pDeviceList(List<WifiP2pDevice> mWifiP2pDevices) {
                mWifiAdapter = new WifiAdapter(MainActivity.this, mWifiP2pDevices);
                mRecyclerView.setAdapter(mWifiAdapter);
                mWifiAdapter.notifyDataSetChanged();
            }
        });
        //        if (Build.VERSION.SDK_INT >= 23) {
        //            MainActivityPermissionsDispatcher.showStorageWithPermissionCheck(MainActivity.this);
        //        } else {
        //            showStorage();
        //        }

        upHelper = new UploadHelper(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            showNotification();
        }

        //        addIconToStatusbar(R.mipmap.ic_launcher);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void showNotification() {
        // 创建一个NotificationManager的引用
        NotificationManager notificationManager = (NotificationManager)
                this.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        // 定义Notification的各种属性
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Wifi Direct")
                .setContentText("open...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher_round))
                .build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT; // 将此通知放到通知栏的"Ongoing"即"正在运行"组中
        notification.flags |= Notification.FLAG_NO_CLEAR; // 表明在点击了通知栏中的"清除通知"后，此通知不清除，经常与FLAG_ONGOING_EVENT一起使用
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.defaults = Notification.DEFAULT_LIGHTS;
        notification.ledARGB = Color.BLUE;
        notification.ledOnMS = 5000;
        notificationManager.notify(0, notification);
    }

    public void closeNotification() {
        // 启动后删除之前我们定义的通知
        NotificationManager notificationManager = (NotificationManager) this
                .getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(0);
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    infoTextView.setText((String) msg.obj);
                    break;
                case 2:
                    //创建文件服务器
                    initServer();
                    break;
                case 3:
                    textView.setText((String) msg.obj);
                    break;

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        WifiDirectMgr.getInstance(this).unregisterReceiver();
    }


    int FILE_CODE   = 1002;
    int FILE_CODE_2 = 1003;

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.open:
                //                WifiDirectMgr.getInstance(this).createWifi();
                WifiDirectMgr.getInstance(this).createReceiver();
                if (Build.VERSION.SDK_INT >= 23) {
                    MainActivityPermissionsDispatcher.showStorageWithPermissionCheck(MainActivity.this);
                } else {
                    showStorage();
                }
                break;
            case R.id.client_init:
                WifiDirectMgr.getInstance(this).createReceiver();
                break;
            case R.id.scan_button:
                //                registerReceiver(mReceiver, mIntentFilter);
                if (Build.VERSION.SDK_INT >= 23) {
                    MainActivityPermissionsDispatcher.showStorageWithPermissionCheck(MainActivity.this);
                } else {
                    showStorage();
                }
                break;
            case R.id.button:

                /**文件库*/
                // This always works
                Intent i = new Intent(MainActivity.this, FilePickerActivity.class);

                // Set these depending on your use case. These are the defaults.
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

                startActivityForResult(i, FILE_CODE);
                break;
            case R.id.disConnect:
                Log.i(TAG, "onClick: btnDisconnect...");

                WifiDirectMgr.getInstance(this).disConnect();

                break;
            case R.id.sendMessage:

                String str = "hello ";
                //                infoTextView.setText(str);
                //                sendString(str);
                WifiDirectMgr.getInstance(this).sendMessage(str);

                //                sendInfo();

                break;
            case R.id.sendFileButton:
                /**文件库*/
                // This always works
                Intent intent = new Intent(MainActivity.this, FilePickerActivity.class);
                // This works if you defined the intent filter
                // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                // Set these depending on your use case. These are the defaults.
                intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

                // Configure initial directory by specifying a String.
                // You could specify a String like "/storage/emulated/0/", but that can
                // dangerous. Always use Android's API calls to get paths to the SD-card or
                // internal memory.
                intent.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

                startActivityForResult(intent, FILE_CODE_2);
                break;
            case R.id.stop:
                flag = false;
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            // Use the provided utility method to parse the result
            List<Uri> files = com.nononsenseapps.filepicker.Utils.getSelectedFilesFromResult(data);
            for (Uri uri : files) {
                File file = com.nononsenseapps.filepicker.Utils.getFileForUri(uri);
                // Do something with the result...
                //                if (isServer) {
                //                    if (connectServerThread != null) {
                //                        connectServerThread.sendData(file);
                //                    }
                //                } else {
                //                    if (connectThread != null) {
                //                        connectThread.sendData(file);
                //                    }
                //                }
            }
        } else if (requestCode == FILE_CODE_2 && resultCode == Activity.RESULT_OK) {

            // Use the provided utility method to parse the result
            List<Uri> files = com.nononsenseapps.filepicker.Utils.getSelectedFilesFromResult(data);
            for (Uri uri : files) {
                File file = com.nononsenseapps.filepicker.Utils.getFileForUri(uri);
                // Do something with the result...
                //                flag = true;
                //                uploadFile(file);

                FileInfo fileInfo = new FileInfo();
                String sourceid = upHelper.getBindId(file);
                fileInfo.setId(sourceid);
                fileInfo.setFilePath(file.getAbsolutePath());
                fileInfo.setName(file.getName());
                fileInfo.setSize(file.length());
                AppContext.getAppContext().addFileInfo(fileInfo);
            }

            //处理
            List<Map.Entry<String, FileInfo>> fileInfoMapList = new ArrayList<Map.Entry<String, FileInfo>>(AppContext.getAppContext().getFileInfoMap().entrySet());
            initSendServer(fileInfoMapList);

        }

    }

    boolean flag = false;

    private Handler handler2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mProgressBar.setProgress(msg.getData().getInt("length"));
            float num = (float) mProgressBar.getProgress() / (float) mProgressBar.getMax();
            int result = (int) (num * 100);
            infoTextView.setText(result + "%");
            if (mProgressBar.getProgress() == mProgressBar.getMax()) {
                Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /***************************************************/
//    private void uploadFile(final File file) {
//        mProgressBar.setMax((int) file.length());
//        final String serverIp = WifiDirectMgr.getInstance(this).getInfo().groupOwnerAddress.getHostAddress();
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    String sourceid = upHelper.getBindId(file);
//                    Socket socket = new Socket(serverIp, 9999);
//                    OutputStream outStream = socket.getOutputStream();
//                    String head = "Content-Length=" + file.length() + ";filename=" + file.getName()
//                            + ";sourceid=" + (sourceid != null ? sourceid : "") + "\r\n";
//
//                    Log.i(TAG, "head:-->" + head);
//                    outStream.write(head.getBytes());
//
//                    PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
//                    String response = StreamTool.readLine(inStream);
//                    String[] items = response.split(";");
//                    String responseSourceid = items[0].substring(items[0].indexOf("=") + 1);
//                    String position = items[1].substring(items[1].indexOf("=") + 1);
//                    if (sourceid == null) {//如果是第一次上传文件，在数据库中不存在该文件所绑定的资源id
//                        upHelper.save(responseSourceid, file);
//                    }
//                    RandomAccessFile fileOutStream = new RandomAccessFile(file, "rwd");
//                    fileOutStream.seek(Integer.valueOf(position));
//                    byte[] buffer = new byte[1024];
//                    int len = -1;
//                    int length = Integer.valueOf(position);
//                    while (flag && (len = fileOutStream.read(buffer)) != -1) {
//                        outStream.write(buffer, 0, len);
//                        length += len;//累加已经上传的数据长度
//                        Message msg = new Message();
//                        msg.getData().putInt("length", length);
//                        handler2.sendMessage(msg);
//                    }
//                    if (length == file.length()) {
//                        upHelper.delete(file);
//                    }
//                    fileOutStream.close();
//                    outStream.close();
//                    inStream.close();
//                    socket.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Log.i(TAG, "上传异常");
//                }
//
//            }
//        };
//        AppContext.FILE_SENDER_EXECUTOR.execute(runnable);
//        new Thread(runnable).start();
//
//    }


    /*******************************************************/

    /*客户端文件发送*******************************************************************************************/

    public static final int MSG_UPDATE_FILE_INFO = 0X6666;
    Handler  mFileHander   = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //TODO 未完成 handler实现细节以及封装
            if (msg.what == MSG_UPDATE_FILE_INFO) {
                updateTotalProgressView();

                //                if(mFileSenderAdapter != null) mFileSenderAdapter.notifyDataSetChanged();
            }

        }
    };
    String[] mStorageArray = null;

    String[] mTimeArray = null;

    /**
     * 更新进度 和 耗时的 View
     */
    private void updateTotalProgressView() {
        try {
            //设置传送的总容量大小
            mStorageArray = FileUtils.getFileSizeArrayStr(mTotalLen);
            storageText.setText(mStorageArray[0] + "~" + mStorageArray[1]);

            //设置传送的时间情况
            mTimeArray = FileUtils.getTimeByArrayStr(mTotalTime);
            timeText.setText(mTimeArray[0] + "~" + mTimeArray[1]);


            //设置传送的进度条情况
            if (mHasSendedFileCount == AppContext.getAppContext().getFileInfoMap().size()) {
                mProgressBar.setProgress(0);
                return;
            }

            long total = AppContext.getAppContext().getAllSendFileInfoSize();
            int percent = (int) (mTotalLen * 100 / total);
            mProgressBar.setProgress(percent);

            if (total == mTotalLen) {
                mProgressBar.setProgress(0);
            }
        } catch (Exception e) {
            //convert storage array has some problem
        }
    }

    /**
     * 所有总文件的进度
     */
    long mTotalLen      = 0;
    /**
     * 每次传送的偏移量
     */
    long mCurOffset     = 0;
    /**
     * 每个文件传送onProgress() 之前的进度
     */
    long mLastUpdateLen = 0;


    long             mTotalTime          = 0;
    long             mCurTimeOffset      = 0;
    long             mLastUpdateTime     = 0;
    int              mHasSendedFileCount = 0;
    List<FileSender> mFileSenderList     = new ArrayList<FileSender>();

    /**
     * 开始传送文件
     *
     * @param fileInfoMapList
     */
    private void initSendServer(List<Map.Entry<String, FileInfo>> fileInfoMapList) {
        String serverIp = WifiDirectMgr.getInstance(this).getInfo().groupOwnerAddress.getHostAddress();
        for (Map.Entry<String, FileInfo> entry : fileInfoMapList) {
            final FileInfo fileInfo = entry.getValue();
            FileSender fileSender = new FileSender(this, fileInfo, serverIp, 8080);
            fileSender.setOnSendListener(new FileSender.OnSendListener() {
                @Override
                public void onStart() {
                    mLastUpdateLen = 0;
                    mLastUpdateTime = System.currentTimeMillis();

                }

                @Override
                public void onProgress(long progress, long total) {
                    //TODO 更新
                    //=====更新进度 流量 时间视图 start ====//
                    mCurOffset = progress - mLastUpdateLen > 0 ? progress - mLastUpdateLen : 0;
                    mTotalLen = mTotalLen + mCurOffset;
                    mLastUpdateLen = progress;

                    mCurTimeOffset = System.currentTimeMillis() - mLastUpdateTime > 0 ? System.currentTimeMillis() - mLastUpdateTime : 0;
                    mTotalTime = mTotalTime + mCurTimeOffset;
                    mLastUpdateTime = System.currentTimeMillis();
                    //=====更新进度 流量 时间视图 end ====//

                    //更新文件传送进度的ＵＩ
                    fileInfo.setProcceed(progress);
                    AppContext.getAppContext().updateFileInfo(fileInfo);
                    mFileHander.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                }

                @Override
                public void onSuccess(FileInfo fileInfo) {
                    //=====更新进度 流量 时间视图 start ====//
                    mHasSendedFileCount++;

                    mTotalLen = mTotalLen + (fileInfo.getSize() - mLastUpdateLen);
                    mLastUpdateLen = 0;
                    mLastUpdateTime = System.currentTimeMillis();
                    //=====更新进度 流量 时间视图 end ====//

                    System.out.println(Thread.currentThread().getName());
                    //TODO 成功
                    fileInfo.setResult(FileInfo.FLAG_SUCCESS);
                    AppContext.getAppContext().updateFileInfo(fileInfo);
                    mFileHander.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                }

                @Override
                public void onFailure(Throwable t, FileInfo fileInfo) {
                    mHasSendedFileCount++;//统计发送文件
                    //TODO 失败
                    fileInfo.setResult(FileInfo.FLAG_FAILURE);
                    AppContext.getAppContext().updateFileInfo(fileInfo);
                    mFileHander.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                }
            });

            mFileSenderList.add(fileSender);
            AppContext.FILE_SENDER_EXECUTOR.execute(fileSender);
        }
    }


    @Override
    public void onBackPressed() {
        //        super.onBackPressed();
        //需要判断是否有文件在发送？
        if (hasFileSending()) {
            showExistDialog();
            return;
        }

        finishNormal();
    }

    /**
     * 正常退出
     */
    private void finishNormal() {
        //        AppContext.FILE_SENDER_EXECUTOR.
        stopAllFileSendingTask();
        AppContext.getAppContext().getFileInfoMap().clear();
        this.finish();
    }

    /**
     * 停止所有的文件发送任务
     */
    private void stopAllFileSendingTask() {
        for (FileSender fileSender : mFileSenderList) {
            if (fileSender != null) {
                fileSender.stop();
            }
        }
    }

    /**
     * 判断是否有文件在传送
     */
    private boolean hasFileSending() {
        for (FileSender fileSender : mFileSenderList) {
            if (fileSender.isRunning()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 显示是否退出 对话框
     */
    private void showExistDialog() {
        //        new AlertDialog.Builder(getContext())
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("当前有文件在传输，是否退出？")
                .setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finishNormal();
                    }
                })
                .setNegativeButton("否", null)
                .create()
                .show();
    }
    /*************************************************************客户端***************************************/

    /*************************************************************服务器端文件接收***************************************/


    public static final int MSG_FILE_RECEIVER_INIT_SUCCESS = 0X4444;
    public static final int MSG_ADD_FILE_INFO              = 0X5555;
    Handler mHandlerReceive = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_FILE_RECEIVER_INIT_SUCCESS) {
                /**
                 * 通知文件发送方 ===>>> 文件接收方初始化完毕
                 */

                //                connectServerThread.sendData("文件接收完毕");
                //                sendMsgToFileSender(mIpPortInfo);
            } else if (msg.what == MSG_ADD_FILE_INFO) {
                //ADD FileInfo 到 Adapter
                FileInfo fileInfo = (FileInfo) msg.obj;
                ToastUtils.show(MainActivity.this, "收到一个任务：" + (fileInfo != null ? fileInfo.getFilePath() : ""));
            } else if (msg.what == MSG_UPDATE_FILE_INFO) {
                //ADD FileInfo 到 Adapter
                updateReceiveTotalProgressView();
            }
        }
    };

    /**
     * 更新进度 和 耗时的 View
     */
    private void updateReceiveTotalProgressView() {
        try {
            //设置传送的总容量大小
            mStorageArray = FileUtils.getFileSizeArrayStr(mTotalLen);
            storageText.setText(mStorageArray[0] + "~" + mStorageArray[1]);

            //设置传送的时间情况
            mTimeArray = FileUtils.getTimeByArrayStr(mTotalTime);
            timeText.setText(mTimeArray[0] + "~" + mTimeArray[1]);


            //设置传送的进度条情况
            if (mHasSendedFileCount == AppContext.getAppContext().getReceiverFileInfoMap().size()) {
                mProgressBar.setProgress(0);
                return;
            }

            long total = AppContext.getAppContext().getAllReceiverFileInfoSize();
            int percent = (int) (mTotalLen * 100 / total);
            mProgressBar.setProgress(percent);

            if (total == mTotalLen) {
                mProgressBar.setProgress(0);
            }
        } catch (Exception e) {
            //convert storage array has some problem
        }
    }

    ServerRunnable mReceiverServer;

    FileInfo mCurFileInfo;

    //    /**
    //     * 支持断点续传----开启文件接收端服务
    //     */
    //    private void initServer() {
    //        final FileServer s = new FileServer(9999);
    //
    //        new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                try {
    //                    s.start();
    //                } catch (Exception e) {
    //                    // e.printStackTrace();
    //                }
    //            }
    //        }).start();
    //
    //    }

    /**
     * 不支持断点续传----开启文件接收端服务
     */
    private void initServer() {

        mReceiverServer = new ServerRunnable(8080);
        new Thread(mReceiverServer).start();
    }

    /**
     * ServerSocket启动线程
     */
    class ServerRunnable implements Runnable {
        ServerSocket serverSocket;
        private int port;


        public ServerRunnable(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            Log.i(TAG, "------>>>Socket已经开启");
            try {
                serverSocket = new ServerSocket(port);
                mHandlerReceive.obtainMessage(MSG_FILE_RECEIVER_INIT_SUCCESS).sendToTarget();
                while (!Thread.currentThread().isInterrupted()) {
                    Socket socket = serverSocket.accept();

                    //生成缩略图
                    FileReceiver fileReceiver = new FileReceiver(socket);
                    fileReceiver.setOnReceiveListener(new FileReceiver.OnReceiveListener() {
                        @Override
                        public void onStart() {
                            //                            handler.obtainMessage(MSG_SHOW_PROGRESS).sendToTarget();
                            mLastUpdateLen = 0;
                            mLastUpdateTime = System.currentTimeMillis();
                        }

                        @Override
                        public void onGetFileInfo(FileInfo fileInfo) {
                            mHandlerReceive.obtainMessage(MSG_ADD_FILE_INFO, fileInfo).sendToTarget();
                            mCurFileInfo = fileInfo;
                            AppContext.getAppContext().addReceiverFileInfo(mCurFileInfo);
                            mHandlerReceive.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }


                        @Override
                        public void onProgress(long progress, long total) {
                            //=====更新进度 流量 时间视图 start ====//
                            mCurOffset = progress - mLastUpdateLen > 0 ? progress - mLastUpdateLen : 0;
                            mTotalLen = mTotalLen + mCurOffset;
                            mLastUpdateLen = progress;

                            mCurTimeOffset = System.currentTimeMillis() - mLastUpdateTime > 0 ? System.currentTimeMillis() - mLastUpdateTime : 0;
                            mTotalTime = mTotalTime + mCurTimeOffset;
                            mLastUpdateTime = System.currentTimeMillis();
                            //=====更新进度 流量 时间视图 end ====//

                            mCurFileInfo.setProcceed(progress);
                            AppContext.getAppContext().updateReceiverFileInfo(mCurFileInfo);
                            mHandlerReceive.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }

                        @Override
                        public void onSuccess(FileInfo fileInfo) {
                            //=====更新进度 流量 时间视图 start ====//
                            mHasSendedFileCount++;

                            mTotalLen = mTotalLen + (fileInfo.getSize() - mLastUpdateLen);
                            mLastUpdateLen = 0;
                            mLastUpdateTime = System.currentTimeMillis();
                            //=====更新进度 流量 时间视图 end ====//

                            fileInfo.setResult(FileInfo.FLAG_SUCCESS);
                            AppContext.getAppContext().updateReceiverFileInfo(fileInfo);
                            mHandlerReceive.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }

                        @Override
                        public void onFailure(Throwable t, FileInfo fileInfo) {
                            mHasSendedFileCount++;//统计发送文件

                            fileInfo.setResult(FileInfo.FLAG_FAILURE);
                            AppContext.getAppContext().updateFileInfo(fileInfo);
                            mHandlerReceive.sendEmptyMessage(MSG_UPDATE_FILE_INFO);
                        }
                    });

                    //                    mFileReceiver = fileReceiver;
                    //                    new Thread(fileReceiver).start();
                    AppContext.MAIN_EXECUTOR.execute(fileReceiver);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        /**
         * 关闭Socket 通信 (避免端口占用)
         */
        public void close() {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    serverSocket = null;
                } catch (IOException e) {
                }
            }
        }
    }

    /*************************************************************服务器端文件接收***************************************/


    class WifiAdapter extends RecyclerView.Adapter<WifiHolder> {
        List<WifiP2pDevice> mWifiP2pDevices;
        Context             context;

        public WifiAdapter(Context context, Collection<WifiP2pDevice> wifiP2pDevices) {
            mWifiP2pDevices = new ArrayList<>();
            Log.i(TAG, "WifiAdapter: " + wifiP2pDevices.size());
            mWifiP2pDevices.addAll(wifiP2pDevices);
            this.context = context;
        }

        @Override
        public WifiHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WifiHolder(LayoutInflater.from(
                    MainActivity.this).inflate(R.layout.item_view, parent,
                    false));
        }

        @Override
        public void onBindViewHolder(WifiHolder holder, final int position) {
            holder.mTextView.setText(mWifiP2pDevices.get(position).deviceName + ":\n"
                    + mWifiP2pDevices.get(position).deviceAddress
                    + "\n" + mWifiP2pDevices.get(position).primaryDeviceType);
            holder.mTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WifiDirectMgr.getInstance(context).connect(mWifiP2pDevices.get(position));
                }
            });
            //            mDevice = mWifiP2pDevices.get(position);

        }

        @Override
        public int getItemCount() {
            return mWifiP2pDevices != null ? mWifiP2pDevices.size() : 0;
        }
    }


    class WifiHolder extends RecyclerView.ViewHolder {
        TextView mTextView;

        public WifiHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.textView);
        }
    }


    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showStorage() {
        WifiDirectMgr.getInstance(this).discoverPeers();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onShowStorage(final PermissionRequest request) {
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onDeniedStorage() {
    }

    @OnNeverAskAgain({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onAgainStorage() {
    }
}
