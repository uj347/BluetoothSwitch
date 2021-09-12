package com.uj.bluetoothswitch.serviceparts;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.uj.bluetoothswitch.serviceparts.broadcastreceivers.SoundprofilesBroadcastReciever;
import com.uj.bluetoothswitch.serviceparts.broadcastreceivers.UserCommandsBroadcastReceiver;
import com.uj.bluetoothswitch.serviceparts.soundprofilepart.SoundProfileManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class BTConnectionService extends Service {

    private final static String TAG = "BTConnectionService";
    private static final UUID MYUUID = UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static final String MYNAME = "BTSWITCHER";
    private static final String PHONEMAC = "F8:C3:9E:8B:28:C6";
    private static final String PLANSHMAC = "74:D2:1D:6B:19:88";

    public static final String COMMAND_USER_SEEKS_CONNECT = "Commander_USER_SEEKS_CONNECT";
    public static final String COMMAND_USER_SEEKS_DISCONNECT = "Commander_USER_SEEKS_DISCONNECT";
    public static final String COMMAND_USER_SEEKS_STOPSERVICE = "Commander_USER_SEEKS_STOPSERVICE";
    public static final String COMMAND_USER_SEEKS_CURRENTSTATE = "Commander_USER_SEEKS_CURRENTSTATE";
    public static final Set<String> USERCOMMAND_LIST=new HashSet<>();
    static {
        USERCOMMAND_LIST.add(COMMAND_USER_SEEKS_CONNECT);
        USERCOMMAND_LIST.add(COMMAND_USER_SEEKS_DISCONNECT);
        USERCOMMAND_LIST.add(COMMAND_USER_SEEKS_STOPSERVICE);
        USERCOMMAND_LIST.add(COMMAND_USER_SEEKS_CURRENTSTATE);
    }

    public static final String STATE_IDLE = "Commander_STATE_IDLE";
    public static final String STATE_LISTENING = "Commander_STATE_LISTENING";
    public static final String STATE_REACHING = "Commander_STATE_REACHING";
    public static final String STATE_DISABLING = "Commander_STATE_DISABLING";
    public static final Map<ServiceState, String> STATE_COMMAND_MAP = new HashMap<>();

    static {
        STATE_COMMAND_MAP.put(ServiceState.STATE_IDLE, BTConnectionService.STATE_IDLE);
        STATE_COMMAND_MAP.put(ServiceState.STATE_DISABLING, BTConnectionService.STATE_DISABLING);
        STATE_COMMAND_MAP.put(ServiceState.STATE_LISTENING, BTConnectionService.STATE_LISTENING);
        STATE_COMMAND_MAP.put(ServiceState.STATE_REACHING, BTConnectionService.STATE_REACHING);
    }

    private BluetoothDevice mCurrentSoundDevice;
    private MutableLiveData<ServiceState> mCurrentStateLD = new MutableLiveData<>();
    private  Commander mCommander;
    private  SoundProfileManager mSoundProfileManager;
    private  AwarenessComponent mAwarenessComponent;

    private NotificationFactory mNotificationfactory;
    private final SoundprofilesBroadcastReciever mSoundProfBroadcastReceiver
            = new SoundprofilesBroadcastReciever();
    private final UserCommandsBroadcastReceiver mUserCommandsBroadcastReceiver =
            new UserCommandsBroadcastReceiver();
    private BroadcastInterpreter mBroadcastInterpreter;
    private final CompositeDisposable mDisposables = new CompositeDisposable();
    private final AtomicBoolean mServiceRunning= new AtomicBoolean();


    public SoundProfileManager exposeSoundProfileManager() {
        return this.mSoundProfileManager;
    }

    public NotificationFactory exposeNotificationfactory() {
        return mNotificationfactory;
    }

    public MutableLiveData<ServiceState> exposeCurrentStateLD() {
        return mCurrentStateLD;
    }

    public void setCurrentState(ServiceState newState) {
        Log.d(TAG, "new state posted: "+newState.name());
        this.mCurrentStateLD.postValue(newState);
    }


    public SoundprofilesBroadcastReciever exposeSoundProfileBR() {
        return mSoundProfBroadcastReceiver;
    }

    public UserCommandsBroadcastReceiver exposeUserCommandsBR() {
        return mUserCommandsBroadcastReceiver;
    }
    public AwarenessComponent exposeAwarenessComponent (){
        return  this.mAwarenessComponent;
    }

    public BroadcastInterpreter exposeBroadcastInterpreter() {
        return mBroadcastInterpreter;
    }


    @Override
    public void onCreate() {
        mNotificationfactory= new NotificationFactory(this);
        mSoundProfileManager= new SoundProfileManager(this);
        mCommander= new Commander(this);
        mBroadcastInterpreter = new BroadcastInterpreter(this);
        mAwarenessComponent= new AwarenessComponent(this);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mDisposables.add(Observable.interval(150, TimeUnit.MILLISECONDS)
                .filter(integer -> mSoundProfileManager.isFullyConstruted())
                .take(1)
                .timeout(10, TimeUnit.SECONDS)
                .subscribe(
                        integer -> {
                           if (!mServiceRunning.get()){
                                mCommander.startCommander();
                                Log.d(TAG, "CommanderStarted");
                                Log.d(TAG, "Notificationfactory created");
                                mSoundProfBroadcastReceiver.registerThisReciver(this);
                                mUserCommandsBroadcastReceiver.registerThisReciver(this);
                                Log.d(TAG, "Receivers registered");
                                mBroadcastInterpreter.startInterpreter();
                                Log.d(TAG, "BroadcastInterpreter Started");
                                mAwarenessComponent.startAwarenessComponent();
                                Log.d(TAG, "Awareness Component Stared");
                                List<BluetoothDevice> connectedDevices = mSoundProfileManager.getConnectedDevices();
                                BluetoothDevice currentDevice =
                                        connectedDevices.isEmpty() ? null : connectedDevices.get(0);

                                Log.d(TAG, "Deciding startMode");
                                if (currentDevice != null) {
                                    mCommander.onListen(currentDevice);
                                    mServiceRunning.set(true);

                                } else {
                                    mCommander.onIdle();
                                    mServiceRunning.set(true);
                                }
                            }else{
                               Log.d(TAG, "Shallow on start service command occured");
                           }
                        },
                        err -> {
                            Log.d(TAG, "Starting of service failed, reason: " + err);
                            BTConnectionService.this.onDestroy();
                        }
                ));
        return Service.START_STICKY;

    }


    @Override
    public void onDestroy() {
        setCurrentState(ServiceState.STATE_DISABLING);
        sendBroadcast(new Intent(STATE_DISABLING));
        mServiceRunning.set(false);
        Log.d(TAG, "In service on destroy");
        mBroadcastInterpreter.stopInterpreter();
        mAwarenessComponent.stopAwarenessComponent();
        unregisterReceiver(mSoundProfBroadcastReceiver);
        unregisterReceiver(mUserCommandsBroadcastReceiver);
        mDisposables.clear();
        mSoundProfileManager.disposeResources();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Было решено отказаться от байндинга");

    }


    public Commander exposeCommander() {
        return mCommander;
    }


}

enum ServiceState {
    STATE_IDLE,
    STATE_LISTENING,
    STATE_REACHING,
    STATE_DISABLING
}





