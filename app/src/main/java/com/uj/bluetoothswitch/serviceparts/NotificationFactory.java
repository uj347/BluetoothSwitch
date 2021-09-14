package com.uj.bluetoothswitch.serviceparts;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.uj.bluetoothswitch.MainActivity;
import com.uj.bluetoothswitch.R;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;

public class NotificationFactory {
    public static final String TAG = "NotificationFactory";
    public static final String NOTIFICATION_CHANNEL_NAME = "BTSwitch Service Notification";
    public static final String NOTIFICATION_CHANNEL_ID = "BTSwitchChannel";
    public static final int NOTIFICATION_ID = 347;

    private final BTConnectionService mService;

    public NotificationFactory(BTConnectionService service) {
        this.mService = service;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = NOTIFICATION_CHANNEL_NAME;
            String description = "Channel for BTSwitch Service Notitfications";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = service.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }


    }

    private Notification getNoDeviceNotification() {
        Intent notificationIntent = new Intent(mService, MainActivity.class);
        PendingIntent pendingActivityIntent =
                PendingIntent.getActivity(mService, 0, notificationIntent, 0);


        Intent stopServiceIntent = new Intent(BTConnectionService.COMMAND_USER_SEEKS_STOPSERVICE);
        PendingIntent pendingStopServiceIntent =
                PendingIntent.getBroadcast(mService, 3, stopServiceIntent, 0);

        return new NotificationCompat.Builder(mService, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("BTSwitch")
                .setContentText("BTSwitchService is active")
                .setSmallIcon(R.drawable.ic_forground_bt_service)
                .setContentIntent(pendingActivityIntent)
                .addAction(new NotificationCompat.Action(R.drawable.ic_stop_service, "STOPSERVICE", pendingStopServiceIntent))
                .build();
    }

    private Notification getHasDeviceNotification(BluetoothDevice connectedDevice) {
        Intent notificationIntent = new Intent(mService, MainActivity.class);
        PendingIntent pendingActivityIntent =
                PendingIntent.getActivity(mService, 0, notificationIntent, 0);

        Intent disconnectDeviceIntent = new Intent(BTConnectionService.COMMAND_USER_SEEKS_DISCONNECT);
        PendingIntent pendingDisconnectIntent =
                PendingIntent.getBroadcast(mService, 2, disconnectDeviceIntent, 0);

        Intent stopServiceIntent = new Intent(BTConnectionService.COMMAND_USER_SEEKS_STOPSERVICE);
        PendingIntent pendingStopServiceIntent =
                PendingIntent.getBroadcast(mService, 3, stopServiceIntent, 0);

        return new NotificationCompat.Builder(mService, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("BTSwitch")
                .setContentText("Currently connected: " + connectedDevice)
                .setSmallIcon(R.drawable.ic_forground_bt_service)
                .setContentIntent(pendingActivityIntent)
                .addAction(new NotificationCompat.Action(R.drawable.ic_disconnect_device, "DISCONNECT DEVICE", pendingDisconnectIntent))
                .addAction(new NotificationCompat.Action(R.drawable.ic_stop_service, "STOPSERVICE", pendingStopServiceIntent))
                .build();
    }

    public void postWithNoDevice() {
        Completable
                .fromAction(() -> mService.startForeground(NOTIFICATION_ID, getNoDeviceNotification()))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Log.d(TAG, "Posted With no Device Succesfully"),
                        err -> Log.d(TAG, "Error occured with posting with no device: " +
                                err.getMessage())
                );

    }


    public void postWithConectedDevice(BluetoothDevice connectedDevice) {
        Completable
                .fromAction(() -> mService.startForeground(NOTIFICATION_ID,
                        getHasDeviceNotification(connectedDevice)))
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Log.d(TAG, "Posted With Device Succesfully"),
                        err -> Log.d(TAG, "Error occured with posting with  device: " +
                                err.getMessage())
                );

    }

}
