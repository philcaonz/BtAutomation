package com.ftechz.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;

public class BtAutomation extends Activity
{
    private TextView mStateText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mStateText = (TextView) findViewById(R.id.stateText);

        Intent intent = new Intent(this, BtAutomationService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        registerReceiver(stateEnterEventReceiver,
                new IntentFilter(State.ENTER_STATE_EVENT));
    }

    private BroadcastReceiver stateEnterEventReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            mStateText.setText(intent.getStringExtra(State.ENTER_STATE_EVENT_EXTRA));
        }
    };

    private BtAutomationService.BtAutomationServiceBinder mBinder;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            mBinder = (BtAutomationService.BtAutomationServiceBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = new Intent(this, BtAutomationService.class);
        stopService(intent);
    }
}
