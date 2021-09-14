package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.uj.bluetoothswitch.serviceparts.broadcastreceivers.SoundprofilesBroadcastReciever;
import com.uj.bluetoothswitch.serviceparts.broadcastreceivers.UserCommandsBroadcastReceiver;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class BroadcastInterpreter {
    public static final String TAG = "BroadcastInterpreter";
    private CompositeDisposable mDisposables = new CompositeDisposable();
    private final BTConnectionService mServiceInstance;
    private final SoundprofilesBroadcastReciever mSoundprofilesBroadcastReciever;
    private final UserCommandsBroadcastReceiver mUserCommandsBroadcastReceiver;
    private final Commander mCommander;
    private final AwarenessComponent mAwarenessCompoent;

    private final AtomicBoolean mSoundDeviceDisconnectManualOverride = new AtomicBoolean();

    public BroadcastInterpreter(BTConnectionService service) {
        this.mServiceInstance = service;
        this.mSoundprofilesBroadcastReciever = service.exposeSoundProfileBR();
        this.mUserCommandsBroadcastReceiver = service.exposeUserCommandsBR();
        this.mCommander = mServiceInstance.exposeCommander();
        this.mAwarenessCompoent = mServiceInstance.exposeAwarenessComponent();
    }

    public void setManualDisconnectOverride(boolean newValue) {
        mSoundDeviceDisconnectManualOverride.set(newValue);
    }

    public void stopInterpreter() {
        mDisposables.clear();
    }

    public void startInterpreter() {
        mDisposables.addAll(
                mSoundprofilesBroadcastReciever.getDisconnectedObservable()
                        .filter(device -> !mSoundDeviceDisconnectManualOverride.get())
                        .subscribe(device -> mCommander.onIdle()),
                mSoundprofilesBroadcastReciever
                        .getConnectedObservable()
                        .subscribe(deviceOptional -> {
                            BluetoothDevice device = deviceOptional.orElse(null);
                            mCommander.onListen(device);
                        }),
                mUserCommandsBroadcastReceiver
                        .getUserSeeksConnectObservable()
                        .subscribe(deviceOptional -> {
                            BluetoothDevice device = deviceOptional.get();
                            Log.d(TAG, "Interpreted reaching to : " + device);
                            mCommander.onReach(device);
                        }),
                mUserCommandsBroadcastReceiver
                        .getUserSeeksDisonnectObservable()
                        .doOnNext(devOpt -> Log.d(TAG, "Interpreted DISCONNECT COMMAND  "))
                        .subscribe(deviceOptional -> {
                            BluetoothDevice device = deviceOptional.orElse(null);
                            mCommander.onDisconnect(device);
                        }),
                mUserCommandsBroadcastReceiver
                        .getUserSeeksServiceStateObservable()
                        .doOnNext(devOpt -> Log.d(TAG, "Interpreted FORCEDSTATENOTIFICATION COMMAND  "))

                        .subscribe(obj -> mAwarenessCompoent.forcedStateNotification()),
                mUserCommandsBroadcastReceiver
                        .getUserSeeksStopServObservable()
                        .subscribe(obj -> mCommander.onStop())

        );
    }


}
