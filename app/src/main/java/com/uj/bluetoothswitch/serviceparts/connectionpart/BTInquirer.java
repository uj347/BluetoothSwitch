package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.uj.bluetoothswitch.BluetoothSwitcherApp;
import com.uj.bluetoothswitch.serviceparts.soundprofilepart.IBluetoothProfileManager;

import java.io.IOException;
import java.util.ArrayList;
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
    private final UUID mUuid = UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final IBluetoothProfileManager mProfileManager;
    private final CompositeDisposable mDisposables = new CompositeDisposable();
    private final AtomicBoolean mFoundFlag = new AtomicBoolean();
    private final AtomicBoolean mStopSignal = new AtomicBoolean();
    private final ArrayList<BTClient> mCurrentBTClients = new ArrayList<>();


    public BTInquirer(IBluetoothProfileManager mProfileManager) {
        this.mProfileManager = mProfileManager;
    }

    @Override
    public void stopInqueries() {
        mStopSignal.set(true);
//        for (BTClient client:mCurrentBTClients) {
//            if (client!=null){
//                try {
//                    client.stopConnection();
//                }catch(IOException exc){
//                    Log.d(TAG, "Error in closing connection on inquirer: "+ exc.getMessage());
//                }
//            }
//        }
        if (!mDisposables.isDisposed()) {
            mDisposables.clear();
        }
    }


    @Override
    public Completable makeInquiries(String whatAboutMAC, BluetoothDevice... devicesToConnect) {
        mFoundFlag.set(false);
        mStopSignal.set(false);
        return Observable.fromArray(devicesToConnect)
                .concatMapCompletableDelayError(device -> {
                    if (mFoundFlag.get()) {
                        return Completable.complete();
                    }
                    if (mStopSignal.get()) {
                        return Completable.complete();
                    }
                    return makeSingleInquiry(whatAboutMAC, device);
                })
                .doOnTerminate(() -> {
                    mFoundFlag.set(false);
                    mStopSignal.set(false);
                });
    }

    private Completable makeSingleInquiry(String whatAboutMAC, BluetoothDevice deviceToConnect) {

        return Completable.create(emitter -> {

            Log.d(TAG, "Starting inquery to: " + deviceToConnect.getName() +
                    " about: " + whatAboutMAC);
            BTClient btClient = new BTClient(mUuid, BluetoothSwitcherApp.APP_NAME);
            mCurrentBTClients.add(btClient);

                    btClient
                            .startConnectionToSpecifiedMAC(deviceToConnect.getAddress())

                            //todo NB
                            .blockingSubscribe(
                                    () -> {
                                        Log.d(TAG, "Client connection connected succesfully");
                                    },
                                    (err) -> {
                                        Log.d(TAG, "Error occured in client connection: " + err.getMessage());
                                    }
                            );
            emitter.setCancellable(btClient::stopConnection);
            Log.d(TAG, "Connected to device: " + deviceToConnect.getName());

            try {

                StringInputStream stringInputStream =
                        new StringInputStream(btClient.getInputStream());
                StringOutputStream stringOutputStream =
                        new StringOutputStream(btClient.getOutputStream());
                while (!emitter.isDisposed()) {
                    Log.d(TAG, "Sending message: " + whatAboutMAC);
                    stringOutputStream.writeString(whatAboutMAC);
                    String answer = stringInputStream.readString();
                    Log.d(TAG, "Recieved answer: " + answer);
                    if (answer.trim().equals("YES")) {
                        Thread.sleep(900);
                        mProfileManager.tryConnectToDevice(whatAboutMAC).blockingSubscribe();
                        mFoundFlag.set(true);
                        emitter.onComplete();
                    }
                }
            } catch (IOException | InterruptedException exc) {
                if (!emitter.isDisposed()) {
                    Log.d(TAG, " Exception occured+ " + exc.getMessage());
                    emitter.onError(exc);
                }
            }
        });
    }
}

