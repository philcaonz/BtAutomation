package com.ftechz.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = new Intent(this, BtAutomationService.class);
        stopService(intent);
    }
}
