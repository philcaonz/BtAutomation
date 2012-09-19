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

    public class EventInfo
    {
        String lastIntentString;
        int bluetoothState;
        boolean wifiConnected;
        boolean screenOn;
    }

    public EventInfo eventInfo = new EventInfo();

    BluetoothAdapter mBluetoothAdapter;

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

        // Initial state
        registerReceiver(displayActionReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(displayActionReceiver,
                new IntentFilter(Intent.ACTION_SCREEN_OFF));
        registerReceiver(btActionReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(wifiActionReceiver,
                new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));

    }

    private BroadcastReceiver displayActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals( Intent.ACTION_SCREEN_ON)) {
                eventInfo.screenOn = true;

            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                eventInfo.screenOn = false;

            }

            // TODO Signal event
        }
    };

    private BroadcastReceiver wifiActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(
                    WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                eventInfo.wifiConnected = intent.getBooleanExtra(
                        WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);

                // TODO Signal event
            }
        }
    };

    private BroadcastReceiver btActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(
                    BluetoothAdapter.ACTION_STATE_CHANGED)) {
                eventInfo.bluetoothState = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
            }

            // TODO Signal event
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
        @Override
        protected void EventHandler(EventInfo info) throws EventHandledException {

        }

        @Override
        protected void EnterState() {
            super.EnterState();
        }

        @Override
        protected void ExitState() {
            super.ExitState();
        }


        private InactiveState inactiveState = new InactiveState();
        private class InactiveState extends State<EventInfo>
        {
            @Override
            protected void EnterState() {
                super.EnterState();
            }

            @Override
            protected void ExitState() {
                super.ExitState();
            }

            @Override
            protected void EventHandler(EventInfo info) throws EventHandledException {
                
            }
        }



        private ActiveState activeState = new ActiveState();
        private class ActiveState extends State<EventInfo>
        {
            @Override
            protected void EnterState()
            {
                this.ChangeState(searchingState);
            }

            @Override
            protected void EventHandler(EventInfo eventInfo)
            {

            }

            /**
             * Searching State
             */
            private State searchingState = new SearchingState();

            private class SearchingState extends State<EventInfo>
            {
                @Override
                protected void EnterState() {
                    // Start process to look for paired device
                    // Start timer to max search pair time
                }

                @Override
                protected void ExitState() {
                    // Delete timer
                }

                @Override
                protected void EventHandler(EventInfo eventInfo) throws EventHandledException
                {
                    // Move to unconnectedState using same conditions as pendingDisconnectState

                    if (eventInfo.lastIntentString.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
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
                    if (eventInfo.lastIntentString.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
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
                    else if (eventInfo.lastIntentString.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
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
                protected void EnterState() {
                    // Create/Start timer
                }

                @Override
                protected void ExitState() {
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
                protected void EnterState() {
                    // Create/Start timer
                }

                @Override
                protected void ExitState() {
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
