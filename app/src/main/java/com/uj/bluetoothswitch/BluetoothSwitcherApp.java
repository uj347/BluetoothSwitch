package com.uj.bluetoothswitch;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.serviceparts.BTConnectionService;

public class BluetoothSwitcherApp extends Application {
    private static final String TAG= "APP_CLASS";
    public static final String APP_NAME="BTSWITCH";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private static final String TRONSMARTMAC="FC:58:FA:C1:03:29";




    private DeviceDB mDeviceDB;



    @Override
    public void onCreate() {
        super.onCreate();

        mDeviceDB=DeviceDB.getInstance(this);


    }

    @Override
    public void onTerminate() {

        super.onTerminate();

    }




}


