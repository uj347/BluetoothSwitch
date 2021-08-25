package com.uj.bluetoothswitch.serviceparts;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Commander {

    public static final String COMMAND_USER_SEEKS_CONNECT = "Commander_USER_SEEKS_CONNECT";
    public static final String COMMAND_USER_SEEKS_DISCONNECT = "Commander_USER_SEEKS_DISCONNECT";
    public static final String COMMAND_BTSOUND_CONNECTED = "Commander_BTSOUND_CONNECTED";
    public static final String COMMAND_BTSOUND_DISCONNECTED = "Commander_BTSOUND_DISCONNECTED";
    public static final String COMMAND_SERVICE_STARTED = "Commander_SERVICE_STARTED";
    public static final String COMMAND_STOP_COMMANDER = "Commander_STOP";
    public static final String STATE_IDLE = "Commander_STATE_IDLE";
    public static final String STATE_LISTENING = "Commander_STATE_LISTENING";
    public static final String STATE_REACHING = "Commander_STATE_REACHING";
    public static final String STATE_DISABLING = "Commander_STATE_DISABLING";





    private final static String TAG = "COMMANDER";

    private final IInquirer<BluetoothDevice> mInquirer;
    private final IReplier<BluetoothDevice> mReplier;
    private final SoundProfileManager mManager;
    private final BTConnectionService mServiceInstance;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final SoundprofilesBroadcastReciever mSoundprofilesBroadcastReciever;
    private BroadcastReceiver commandsReceiver;
    private Intent lastKnownState;
    private MutableLiveData<BluetoothDevice> mCurrentSoundDeviceLD;



    private final Completable mMainDispatcher;

    public Commander(BTConnectionService serviceInstance, SoundProfileManager manager, IInquirer<BluetoothDevice> inquirer, IReplier<BluetoothDevice> replier) {

        this.mServiceInstance=serviceInstance;
        this.mManager = manager;
        this.mInquirer = inquirer;
        this.mReplier = replier;
        mSoundprofilesBroadcastReciever = new SoundprofilesBroadcastReciever();
        mCurrentSoundDeviceLD = mSoundprofilesBroadcastReciever.getCurrentBTSoundDeviceLivedata();
        mMainDispatcher= Completable.create(
                (completableEmitter) -> {

                    IntentFilter commandIntentFilter=new IntentFilter();
                    commandIntentFilter.addAction(COMMAND_USER_SEEKS_CONNECT);
                    commandIntentFilter.addAction(COMMAND_USER_SEEKS_DISCONNECT);
                    commandIntentFilter.addAction(COMMAND_BTSOUND_CONNECTED);
                    commandIntentFilter.addAction(COMMAND_BTSOUND_DISCONNECTED);
                    commandIntentFilter.addAction(COMMAND_SERVICE_STARTED);
                    commandIntentFilter.addAction(COMMAND_STOP_COMMANDER);


                    commandsReceiver=new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            if (intent.getAction().equals(COMMAND_STOP_COMMANDER)) {
                                if (commandsReceiver!=null){
                                    mServiceInstance.unregisterReceiver(commandsReceiver);
                                }
                                if (!completableEmitter.isDisposed()) {
                                    completableEmitter.onComplete();
                                }
                            } else {
                                try {
                                    processIntent(intent);
                                } catch (NullParcelableDeviceException exc) {
                                    Log.d(TAG, "One ugly motherFucker forget to pass device here");
                                }
                            }
                        }
                    };

                    mServiceInstance.registerReceiver(commandsReceiver,commandIntentFilter);
                    Log.d(TAG, "Commander: Command reciever registred");

                }
        )
                .subscribeOn(Schedulers.computation());


    }




    /**
     * Запустить мэйнДиспетчер этого командера, нужно буте выполнить при страрте сервиса
     */
    public void startCommander() {

        CompositeDisposable stateCheckerDispodable=new CompositeDisposable();
        Observable.interval(2, TimeUnit.SECONDS)
                .observeOn(Schedulers.newThread())
                .map(integer->getLastKnownState())
                .filter(state->state!=null)
                .distinctUntilChanged(
                        (intent, intent2) -> intent.getAction().equals(intent2.getAction())
                )
                .subscribe(
                        new Observer<Intent>() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                               if(!stateCheckerDispodable.isDisposed())
                                   stateCheckerDispodable.add(d);
                            }

                            @Override
                            public void onNext(@NonNull Intent intent) {
                                  mServiceInstance.sendBroadcast(intent);
                                               }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG, "onError: in StateChecker");
                            }

                            @Override
                            public void onComplete() {

                            }
                        }
                );

        mMainDispatcher.subscribe(() -> {
            if(!stateCheckerDispodable.isDisposed()){
                stateCheckerDispodable.clear();
            }
            mServiceInstance.stopSelf();
            Log.d(TAG, "Main Discpatcher in Commander done its job");

        });
        Intent startIntent = new Intent(Commander.COMMAND_SERVICE_STARTED);
        sendStateBroadcastForRecord(new Intent(STATE_IDLE));
        mServiceInstance.sendBroadcast(startIntent);
        checkIsSoundDevicesConnected();
        mServiceInstance.registerReceiver(mSoundprofilesBroadcastReciever, SoundprofilesBroadcastReciever.sIntentFilter);

        }


    /**
     * Остановить мэйнДиспетчер, нужно будет запустить при остановке сервиса
     */
    public void stopCommander() {
        mServiceInstance.unregisterReceiver(mSoundprofilesBroadcastReciever);
        sendStateBroadcastForRecord(new Intent(STATE_DISABLING));
        Intent stopIntent = new Intent(Commander.COMMAND_STOP_COMMANDER);
        mServiceInstance.sendBroadcast(stopIntent);

    }

    private void onWait() {
        Log.d(TAG, "onWait: mode is active");
        sendStateBroadcastForRecord(new Intent(STATE_IDLE));
        mReplier.stopWaitingForInquiry();
        mInquirer.stopInqueries();
    }



    private void onListen(BluetoothDevice currentBTSOUNDDevice) {
        Log.d(TAG, "onListen: mode is active with device: " + currentBTSOUNDDevice.getAddress());
        mInquirer.stopInqueries();
        mReplier.waitForInquiry(currentBTSOUNDDevice);
        Intent listeningStateIntent= new Intent (STATE_LISTENING);
        listeningStateIntent.putExtra(BluetoothDevice.EXTRA_DEVICE,currentBTSOUNDDevice);
        sendStateBroadcastForRecord(listeningStateIntent);

    }

    public synchronized Intent getLastKnownState() {
        return lastKnownState;
    }

    public LiveData<BluetoothDevice> getCurrentSoundDeviceLD() {
        return mCurrentSoundDeviceLD;
    }

    private synchronized void setLastKnownState(Intent lastKnownState) {
        this.lastKnownState = lastKnownState;
    }
    private void sendStateBroadcastForRecord(Intent intentToSend){
        setLastKnownState(intentToSend);
        mServiceInstance.sendBroadcast(intentToSend);

    }

    private void onReaching(BluetoothDevice desirableBTSOUNDDevice) {
        Log.d(TAG, "In commander on Reaching, starting to check every paired device");
        Intent reachingStateIntent= new Intent(STATE_REACHING);
        reachingStateIntent.putExtra(BluetoothDevice.EXTRA_DEVICE,desirableBTSOUNDDevice);
        sendStateBroadcastForRecord(reachingStateIntent);

        Set<BluetoothDevice> pairedDevices = new HashSet<>(mAdapter.getBondedDevices());
        pairedDevices = pairedDevices.stream()
                .filter(device -> {
                    return
                            (device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_CELLULAR ||
                                    device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_CORDLESS ||
                                    device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_ISDN ||
                                    device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY ||
                                    device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_SMART ||
                                    device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_UNCATEGORIZED ||
                                    device.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.PHONE_CELLULAR);
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

            if (devices.length > 0) {
                mInquirer.makeInquiry(desirableBTSOUNDDevice.getAddress(), devices);
            }
        }
    }

    private void checkIsSoundDevicesConnected(){
       List<BluetoothDevice>currentlyConnected=mManager.getConnectedDevices();
       if (!currentlyConnected.isEmpty()) {
           mCurrentSoundDeviceLD.postValue(currentlyConnected.get(0));
           Intent soundConnectedIntent=new Intent(Commander.STATE_LISTENING);
           soundConnectedIntent.putExtra(BluetoothDevice.EXTRA_DEVICE,currentlyConnected.get(0));
       setLastKnownState(soundConnectedIntent);
       }

    }

    private void processIntent(Intent incomingIntent) {
        Log.d(TAG, "processing incoming Intent: "+incomingIntent.getAction());
        String intentAction = incomingIntent.getAction();
        BluetoothDevice parcelableDevice=incomingIntent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (intentAction.equals(COMMAND_SERVICE_STARTED)) {
            List<BluetoothDevice> currentlyConnectedDevices = mManager.getConnectedDevices();
            if (currentlyConnectedDevices.isEmpty()) {
                Log.d(TAG, "No connected BTSOUND devices detected");
                onWait();
            } else {
                onListen(currentlyConnectedDevices.get(0));
            }
        }
        if (intentAction.equals(COMMAND_BTSOUND_CONNECTED)) {
            if (parcelableDevice == null) {
                throw new NullParcelableDeviceException();
            }
            onListen(mAdapter.getRemoteDevice(parcelableDevice.getAddress()));
        }

        if (intentAction.equals(COMMAND_BTSOUND_DISCONNECTED)) {
            onWait();
        }
        if(intentAction.equals(COMMAND_USER_SEEKS_DISCONNECT)){
            if (parcelableDevice == null) {
                throw new NullParcelableDeviceException();
            }
            mManager.tryDisconnectFromDevice(parcelableDevice.getAddress()).subscribe();
            onWait();
        }

        if (intentAction.equals(COMMAND_USER_SEEKS_CONNECT)) {
            if (parcelableDevice == null) {
                throw new NullParcelableDeviceException();
            }
            onReaching(parcelableDevice);
        }


    }

}


class NullParcelableDeviceException extends RuntimeException {

}
