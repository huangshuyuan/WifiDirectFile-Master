package com.example.syhuang.wifidirectdemo.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.syhuang.wifidirectdemo.utils.Constant;
import com.example.syhuang.wifidirectdemo.R;
import com.example.syhuang.wifidirectdemo.utils.Utils;
import com.example.syhuang.wifidirectdemo.oldthread.ClientSendThread;
import com.example.syhuang.wifidirectdemo.oldthread.ServerThread;
import com.example.syhuang.wifidirectdemo.service.FileTransferService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
public class Main2Activity extends AppCompatActivity implements View.OnClickListener {
    public static final  int    DEVICE_CONNECTING       = 1;//有设备正在连接热点
    public static final  int    DEVICE_CONNECTED        = 2;//有设备连上热点
    public static final  int    SEND_MSG_SUCCSEE        = 3;//发送消息成功
    public static final  int    SEND_MSG_ERROR          = 4;//发送消息失败
    public static final  int    GET_MSG                 = 6;//获取新消息
    private static final String TAG                     = "MainActivity";
    private static final int    CHOOSE_FILE_RESULT_CODE = 1001;
    WifiP2pManager                  mManager;
    WifiP2pManager.Channel          mChannel;
    BroadcastReceiver               mReceiver;
    WifiP2pManager.PeerListListener myPeerListListener;//扫描到设备

    WifiP2pManager.ConnectionInfoListener myConnectionInfoListener;//连接到设备

    IntentFilter mIntentFilter;
    private List<WifiP2pDevice> peers = new ArrayList<>();

    WifiP2pInfo mInfo;//当前连接

    Button button, disConnect;
    TextView textView, infoTextView;
    private RecyclerView mRecyclerView;
    WifiAdapter mWifiAdapter;
    boolean     isServer;

