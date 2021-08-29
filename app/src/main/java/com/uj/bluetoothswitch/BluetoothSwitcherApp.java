package com.uj.bluetoothswitch;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.uj.bluetoothswitch.dbStuff.DeviceDB;
import com.uj.bluetoothswitch.serviceparts.BTConnectionService;
import com.uj.bluetoothswitch.serviceparts.Commander;

public class BluetoothSwitcherApp extends Application {
    private static final String TAG= "APP_CLASS";
    public static final String APP_NAME="BTSWITCH";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private static final String TRONSMARTMAC="FC:58:FA:C1:03:29";
    private static final IntentFilter stateIntentFilter=new IntentFilter();
    static {stateIntentFilter.addAction(Commander.STATE_DISABLING);
        stateIntentFilter.addAction(Commander.STATE_IDLE);
        stateIntentFilter.addAction(Commander.STATE_LISTENING);
        stateIntentFilter.addAction(Commander.STATE_REACHING);
     };



    private DeviceDB mDeviceDB;
    private MutableLiveData<Boolean> isServerRunningLD=new MutableLiveData<>(true);
    private MutableLiveData<BluetoothDevice> currentlyConnectedSoundDevice=new MutableLiveData<>();
    private final BroadcastReceiver serverStateBroadcastReceiver=new ServiceStateBR();


    @Override
    public void onCreate() {
        super.onCreate();

        mDeviceDB=DeviceDB.getInstance(this);
        Intent serviceIntent=new Intent(this,BTConnectionService.class);
        registerReceiver(serverStateBroadcastReceiver,stateIntentFilter);
        startService(serviceIntent);
    }

    @Override
    public void onTerminate() {
        unregisterReceiver(serverStateBroadcastReceiver);
        super.onTerminate();

    }

    public MutableLiveData<Boolean> getIsServerRunningLD() {
        return isServerRunningLD;
    }

    public MutableLiveData<BluetoothDevice> getCurrentlyConnectedSoundDevice() {
        return currentlyConnectedSoundDevice;
    }

    private class ServiceStateBR extends BroadcastReceiver{


    public static final String STATE_IDLE = "Commander_STATE_IDLE";
    public static final String STATE_LISTENING = "Commander_STATE_LISTENING";
    public static final String STATE_REACHING = "Commander_STATE_REACHING";
    public static final String STATE_DISABLING = "Commander_STATE_DISABLING";


    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()){
            case (STATE_IDLE):
                isServerRunningLD.setValue(true);
                currentlyConnectedSoundDevice.setValue(null);
                ;break;
            case(STATE_LISTENING):
                isServerRunningLD.setValue(true);
                BluetoothDevice currentDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(currentDevice!=null) {
                    currentlyConnectedSoundDevice.setValue(currentDevice);
                    Toast.makeText(getApplicationContext()
                            ,"Connected to sound device: "+currentDevice
                            ,Toast.LENGTH_SHORT)
                            .show();
                };
                break;
            case(STATE_REACHING):
                isServerRunningLD.setValue(true);
                currentlyConnectedSoundDevice.setValue(null);
                break;
            case(STATE_DISABLING):
                isServerRunningLD.setValue(false);
                currentlyConnectedSoundDevice.setValue(null);
                break;
        }
    }
}

}
