package com.ftechz.tools;

import android.app.Service;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import java.util.*;

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

    final long PENDING_OFF_DELAY = 60*4;
    final long PENDING_ON_DELAY = 60*2;
    final long SEARCHING_TIMEOUT_DELAY = 30;

    public class EventInfo
    {
        String lastIntentString;
        int bluetoothState;
        boolean wifiConnected;
        boolean screenOn;
    }

    public EventInfo mEventInfo = new EventInfo();

    private BluetoothAdapter mBluetoothAdapter;

    String deviceName = "HTC Vision";
    BluetoothDevice connectingDevice;

    @Override
    public void onCreate()
    {
        super.onCreate();
        try
        {
            Class.forName("android.bluetooth.BluetoothPan");
        }
        catch(ClassNotFoundException classnotfoundexception)
        {

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
        // Get bluetooth state
        mEventInfo.bluetoothState = mBluetoothAdapter.getState();
        if (mEventInfo.bluetoothState == BluetoothAdapter.STATE_ON) {
            mEventInfo.bluetoothState = mBluetoothAdapter.getProfileConnectionState(BT_PAN_PROFILE);
        }

        // Get wifi connected state
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        mEventInfo.wifiConnected = wifiNetworkInfo.isConnected();

        stateMachine.SyncState(mEventInfo);
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

    private BroadcastReceiver displayActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals( Intent.ACTION_SCREEN_ON)) {
                mEventInfo.screenOn = true;

            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                mEventInfo.screenOn = false;

            }

            mEventInfo.lastIntentString = intent.getAction();
            // Signal state machine
            stateMachine.HandleEvent(mEventInfo);
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
                stateMachine.HandleEvent(mEventInfo);
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
                stateMachine.HandleEvent(mEventInfo);
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
                stateMachine.HandleEvent(mEventInfo);
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
                stateMachine.HandleEvent(mEventInfo);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    BluetoothDevice findBtDevice(String deviceName)
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

    BluetoothProfile.ServiceListener btConnectServiceListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int i, BluetoothProfile bluetoothprofile)
        {
            try {
                Log.d(TAG, "Class: " + bluetoothprofile.getClass().getName());
                bluetoothprofile.getClass().getMethod(
                        "connect", new java.lang.Class[] { BluetoothDevice.class }).invoke(
                        bluetoothprofile, connectingDevice);
            }
            catch (Exception ex) {
                Log.d(TAG, ex.getMessage());
            }

            mBluetoothAdapter.closeProfileProxy(BT_PAN_PROFILE, bluetoothprofile);
        }

        public void onServiceDisconnected(int i)
        {

        }
    };


    BluetoothProfile.ServiceListener btdisconnectServiceListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int i, BluetoothProfile bluetoothprofile)
        {
            try {
                Log.d(TAG, "Class: " + bluetoothprofile.getClass().getName());
                bluetoothprofile.getClass().getMethod(
                        "disconnect", new java.lang.Class[] { BluetoothDevice.class }).invoke(
                        bluetoothprofile, connectingDevice);
            }
            catch (Exception ex) {
                Log.d("BtAutomationService", ex.getMessage());
            }

        }

        public void onServiceDisconnected(int i)
        {

        }
    };


    /**
     * State Machine!
     */

    private BtAutomationStateMachine stateMachine = new BtAutomationStateMachine();
    private class BtAutomationStateMachine extends State<EventInfo>
    {
        private static final String TAG = "BtAutomationService.StateMachine";

        public static final String TIMEOUT_EVENT =
                "com.ftechz.tools.BtAutomationService.BtAutomationStateMachine.TimeoutEvent";

        private Timer mEventTimer;

        public void StartTimer(long delay)
        {
            if (mEventTimer != null)
            {
                mEventTimer.cancel();
                Log.d(TAG, "Timer stopped");
            }

            mEventTimer = new Timer(true);
            mEventTimer.schedule(new TimeoutTask(), delay * 1000);

            Log.d(TAG, "Timer started");
        }

        public void StopTimer()
        {
            if (mEventTimer != null) {
                mEventTimer.cancel();
                mEventTimer = null;
                Log.d(TAG, "Timer stopped");
            }

        }

        private class TimeoutTask extends TimerTask
        {
            @Override
            public void run()
            {
                sendBroadcast(new Intent(TIMEOUT_EVENT));
            }
        }

        public void SyncState(EventInfo eventInfo)
        {
            if ((eventInfo.bluetoothState == BluetoothAdapter.STATE_OFF)
                    || (eventInfo.bluetoothState == BluetoothAdapter.STATE_TURNING_ON) ) {
                this.ChangeState(BtAutomationService.this, this.inactiveState);
            }
            else {
                if (eventInfo.screenOn) {
                    switch (eventInfo.bluetoothState) {
                        case BluetoothAdapter.STATE_CONNECTED:
                            this.activeState.ChangeState(BtAutomationService.this, this.activeState.connectedState);
                            break;
                        case BluetoothAdapter.STATE_CONNECTING:
                            this.activeState.ChangeState(BtAutomationService.this, this.activeState.connectingState);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_DISCONNECTED:
                        case BluetoothAdapter.STATE_DISCONNECTING:
                        case BluetoothAdapter.STATE_ON:
                            this.activeState.ChangeState(BtAutomationService.this, this.activeState.searchingState);
                            break;
                    }
                }
                else {
                    this.activeState.ChangeState(BtAutomationService.this, this.activeState.pendingDisconnectState);
                }

                this.ChangeState(BtAutomationService.this, this.activeState);
            }
        }

        @Override
        protected void EventHandler(EventInfo eventInfo) throws EventHandledException
        {

        }

        @Override
        protected void EnterState(Context context)
        {
            super.EnterState(context);
            this.ChangeState(context, inactiveState);
        }

        @Override
        protected void ExitState()
        {
            super.ExitState();
            this.ChangeState(BtAutomationService.this, null);
        }


        public InactiveState inactiveState = new InactiveState();
        public class InactiveState extends State<EventInfo>
        {
            @Override
            protected void EnterState(Context context)
            {
                super.EnterState(context);
            }

            @Override
            protected void ExitState()
            {
                super.ExitState();
            }

            @Override
            protected void EventHandler(EventInfo eventInfo) throws EventHandledException
            {
                // Transition to active state
                if (eventInfo.lastIntentString.equals(BT_EVENT_INTENT)
                        || eventInfo.lastIntentString.equals(WIFI_EVENT_INTENT)) {
                    if ((eventInfo.bluetoothState != BluetoothAdapter.STATE_OFF)
                            && !eventInfo.wifiConnected) {
                        BtAutomationStateMachine.this.ChangeState(BtAutomationService.this,
                                BtAutomationStateMachine.this.activeState);
                        this.EventHandled();
                    }
                }
            }
        }

        public ActiveState activeState = new ActiveState();
        public class ActiveState extends State<EventInfo>
        {
            @Override
            protected void EnterState(Context context)
            {
                super.EnterState(context);
                this.ChangeState(context, searchingState);
            }

            @Override
            protected void EventHandler(EventInfo eventInfo) throws EventHandledException
            {
                // Transition to inactive state
                if (eventInfo.lastIntentString.equals(BT_EVENT_INTENT)
                        | eventInfo.lastIntentString.equals(WIFI_EVENT_INTENT)) {
                    if ((eventInfo.bluetoothState == BluetoothAdapter.STATE_OFF)
                            || eventInfo.wifiConnected) {
                            BtAutomationStateMachine.this.ChangeState(
                                    BtAutomationService.this,
                                    BtAutomationStateMachine.this.inactiveState);
                            this.EventHandled();
                    }
                }
            }

            /**
             * Searching State
             */
            private State<EventInfo> searchingState = new SearchingState();

            private class SearchingState extends State<EventInfo>
            {
                @Override
                protected void EnterState(Context context)
                {
                    super.EnterState(context);

                    BluetoothDevice device = findBtDevice(deviceName);
                    if (device != null) {
                        connectingDevice = device;
                        mBluetoothAdapter.getProfileProxy(BtAutomationService.this,
                                btConnectServiceListener, BT_PAN_PROFILE);
                    }

                    // Start process to look for paired device
                    // Start timer to max search pair time
                    StartTimer(SEARCHING_TIMEOUT_DELAY);
                }

                @Override
                protected void ExitState()
                {
                    super.ExitState();
                    // Delete timer
                    StopTimer();
                }

                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    // Move to unconnectedState using same conditions as pendingDisconnectState

                    // Transition to Connecting state
                    if (eventInfo.lastIntentString.equals(ACTION_CONNECTION_STATE_CHANGED)) {
//                        ActiveState.this.ChangeState(ActiveState.this.connectingState);
                        if (mEventInfo.bluetoothState == BluetoothAdapter.STATE_CONNECTED) {

                            if (mEventInfo.screenOn) {
                                ActiveState.this.ChangeState(BtAutomationService.this,
                                        ActiveState.this.connectedState);
                            }
                            else {
                                ActiveState.this.ChangeState(BtAutomationService.this,
                                        ActiveState.this.pendingDisconnectState);
                            }
                        }
                    }
                    else if (eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                        ActiveState.this.ChangeState(BtAutomationService.this,
                                ActiveState.this.unconnectedState);
                    }
                }
            }

            /**
             * Connecting State
             */
            private State<EventInfo> connectingState = new ConnectingState();

            private class ConnectingState extends State<EventInfo>
            {
                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    // Doesn't work - need to check profile/device state
                    if (eventInfo.lastIntentString.equals(BT_EVENT_INTENT)) {
                        switch (eventInfo.bluetoothState) {
                            case BluetoothAdapter.STATE_CONNECTED:
                                // Transition to Connected state
                                ActiveState.this.ChangeState(BtAutomationService.this,
                                        ActiveState.this.connectedState);
                                break;
                            case BluetoothAdapter.STATE_DISCONNECTED:
                                ActiveState.this.ChangeState(BtAutomationService.this,
                                        ActiveState.this.searchingState);
                                break;
                        }
                    }

                    EventHandled();
                }
            }

            /**
             * Connected State
             */
            private State<EventInfo> connectedState =  new ConnectedState();

            private class ConnectedState extends State<EventInfo>
            {
                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (!eventInfo.screenOn) {
                        ActiveState.this.ChangeState(BtAutomationService.this,
                                ActiveState.this.pendingDisconnectState);
                    }
                    else if  (eventInfo.lastIntentString.equals(ACTION_CONNECTION_STATE_CHANGED)) {
                        if (mEventInfo.bluetoothState == BluetoothAdapter.STATE_DISCONNECTED) {
//                    else if (eventInfo.lastIntentString.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
//                        ActiveState.this.ChangeState(ActiveState.this.connectingState);
                            ActiveState.this.ChangeState(BtAutomationService.this,
                                    ActiveState.this.searchingState);
                        }
                    }

                    EventHandled();
                }
            }


            /**
             * Pending Disconnect State
             */
            private State<EventInfo> pendingDisconnectState =  new pendingDisconnectState();

            private class pendingDisconnectState extends State<EventInfo>
            {
                @Override
                protected void EnterState(Context context)
                {
                    super.EnterState(context);
                    // Create/Start timer
                    StartTimer(PENDING_OFF_DELAY);
                }

                @Override
                protected void ExitState()
                {
                    super.ExitState();
                    // Delete timer
                    StopTimer();
                }

                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (eventInfo.screenOn) {
                        // Screen turns on
                        ActiveState.this.ChangeState(BtAutomationService.this,
                                ActiveState.this.connectedState);
                    }
                    else if (eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                        // If timer expires
                        ActiveState.this.ChangeState(BtAutomationService.this,
                                ActiveState.this.unconnectedState);
                    }

                    EventHandled();
                }
            }


            /**
             * Unconnected State
             */
            private State<EventInfo> unconnectedState =  new UnconnectedState();

            private class UnconnectedState extends State<EventInfo>
            {
                @Override
                protected void EnterState(Context context)
                {
                    super.EnterState(context);

                    BluetoothDevice device = findBtDevice(deviceName);
                    if (device != null) {
                        connectingDevice = device;
                        mBluetoothAdapter.getProfileProxy(BtAutomationService.this,
                                btdisconnectServiceListener, BT_PAN_PROFILE);
                    }

                    // Create/Start timer
                    StartTimer(PENDING_ON_DELAY);
                }

                @Override
                protected void ExitState()
                {
                    super.ExitState();
                    // Delete timer
                    StopTimer();
                }

                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (eventInfo.screenOn || eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                        // If screen on or timer expires
                        ActiveState.this.ChangeState(BtAutomationService.this,
                                ActiveState.this.searchingState);
                    }
                }
            }
        }
    }
}
