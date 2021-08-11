package com.uj.bluetoothswitch;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.uj.bluetoothswitch.disposables.ChannelGate;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.UUID;

import io.reactivex.rxjava3.core.Observable;

public class BTConnectionService extends Service {

   enum ConenctionState{NOCONNECTEDAUDIO,WAITFORINCOMING,PING,TALK}
   private final static String TAG="BTConnectionService";
   private final static Byte[] COMMANDDPATTERN={125,125,125};
   private final static ChannelGate PATTERNGATE=ChannelGate.getSingleModeChannelGate(COMMANDDPATTERN);



    private ConenctionState conenctionState;
   private BluetoothServerSocket serverSocket;
   private BluetoothSocket connectionSocket;

   private BluetoothA2dp a2dpProfile;
   private BluetoothDevice connectedA2DPDevice;
   private final UUID uuid=UUID.fromString("70a381c8-2486-47b8-acad-82d84e367eee");
   private final BluetoothAdapter bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
   private final IntentFilter a2dpConnectionIntentFilter=new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);


   public BTConnectionService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

//TODO
    private Observable<BluetoothDevice> getInputConnectionObservable(@NotNull BluetoothSocket rfcSocket){

      return Observable.<BluetoothDevice,BluetoothSocket>using(
              ()->rfcSocket,

              (socket)->Observable.<BluetoothDevice>create(
                      (emitter)->{
                          if(!socket.isConnected())
                              emitter.onError(new RuntimeException("RFC Socket is not connected"));
                          bluetoothAdapter.cancelDiscovery();
                          InputStream bluetoothInput=socket.getInputStream();
                         DataInputStream dataInputStream =new DataInputStream( PATTERNGATE.singleMatch(bluetoothInput));
                         int lengthOfMeassage=dataInputStream.readInt();

//TODO
                      }
              ),

              (socket)->socket.close()
              );


    }
}