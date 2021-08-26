package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BTInquirer implements IInquirer<BluetoothDevice> {
    private final static String TAG = "BTInquirer";
    private static final String PHONEMAC = "F8:C3:9E:8B:28:C6";
    private static final String PLANSHMAC = "74:D2:1D:6B:19:88";
    private Byte[] mKey = new Byte[]{125, 125, 125};
    private UUID mUuid = UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static BluetoothAdapter sAdapter = BluetoothAdapter.getDefaultAdapter();
    private final IClient mClientConnection;
    private final IBluetoothProfileManager mProfileManager;
    private final CompositeDisposable hooks = new CompositeDisposable();
    private AtomicBoolean breakFlag = new AtomicBoolean();


    @Override
    public void stopInqueries() {
        breakFlag.set(true);
    }


    public BTInquirer(IClient clientConenction, IBluetoothProfileManager manager) {
        this.mClientConnection = clientConenction;
        this.mProfileManager = manager;
    }

    @Override
    public void makeInquiry(String whatAboutMAC, BluetoothDevice... devicesToConnect) {
        breakFlag.set(false);
        for (BluetoothDevice device : devicesToConnect) {
            if (breakFlag.getAndSet(false)) {
                break;
            }


            makeSingleInquiry(whatAboutMAC, device)
                    .doOnSubscribe((oo) -> Log.d(TAG, "makeInquiry: Thread: " + Thread.currentThread().getName()))
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            if (!d.isDisposed()) {
                                hooks.clear();
                                hooks.add(d);
                            }
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "Single inquery successful");

                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.d(TAG, "onError: in single inquery");

                        }
                    });


        }
    }

    private Completable makeSingleInquiry(String whatAboutMAC, BluetoothDevice deviceToConnect) {

        Log.d(TAG, "makeSingleInquiry: Making single inquiry to: "
                + deviceToConnect.getAddress() + deviceToConnect.getName());

        return Completable.create(

                emitter -> {
                    Observer<String> outHook = mClientConnection.getExternalOutputHook();
                    Observable<String> inputHook = mClientConnection.getExternalInputHook();


                    mClientConnection.startConnectionToSpecifiedMAC(deviceToConnect.getAddress());

                    Thread.sleep(120);


                    outHook.onNext(whatAboutMAC);


                    inputHook
                            .doOnNext((s) -> Log.d(TAG, "makeSingleInquiry: nextString from inputHook on thread: "
                                    + Thread.currentThread().getName()))

                            .subscribe(
                                    (s) -> {
                                        Log.d(TAG, "makeSingleInquiry: Listening for incoming message from inputHook");

                                        if(s.equals(IClient.NO_CONNECTION)){

                                            Log.d(TAG, "No Connection with device: "+deviceToConnect.getName());
                                            if (!emitter.isDisposed()){
                                                emitter.onComplete();
                                            }
                                        }

                                        if (s.equals("YES")) {
                                            Thread.sleep(100);
                                            Log.d(TAG, "Answer YES is Aquired on Thread " + Thread.currentThread().getName());
                                            mProfileManager.tryConnectToDevice(whatAboutMAC).subscribe();
                                            if (!emitter.isDisposed()) emitter.onComplete();
                                        } else {
                                            if (!emitter.isDisposed()) {
                                                emitter.onComplete();
                                            }
                                        }
                                    },
                                    (err) -> {
                                        Log.d(TAG, "Error in listening for incoming Hook: " + err.getMessage());
                                        if (!emitter.isDisposed()) emitter.onError(err);
                                    }
                            );

                })
        .subscribeOn(Schedulers.io());
        //.observeOn(Schedulers.io());

    }
}

