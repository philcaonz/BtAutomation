package com.ftechz.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;

public class BtAutomation extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intent = new Intent(this, BtAutomationService.class);
        startService(intent);
    }


    public void SomeFunction() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return;
        }

        if (mBluetoothAdapter.isEnabled()) {

        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent intent = new Intent(this, BtAutomationService.class);
        stopService(intent);
    }
}
