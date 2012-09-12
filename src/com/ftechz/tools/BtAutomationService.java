package com.ftechz.tools;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: root
 * Date: 12/09/12
 * Time: 3:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class BtAutomationService extends Service {

    BluetoothAdapter mBluetoothAdapter;

    @Override
    public void onCreate() {
        super.onCreate();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null) {
            return;
        }

        if (mBluetoothAdapter.isEnabled()) {

        }


        registerReceiver(btActionReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));


    }

    private BroadcastReceiver btActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    BluetoothAdapter.ACTION_STATE_CHANGED)) {

                intent.getExtras().get(BluetoothAdapter.EXTRA_STATE);
                Log.v("BtAutomationService", "State change!!!!!!!");

            }
        }
    };

    public IBinder onBind(Intent intent) {
        return null;
    }
}
