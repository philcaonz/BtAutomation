package com.ftechz.tools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: Phil
 * Date: 9/10/12
 * Time: 10:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class BtAutomationStateMachine
{
    private static final String TAG = "BtAutomationStateMachine";

    public static final String TIMEOUT_EVENT =
            "com.ftechz.tools.BtAutomationService.BtAutomationStateMachine.TimeoutEvent";
    final String ACTION_CONNECTION_STATE_CHANGED = "android.bluetooth.pan.profile.action.CONNECTION_STATE_CHANGED";
    final String ACTION_CONNECTION_STATE = BluetoothProfile.EXTRA_STATE;
    final String WIFI_EVENT_INTENT = WifiManager.NETWORK_STATE_CHANGED_ACTION;
    final String BT_EVENT_INTENT = BluetoothAdapter.ACTION_STATE_CHANGED;

    final long PENDING_OFF_DELAY = 60 * 4;
    final long PENDING_ON_DELAY = 60 * 2;
    final long SEARCHING_TIMEOUT_DELAY = 30;

    private Context mContext;
    public EventInfo mEventInfo;

    String deviceName = "HTC Vision";
    BtManager mBtManager;

    public BtAutomationStateMachine(Context context, EventInfo eventInfo)
    {
        mContext = context;
        mEventInfo = eventInfo;
        mBtManager = new BtManager(context);
    }

    public void HandleEvent(EventInfo eventInfo)
    {
        topLevelState.HandleEvent(eventInfo);
    }

    public void SyncState(EventInfo eventInfo)
    {
        if ((eventInfo.bluetoothState == BluetoothAdapter.STATE_OFF)
                || (eventInfo.bluetoothState == BluetoothAdapter.STATE_TURNING_ON)) {
            topLevelState.ChangeState(mContext, topLevelState.inactiveState);
        } else {
            if (eventInfo.screenOn) {
                switch (eventInfo.bluetoothState) {
                    case BluetoothAdapter.STATE_CONNECTED:
                        topLevelState.activeState.ChangeState(mContext,
                                topLevelState.activeState.connectedState);
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                    case BluetoothAdapter.STATE_DISCONNECTED:
                    case BluetoothAdapter.STATE_DISCONNECTING:
                    case BluetoothAdapter.STATE_ON:
                        topLevelState.activeState.ChangeState(mContext,
                                topLevelState.activeState.searchingState);
                        break;
                }
            } else {
                topLevelState.activeState.ChangeState(mContext,
                        topLevelState.activeState.pendingDisconnectState);
            }

            topLevelState.ChangeState(mContext, topLevelState.activeState);
        }
    }


    private TopLevelState topLevelState = new TopLevelState();

    /*****************
     * Top level state
     *****************/
    private class TopLevelState extends State<EventInfo>
    {
        private static final String TAG = "BtAutomationStateMachine.TopLevelState";

        private Timer mEventTimer;

        public void StartTimer(long delay)
        {
            if (mEventTimer != null) {
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
                mContext.sendBroadcast(new Intent(TIMEOUT_EVENT));
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
            this.ChangeState(mContext, null);
        }


        /*****************
         * Inactive State
         *****************/
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
                // TODO Add enable/disable intent
                if (eventInfo.lastIntentString.equals(WIFI_EVENT_INTENT)) {
                    if ((eventInfo.enabled) && !eventInfo.wifiConnected) {
                        TopLevelState.this.ChangeState(mContext,
                                TopLevelState.this.activeState);
                        this.EventHandled();
                    }
                }
            }
        }


        /***************
         * Active State
         ***************/
        public ActiveState activeState = new ActiveState();

        public class ActiveState extends State<EventInfo>
        {
            @Override
            protected void EnterState(Context context)
            {
                super.EnterState(context);
                if (mBtManager.IsAdaptorEnabled()) {
                    this.ChangeState(context, searchingState);
                }
                else {
                    this.ChangeState(context, btEnablingState);
                }
            }

            @Override
            protected void EventHandler(EventInfo eventInfo) throws EventHandledException
            {
                // Transition to inactive state
                // TODO Add enable/disable intent
                if (eventInfo.lastIntentString.equals(WIFI_EVENT_INTENT)) {
                    if ((!eventInfo.enabled) || eventInfo.wifiConnected) {
                        TopLevelState.this.ChangeState(
                                mContext,
                                TopLevelState.this.inactiveState);
                        this.EventHandled();
                    }
                }
            }


            /****************************
             * Bluetooth Enabling State
             ****************************/
            private State<EventInfo> btEnablingState = new BtEnablingState();

            private class BtEnablingState extends State<EventInfo>
            {
                @Override
                protected void EnterState(Context context)
                {
                    super.EnterState(context);

                    mBtManager.EnableAdaptor();

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
                    // Transition to Connecting state
                    if (eventInfo.lastIntentString.equals(BT_EVENT_INTENT)) {
                        if (mEventInfo.bluetoothState == BluetoothAdapter.STATE_ON) {
                            ActiveState.this.ChangeState(mContext,
                                    ActiveState.this.searchingState);
                        }
                    } else if (eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                        // Try again on timeout
                        ActiveState.this.ChangeState(mContext,
                                ActiveState.this.btEnablingState);
                    }
                }
            }

            /******************
             * Searching State
             ******************/
            private State<EventInfo> searchingState = new SearchingState();

            private class SearchingState extends State<EventInfo>
            {
                @Override
                protected void EnterState(Context context)
                {
                    super.EnterState(context);

                    mBtManager.ConnectToDevice(deviceName);

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
                        if (mEventInfo.bluetoothState == BluetoothAdapter.STATE_CONNECTED) {

                            if (mEventInfo.screenOn) {
                                ActiveState.this.ChangeState(mContext,
                                        ActiveState.this.connectedState);
                            } else {
                                ActiveState.this.ChangeState(mContext,
                                        ActiveState.this.pendingDisconnectState);
                            }
                        }
                    } else if (eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                        ActiveState.this.ChangeState(mContext,
                                ActiveState.this.unconnectedState);
                    }
                }
            }

            /******************
             * Connected State
             ******************/
            private State<EventInfo> connectedState = new ConnectedState();

            private class ConnectedState extends State<EventInfo>
            {
                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (!eventInfo.screenOn) {
                        ActiveState.this.ChangeState(mContext,
                                ActiveState.this.pendingDisconnectState);
                    } else if (eventInfo.lastIntentString.equals(ACTION_CONNECTION_STATE_CHANGED)) {
                        if (mEventInfo.bluetoothState == BluetoothAdapter.STATE_DISCONNECTED) {
                            ActiveState.this.ChangeState(mContext,
                                    ActiveState.this.searchingState);
                        }
                    }

                    EventHandled();
                }
            }


            /***************************
             * Pending Disconnect State
             ***************************/
            private State<EventInfo> pendingDisconnectState = new pendingDisconnectState();

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
                        ActiveState.this.ChangeState(mContext,
                                ActiveState.this.connectedState);
                    } else if (eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                        // If timer expires
                        ActiveState.this.ChangeState(mContext,
                                ActiveState.this.unconnectedState);
                    }

                    EventHandled();
                }
            }


            /********************
             * Unconnected State
             ********************/
            private State<EventInfo> unconnectedState = new UnconnectedState();

            private class UnconnectedState extends State<EventInfo>
            {
                @Override
                protected void EnterState(Context context)
                {
                    super.EnterState(context);

                    mBtManager.DisableAdaptor();

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
                        ActiveState.this.ChangeState(mContext,
                                ActiveState.this.btEnablingState);
                    }
                }
            }
        }
    }
}
