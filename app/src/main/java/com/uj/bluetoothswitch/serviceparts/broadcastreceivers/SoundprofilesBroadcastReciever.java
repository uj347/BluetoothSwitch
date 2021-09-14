package com.uj.bluetoothswitch.serviceparts.broadcastreceivers;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.uj.bluetoothswitch.serviceparts.Commander;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class SoundprofilesBroadcastReciever extends BroadcastReceiver {
    private static String TAG = "SoundProfilesBroadcastReceiver";
    private PublishSubject<Optional<BluetoothDevice>> mConnectedSubj = PublishSubject.create();
    private PublishSubject<Optional<BluetoothDevice>> mDisconnectedSubj = PublishSubject.create();


    public static IntentFilter sIntentFilter;

    static {
        sIntentFilter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        sIntentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
    }

    public void registerThisReciver(Context context) {
        context.registerReceiver(this, sIntentFilter);
    }

    public Observable<Optional<BluetoothDevice>> getConnectedObservable() {
        return mConnectedSubj.debounce(700, TimeUnit.MILLISECONDS);
    }

    public Observable<Optional<BluetoothDevice>> getDisconnectedObservable() {
        return mDisconnectedSubj.debounce(700, TimeUnit.MILLISECONDS);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED) ||
                intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            Log.d(TAG, "recieved broadcast");
            processIntent(intent);
        }
    }

    private void processIntent(Intent intent) {
        int extraState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1);
        BluetoothDevice extraDevice = intent
                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Optional<BluetoothDevice> deviceOptional = Optional.ofNullable(extraDevice);
        if (extraState == BluetoothProfile.STATE_CONNECTED) {
            mConnectedSubj.onNext(deviceOptional);
        }
        if (extraState == BluetoothProfile.STATE_DISCONNECTED) {
            mDisconnectedSubj.onNext(deviceOptional);
        }
    }

}




