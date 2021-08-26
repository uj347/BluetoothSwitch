package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.UUID;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class BTReplier implements IReplier<BluetoothDevice> {
    private final static String TAG = "BTReplier";
    private static final String PHONEMAC = "F8:C3:9E:8B:28:C6";
    private static final String PLANSHMAC = "74:D2:1D:6B:19:88";
    private Byte[] mKey = new Byte[]{125, 125, 125};
    private UUID mUuid = UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static BluetoothAdapter sAdapter = BluetoothAdapter.getDefaultAdapter();
    private final IListener mListenerConnection;
    private final Subject<Object> stopSubject = PublishSubject.<Object>create();
    private final IBluetoothProfileManager mProfileManager;
    private final CompositeDisposable mHooks =new CompositeDisposable();


    public BTReplier(IListener listenerConnection, IBluetoothProfileManager profileManager) {
        this.mListenerConnection = listenerConnection;
        this.mProfileManager = profileManager;
    }
//TODO Сделать так, чтобы всегда был только один экземляр ожидания ввода

    @Override
    public void waitForInquiry(BluetoothDevice deviceOfIntrest) {
        Completable.create(
                emitter -> {
                    mListenerConnection.startListening();
                    Thread.sleep(100);
                    Observer<String> outHook = mListenerConnection.getExternalOutputHook();
                    Observable<String> inputHook = mListenerConnection.getExternalInputHook();

                    stopSubject
                            .observeOn(Schedulers.newThread())
                            .subscribe((o) -> emitter.onComplete());


                           Disposable inpHookDisp=inputHook

                            .subscribe(
                                    (s) -> {
                                        if (s.equals(deviceOfIntrest.getAddress())) {
                                            Thread.sleep(150);
                                            outHook.onNext("YES");
                                            if (mProfileManager.isConnectedToDevice(deviceOfIntrest.getAddress())) {
                                                mProfileManager.tryDisconnectFromDevice(deviceOfIntrest.getAddress()).blockingSubscribe();
                                            }
                                            Thread.sleep(150);

                                            if (!emitter.isDisposed()) {
                                                emitter.onComplete();
                                            }
                                        } else {
                                            Thread.sleep(150);
                                            outHook.onNext("NO");
                                        }
                                    },
                                    (err) -> {
                                        emitter.onError(err);
                                    }
                            );
                           mHooks.add(inpHookDisp);
                }
        )
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {
                            Log.d(TAG, "waitForInquiry: completed ");
                            mListenerConnection.stopListening();
                            if(!mHooks.isDisposed()){
                                mHooks.clear();
                            }
                        },
                        (err) -> {
                            mListenerConnection.stopListening();
                            Log.d(TAG, "wait for inquiry failed because of: " + err.getMessage());
                            if(!mHooks.isDisposed()){
                                mHooks.clear();
                            }
                        }
                );

    }


    @Override
    public void stopWaitingForInquiry() {
        stopSubject.onNext(1);
    }


}
