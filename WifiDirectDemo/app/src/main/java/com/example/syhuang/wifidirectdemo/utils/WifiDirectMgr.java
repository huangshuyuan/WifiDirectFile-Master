package com.example.syhuang.wifidirectdemo.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.example.syhuang.wifidirectdemo.thread.ClientConnectThread;
import com.example.syhuang.wifidirectdemo.thread.ListenerThread;
import com.example.syhuang.wifidirectdemo.thread.ServerConnectThread;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Wifi Direct 管理类
 *
 * @author: syhuang
 * @date: 2018/4/13
 */

public class WifiDirectMgr implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {
    private static final String TAG = WifiDirectMgr.class.getSimpleName();
    private static WifiDirectMgr mWifiDirectMgr;
    private        Context       mContext;
    public static final int DEVICE_CONNECTING = 1;
    //有设备正在连接热点
    public static final int DEVICE_CONNECTED  = 2;
    //有设备连上热点
    public static final int SEND_MSG_SUCCSEE  = 3;
    //发送消息成功
    public static final int SEND_MSG_ERROR    = 4;
    //发送消息失败
    public static final int GET_MSG           = 6;
    //获取新消息
    public static final int PORT              = 8989;
    WifiP2pManager         mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver      mReceiver;

    IntentFilter mIntentFilter;

    private List<WifiP2pDevice> mWifiP2pDevices = new ArrayList<>();
    /**
     * 当前连接
     */
    private WifiP2pInfo mInfo;


    /**
     * 当前连接设备
     */
    private WifiP2pDevice mDevice;
    /**
     * 记录连接状态，是服务器，还是客户端
     */
    private boolean       isServer;

    /**
     * 服务器-连接线程
     */
    private ServerConnectThread connectServerThread;


    /**
     * 监听线程
     */
    private ListenerThread listenerThread;


    /**
     * 客户端-连接线程
     */
    private ClientConnectThread connectThread;

    public static WifiDirectMgr getInstance(Context context) {
        if (mWifiDirectMgr == null) {
            synchronized (WifiMgr.class) {
                if (mWifiDirectMgr == null) {
                    mWifiDirectMgr = new WifiDirectMgr(context);
                }
            }
        }
        return mWifiDirectMgr;
    }

    private WifiDirectMgr(Context context) {
        this.mContext = context;

    }

    public WifiP2pInfo getInfo() {
        return mInfo;
    }

    public void setInfo(WifiP2pInfo info) {
        mInfo = info;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_CONNECTING:
                    try {
                        if (isServer) {
                            connectServerThread = new ServerConnectThread(mContext, listenerThread.getSocket(), mHandler);
                            connectServerThread.start();
                        } else {
                            connectThread = new ClientConnectThread(listenerThread.getSocket(), mHandler);
                            connectThread.start();

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case DEVICE_CONNECTED:
                    Log.i(TAG, "连接成功");
                    manHander.obtainMessage(3, "连接成功" + "--服务器" + isServer).sendToTarget();
                    break;
                case SEND_MSG_SUCCSEE:
                    Log.i(TAG, "发送成功" + msg.getData().getString("MSG"));
                    manHander.obtainMessage(1, "发送成功：" + msg.getData().getString("MSG")).sendToTarget();

                    break;
                case SEND_MSG_ERROR:
                    Log.i(TAG, "发送失败");
                    manHander.obtainMessage(1, "发送失败：" + msg.getData().getString("MSG")).sendToTarget();
                    break;
                case GET_MSG:
                    String str = msg.getData().getString("MSG");
                    if (str.startsWith("ip:")) {
                        str = str.replace("ip:", "");
                        //                [0] 是ip [1] device name
                        String[] deviceInfo = str.split("name:");
                        //获取连接设备的IP

                        //                        deviceMap.put("ClientIP", deviceInfo[0]);

                    } else {

                    }
                    manHander.obtainMessage(1, "收到消息：" + msg.getData().getString("MSG")).sendToTarget();

                    Log.i(TAG, "收到消息：" + msg.getData().getString("MSG"));

                    break;
            }
        }
    };

    /**
     * 发送消息
     *
     * @param message
     */

