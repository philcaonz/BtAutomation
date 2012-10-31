package com.ftechz.tools;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Binder;
import android.os.IBinder;

/**
 * Created with IntelliJ IDEA.
 * User: Phil
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

    final int RUNNING_NOTIFICATION_ID = 1;

    private final IBinder mBinder = new BtAutomationServiceBinder();

    BtAutomationStateMachine mBtAutomationStateMachine;
    public EventInfo mEventInfo = new EventInfo();

    private BluetoothAdapter mBluetoothAdapter;

    Notification.Builder mNotificationBuilder;
    NotificationManager mNotificationManager;

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

        mNotificationManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        showNotification();
    }

    /**
     * Initialise the statemachine to the correct state
     */
    private void initialiseStateMachine()
    {
        mEventInfo.enabled = true; // Always true for now

        // Get bluetooth state
        mEventInfo.bluetoothState = mBluetoothAdapter.getState();
        if (mEventInfo.bluetoothState == BluetoothAdapter.STATE_ON) {
            mEventInfo.bluetoothState = mBluetoothAdapter.getProfileConnectionState(BT_PAN_PROFILE);
        }

        // Get wifi connected state
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        mEventInfo.wifiConnected = wifiNetworkInfo.isConnected();

        mBtAutomationStateMachine = new BtAutomationStateMachine(this, mEventInfo);
        mBtAutomationStateMachine.SyncState(mEventInfo);
    }

    private void registerEventHandlers()
    {
        registerReceiver(stateEnterEventReceiver,
                new IntentFilter(State.ENTER_STATE_EVENT));

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
        return mBinder;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        hideNotification();
    }

    public void showNotification()
    {
        mNotificationBuilder = new Notification.Builder(this);
        mNotificationBuilder.setOngoing(true);
        mNotificationBuilder.setSmallIcon(R.drawable.play);
        mNotificationBuilder.setWhen(0);

        Intent notificationIntent = new Intent(this, BtAutomation.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        mNotificationBuilder.addAction(R.drawable.play, "Open", contentIntent);
        mNotificationBuilder.setContentIntent(contentIntent);
        mNotificationBuilder.setPriority(Notification.PRIORITY_HIGH);

        updateNotification("");
        this.startForeground(RUNNING_NOTIFICATION_ID, mNotificationBuilder.build());
    }

    public void updateNotification(String text)
    {
        if (mNotificationBuilder == null) {
            return;
        }

        mNotificationBuilder.setContentTitle("Bt Automation active");
        mNotificationBuilder.setContentText(text);

        mNotificationManager.notify(RUNNING_NOTIFICATION_ID, mNotificationBuilder.build());
    }

    public void hideNotification()
    {
        this.stopForeground(true);
    }


    private BroadcastReceiver stateEnterEventReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            updateNotification(intent.getStringExtra(State.ENTER_STATE_EVENT_EXTRA));
        }
    };

    public class BtAutomationServiceBinder extends Binder
    {

    }
}
