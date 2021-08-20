package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import io.reactivex.rxjava3.core.Observer;

public class SoundprofilesBroadcastReciever extends BroadcastReceiver {
    private static  String TAG="A2DPBroadcatReciever";
    private final Observer<Intent> mIntentHook;
    public SoundprofilesBroadcastReciever(Observer<Intent> commanderHook){
        this.mIntentHook=commanderHook;
    };

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
                mIntentHook.onNext(processedIntent);
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
            String actionToPass=extraState==BluetoothProfile.STATE_CONNECTED?
                    Commander.COMMAND_BTSOUND_CONNECTED :Commander.COMMAND_BTSOUND_DISCONNECTED;
            String macAdressToPass=extraDevice.getAddress();
            resultingCustomIntent.setAction(actionToPass);
            resultingCustomIntent.putExtra("DEVICE",macAdressToPass);
            return resultingCustomIntent;
        }
    }


}