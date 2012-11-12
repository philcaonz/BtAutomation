package com.ftechz.tools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: phillip
 * Date: 12/11/12
 * Time: 12:57 AM
 * To change this template use File | Settings | File Templates.
 */
public class BtManager {
    private static final String TAG = "BtAutomation.BtManager";

    // Intent broadcast actions
    final String PAN_ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
    final String BT_ACTION_CONNECTION_STATE = BluetoothProfile.EXTRA_STATE;
    final String BT_STATE_CHANGED = BluetoothAdapter.ACTION_STATE_CHANGED;

    public static final String ACTION_CONNECTION_STATE_CHANGED = "com.ftechz.tools.BtManager.action.CONNECTION_STATE_CHANGED";
    public static final String EXTRA_STATE = "com.ftechz.tools.BtManager.extra.STATE";
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTED = 1;


    private Context mContext;
    private boolean mDisableOnDisconnect = true;
    private String mConnectDeviceName = "";
    private boolean mConnectEnable = false;

    private int mConnectionState = STATE_DISCONNECTED;

    private BtHelper mBtHelper;

    public BtManager(Context context)
    {
        mContext = context;
        mBtHelper = new BtHelper(context);
        registerEventHandlers(context);
    }

    public void Connect(String deviceName)
    {
        mConnectDeviceName = deviceName;
        mConnectEnable = true;

        if (mDisableOnDisconnect) {
            if (!mBtHelper.IsAdaptorEnabled()) {
                mBtHelper.EnableAdaptor();
            }
            else {
                mBtHelper.ConnectToDevice(deviceName);
            }
        }
        else {

        }
    }

    public void Disconnect(String deviceName)
    {
        if (mDisableOnDisconnect) {
            mBtHelper.DisableAdaptor();
        }
        else {
            if (mBtHelper.IsConnectedToDevice(deviceName)) {
                mBtHelper.DisconnectFromDevice(deviceName);
            }
        }
    }

    public boolean IsConnected()
    {
        return mBtHelper.IsConnectedToDevice("");
    }

    private void registerEventHandlers(Context context)
    {
        context.registerReceiver(btAdaptorActionReceiver,
                new IntentFilter(BT_STATE_CHANGED));

        context.registerReceiver(btDeviceActionReceiver,
                new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        context.registerReceiver(btDeviceActionReceiver,
                new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));
        context.registerReceiver(btDeviceActionReceiver,
                new IntentFilter(PAN_ACTION_CONNECTION_STATE_CHANGED));
    }

    private BroadcastReceiver btAdaptorActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.STATE_OFF);

            // Once bluetooth adaptor turns on, connect to device
            if (state == BluetoothAdapter.STATE_ON) {
                mBtHelper.ConnectToDevice(mConnectDeviceName);
            }
        }
    };

    private BroadcastReceiver btDeviceActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            int state = intent.getIntExtra(BT_ACTION_CONNECTION_STATE,
                    BluetoothAdapter.STATE_DISCONNECTED);

            if (state == BluetoothAdapter.STATE_CONNECTED) {
                BroadcastConnectionStateChanged(STATE_CONNECTED);
            }
            else if(state == BluetoothAdapter.STATE_DISCONNECTED) {
                BroadcastConnectionStateChanged(STATE_DISCONNECTED);
            }
        }
    };

    private void BroadcastConnectionStateChanged(int connectionState) {
        if (mConnectionState != connectionState) {
            mConnectionState = connectionState;
            Log.d(TAG, "Broadcast connection state change: " + connectionState);
            Intent intent = new Intent(ACTION_CONNECTION_STATE_CHANGED);
            intent.putExtra(EXTRA_STATE, connectionState);
            mContext.sendBroadcast(intent);
        }
    }
}
