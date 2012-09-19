package com.ftechz.tools;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

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
            if (intent.getAction().equals(
                    Intent.ACTION_SCREEN_ON)) {

            } else if (intent.getAction().equals(
                    Intent.ACTION_SCREEN_OFF)) {
            }
        }
    };

    private BroadcastReceiver wifiActionReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (intent.getAction().equals(
                    WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                boolean wifiConnected = intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false);
                if (wifiConnected) {

                } else {

                }
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
                int btState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                switch (btState) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        break;
                    case BluetoothAdapter.STATE_DISCONNECTED:
                        break;
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    private class ActiveState extends State<EventInfo>
    {
        public State currentState;

        @Override
        public void EnterState()
        {
            currentState = searchingState;
        }

        @Override
        public void TriggerState(EventInfo eventInfo)
        {
            currentState.TriggerState(eventInfo);
        }

        public void ChangeState(State nextState)
        {
            currentState = nextState;
            nextState.EnterState();
        }

        /**
         * Searching State
         */
        private State searchingState = new SearchingState();

        private class SearchingState extends State<EventInfo>
        {
            @Override
            public void TriggerState(EventInfo eventInfo)
            {
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
        private State connectingState = new ConnectingState();

        private class ConnectingState extends State<EventInfo>
        {
            @Override
            public void TriggerState(EventInfo eventInfo)
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
            }
        }

        /**
         * Connected State
         */
        private State connectedState = new ConnectedState();

        private class ConnectedState extends State<EventInfo>
        {
            @Override
            public void TriggerState(EventInfo eventInfo)
            {
                if (!eventInfo.screenOn) {
                    ActiveState.this.ChangeState(ActiveState.this.unconnectedState);
                }
            }
        }


        /**
         * Unconnected State
         */
        private State unconnectedState = new UnconnectedState();

        private class UnconnectedState extends State<EventInfo>
        {
            @Override
            public void TriggerState(EventInfo eventInfo)
            {
                if (eventInfo.screenOn) {
                    ActiveState.this.ChangeState(ActiveState.this.searchingState);
                }
            }
        }
    }
}
