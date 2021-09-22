package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.uj.bluetoothswitch.BluetoothSwitcherApp;
import com.uj.bluetoothswitch.serviceparts.soundprofilepart.IBluetoothProfileManager;
import com.uj.bluetoothswitch.serviceparts.soundprofilepart.SoundProfileManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

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
        if (!mDisposables.isDisposed()) {
            mDisposables.clear();
        }
    }


    @Override
    public Single<Boolean> makeInquiries(String whatAboutMAC, BluetoothDevice... devicesToConnect) {
        mFoundFlag.set(false);
        mStopSignal.set(false);
        return Observable.fromArray(devicesToConnect)
                .concatMapSingle(device -> makeSingleInquiry(whatAboutMAC, device))
                .filter(result -> result)
                .first(false);

    }


    private Single<Boolean> makeSingleInquiry(String whatAboutMAC, BluetoothDevice deviceToConnect) {

        return Single.<Boolean>create(emitter -> {

            Log.d(TAG, "Starting inquery to: " + deviceToConnect.getName() +
                    " about: " + whatAboutMAC);
            BTClient btClient = new BTClient(mUuid, BluetoothSwitcherApp.APP_NAME);
            mCurrentBTClients.add(btClient);
            emitter.setCancellable(btClient::stopConnection);

            mDisposables.add(
                btClient
                        .startConnectionToSpecifiedMAC(deviceToConnect.getAddress())

                        .subscribe(
                                () -> {
                                    Log.d(TAG, "Client connection connected succesfully");
                                    Log.d(TAG, "Connected to device: " + deviceToConnect.getName());

                                },
                                (err) -> {
                                    Log.d(TAG, "Error occured in client connection: " + err.getMessage());
                                    if (!emitter.isDisposed()) {
                                        emitter.onSuccess(false);
                                    }
                                }
                        )
            );

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
                        Thread.sleep(1200);
                        mProfileManager.tryConnectToSpecifiedDevice(whatAboutMAC).blockingSubscribe(
                                ()-> Log.d(TAG, "Inquirer: tryConnect success: "),
                                (err)-> Log.d(TAG, "Inquirer: DisConnect error: "+err)
                        );
                        // mFoundFlag.set(true);
                        if (!emitter.isDisposed()) {
                            emitter.onSuccess(true);
                        }
                    } else {
                        if (!emitter.isDisposed()) {
                            emitter.onSuccess(false);
                        }
                    }
                }
            } catch (IOException | InterruptedException exc) {
                if (!emitter.isDisposed()) {
                    Log.d(TAG, " Exception occured+ " + exc.getMessage());
                    emitter.onSuccess(false);
                }
            }
        });
    }

}

