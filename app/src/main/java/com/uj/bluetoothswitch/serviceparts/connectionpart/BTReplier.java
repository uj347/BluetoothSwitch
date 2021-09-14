package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.uj.bluetoothswitch.BluetoothSwitcherApp;
import com.uj.bluetoothswitch.serviceparts.soundprofilepart.IBluetoothProfileManager;

import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.CancellableDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class BTReplier implements IReplier<BluetoothDevice> {
    private final static String TAG = "BTReplier";
    private static final String PHONEMAC = "F8:C3:9E:8B:28:C6";
    private static final String PLANSHMAC = "74:D2:1D:6B:19:88";
    private UUID mUuid = UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final IBluetoothProfileManager mProfileManager;
    private final AtomicBoolean mLegalStopFlag = new AtomicBoolean();
    private final CompositeDisposable mDisposables = new CompositeDisposable();

    public BTReplier(IBluetoothProfileManager profileManager) {
        this.mProfileManager = profileManager;
    }


    @Override
    public Completable waitForInquiry(BluetoothDevice deviceOfIntrest) {
        mLegalStopFlag.set(false);
        String deviceMAC = deviceOfIntrest.getAddress();
        return Completable.create(
                emitter -> {
                    while (!emitter.isDisposed()) {
                        if (mLegalStopFlag.get()) emitter.onComplete();
                        mDisposables.clear();
                        BTListener btListener = new BTListener(mUuid, BluetoothSwitcherApp.APP_NAME);
                        emitter.setCancellable(btListener::stopConnection);
                        mDisposables.add(new CancellableDisposable(btListener::stopConnection));
                        Log.d(TAG, "Waiting for incoming connection");
                        btListener
                                .startListening()
//TODO
                                .blockingSubscribe(
                                        () -> Log.d(TAG, "Incomming connection accepted"),
                                        (err) -> Log.d(TAG, "Incomming connection failed")

                                );
                        if (!btListener.isConnected()) {
                            continue;
                        }


                        Log.d(TAG, "Incomming connection accepted");
                        try {

                            StringInputStream stringInputStream =
                                    new StringInputStream(btListener.getInputStream());
                            StringOutputStream stringOutputStream =
                                    new StringOutputStream(btListener.getOutputStream());


                            String incomingMsg = stringInputStream.readString();
                            Log.d(TAG, "Recieved message : " + incomingMsg);
                            if (incomingMsg.trim().equals(deviceMAC)) {
                                stringOutputStream.writeString("YES");
                                mLegalStopFlag.set(true);
                                Thread.sleep(50);
                                mProfileManager.tryDisconnectFromDevice(deviceMAC).blockingSubscribe();
                                if (!emitter.isDisposed()) {
                                    emitter.onComplete();
                                }
                            } else {
                                stringOutputStream.writeString("NO");
                                btListener.stopConnection();
                                continue;
                            }

                        } catch (IOException | InterruptedException exc) {
                            Log.d(TAG, "Error in Replier: " + exc);
                            if (!emitter.isDisposed()) {
                                emitter.onError(exc);

                            }
                        }

                    }

                }

        ).retry(err -> !mLegalStopFlag.get());

    }


    @Override
    public void stopWaitingForInquiry() {
        mLegalStopFlag.set(true);
        Log.d(TAG, "stopWaitingForInquiry: invoked");
        mDisposables.clear();
    }


}