    public void sendMessage(String message) {
        //暂时发送
        if (isServer) {
            message += "你好呀 客户端";
        } else {
            message += "你好呀 服务器";
        }
        if (isServer) {
            if (connectServerThread != null) {
                connectServerThread.sendData(message);
            } else {
                Log.i(TAG, "connectServerThread == null");
            }
        } else {
            if (connectThread != null) {
                connectThread.sendData(message);
            } else {
                Log.i(TAG, "connectThread == null");
            }
        }

    }

    public void updateName(final String name) {
        try {
            Method m = mManager.getClass().getMethod(
                    "setDeviceName",
                    new Class[]{WifiP2pManager.Channel.class, String.class,
                            WifiP2pManager.ActionListener.class});

            m.invoke(mManager, mChannel, name, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    //Code for Success in changing name
                }

                @Override
                public void onFailure(int reason) {
                    //Code to be done while name change Fails
                }
            });
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public void init() {
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mManager = (WifiP2pManager) mContext.getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(mContext, mContext.getMainLooper(), null);


    }

    public void createReceiver() {
        if (mReceiver == null) {
            mReceiver = new WiFiDirectBroadcastReceiver(this, mManager, mChannel);
            mContext.registerReceiver(mReceiver, mIntentFilter);
        }
    }

    public void createWifi() {
        //修改WifiDirect 名称
        updateName("WiFi-Car");
        mManager.createGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                // we are connected with the other device, request connection
                // info to find group owner IP

                Log.i(TAG, "createGroup-->success");

            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "createGroup-->failed");

            }
        });
    }

    public void registerReceiver() {
        mContext.registerReceiver(mReceiver, mIntentFilter);

    }

    public void unregisterReceiver() {
        mContext.unregisterReceiver(mReceiver);
    }

    public void disConnect() {
        if (mManager != null) {

            mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                @Override
                public void onFailure(int reasonCode) {
                    Log.i(TAG, "Disconnect failed. Reason :" + reasonCode);

                }

                @Override
                public void onSuccess() {

                }

            });


        }

    }

    /**
     * 扫描Wifi Direct设备
     */

    public void discoverPeers() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.i(TAG, "discoverPeers-->onSuccess: ");

            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i(TAG, "onFailure: ");
            }
        });
    }

    /**
     * 主页面的
     */
    Handler manHander;

    public void initHandler(Handler handler) {
        manHander = handler;
    }


    /**
     * 连接成功监听
     */
    public interface OnWifiP2pDeviceListListener {
        void onWifiP2pDeviceList(List<WifiP2pDevice> mWifiP2pDevices);
    }

    OnWifiP2pDeviceListListener mOnWifiP2pDeviceListListener;

    public OnWifiP2pDeviceListListener getOnWifiP2pDeviceListListener() {
        return mOnWifiP2pDeviceListListener;
    }

    public void setOnWifiP2pDeviceListListener(OnWifiP2pDeviceListListener onWifiP2pDeviceListListener) {
        mOnWifiP2pDeviceListListener = onWifiP2pDeviceListListener;
    }

    /**
     * 扫描到设备
     *
     * @param peers
     */
    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        Log.i(TAG, "扫描到设备：" + peers.toString());
        mWifiP2pDevices.clear();
        mWifiP2pDevices.addAll(peers.getDeviceList());

        //获取连接到的设备
        mOnWifiP2pDeviceListListener.onWifiP2pDeviceList(mWifiP2pDevices);
    }

    /**
     * 连接到设备
     *
     * @param info
     */
    @Override
    public void onConnectionInfoAvailable(final WifiP2pInfo info) {
        if (info != null) {
            mInfo = info;
            Log.i(TAG, "WifiP2pInfo-->" + info.toString());
        }
        // InetAddress from WifiP2pInfo struct.
        InetAddress groupOwnerAddress = info.groupOwnerAddress;
        Log.i(TAG, "onConnectionInfoAvailable-->");

        if (info.groupFormed && info.isGroupOwner) {
            Log.i(TAG, "服务器");
            isServer = true;

            if (connectServerThread == null) {
                /**
                 * 先开启监听线程，在开启连接
                 */
                listenerThread = new ListenerThread(PORT, mHandler);
                listenerThread.start();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //        开启连接线程
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Log.i("ip", "IpAddress" + info.groupOwnerAddress.getHostAddress());
                            //本地路由开启通信
                            String ip = info.groupOwnerAddress.getHostAddress();
                            Socket socket = new Socket(ip, PORT);
                            connectServerThread = new ServerConnectThread(mContext, socket, mHandler);
                            connectServerThread.start();

                        } catch (IOException e) {
                            e.printStackTrace();
                            connectServerThread = null;
                        }
                    }
                }).start();
            }

            //服务器文件接收
            manHander.obtainMessage(2, "文件服务器创建").sendToTarget();

            //            initServer();

        } else if (info.groupFormed) {

            //如果自己不是groupowner角色就将自己的ip传给groupowner端
            Log.i(TAG, "客户端");
            isServer = false;

            if (connectThread == null) {
                //        开启连接线程
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Socket socket = new Socket(info.groupOwnerAddress.getHostAddress(), PORT);
                            connectThread = new ClientConnectThread(socket, mHandler);
                            connectThread.start();
                            //将自己的ip传给server端
                            connectThread.sendData("ip:" + Constant.getLocalIpAddress() + "name:" + "");
                        } catch (IOException e) {
                            e.printStackTrace();
                            connectThread = null;
                        }
                    }
                }).start();


                listenerThread = new ListenerThread(PORT, mHandler);
                listenerThread.start();

            }

        }

    }

    /**
     * 连接设备
     *
     * @param device
     */
    public void connect(final WifiP2pDevice device) {
        // Picking the first device found on the network.
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "connect success");
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "connect fail");
            }
        });
    }

    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
        String TAG = WiFiDirectBroadcastReceiver.class.getSimpleName();
        private WifiP2pManager         mManager;
        private WifiP2pManager.Channel mChannel;
        private WifiDirectMgr          wifiDirectMgr;

        public WiFiDirectBroadcastReceiver(WifiDirectMgr wifiDirectMgr, WifiP2pManager manager, WifiP2pManager.Channel channel) {
            super();
            this.wifiDirectMgr = wifiDirectMgr;
            this.mManager = manager;
            this.mChannel = channel;
        }

        public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel) {
            super();
            this.mManager = manager;
            this.mChannel = channel;

        }

        //        WIFI_P2P_THIS_DEVICE_CHANGED_ACTION 这个分支中可以获取自己这台设备的信息。
        //        WIFI_P2P_PEERS_CHANGED_ACTION 是查找设备（调用discoverPeers()），
        //        然后在这个分支中调用requestPeers()获取设备列表
        //        WIFI_P2P_CONNECTION_CHANGED_ACTION wifi连接状态的改变。
        //        WIFI_P2P_STATE_CHANGED_ACTION 就是wifi p2p可用还是不可用
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Check to see if Wi-Fi is enabled and notify appropriate activity
                Log.i(TAG, "onReceive: WIFI_P2P_STATE_CHANGED_ACTION ");
                //判断是否支持 wifi点对点传输
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // Call WifiP2pManager.requestPeers() to get a list of current mWifiP2pDevices
                //查找到设备列表

                Log.i(TAG, "onReceive: WIFI_P2P_PEERS_CHANGED_ACTION ");

                if (mManager != null) {

                    mManager.requestPeers(mChannel, wifiDirectMgr);
                }

            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Respond to new connection or disconnections
                //获取到连接状态改变的详细信息

                Log.i(TAG, "onReceive: WIFI_P2P_CONNECTION_CHANGED_ACTION");
                if (mManager == null) {
                    return;
                }

                NetworkInfo networkInfo = intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected()) {

                    // we are connected with the other device, request connection
                    // info to find group owner IP

                    Log.i(TAG, "onReceive: isConnected");
                    mManager.requestConnectionInfo(mChannel, wifiDirectMgr);

                } else {
                    // It's a disconnect
                    Log.i(TAG, "onReceive: disconnect");

                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
                //自身设备信息改变
                mDevice = intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                //                infoTextView.setText(mDevice.deviceName);

                Log.i(TAG, "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            }
        }
    }
}



