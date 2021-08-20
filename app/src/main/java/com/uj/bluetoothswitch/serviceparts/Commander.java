package com.uj.bluetoothswitch.serviceparts;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class Commander {

    public static final String COMMAND_USER_SEEKS ="Commander_USER_SEEKS";
    public static final String COMMAND_BTSOUND_CONNECTED ="Commander_BTSOUND_CONNECTED";
    public static final String COMMAND_BTSOUND_DISCONNECTED ="Commander_BTSOUND_DISCONNECTED";
    public static final String COMMAND_SERVICE_STARTED ="Commander_SERVICE_STARTED";
    public static final String COMMAND_STOP_COMMANDER ="Commander_STOP";



    private final static String TAG="COMMANDER";
    private static final UUID MYUUID= UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static final String NAME= "BTSWITCHER";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private static final Byte[] KEYPATTERN=new Byte[]{125,125,125};

    private final IInquirer<BluetoothDevice> mInquirer;
    private final IReplier <BluetoothDevice> mReplier;
    private final SoundProfileManager mManager;
    private final BehaviorSubject<Intent> mInputSubject =BehaviorSubject.create();

    public Observer<Intent> getInputHook() {
        return mInputSubject;
    }

    private final  BluetoothAdapter mAdapter=BluetoothAdapter.getDefaultAdapter();
    private final Context mContext;
    private final SoundprofilesBroadcastReciever mBroadcastReciever;
    private boolean mCommanderRunning=false;



    private final Completable mMainDispatcher =Completable.create(
            (completableEmitter)->{

                mInputSubject
                        .observeOn(Schedulers.newThread())
                        .subscribe(new Observer<Intent>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                            if (!completableEmitter.isDisposed()) {
                                completableEmitter.setDisposable(d);
                            }
                            }

                            @Override
                            public void onNext(@NonNull Intent intent) {
                                if(intent.getAction().equals(COMMAND_STOP_COMMANDER)){
                                    if(!completableEmitter.isDisposed()) {
                                        completableEmitter.onComplete();
                                    }
                                }
                                else{
                                    try{
                                        processIntent(intent);
                                    }catch (NullParcelableDeviceException exc)
                                    {
                                        Log.d(TAG, "One ugly motherFucker forget to pass device here");
                                    }
                                }


                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG, "onError in inputSubjectObserver: "+ e);
                            }

                            @Override
                            public void onComplete() {
                                setCommanderRunning(false);
                                Log.d(TAG, "onComplete in inputSubjectObserver ");

                            }
                        });

            }
    )
            .subscribeOn(Schedulers.computation());




    public Commander(SoundProfileManager manager, Context context, IInquirer<BluetoothDevice> inquirer, IReplier<BluetoothDevice> replier) {
        this.mManager =manager;
        this.mInquirer = inquirer;
        this.mReplier = replier;
        this.mContext=context;
        mBroadcastReciever=new SoundprofilesBroadcastReciever(this.mInputSubject);
    }


    public synchronized boolean isCommanderRunning() {
        return mCommanderRunning;
    }

    private synchronized void setCommanderRunning(boolean commanderRunning) {
        this.mCommanderRunning = commanderRunning;
    }


/** Запустить мэйнДиспетчер этого командера, нужно буте выполнить при страрте сервиса*/
    public void startCommander(){

mMainDispatcher.subscribe(()-> Log.d(TAG, "Main Discpatcher in Commander done its job"));
Intent startIntent=new Intent(Commander.COMMAND_SERVICE_STARTED);
mInputSubject.onNext(startIntent);
mContext.registerReceiver(mBroadcastReciever, SoundprofilesBroadcastReciever.sIntentFilter);
setCommanderRunning(true);
    }
   /**Остановить мэйнДиспетчер, нужно будет запустить при остановке сервиса*/
    public void stopCommander (){
        mContext.unregisterReceiver(mBroadcastReciever);
        Intent stopIntent=new Intent(Commander.COMMAND_STOP_COMMANDER);
mInputSubject.onNext(stopIntent);
    }
    private void onWait(){
        Log.d(TAG, "onWait: mode is active");
        mReplier.stopWaitingForInquiry();
        mInquirer.stopInqueries();
    };

    private void onListen(BluetoothDevice currentBTSOUNDDevice){
        Log.d(TAG, "onListen: mode is active with device: "+ currentBTSOUNDDevice.getAddress());
        mInquirer.stopInqueries();
        mReplier.waitForInquiry(currentBTSOUNDDevice);

    };

//TODO Нужно проверить работает ли эта штука
    private void onReaching(BluetoothDevice desirableBTSOUNDDevice){
        Log.d(TAG, "In commander on Reaching, starting to check every paired device");
        Set<BluetoothDevice> pairedDevices=new HashSet<>(mAdapter.getBondedDevices());
       pairedDevices= pairedDevices.stream()
                .filter(device->{return
                            (device.getBluetoothClass().getDeviceClass()==BluetoothClass.Device.PHONE_CELLULAR||
                    device.getBluetoothClass().getDeviceClass()==BluetoothClass.Device.PHONE_CORDLESS||
                    device.getBluetoothClass().getDeviceClass()==BluetoothClass.Device.PHONE_ISDN||
                    device.getBluetoothClass().getDeviceClass()==BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY||
                    device.getBluetoothClass().getDeviceClass()==BluetoothClass.Device.PHONE_SMART||
                    device.getBluetoothClass().getDeviceClass()==BluetoothClass.Device.PHONE_UNCATEGORIZED||
                    device.getBluetoothClass().getDeviceClass()==BluetoothClass.Device.PHONE_CELLULAR);
                }).collect(Collectors.toSet());
        pairedDevices.remove(desirableBTSOUNDDevice);

        mManager.tryConnectToDevice(desirableBTSOUNDDevice.getAddress()).blockingSubscribe();
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!mManager.isConnected(desirableBTSOUNDDevice.getAddress())) {
            BluetoothDevice[] devices = pairedDevices.toArray(new BluetoothDevice[0]);
            mReplier.stopWaitingForInquiry();

            //TODO Что то здесь не так, лечим укастыливанием
            if (devices.length>0) {
                mInquirer.makeInquiry(desirableBTSOUNDDevice.getAddress(), devices);
            }
        }
    };

    private void processIntent (Intent incomingIntent){
        String intentAction=incomingIntent.getAction();
        String parcelableDeviceMAC=incomingIntent.getStringExtra("DEVICE");
        if (intentAction.equals(COMMAND_SERVICE_STARTED)){
            List<BluetoothDevice> currentlyConnectedDevices=mManager.getConnectedDevices();
            if (currentlyConnectedDevices.isEmpty()) {
                Log.d(TAG, "No connected BTSOUND devices detected");
                onWait();
            }else{onListen(currentlyConnectedDevices.get(0));}
        }
        if(intentAction.equals(COMMAND_BTSOUND_CONNECTED)){
            if (parcelableDeviceMAC==null) {
                throw new NullParcelableDeviceException();
            }
            onListen(mAdapter.getRemoteDevice(parcelableDeviceMAC));
        }

        if(intentAction.equals(COMMAND_BTSOUND_DISCONNECTED)){
            onWait();
        }

        if(intentAction.equals(COMMAND_USER_SEEKS)){
            if (parcelableDeviceMAC==null) {
                throw new NullParcelableDeviceException();
            }
            onReaching(mAdapter.getRemoteDevice(parcelableDeviceMAC));
        }


    }


}






class NullParcelableDeviceException extends RuntimeException{

}
