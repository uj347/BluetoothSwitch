package com.uj.bluetoothswitch.serviceparts.broadcastreceivers;


import android.bluetooth.BluetoothDevice;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.util.Log;

import com.uj.bluetoothswitch.serviceparts.BTConnectionService;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class UserCommandsBroadcastReceiver extends BroadcastReceiver {
    private static String TAG = "UserCommandsBroadcastReceiver";
    private final PublishSubject<Optional<BluetoothDevice>> mUserConnectSJ = PublishSubject.create();
    private final PublishSubject<Optional<BluetoothDevice>> mUserDisconnectSJ = PublishSubject.create();
    private final PublishSubject<Object> mUserStopServiceSJ = PublishSubject.create();
    private final PublishSubject<Object> mUserSeeksServiceStateSJ = PublishSubject.create();
    public static IntentFilter sIntentFilter=new IntentFilter();

    static {
        for (String action:
             BTConnectionService.USERCOMMAND_LIST) {
            sIntentFilter.addAction(action);
        }
    }

    public void registerThisReciver(Context context) {
        context.registerReceiver(this, sIntentFilter);
    }

    public Observable<Optional<BluetoothDevice>>  getUserSeeksConnectObservable() {
        return mUserConnectSJ.debounce(200, TimeUnit.MILLISECONDS);
    }

    public Observable<Optional<BluetoothDevice>>  getUserSeeksDisonnectObservable() {
        return mUserDisconnectSJ.debounce(200, TimeUnit.MILLISECONDS);
    }


    public Observable<Object> getUserSeeksStopServObservable() {
        return mUserStopServiceSJ.debounce(200, TimeUnit.MILLISECONDS);
    }

    public Observable<Object> getUserSeeksServiceStateObservable() {
        return mUserSeeksServiceStateSJ.debounce(200, TimeUnit.MILLISECONDS);
    }


    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(TAG, "recieved broadcast");
        processIntent(intent);

    }

    private void processIntent(Intent intent) {

        BluetoothDevice extraDevice = intent
                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Log.d(TAG, "In processing user intent Device Obtained: "+ extraDevice);
        Optional<BluetoothDevice>deviceOptional=Optional.ofNullable(extraDevice);
        Log.d(TAG, "Contents of Optional: "+deviceOptional.orElse(null));
        switch (intent.getAction()) {
            case BTConnectionService.COMMAND_USER_SEEKS_CONNECT:
                mUserConnectSJ.onNext(deviceOptional);
                break;
            case BTConnectionService.COMMAND_USER_SEEKS_DISCONNECT:
                mUserDisconnectSJ.onNext(deviceOptional);
                break;
            case BTConnectionService.COMMAND_USER_SEEKS_STOPSERVICE:
                mUserStopServiceSJ.onNext(1);
                break;
            case BTConnectionService.COMMAND_USER_SEEKS_CURRENTSTATE:
                mUserSeeksServiceStateSJ.onNext(1);
                break;
        }

    }
}
