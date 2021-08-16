package com.uj.bluetoothswitch;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.uj.bluetoothswitch.disposables.ChannelGate;
import com.uj.bluetoothswitch.disposables.StringMessageIOProcessors;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class BTConnectionService extends Service {

   enum ConenctionState{NO_CONNECTED_A2DP, A2DP_CONNECTED,REACHING}
   private final static String TAG="BTConnectionService";
   private static final UUID MYUUID= UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
    private static final String NAME= "BTSWITCHER";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private static final Byte[] KEYPATTERN=new Byte[]{125,125,125};



   private final BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
   private final IntentFilter a2dpConnectionIntentFilter=new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);


    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }



}

