package com.uj.bluetoothswitch;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.uj.bluetoothswitch.serviceparts.SoundProfileManager;
import com.uj.bluetoothswitch.serviceparts.BTClient;
import com.uj.bluetoothswitch.serviceparts.BTInquirer;
import com.uj.bluetoothswitch.serviceparts.BTListener;
import com.uj.bluetoothswitch.serviceparts.BTReplier;
import com.uj.bluetoothswitch.serviceparts.Commander;
import com.uj.bluetoothswitch.serviceparts.IInquirer;
import com.uj.bluetoothswitch.serviceparts.IReplier;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;

public class BTConnectionService extends Service {

    private final static String TAG="BTConnectionService";
   private static final UUID MYUUID= UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static final String MYNAME= "BTSWITCHER";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private static final Byte[] MYKEYPATTERN=new Byte[]{125,125,125};

    private SoundProfileManager mManager;
    private  IInquirer<BluetoothDevice>   mInquirer;
    private IReplier<BluetoothDevice> mReplier;
    private  Commander mCommander;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mManager=new SoundProfileManager(this);
        Observable
                .interval(250, TimeUnit.MILLISECONDS)
                .filter(i->mManager.isFullyConstruted())
                .take(1)
                .subscribe((obj)->{
                    mInquirer=new BTInquirer(new BTClient(MYKEYPATTERN,MYUUID,MYNAME),mManager);
                    mReplier=new BTReplier(new BTListener(MYKEYPATTERN,MYUUID,MYNAME),mManager);
                    mCommander=new Commander(mManager,this,mInquirer,mReplier);
                    Log.d(TAG, "Service on start Command with startID: "+ startId);
                    if(mCommander!=null) {
                        Log.d(TAG, "onStartCommand: Starting commander");
                        mCommander.startCommander();}
                });
    return Service.START_STICKY;

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "In service on destroy");
       if(mCommander!=null) {
           mCommander.stopCommander();
       }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "In service onbind");
        return new BTBinder();
    }

    public class BTBinder extends Binder {
        public Observer<Intent> getInputHook(){
            if (mCommander!=null){
                return mCommander.getInputHook();
            }
            else return null;
        }

    }

}


