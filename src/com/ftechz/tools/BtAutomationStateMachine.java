package com.ftechz.tools;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * State machine for the BT automation
 *
 * This implements an HFSM (Heirachical Finite State Machine)
 */
public class BtAutomationStateMachine
{
    private static final String TAG = "BtAutomationStateMachine";

    public static final String TIMEOUT_EVENT =
            "com.ftechz.tools.BtAutomationService.BtAutomationStateMachine.TimeoutEvent";
    final String WIFI_EVENT_INTENT = WifiManager.NETWORK_STATE_CHANGED_ACTION;

    final long BT_DISCONNECTED_PERIOD = 40;
    final long BT_CONNECTED_PERIOD = 20;
    final long BT_SCREEN_OFF_DELAY = 20;
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
            topLevelState.ChangeState(mContext, topLevelState.disabledState);
        }
        else {
            if (eventInfo.screenOn) {
                topLevelState.enabledState.ChangeState(mContext,
                        topLevelState.enabledState.screenOnState);
            }
            else {
                topLevelState.enabledState.ChangeState(mContext,
                        topLevelState.enabledState.screenOffState);
            }

            topLevelState.ChangeState(mContext, topLevelState.enabledState);
        }
    }

    /*****************
     * Top level state
     *****************/
    private TopLevelState topLevelState = new TopLevelState();
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
            this.ChangeState(context, disabledState);
        }

        @Override
        protected void ExitState()
        {
            super.ExitState();
            this.ChangeState(mContext, null);
        }


        /*****************
         * Disabled State
         *****************/
        public DisabledState disabledState = new DisabledState();
        public class DisabledState extends State<EventInfo>
        {
            @Override
            protected void EnterState(Context context)
            {
                super.EnterState(context);
                mBtManager.Disconnect(deviceName);
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
                                TopLevelState.this.enabledState);
                        this.EventHandled();
                    }
                }
            }
        }


        /***************
         * Enabled State
         ***************/
        public EnabledState enabledState = new EnabledState();
        public class EnabledState extends State<EventInfo>
        {
            @Override
            protected void EnterState(Context context)
            {
                super.EnterState(context);
                this.ChangeState(context, screenOnState);
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
                                TopLevelState.this.disabledState);
                        this.EventHandled();
                    }
                }
            }

            /******************
             * Screen On State
             ******************/
            private State<EventInfo> screenOnState = new ScreenOnState();
            private class ScreenOnState extends State<EventInfo>
            {
                @Override
                protected void EnterState(Context context)
                {
                    super.EnterState(context);
                    if (!mBtManager.IsConnected()) {
                        mBtManager.Connect(deviceName);
                    }
                }


                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (!eventInfo.screenOn) {
                        EnabledState.this.ChangeState(mContext,
                                EnabledState.this.screenOffState);
                        EventHandled();
                    }
                }
            }

            /******************
             * Screen Off State
             ******************/
            private State<EventInfo> screenOffState = new ScreenOffState();
            private class ScreenOffState extends State<EventInfo>
            {
                @Override
                protected void EnterState(Context context)
                {
                    super.EnterState(context);
                    this.ChangeState(context, screenOffDelayState);
                }


                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (eventInfo.screenOn) {
                        EnabledState.this.ChangeState(mContext,
                                EnabledState.this.screenOnState);
                        EventHandled();
                    }
                }

                /******************
                 * Screen Off Delay State
                 ******************/
                private State<EventInfo> screenOffDelayState = new ScreenOffDelayState();
                private class ScreenOffDelayState extends State<EventInfo>
                {
                    @Override
                    protected void EnterState(Context context)
                    {
                        super.EnterState(context);

                        // Create/Start timer
                        StartTimer(BT_SCREEN_OFF_DELAY);
                    }

                    @Override
                    protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                    {
                        if (eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                            // If timer expires
                            ScreenOffState.this.ChangeState(mContext,
                                    ScreenOffState.this.btDisconnectedState);
                        }
                    }

                    @Override
                    protected void ExitState() {
                        super.ExitState();
                        StopTimer();
                    }
                }

                /******************
                 * Bluetooth Disconnected State
                 ******************/
                private State<EventInfo> btDisconnectedState = new BtDisconnectedState();
                private class BtDisconnectedState extends State<EventInfo>
                {
                    @Override
                    protected void EnterState(Context context)
                    {
                        super.EnterState(context);

                        // Create/Start timer
                        StartTimer(BT_DISCONNECTED_PERIOD);

                        if (mBtManager.IsConnected()) {
                            mBtManager.Disconnect(deviceName);
                        }
                    }

                    @Override
                    protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                    {
                        if (eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                            // If timer expires
                            ScreenOffState.this.ChangeState(mContext,
                                    ScreenOffState.this.btConnectedState);
                        }
                    }

                    @Override
                    protected void ExitState() {
                        super.ExitState();
                        StopTimer();
                    }
                }

                /******************
                 * Bluetooth Connected State
                 ******************/
                private State<EventInfo> btConnectedState = new BtConnectedState();
                private class BtConnectedState extends State<EventInfo>
                {
                    @Override
                    protected void EnterState(Context context)
                    {
                        super.EnterState(context);

                        // Create/Start timer
                        StartTimer(BT_CONNECTED_PERIOD);

                        if (!mBtManager.IsConnected()) {
                            mBtManager.Connect(deviceName);
                        }
                    }

                    @Override
                    protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                    {
                        if (eventInfo.lastIntentString.equals(TIMEOUT_EVENT)) {
                            // If timer expires
                            ScreenOffState.this.ChangeState(mContext,
                                    ScreenOffState.this.btDisconnectedState);
                        }
                    }

                    @Override
                    protected void ExitState() {
                        super.ExitState();
                        StopTimer();
                    }
                }
            }
        }
    }
}
