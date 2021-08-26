package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.uj.bluetoothswitch.disposables.StringMessageIOProcessors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class BTListener implements IListener {
    private final static String TAG="LISTENER";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private final String mName;
    private  Byte[] mKey;
    private UUID mUuid;
    private final BluetoothAdapter mAdapter=BluetoothAdapter.getDefaultAdapter();
    private final PublishSubject<String> mExternalOutputHook =PublishSubject.create();
    private  final PublishSubject<String> mExternalInputHook =PublishSubject.create();
    private final Subject<Object> mStopSignalSubject=PublishSubject.create();
    private final AtomicBoolean mIsConnected=new AtomicBoolean();

    public BTListener(Byte[] mKey, UUID mUuid, String mName) {
        this.mKey = mKey;
        this.mUuid = mUuid;
        this.mName = mName;
        if (mAdapter==null) throw new RuntimeException("Cannot recieve default adapter");
    }

    @Override
    public void startListening(){
        if (mIsConnected.compareAndSet(false,true)){
            Completable.using(
                ()->{
                    mAdapter.cancelDiscovery();
                    Thread.sleep(150);
                    Log.d(TAG, "startListening: ПРобуем получить серверсокет");
                    BluetoothServerSocket SSocket=mAdapter.listenUsingRfcommWithServiceRecord(mName,mUuid);
                    BluetoothSocket socket=null;
                    Thread.sleep(500);

                    Log.d(TAG, "before ssAccept "+SSocket.toString());
                    socket=SSocket.accept();
                    Log.d(TAG, "after ssAccept we mos not apear there");

                    SSocket.close();
                    return socket;},


                (socket)->{return Completable.create(
                        (emitter)-> {
                            InputStream inputStream = socket.getInputStream();
                            OutputStream outputStream = socket.getOutputStream();
                            mStopSignalSubject
                                    .observeOn(Schedulers.newThread())
                                    .subscribe(Void-> {
                                        if(!emitter.isDisposed())emitter.onComplete();
                                    });


                            Observable.<String>create(em->{
                                String msg="";
                            try{
                                   msg = StringMessageIOProcessors.extract(inputStream, mKey);
                                }catch (IOException exc){
                                    if(!emitter.isDisposed()){
                                        emitter.onError(exc);
                                    }
                                }
                                if(!em.isDisposed())em.onNext(msg);
                            })
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(
                                            (msg)->{
                                                mExternalInputHook.onNext(msg);
                                                Log.d(TAG, "Msg recieved in listener: "+msg+"on Thread: "+
                                                        Thread.currentThread().getName());},
                                            (err)->{
                                                Log.d(TAG, "Error occured in message recieving "+ err);
                                            }
                                    );
                                            //mExternalRecieverHook);

                            mExternalOutputHook
                                    //.observeOn(Schedulers.io())
                                    .subscribeOn(Schedulers.io())
                                    .map((msg)->{

                                        try{
                                            StringMessageIOProcessors.send(msg, outputStream, mKey);
                                        }catch (IOException exc){if(!emitter.isDisposed()) {
                                        emitter.onError(exc);}
                                        }
                                    return msg;
                                    })
                                    .subscribe(
                                    (msg)-> {
                                        Log.d(TAG, "Message sended from listener: "+ msg+" on Thread: " +
                                               Thread.currentThread().getName() );
                                        },
                                    (err)->{
                                        Log.d(TAG, "Error ocured on sending from listener: "+err);

                                    }
                            );
                        }
                );
                },
                (socket -> {
                    if(socket.isConnected())socket.close();
                    Log.d(TAG, "Dsisposing connection socket in listener");
                })
        )
                .subscribeOn(Schedulers.io())
               // .retry(5)
                .delay(350, TimeUnit.MILLISECONDS)
                .subscribe(()-> {Log.d(TAG, "connection completed");},
                        (error)-> Log.d(TAG, "connection failed "));

        }
    };
    @Override
    public void stopListening(){
        if (mIsConnected.compareAndSet(true,false)) {
            mStopSignalSubject.onNext(1);
        }
    };

    /** Получить коннектор для получения сообщений, он будет что то посылать только при включенном ресивере*/
    @Override
    public Observable<String> getExternalInputHook(){
           return mExternalInputHook;
    };

    @Override
    public Subject<String> getExternalOutputHook() {

        return mExternalOutputHook;
    }

    @Override
    public boolean isRunning(){

        return mIsConnected.get();
    };


    }






