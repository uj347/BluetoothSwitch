package com.uj.bluetoothswitch.serviceparts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.uj.bluetoothswitch.MainActivity;
import com.uj.bluetoothswitch.R;
import com.uj.bluetoothswitch.serviceparts.connectionpart.BTClient;
import com.uj.bluetoothswitch.serviceparts.connectionpart.BTInquirer;
import com.uj.bluetoothswitch.serviceparts.connectionpart.BTListener;
import com.uj.bluetoothswitch.serviceparts.connectionpart.BTReplier;
import com.uj.bluetoothswitch.serviceparts.connectionpart.IInquirer;
import com.uj.bluetoothswitch.serviceparts.connectionpart.IReplier;
import com.uj.bluetoothswitch.serviceparts.soundprofilepart.SoundProfileManager;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;

public class BTConnectionService extends Service {

    private final static String TAG = "BTConnectionService";
    private static final UUID MYUUID = UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static final String MYNAME = "BTSWITCHER";
    private static final String PHONEMAC = "F8:C3:9E:8B:28:C6";
    private static final String PLANSHMAC = "74:D2:1D:6B:19:88";
    private static final Byte[] MYKEYPATTERN = new Byte[]{125, 125, 125};
    public   static final String NOTIFICATION_CHANNEL_NAME="BTSwitch Service Notification";
    public static final String NOTIFICATION_CHANNEL_ID="BTSwitchChannel";
    public static final int NOTIFICATION_ID=347;




    private SoundProfileManager mManager;
    private IInquirer<BluetoothDevice> mInquirer;
    private IReplier<BluetoothDevice> mReplier;
    private Commander mCommander;
    private LiveData<BluetoothDevice> mCurrentSoundDeviceLD;
    private final Observer<BluetoothDevice> mCurrentDeviceObserver =new Observer<BluetoothDevice>() {
        @Override
        public void onChanged(BluetoothDevice device) {
            if (device==null){
                startForeground(NOTIFICATION_ID,getNoDeviceNotification());
            }else{
                startForeground(NOTIFICATION_ID,getHasDeviceNotification());
            }
        }
    };


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        mManager = new SoundProfileManager(this);
        Observable
                .interval(250, TimeUnit.MILLISECONDS)
                .filter(i -> mManager.isFullyConstruted())
                .take(1)
                .subscribe((obj) -> {
                    mInquirer = new BTInquirer(mManager);
                    mReplier = new BTReplier(mManager);
                    mCommander = new Commander(this, mManager, mInquirer, mReplier);
                    Log.d(TAG, "Service on start Command with startID: " + startId);
                    if (mCommander != null) {
                        Log.d(TAG, "onStartCommand: Starting commander on Thread: "+ Thread.currentThread().getName());
                        mCommander.startCommander();
                        mCurrentSoundDeviceLD=mCommander.getCurrentSoundDeviceLD();
                        startForeground(NOTIFICATION_ID, getNoDeviceNotification());
                    }
                });
        Observable.interval(450,TimeUnit.MILLISECONDS)
                .filter(i->mCurrentSoundDeviceLD!=null)
                .take(1)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(i->mCurrentSoundDeviceLD.observeForever(mCurrentDeviceObserver));
        return Service.START_STICKY;

    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "In service on destroy");
        if (mCommander != null) {
            if(mCommander.getLastKnownState()!=null){
                if(!mCommander.getLastKnownState()
                        .getAction()
                        .equals(Commander.STATE_DISABLING)){
                    mCommander.stopCommander();
                }
            }

        }
        if(mCurrentSoundDeviceLD!=null){
            mCurrentSoundDeviceLD.removeObserver(mCurrentDeviceObserver);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Было решено отказаться от байндинга");

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = NOTIFICATION_CHANNEL_NAME;
            String description = "Channel for BTSwitch Service Notitfications";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification getNoDeviceNotification(){
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingActivityIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);


        Intent stopServiceIntent = new Intent(Commander.COMMAND_STOP_COMMANDER);
        PendingIntent pendingStopServiceIntent =
                PendingIntent.getBroadcast(this, 3, stopServiceIntent, 0);

//Заготовка на будущее_______________________________________
//        Intent disconnectDeviceIntent = new Intent(Commander.COMMAND_USER_SEEKS_DISCONNECT);
//        disconnectDeviceIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mManager.getConnectedDevices().get(0));
//        PendingIntent pendingDisconnectIntent =
//                PendingIntent.getBroadcast(this, 0, notificationIntent, 0);
//___________________________________________________________________________________
       return   new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setContentTitle("BTSwitch")
                        .setContentText("BTSwitchService is active")
                        .setSmallIcon(R.drawable.ic_forground_bt_service)
                        .setContentIntent(pendingActivityIntent)
                        .addAction(new NotificationCompat.Action(R.drawable.ic_stop_service,"STOPSERVICE",pendingStopServiceIntent))
                        .build();
    }

  private Notification  getHasDeviceNotification(){
      Intent notificationIntent = new Intent(this, MainActivity.class);
      PendingIntent pendingActivityIntent =
              PendingIntent.getActivity(this, 0, notificationIntent, 0);
//Заготовка на будущее_______________________________________
        Intent disconnectDeviceIntent = new Intent(Commander.COMMAND_USER_SEEKS_DISCONNECT);
        disconnectDeviceIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, mManager.getConnectedDevices().get(0));
        PendingIntent pendingDisconnectIntent =
                PendingIntent.getBroadcast(this, 2, disconnectDeviceIntent, 0);

      Intent stopServiceIntent = new Intent(Commander.COMMAND_STOP_COMMANDER);
      PendingIntent pendingStopServiceIntent =
              PendingIntent.getBroadcast(this, 3, stopServiceIntent, 0);
//___________________________________________________________________________________
      return   new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
              .setContentTitle("BTSwitch")
              .setContentText("Currently connected: "+ mCurrentSoundDeviceLD.getValue())
              .setSmallIcon(R.drawable.ic_forground_bt_service)
              .setContentIntent(pendingActivityIntent)
              .addAction(new NotificationCompat.Action(R.drawable.ic_disconnect_device,"DISCONNECT DEVICE", pendingDisconnectIntent))
              .addAction(new NotificationCompat.Action(R.drawable.ic_stop_service,"STOPSERVICE",pendingStopServiceIntent))
              .build();
    }


}






