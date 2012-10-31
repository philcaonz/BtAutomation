package com.ftechz.tools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: Phil
 * Date: 31/10/12
 * Time: 4:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class BtManager {
    private static final String TAG = "BtAutomation.BtManager";

    final int BT_PAN_PROFILE = 5;

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice;

    public BtManager(Context context)
    {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public boolean EnableAdaptor()
    {
        if (!mBluetoothAdapter.isEnabled()) {
            return mBluetoothAdapter.enable();
        }
        else {
            return false;
        }
    }

    public boolean DisableAdaptor()
    {
        if (mBluetoothAdapter.isEnabled()) {
            return mBluetoothAdapter.disable();
        }
        else {
            return false;
        }
    }

    public boolean IsAdaptorEnabled()
    {
        return mBluetoothAdapter.isEnabled();
    }

    public void ConnectToDevice(String deviceName)
    {
        BluetoothDevice device = findBtDevice(deviceName);
        if (device != null) {
            mDevice = device;
            mBluetoothAdapter.getProfileProxy(mContext,
                    btConnectServiceListener, BT_PAN_PROFILE);
        }
    }

    public void DisconnectFromDevice(String deviceName)
    {
        BluetoothDevice device = findBtDevice(deviceName);
        if (device != null) {
            mDevice = device;
            mBluetoothAdapter.getProfileProxy(mContext,
                    btdisconnectServiceListener, BT_PAN_PROFILE);
        }
    }

    private BluetoothDevice findBtDevice(String deviceName)
    {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : devices) {
            Log.d(TAG, "Pair device: " + device.getName() + " " + device.getAddress());
            if (device.getName().equals(deviceName)) {
                return device;
            }
        }

        return null;
    }

    BluetoothProfile.ServiceListener btConnectServiceListener = new BluetoothProfile.ServiceListener()
    {
        public void onServiceConnected(int i, BluetoothProfile bluetoothprofile)
        {
            try {
                Log.d(TAG, "Class: " + bluetoothprofile.getClass().getName() + " connect");
                Method connectMethod = bluetoothprofile.getClass().getMethod(
                        "connect", new java.lang.Class[]{BluetoothDevice.class});

                if ((Boolean) connectMethod.invoke(bluetoothprofile, mDevice)) {
                    Log.d(TAG, "Connect successful");
                } else {
                    Log.d(TAG, "Connect failed");
                }

            } catch (Exception ex) {
                Log.d(TAG, ex.getMessage());
            }

            mBluetoothAdapter.closeProfileProxy(BT_PAN_PROFILE, bluetoothprofile);
        }

        public void onServiceDisconnected(int i)
        {

        }
    };


    BluetoothProfile.ServiceListener btdisconnectServiceListener = new BluetoothProfile.ServiceListener()
    {
        public void onServiceConnected(int i, BluetoothProfile bluetoothprofile)
        {
            try {
                Log.d(TAG, "Class: " + bluetoothprofile.getClass().getName() + " disconnect");
                Method disconnectMethod = bluetoothprofile.getClass().getMethod(
                        "disconnect", new java.lang.Class[]{BluetoothDevice.class});

                if ((Boolean) disconnectMethod.invoke(bluetoothprofile, mDevice)) {
                    Log.d(TAG, "disconnect successful");
                } else {
                    Log.d(TAG, "disconnect failed");
                }

            } catch (Exception ex) {
                Log.d("BtAutomationService", ex.getMessage());
            }

            mBluetoothAdapter.closeProfileProxy(BT_PAN_PROFILE, bluetoothprofile);
        }

        public void onServiceDisconnected(int i)
        {

        }
    };
}
