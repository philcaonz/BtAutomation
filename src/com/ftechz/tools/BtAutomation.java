package com.ftechz.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
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


        final Button connectionEnableBtn = (Button) findViewById(R.id.conEnable);
        final Button connectionDisableBtn = (Button) findViewById(R.id.conDisable);
        final Button simScreenOnBtn = (Button) findViewById(R.id.simScreenOn);
        final Button simScreenOffBtn = (Button) findViewById(R.id.simScreenOff);

        connectionEnableBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

            }
        });

        connectionDisableBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Do something in response to button click
            }
        });

        simScreenOnBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBinder.SimulateScreenOn();
            }
        });

        simScreenOffBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mBinder.SimulateScreenOff();
            }
        });

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
