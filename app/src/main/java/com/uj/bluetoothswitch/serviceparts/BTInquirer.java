package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class BTInquirer implements IInquirer<BluetoothDevice> {
    private final static String TAG="BTInquirer";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private  Byte[] mKey=new Byte[]{125,125,125};
    private UUID mUuid=UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static BluetoothAdapter sAdapter= BluetoothAdapter.getDefaultAdapter();
    private final IClient mClientConnection;
    private final IBluetoothProfileManager mProfileManager;


    public BTInquirer(IClient clientConenction,IBluetoothProfileManager manager){
        this.mClientConnection=clientConenction;
        this.mProfileManager=manager;
    }
    @Override
    public boolean makeInquiry(String whatAboutMAC, BluetoothDevice... devicesToConnect) {
       boolean result=false;
        for (BluetoothDevice device:
             devicesToConnect) {
            if (makeSingleInquiry(whatAboutMAC,device)) {
                result = true;
                break;
            }

        }
        return result;
    }

    private boolean makeSingleInquiry(String whatAboutMAC, BluetoothDevice deviceToConnect){
        AtomicBoolean result=new AtomicBoolean();
        Completable.create(

                emitter->{
                    mClientConnection.starConnectionToSpecifiedMAC(deviceToConnect.getAddress());
                    Observer<String> outHook=mClientConnection.getExternalOutputHook();
                    Observable <String>inputHook=mClientConnection.getExternalInputHook();
                    Thread.sleep(150);
                      outHook.onNext(whatAboutMAC);
                        inputHook
                                
                                .subscribe(
                                (s)->{
                                    Log.d(TAG, "makeSingleInquiry: Listening for incoming messagec from inputHook");
                                    if (s.equals("YES")) {
                                        Thread.sleep(250);
                                        Log.d(TAG, "makeSingleInquiry: Answer YES is Aquired");
                                        mProfileManager.tryConnectToDevice(whatAboutMAC).subscribe();
                                        if(!emitter.isDisposed())emitter.onComplete();
                                    }
                                    else {
                                        if(!emitter.isDisposed()) {
                                            emitter.onError(new RuntimeException("Answer is no"));
                                        }
                                    }
                                },
                                (err)->{
                                    Log.d(TAG, "Error in listening for incoming Hook: "+ err.getMessage());
                                    if (!emitter.isDisposed())emitter.onError(err);}
                                );

                    })
                .subscribeOn(Schedulers.io())
                .timeout(10,TimeUnit.SECONDS)
                .doOnError(err->mClientConnection.stopConnection())
                .retry(2)
                .subscribe(
                ()->{
                    Log.d(TAG, "makeSingleInquiry: was succesfull with device: "+whatAboutMAC);
                    result.set(true);
                    mClientConnection.stopConnection();},
                (err)->{result.set(false);
                    mClientConnection.stopConnection();
                    Log.d(TAG, "inquiry returned false because of: "+ err.getMessage()+Thread.currentThread().getName());}
        );

        return  result.get();
    }
}
