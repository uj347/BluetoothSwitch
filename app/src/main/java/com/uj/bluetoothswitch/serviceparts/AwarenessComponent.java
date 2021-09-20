package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.uj.bluetoothswitch.serviceparts.soundprofilepart.SoundProfileManager;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class AwarenessComponent {
    public static final String TAG = "AwarenessComponent";
    private final PublishSubject<BTDeviceCheckingIntent> mMainSubject = PublishSubject.create();
    private final CompositeDisposable mMainDisposable = new CompositeDisposable();
    private final BTConnectionService mServiceInstance;
    private final SoundProfileManager mManager;
    private final Commander mCommander;
    private final LiveData<ServiceState> mServiceStateLD;
    private final Observer<ServiceState> mStateObserver =
            (serviceState -> mMainSubject.onNext(processStateToIntent(serviceState)));


    public AwarenessComponent(BTConnectionService serviceInstance) {
        this.mServiceInstance = serviceInstance;
        this.mManager = mServiceInstance.exposeSoundProfileManager();
        this.mCommander = mServiceInstance.exposeCommander();
        this.mServiceStateLD = mServiceInstance.exposeCurrentStateLD();
    }

    public void stopAwarenessComponent() {

        Completable
                .fromAction(() -> mServiceStateLD.removeObserver(mStateObserver))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Log.d(TAG, "StateLD observer removed in stopCommand"),
                        err -> Log.d(TAG, "Error occured in stopping observing StateLD " +
                                err.getMessage())
                );

        mMainDisposable.clear();
    }

    public void startAwarenessComponent() {
        mMainDisposable.add(mMainSubject

                //todo check there
                .subscribe(
                        (stateIntent) -> {
                            Log.d(TAG, "sending broadcast, intent: " + stateIntent.getAction()
                                    + " containing device "
                                    + stateIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                            mServiceInstance.sendBroadcast(stateIntent);
                        },
                        err -> Log.d(TAG, "Error occured in StateAwarenessComponent: " + err),
                        () -> Log.d(TAG, "MainObservable of awareness component is completed")
                ));
        launchStateChangedNotifier();
        launchTimedChecker();
    }

    private void launchTimedChecker() {
        mMainDisposable.add(Observable.interval(5, TimeUnit.SECONDS)

                .filter(num -> this.checkAndCorrectCurrentState())
                .map(num -> mServiceStateLD.getValue())
                .filter(state -> state != null)
                .map(this::processStateToIntent)
                .distinctUntilChanged()
                .subscribe(
                        mMainSubject::onNext,
                        err -> Log.d(TAG, "Err in timedChecker: " + err)
                ));
    }

    private void launchStateChangedNotifier() {
        mMainDisposable.add(Completable.fromAction(() -> {
            mServiceStateLD.observeForever(mStateObserver);
        })
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> Log.d(TAG, "StateChangedNotifier: launched succeessfully "),
                        (err) -> Log.d(TAG, "StateChangedNotifier: launch failed,reason:  " + err)));


    }

    public void forcedStateNotification() {
        ServiceState currentState = mServiceStateLD.getValue();
        if (currentState != null) {
            Log.d(TAG, "Sending forced State notification");
            mServiceInstance.sendBroadcast(processStateToIntent(currentState));
        }

    }

    /**
     * Returns true if everything is ok and no correction was required,false if state was corrected
     */
    private boolean checkAndCorrectCurrentState() {
        Log.d(TAG, "checkAndCorrectCurrentState: invoked");
        List<BluetoothDevice> connectedDevices = mManager.getConnectedDevices();
        BluetoothDevice currentBTDevice =
                connectedDevices.isEmpty() ? null : connectedDevices.get(0);
        ServiceState currentState = mServiceStateLD.getValue();
        if (currentState == null) {
            Log.d(TAG, "checkAndCorrectCurrentState: current stae is null");
            return false;
        }
        if (currentBTDevice != null &&
                !(currentState == ServiceState.STATE_LISTENING ||
                        currentState == ServiceState.STATE_REACHING)) {
            Log.d(TAG, "Perform correction of state, currentstate is: " + currentState.name()
                    + "but there is sounddevice connected, so changing to LISTEN");
            mCommander.onListen(currentBTDevice);
            return false;
        }
        if (currentBTDevice == null && currentState == ServiceState.STATE_LISTENING) {
            Log.d(TAG, "Perform correction of state, currentstate is: " + currentState.name()
                    + "but there is no sounddevice connected, so changing to IDLE");
            mCommander.onIdle();
            return false;
        }
        return true;
    }


    private BTDeviceCheckingIntent processStateToIntent(ServiceState state) {
        List<BluetoothDevice> connectedDevices = mManager.getConnectedDevices();
        BluetoothDevice currentBTDevice =
                connectedDevices.isEmpty() ? null : connectedDevices.get(0);
        return new BTDeviceCheckingIntent(BTConnectionService.STATE_COMMAND_MAP.get(state),
                currentBTDevice);

    }

}

class BTDeviceCheckingIntent extends Intent {
    public static final String TAG = "DeviceCheckingIntent";

    public BTDeviceCheckingIntent(String action, BluetoothDevice btDeviceParcelable) {
        super(action);
        this.putExtra(BluetoothDevice.EXTRA_DEVICE, btDeviceParcelable);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (!(obj instanceof Intent)) {
            return false;
        } else {
            BluetoothDevice thisDevice = this.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            BluetoothDevice otherDevice = ((Intent) obj).getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String thisAction = this.getAction();
            String otherAction = ((Intent) obj).getAction();
            if (thisDevice == null ^ otherDevice == null) {
                return false;
            }
            if (thisDevice == null && otherDevice == null) {

                return thisAction.equals(otherAction);
            }

            return thisAction.equals(otherAction) && thisDevice.equals(otherDevice);
        }
    }

    @Override
    public int hashCode() {
        BluetoothDevice thisDevice = this.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (thisDevice != null) {
            return Objects.hash((Intent) this, thisDevice);
        } else {
            return super.hashCode();
        }
    }
}