    WifiP2pDevice    mDevice;
    MyHandler        mHandler;
    ServerThread     clientListenrThread;
    ClientSendThread mClientSendThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new MyHandler(this, deviceMap);
        mRecyclerView = findViewById(R.id.recylerview);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.scan_button).setOnClickListener(this);
        findViewById(R.id.sendMessage).setOnClickListener(this);
        disConnect = findViewById(R.id.disConnect);
        disConnect.setOnClickListener(this);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
        textView = (TextView) findViewById(R.id.text);
        infoTextView = findViewById(R.id.info);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel);

        clientListenrThread = new ServerThread(mHandler, Main2Activity.this, Constant.PORT);
        clientListenrThread.start();
        myPeerListListener = new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peerList) {
                //                mAdapter = new WifiAdapter(list.getDeviceList());
                //                mRecyclerView.setAdapter(mAdapter);
                //                mAdapter.notifyDataSetChanged();
                Log.i(TAG, "扫描到设备：" + peerList.toString());
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                mWifiAdapter = new WifiAdapter(peers);
                mRecyclerView.setAdapter(mWifiAdapter);
                mWifiAdapter.notifyDataSetChanged();
            }
        };


        myConnectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {

            @Override
            public void onConnectionInfoAvailable(final WifiP2pInfo info) {

                if (info != null) {
                    mInfo = info;
                    Log.i(TAG, "info" + info.toString());
                }
                textView.setVisibility(View.VISIBLE);
                textView.setText("(connected)");
                // InetAddress from WifiP2pInfo struct.
                InetAddress groupOwnerAddress = info.groupOwnerAddress;
                Log.i(TAG, "onConnectionInfoAvailable");
                Log.i(TAG, info.toString());

                if (info.groupFormed && info.isGroupOwner) {
                    infoTextView.setText("服务器");
                    Log.i(TAG, "服务器");

                    new FileServerAsyncTask(Main2Activity.this, textView)
                            .execute();
                    isServer = true;

                } else if (info.groupFormed) {

                    //如果自己不是groupowner角色就将自己的ip传给groupowner端
                    Log.i("ip", Constant.getLocalIpAddress());
                    //将自己的ip传给server端
                    if (mClientSendThread == null) {
                        mClientSendThread = new ClientSendThread(mHandler, info.groupOwnerAddress.getHostAddress(), "ip:" + Constant.getLocalIpAddress() + "name:" + mDevice.deviceName);
                        mClientSendThread.start();
                    } else {

                    }

                    infoTextView.setText("客户端");
                    Log.i(TAG, "客户端");
                    new FileServerAsyncTask(Main2Activity.this, textView)
                            .execute();
                    isServer = false;

                }

            }
        };
        //        serverThread = new ServerThread(mHandler, this, Constant.PORT_SERVER);
        //        serverThread.start();
        //


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clientListenrThread.close();
    }

    static Map<String, String> deviceMap = new HashMap<>();

    public void sendInfo() {
        String host = null;
        if (mInfo.groupFormed && mInfo.isGroupOwner) {
            Log.i(TAG, "send服务器");
            host = deviceMap.get("ClientIP");
            Log.i(TAG, "deviceMap=" + deviceMap.toString() + deviceMap.get("ClientIP"));
            clientListenrThread.sendMessage("hello client");

        } else if (mInfo.groupFormed) {
            host = mInfo.groupOwnerAddress.getHostAddress();
            Log.i(TAG, host);
            mClientSendThread.sendMessage("hello server");

        }


    }

    private class MyHandler extends Handler {

        private final WeakReference<Main2Activity> mActivity;
        Map<String, String> ipNameMap;

        public MyHandler(Main2Activity activity, Map<String, String> map) {
            mActivity = new WeakReference<>(activity);
            ipNameMap = map;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    String str = (String) msg.obj;
                    Toast.makeText(mActivity.get(), str, Toast.LENGTH_LONG).show();
                    break;
                case 2:
                    Toast.makeText(mActivity.get(), "发送成功", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    String[] deviceInfo = (String[]) msg.obj;
                    ipNameMap.put("ClientIP", deviceInfo[0]);
                    deviceMap.put("ClientIP", deviceInfo[0]);
                    Log.i(TAG, "deviceMap=" + deviceMap.toString());
                    //                    DebugFile.getInstance(mActivity.get()).writeLog("接收到的ip数据","ip"+deviceInfo[0]+"name"+deviceInfo[1]);

                    break;
            }
        }
    }

    public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context  context;
        private TextView statusText;

        public FileServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
            Log.i(TAG, "FileServerAsyncTask: ");
        }

        @Override
        protected String doInBackground(Void... params) {
            try {

                Log.i(TAG, "doInBackground: serverSocket");

                ServerSocket serverSocket = new ServerSocket(8987);
                Socket client = serverSocket.accept();

                final File f = new File(Environment.getExternalStorageDirectory() + "/"
                        + context.getPackageName() + "/wifip2pshared-" + System.currentTimeMillis()
                        + ".jpg");

                File dirs = new File(f.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                f.createNewFile();
                InputStream inputstream = client.getInputStream();
                Utils.copyFile(inputstream, new FileOutputStream(f));
                serverSocket.close();
                return f.getAbsolutePath();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "onPostExecute: " + result);

            if (result != null) {
                statusText.setText("File copied - " + result);
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("file://" + result), "image/*");
                context.startActivity(intent);
            }
        }
    }

    public static class StringServerAsyncTask extends AsyncTask<Void, Void, String> {

        private Context  context;
        private TextView statusText;

        public StringServerAsyncTask(Context context, View statusText) {
            this.context = context;
            this.statusText = (TextView) statusText;
            Log.i(TAG, "FileServerAsyncTask: ");
        }

        @Override
        protected String doInBackground(Void... params) {
            try {

                Log.i(TAG, "doInBackground: serverSocket");

                ServerSocket serverSocket = new ServerSocket(8987);
                Socket client = serverSocket.accept();

                InputStream inputstream = client.getInputStream();
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                byte[] data = new byte[inputstream.available()];
                int count;
                while ((count = inputstream.read(data, 0, inputstream.available())) != -1) {
                    outStream.write(data, 0, count);
                }

                String str = new String(outStream.toByteArray(), "UTF-8");
                Log.i("str", str);

                serverSocket.close();
                return str;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i(TAG, "onPostExecute: " + result);

            if (result != null) {
                statusText.setText("File copied - " + result);
                if (result.startsWith("IP:")) {
                    result = result.replace("ip:", "");
                    //                [0] 是ip [1] device name

                    deviceMap.put("ClientIP", result);
                } else {
                }

            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);
        if (Build.VERSION.SDK_INT >= 23)
            Main2ActivityPermissionsDispatcher.showStorageWithPermissionCheck(Main2Activity.this);
        else {
            showStorage();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public void connect(final WifiP2pDevice device) {
        // Picking the first device found on the network.
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "connect sucess");
            }

            @Override
            public void onFailure(int reason) {
                Log.i(TAG, "connect fail");
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scan_button:
                registerReceiver(mReceiver, mIntentFilter);
                if (Build.VERSION.SDK_INT >= 23)
                    Main2ActivityPermissionsDispatcher.showStorageWithPermissionCheck(Main2Activity.this);
                else {
                    showStorage();
                }
                break;
            case R.id.button:
                //打开相册
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, CHOOSE_FILE_RESULT_CODE);
                break;
            case R.id.disConnect:
                Log.i(TAG, "onClick: btnDisconnect...");
                if (mManager != null) {

                    if (mDevice == null
                            || mDevice.status == WifiP2pDevice.CONNECTED) {

                        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onFailure(int reasonCode) {
                                Log.i(TAG, "Disconnect failed. Reason :" + reasonCode);

                            }

                            @Override
                            public void onSuccess() {
                                infoTextView.setText("断开连接");
                            }

                        });
                    } else if (mDevice.status == WifiP2pDevice.AVAILABLE
                            || mDevice.status == WifiP2pDevice.INVITED) {

                        mManager.cancelConnect(mChannel, new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                infoTextView.setText("断开连接");
                                Toast.makeText(Main2Activity.this, "Aborting connection",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int reasonCode) {
                                Toast.makeText(Main2Activity.this,
                                        "Connect abort request failed. Reason Code: " + reasonCode,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                break;
            case R.id.sendMessage:

                sendInfo();

                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            infoTextView.setText("Sending: " + uri);
            Log.i(TAG, "Intent----------- " + uri);
            Intent serviceIntent = new Intent(this, FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, uri.toString());
            if (isServer) {
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        deviceMap.get("ClientIP"));
            } else {
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS,
                        mInfo.groupOwnerAddress.getHostAddress());
            }
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8987);
            startService(serviceIntent);
        }


    }

    class WifiAdapter extends RecyclerView.Adapter<WifiHolder> {
        List<WifiP2pDevice> mWifiP2pDevices;

        public WifiAdapter(Collection<WifiP2pDevice> wifiP2pDevices) {
            mWifiP2pDevices = new ArrayList<>();
            Log.i(TAG, "WifiAdapter: " + wifiP2pDevices.size());
            mWifiP2pDevices.addAll(wifiP2pDevices);
        }

        @Override
        public WifiHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WifiHolder(LayoutInflater.from(
                    Main2Activity.this).inflate(R.layout.item_view, parent,
                    false));
        }

        @Override
        public void onBindViewHolder(WifiHolder holder, final int position) {
            holder.tv.setText(mWifiP2pDevices.get(position).deviceName + ":\n"
                    + mWifiP2pDevices.get(position).deviceAddress
                    + "\n" + mWifiP2pDevices.get(position).primaryDeviceType);
            holder.tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    connect(mWifiP2pDevices.get(position));
                }
            });
            mDevice = mWifiP2pDevices.get(position);

        }

        @Override
        public int getItemCount() {
            return mWifiP2pDevices != null ? mWifiP2pDevices.size() : 0;
        }
    }

    class WifiHolder extends RecyclerView.ViewHolder {
        TextView tv;

        public WifiHolder(View itemView) {
            super(itemView);
            tv = (TextView) itemView.findViewById(R.id.textView);
        }
    }

    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

        private WifiP2pManager         mManager;
        private WifiP2pManager.Channel mChannel;

        public WiFiDirectBroadcastReceiver() {
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
                // Call WifiP2pManager.requestPeers() to get a list of current peers
                //查找到设备列表

                Log.i(TAG, "onReceive: WIFI_P2P_PEERS_CHANGED_ACTION ");

                if (mManager != null) {

                    mManager.requestPeers(mChannel, myPeerListListener);
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
                    mManager.requestConnectionInfo(mChannel, myConnectionInfoListener);

                } else {
                    // It's a disconnect
                    Log.i(TAG, "onReceive: disconnect");

                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
                //自身设备信息改变
                mDevice = intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
                infoTextView.setText(mDevice.deviceName);

                Log.i(TAG, "onReceive: WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
            }
        }
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void showStorage() {
        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {

                Log.i(TAG, "onSuccess: ");

            }

            @Override
            public void onFailure(int reasonCode) {
                Log.i(TAG, "onFailure: ");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Main2ActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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
