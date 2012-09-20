package com.ftechz.tools;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
    final String WIFI_EVENT_INTENT = WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION;
    final String BT_EVENT_INTENT = BluetoothAdapter.ACTION_STATE_CHANGED;

    public class EventInfo
    {
        String lastIntentString;
        int bluetoothState;
        boolean wifiConnected;
        boolean screenOn;
    }

    public EventInfo mEventInfo = new EventInfo();

    private BluetoothAdapter mBluetoothAdapter;
    private WifiManager mWifiManager;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            return;
        }

        if (mBluetoothAdapter.isEnabled()) {

        }

        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        // Get the initial event statuses
        mEventInfo.bluetoothState = mBluetoothAdapter.getState();
        mEventInfo.wifiConnected = mWifiManager.pingSupplicant();

        stateMachine.SyncState(mEventInfo);

        registerReceiver(displayActionReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(displayActionReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
        registerReceiver(btActionReceiver,
                new IntentFilter(BT_EVENT_INTENT));
        registerReceiver(wifiActionReceiver,
                new IntentFilter(WIFI_EVENT_INTENT));

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
                mEventInfo.wifiConnected = intent.getBooleanExtra(
                        WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);

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

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    /**
     * State Machine!
     */

    private BtAutomationStateMachine stateMachine = new BtAutomationStateMachine();
    private class BtAutomationStateMachine extends State<EventInfo>
    {
        public void SyncState(EventInfo eventInfo)
        {
            if ((eventInfo.bluetoothState == BluetoothAdapter.STATE_OFF)
                    || (eventInfo.bluetoothState == BluetoothAdapter.STATE_TURNING_ON) ) {
                this.ChangeState(this.inactiveState);
            }
            else {
                if (eventInfo.screenOn) {
                    switch (eventInfo.bluetoothState) {
                        case BluetoothAdapter.STATE_CONNECTED:
                            this.activeState.ChangeState(this.activeState.connectedState);
                            break;
                        case BluetoothAdapter.STATE_CONNECTING:
                            this.activeState.ChangeState(this.activeState.connectingState);
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                        case BluetoothAdapter.STATE_DISCONNECTED:
                        case BluetoothAdapter.STATE_DISCONNECTING:
                        case BluetoothAdapter.STATE_ON:
                            this.activeState.ChangeState(this.activeState.searchingState);
                            break;
                    }
                }
                else {
                    this.activeState.ChangeState(this.activeState.pendingDisconnectState);
                }

                this.ChangeState(this.activeState);
            }
        }

        @Override
        protected void EventHandler(EventInfo eventInfo) throws EventHandledException
        {

        }

        @Override
        protected void EnterState()
        {
            super.EnterState();
            this.ChangeState(inactiveState);
        }

        @Override
        protected void ExitState()
        {
            super.ExitState();
            this.ChangeState(null);
        }


        public InactiveState inactiveState = new InactiveState();
        public class InactiveState extends State<EventInfo>
        {
            @Override
            protected void EnterState()
            {
                super.EnterState();
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
                        | eventInfo.lastIntentString.equals(WIFI_EVENT_INTENT)) {
                    if ((eventInfo.bluetoothState == BluetoothAdapter.STATE_ON)
                            && !eventInfo.wifiConnected) {
                        BtAutomationStateMachine.this.ChangeState(
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
            protected void EnterState()
            {
                super.EnterState();
                this.ChangeState(searchingState);
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
                                    BtAutomationStateMachine.this.inactiveState);
                            this.EventHandled();
                    }
                }
            }

            /**
             * Searching State
             */
            private State searchingState = new SearchingState();

            private class SearchingState extends State<EventInfo>
            {
                @Override
                protected void EnterState()
                {
                    super.EnterState();
                    // Start process to look for paired device
                    // Start timer to max search pair time
                }

                @Override
                protected void ExitState()
                {
                    super.ExitState();
                    // Delete timer
                }

                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    // Move to unconnectedState using same conditions as pendingDisconnectState

                    // Transition to Connecting state
                    if (eventInfo.lastIntentString.equals(BT_EVENT_INTENT)) {
                        if (eventInfo.bluetoothState == BluetoothAdapter.STATE_CONNECTING) {
                            ActiveState.this.ChangeState(ActiveState.this.connectingState);
                        }
                    }
                }
            }

            /**
             * Connecting State
             */
            private State connectingState = new State<EventInfo>()
            {
                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (eventInfo.lastIntentString.equals(BT_EVENT_INTENT)) {
                        switch (eventInfo.bluetoothState) {
                            case BluetoothAdapter.STATE_CONNECTED:
                                // Transition to Connected state
                                ActiveState.this.ChangeState(ActiveState.this.connectedState);
                                break;
                            case BluetoothAdapter.STATE_DISCONNECTED:
                                ActiveState.this.ChangeState(ActiveState.this.searchingState);
                                break;
                        }
                    }

                    EventHandled();
                }
            };

            /**
             * Connected State
             */
            private State connectedState = new State<EventInfo>()
            {
                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (!eventInfo.screenOn) {
                        ActiveState.this.ChangeState(ActiveState.this.pendingDisconnectState);
                    }
                    else if (eventInfo.lastIntentString.equals(BT_EVENT_INTENT)) {
                        switch (eventInfo.bluetoothState) {
                            case BluetoothAdapter.STATE_CONNECTED:
                                ActiveState.this.ChangeState(ActiveState.this.connectedState);
                                break;
                            case BluetoothAdapter.STATE_DISCONNECTED:
                                ActiveState.this.ChangeState(ActiveState.this.connectedState);
                                break;
                        }
                    }

                    EventHandled();
                }
            };


            /**
             * Pending Disconnect State
             */
            private State pendingDisconnectState = new State<EventInfo>()
            {
                @Override
                protected void EnterState()
                {
                    super.EnterState();
                    // Create/Start timer
                }

                @Override
                protected void ExitState()
                {
                    super.ExitState();
                    // Delete timer
                }

                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (eventInfo.screenOn) {
                        // Screen turns on
                        ActiveState.this.ChangeState(ActiveState.this.connectedState);
                    }
                    else if (true) {
                        // If timer expires
                        ActiveState.this.ChangeState(ActiveState.this.unconnectedState);
                    }

                    EventHandled();
                }
            };


            /**
             * Unconnected State
             */
            private State unconnectedState = new State<EventInfo>()
            {
                @Override
                protected void EnterState()
                {
                    super.EnterState();
                    // Create/Start timer
                }

                @Override
                protected void ExitState()
                {
                    super.ExitState();
                    // Delete timer
                }

                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    if (eventInfo.screenOn) {
                        // If screen on or timer expires
                        ActiveState.this.ChangeState(ActiveState.this.searchingState);

                    }
                }
            };
        }
    }


}
