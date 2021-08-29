package com.uj.bluetoothswitch.serviceparts.soundprofilepart;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.uj.bluetoothswitch.serviceparts.Commander;

public class SoundprofilesBroadcastReciever extends BroadcastReceiver {
    private static  String TAG="SoundProfilesBroadcastReceiver";
    private MutableLiveData<BluetoothDevice> currentBtDeviceLD=new MutableLiveData<>(null);

    public MutableLiveData<BluetoothDevice> getCurrentBTSoundDeviceLivedata(){
        return currentBtDeviceLD;}

    public static IntentFilter sIntentFilter;
    static{sIntentFilter=new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        sIntentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);}

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)||
                intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)){
            Log.d(TAG, "recieved broadcast");
            Intent processedIntent=resolveRecievedIntentToCustomIntent(intent);
            if(processedIntent!=null){
                context.sendBroadcast(processedIntent);
            }
        }
    }


    private Intent resolveRecievedIntentToCustomIntent(Intent recievedStateChangedIntent){
        int extraState=recievedStateChangedIntent.getIntExtra(BluetoothProfile.EXTRA_STATE,-1);
        BluetoothDevice extraDevice=recievedStateChangedIntent
                .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        if (!(extraState==BluetoothProfile.STATE_CONNECTED||
                extraState==BluetoothProfile.STATE_DISCONNECTED)||extraDevice==null){
            Log.d(TAG, "resolveRecievedIntentToCustomIntent: Intent discarded in sorting ");
            return null;
        }
        else{
            Log.d(TAG, "resolveRecievedIntentToCustomIntent: Intent accepted and prepaired to transmit to commander");
            Intent resultingCustomIntent=new Intent();
            String actionToPass;
            if(extraState==BluetoothProfile.STATE_CONNECTED){
                actionToPass= Commander.COMMAND_BTSOUND_CONNECTED;
                putDeviceToLiveData(extraDevice);
            }else{
                actionToPass=Commander.COMMAND_BTSOUND_DISCONNECTED;
                putNullToLiveData();
            }

            resultingCustomIntent.setAction(actionToPass);
            resultingCustomIntent.putExtra(BluetoothDevice.EXTRA_DEVICE,extraDevice);
            return resultingCustomIntent;
        }
    }

    private void putDeviceToLiveData (BluetoothDevice device){
        currentBtDeviceLD.setValue(device);
    }
    private void putNullToLiveData(){
        currentBtDeviceLD.setValue(null);

    }


}