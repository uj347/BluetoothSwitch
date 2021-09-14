package com.uj.bluetoothswitch.serviceparts;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import com.uj.bluetoothswitch.serviceparts.broadcastreceivers.SoundprofilesBroadcastReciever;
import com.uj.bluetoothswitch.serviceparts.connectionpart.BTInquirer;
import com.uj.bluetoothswitch.serviceparts.connectionpart.BTReplier;
import com.uj.bluetoothswitch.serviceparts.connectionpart.IInquirer;
import com.uj.bluetoothswitch.serviceparts.connectionpart.IReplier;
import com.uj.bluetoothswitch.serviceparts.soundprofilepart.SoundProfileManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class Commander {


    private final static String TAG = "MAINCOMMANDER";

    private IInquirer<BluetoothDevice> mInquirer;
    private IReplier<BluetoothDevice> mReplier;
    private final SoundProfileManager mManager;
    private final BTConnectionService mServiceInstance;
    private final NotificationFactory mNotificationFactory;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private SoundprofilesBroadcastReciever mSoundProfBR;
    private final CompositeDisposable mMainDisposable = new CompositeDisposable();
    private final CompositeDisposable mCommandDisposable = new CompositeDisposable();
    private final PublishSubject<Pair<String, Completable>> mCommandSubject = PublishSubject.create();


    public Commander(BTConnectionService serviceInstance) {
        mServiceInstance = serviceInstance;
        mManager = mServiceInstance.exposeSoundProfileManager();
        mInquirer = new BTInquirer(mManager);
        mReplier = new BTReplier(mManager);
        mSoundProfBR = mServiceInstance.exposeSoundProfileBR();
        mNotificationFactory = mServiceInstance.exposeNotificationfactory();

    }


    public void startCommander() {
        mCommandSubject
                .observeOn(Schedulers.newThread())
                .filter(completable -> mManager.isFullyConstruted())
                .subscribe(new Observer<Pair<String, Completable>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mMainDisposable.add(d);
                    }

                    @Override
                    public void onNext(@NonNull Pair<String, Completable> pair) {
                        Log.d(TAG, "NExt command in mainDispatcher");

                        pair.second
                                .subscribeOn(Schedulers.io())
                                .doOnTerminate(() -> {
                                    mCommandDisposable.clear();
                                    Log.d(TAG, "Command disposable cleared ");
                                })
                                .subscribe(new CompletableObserver() {
                                    @Override
                                    public void onSubscribe(@NonNull Disposable d) {

                                        mCommandDisposable.add(d);
                                        Log.d(TAG, "Command subscribed: " + pair.first);
                                    }

                                    @Override
                                    public void onComplete() {
                                        Log.d(TAG, "Command completed: " + pair.first);
                                    }

                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        Log.d(TAG, "Error occured in command" + pair.first);
                                        onIdle();

                                    }
                                });


                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "Error occured in mainObservable: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "Main observable onComplete");
                    }
                });

    }


    public void onIdle() {
        stopInquiriesAndReplies();
        mServiceInstance.setCurrentState(ServiceState.STATE_IDLE);
        mNotificationFactory.postWithNoDevice();
        Pair<String, Completable> command = new Pair<>("onIdle", Completable.never());
        mCommandSubject.onNext(command);
    }


    public void onReach(BluetoothDevice seekableSoundDevice) {
        Log.d(TAG, "onReach with device:" + seekableSoundDevice);
        stopInquiriesAndReplies();
        mServiceInstance.setCurrentState(ServiceState.STATE_REACHING);
        Pair<String, Completable> command = new Pair<>("onReach " + seekableSoundDevice,
                Completable.create(
                        emitter -> {
                            AtomicBoolean independentInquiryRequired = new AtomicBoolean(true);
                            Log.d(TAG, "onReach: enabling manual disconnect override");
                            mServiceInstance.exposeBroadcastInterpreter()
                                    .setManualDisconnectOverride(true);
                            emitter.setCancellable(() ->
                            {
                                Log.d(TAG, "onReach: disabling manual disconnect override");
                                mServiceInstance.exposeBroadcastInterpreter()
                                        .setManualDisconnectOverride(false);
                            });
                            if (mManager.isConnectedViaThisProfile()) {
                                Log.d(TAG, "onReach: Connected via this profile");
                                List<BluetoothDevice> connectedDevices = mManager.getConnectedDevices();
                                BluetoothDevice currentlyConnected =
                                        connectedDevices.isEmpty() ? null : connectedDevices.get(0);

                                if (currentlyConnected != null) {
                                    Log.d(TAG, "CurrentlyConnectedDevice: " + currentlyConnected.getName());
                                    independentInquiryRequired.set(false);
                                    if (currentlyConnected.equals(seekableSoundDevice)) {
                                        Log.d(TAG, "onReach: already connected to required device, proceeding on listen");
                                        onListen(currentlyConnected);
                                    } else {
                                        mManager.tryDisconnectFromCurrentDevice().subscribe();
//                                        mCommandDisposable.add(
                                        mSoundProfBR
                                                .getDisconnectedObservable()
                                                .timeout(6, TimeUnit.SECONDS)
                                                .take(1)
                                                .blockingSubscribe(
                                                        (device) -> {
                                                            mManager
                                                                    .tryConnectToSpecifiedDevice(seekableSoundDevice.getAddress())
                                                                    .subscribe();
                                                        },
                                                        (err) -> {
                                                            Log.d(TAG, "Disconnection from device doesn't occured" +
                                                                    ", trying connect to new device anyway");
                                                            mManager
                                                                    .tryConnectToSpecifiedDevice(seekableSoundDevice.getAddress())
                                                                    .subscribe();
                                                        });
//                                        );

//                                        mCommandDisposable.add(
                                        mSoundProfBR
                                                .getConnectedObservable()
                                                .timeout(4, TimeUnit.SECONDS)
                                                .take(1)
                                                .blockingSubscribe(
                                                        (device) -> {
                                                            Log.d(TAG, "Successfully connected to device without any inquiries");
                                                        },
                                                        (err) -> {
                                                            independentInquiryRequired.set(true);
                                                            Log.d(TAG, "Connection to soundDevice without inquries doesn't occured, " +
                                                                    "proceeding to inquiries");

                                                        });
//                                        );


                                    }
                                }

                            }

                            if (independentInquiryRequired.get()) {
                                Log.d(TAG, "Starting independent Inquiries");
                                Single.<Boolean>create(
                                        sinEmmiter -> {
                                            mManager
                                                    .tryConnectToSpecifiedDevice(seekableSoundDevice.getAddress())
                                                    .blockingSubscribe();

                                            mSoundProfBR
                                                    .getConnectedObservable()
                                                    .timeout(4, TimeUnit.SECONDS)
                                                    .take(1)
                                                    .blockingSubscribe(
                                                            (device) -> {
                                                                Log.d(TAG, "Successfully connected to device without any inquiries");
                                                                if (device.equals(seekableSoundDevice)) {
                                                                    if (!sinEmmiter.isDisposed()) {
                                                                        sinEmmiter.onSuccess(true);
                                                                    }
                                                                } else {
                                                                    if (!sinEmmiter.isDisposed()) {
                                                                        sinEmmiter.onSuccess(false);
                                                                    }
                                                                }
                                                            },
                                                            (err) -> {
                                                                Log.d(TAG, "Connection to soundDevice without inquries doesn't occured, " +
                                                                        "proceeding to inquiries");
                                                                if (!sinEmmiter.isDisposed()) {
                                                                    sinEmmiter.onSuccess(false);
                                                                }

                                                            });
                                        }
                                ).flatMap(
                                        directConnection -> {
                                            if (directConnection) {
                                                return Single.just(true);
                                            } else {
                                                return mInquirer
                                                        .makeInquiries(seekableSoundDevice.getAddress(), prepareInquiryList(seekableSoundDevice)
                                                                .toArray(new BluetoothDevice[1]));
                                            }
                                        }
                                )
                                        .blockingSubscribe(
                                                (result) -> {
                                                    Log.d(TAG, "reach completed with result: " + result);
                                                    if (!emitter.isDisposed()) {
                                                        if (!result) {
                                                            onIdle();
                                                            emitter.onComplete();
                                                        }
                                                    }
                                                },
                                                err -> {
                                                    Log.d(TAG, "onReach: Completed with failure: " + err);
                                                    if (!emitter.isDisposed()) {
                                                        emitter.onError(err);
                                                    }
                                                }

                                        );
                            }
                        }));
        mCommandSubject.onNext(command);
    }


    public void onListen(BluetoothDevice deviceToListenWith) {
        mServiceInstance.setCurrentState(ServiceState.STATE_LISTENING);
        mNotificationFactory.postWithConectedDevice(deviceToListenWith);
        stopInquiriesAndReplies();
        Pair<String, Completable> command = new Pair<>("onListen " + deviceToListenWith,
                mReplier.waitForInquiry(deviceToListenWith));
        mCommandSubject.onNext(command);

    }

    //TODO
    public void onDisconnect(BluetoothDevice deviceToDisconnectFrom) {
        Log.d(TAG, "onDisconnect: invoked");
        if (mManager.isFullyConstruted()) {
            if (deviceToDisconnectFrom != null) {
                Log.d(TAG, "onDisconnect: withSpecifiedDeviceBranch");
                mCommandDisposable.add(
                        mManager.tryDisconnectFromDevice(deviceToDisconnectFrom.getAddress())
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> Log.d(TAG, "onDisconnect: Disconnection completed"),
                                        (err) -> Log.d(TAG, "onDisconnect: error ocurred: " + err)
                                )
                );
            } else {
                Log.d(TAG, "onDisconnect: withOutSpecifiedDeviceBranch");
                mCommandDisposable.add(
                        mManager.tryDisconnectFromCurrentDevice()
                                .subscribeOn(Schedulers.io())
                                .subscribe(
                                        () -> Log.d(TAG, "onDisconnect: Disconnection completed"),
                                        (err) -> Log.d(TAG, "onDisconnect: error ocurred: " + err)
                                )
                );
            }

        } else {
            Toast.makeText(mServiceInstance, "SoundProfileManager is not ready yet!", Toast.LENGTH_SHORT).show();
        }
    }

    public void onStop() {
        Log.d(TAG, "onStop command invoked");
        disposeCommaderResources();
        mServiceInstance.stopSelf();

    }

    public void disposeCommaderResources() {
        Log.d(TAG, "Disposing commander resources");
        stopInquiriesAndReplies();
        mCommandDisposable.clear();
        mMainDisposable.clear();
    }


    private void stopReplies() {
        if (mReplier != null) {
            mReplier.stopWaitingForInquiry();
        }
    }

    private void stopInquiries() {
        if (mServiceInstance != null) {
            mServiceInstance.exposeBroadcastInterpreter()
                    .setManualDisconnectOverride(false);
        }
        if (mInquirer != null) {
            mInquirer.stopInqueries();
        }
    }

    private void stopInquiriesAndReplies() {
        stopInquiries();
        stopReplies();
    }


    private Set<BluetoothDevice> prepareInquiryList(BluetoothDevice seekableSoundDevice) {
        Set<BluetoothDevice> pairedDevices = new HashSet<>(mAdapter.getBondedDevices());
        Set<BluetoothDevice> inquirySet = pairedDevices.stream()
                .filter(device ->

                        device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_CELLULAR ||
                                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_CORDLESS ||
                                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_ISDN ||
                                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY ||
                                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART ||
                                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_UNCATEGORIZED ||
                                device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_CELLULAR
                )
                .filter(device -> !device.equals(seekableSoundDevice))
                .collect(Collectors.toSet());
        return inquirySet;
    }

}
