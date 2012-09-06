package com.ftechz.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;

public class BtAutomation extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }


    public void SomeFunction() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            return;
        }

        if (mBluetoothAdapter.isEnabled()) {

        }
    }

}
