package com.ftechz.tools;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;

/**
 * Created with IntelliJ IDEA.
 * User: root
 * Date: 12/09/12
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class BtAutomationService extends Service
{
    private static final String TAG = "BtAutomationService";

    final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
    final String ACTION_CONNECTION_STATE = BluetoothProfile.EXTRA_STATE;
    final String WIFI_EVENT_INTENT = WifiManager.NETWORK_STATE_CHANGED_ACTION;
    final String BT_EVENT_INTENT = BluetoothAdapter.ACTION_STATE_CHANGED;
    final int BT_PAN_PROFILE = 5;

    BtAutomationStateMachine mBtAutomationStateMachine;
    public EventInfo mEventInfo = new EventInfo();

    private BluetoothAdapter mBluetoothAdapter;

    @Override
    public void onCreate()
    {
        super.onCreate();
        try {
            Class.forName("android.bluetooth.BluetoothPan");
        } catch (ClassNotFoundException classnotfoundexception) {

        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            return;
        }

        initialiseStateMachine();

        registerEventHandlers();

    }

    /**
     * Initialise the statemachine to the correct state
     */
    private void initialiseStateMachine()
    {
        mBtAutomationStateMachine = new BtAutomationStateMachine(this, mEventInfo);

        // Get bluetooth state
        mEventInfo.bluetoothState = mBluetoothAdapter.getState();
        if (mEventInfo.bluetoothState == BluetoothAdapter.STATE_ON) {
            mEventInfo.bluetoothState = mBluetoothAdapter.getProfileConnectionState(BT_PAN_PROFILE);
        }

        // Get wifi connected state
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        mEventInfo.wifiConnected = wifiNetworkInfo.isConnected();

        mBtAutomationStateMachine.SyncState(mEventInfo);
    }

    private void registerEventHandlers()
    {
        registerReceiver(displayActionReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(displayActionReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
        registerReceiver(btActionReceiver,
                new IntentFilter(BT_EVENT_INTENT));
        registerReceiver(wifiActionReceiver,
                new IntentFilter(WIFI_EVENT_INTENT));
        registerReceiver(timerEventReceiver,
                new IntentFilter(BtAutomationStateMachine.TIMEOUT_EVENT));

        registerReceiver(btDeviceActionReceiver,
                new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));
        registerReceiver(btDeviceActionReceiver,
                new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED));

        registerReceiver(btDeviceActionReceiver,
                new IntentFilter(ACTION_CONNECTION_STATE_CHANGED));
    }


    /*
     * Broadcast receivers to forward events to the statemachine
     */
    private BroadcastReceiver displayActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                mEventInfo.screenOn = true;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mEventInfo.screenOn = false;
            }

            mEventInfo.lastIntentString = intent.getAction();
            // Signal state machine
            mBtAutomationStateMachine.HandleEvent(mEventInfo);
        }
    };

    private BroadcastReceiver wifiActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(WIFI_EVENT_INTENT)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                mEventInfo.wifiConnected = networkInfo.isConnected();
                mEventInfo.lastIntentString = intent.getAction();

                // Signal state machine
                mBtAutomationStateMachine.HandleEvent(mEventInfo);
            }
        }
    };

    private BroadcastReceiver btActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(BT_EVENT_INTENT)) {
                mEventInfo.bluetoothState = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                mEventInfo.lastIntentString = intent.getAction();

                // Signal state machine
                mBtAutomationStateMachine.HandleEvent(mEventInfo);
            }
        }
    };

    private BroadcastReceiver btDeviceActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(ACTION_CONNECTION_STATE_CHANGED)) {
                mEventInfo.bluetoothState = intent.getIntExtra(
                        ACTION_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED);
                mEventInfo.lastIntentString = intent.getAction();

                // Signal state machine
                mBtAutomationStateMachine.HandleEvent(mEventInfo);
            }
        }
    };

    private BroadcastReceiver timerEventReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(BtAutomationStateMachine.TIMEOUT_EVENT)) {
                mEventInfo.lastIntentString = intent.getAction();

                // Signal state machine
                mBtAutomationStateMachine.HandleEvent(mEventInfo);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
