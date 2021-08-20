package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.uj.bluetoothswitch.disposables.StringMessageIOProcessors;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class BTClient implements IClient {
    private final static String TAG="CLIENT";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private  Byte[] mKey;
    private UUID mUuid;
    private final BluetoothAdapter mAdapter=BluetoothAdapter.getDefaultAdapter();
    private final PublishSubject<String> mExternalOutputHook =PublishSubject.create();
    private  final PublishSubject<String> mExternalInputHook =PublishSubject.create();
    private final Subject<Object> mStopSignalSubject=PublishSubject.create();
    private final AtomicBoolean mIsConnected=new AtomicBoolean();




    public BTClient(Byte[] mKey, UUID mUuid, String mName) {
        this.mKey = mKey;
        this.mUuid = mUuid;
        if (mAdapter==null) throw new RuntimeException("Cannot recieve default adapter");


    }

    @Override
    public boolean isRunning() {
        return mIsConnected.get();
    }

    @Override
    public Observable<String> getExternalInputHook() {
        return mExternalInputHook;//.observeOn(Schedulers.io());
    }

    /** Возвращает хук, который ничего не делает, если сендер никуда не подключен */
    @Override
    public Observer<String> getExternalOutputHook(){
        return mExternalOutputHook;

    };

    @Override
    public synchronized void startConnectionToSpecifiedMAC(String MAC){
        mIsConnected.set(true);
        CompositeDisposable hooks=new CompositeDisposable();

            Completable.using(
                    ()->{
                        mAdapter.cancelDiscovery();
                        BluetoothDevice deviceToWorkWith=mAdapter.getRemoteDevice(MAC);
                        BluetoothSocket socket =deviceToWorkWith.createRfcommSocketToServiceRecord(mUuid);
                        Log.d(TAG, "trying to Connect Socket on thread: "+Thread.currentThread().getName());
                        socket.connect();
                        Log.d(TAG, "After Socket.connect()");
                        if (!socket.isConnected()) throw new RuntimeException("Socket isn't connected");
                        return socket;},


                    (socket)->{return Completable.create(
                        (emitter)-> {
                            InputStream inputStream = socket.getInputStream();
                            OutputStream outputStream = socket.getOutputStream();
                            mStopSignalSubject
                                    //.subscribeOn(Schedulers.newThread())

                                    .subscribe(new Observer<Object>() {
                                                   @Override
                                                   public void onSubscribe(@NonNull Disposable d) {
                                                       hooks.add(d);
                                                   }

                                                   @Override
                                                   public void onNext(@NonNull Object o) {
                                                       Log.d(TAG, "Stop signal triggered ");
                                                       if(!emitter.isDisposed())emitter.onComplete();
                                                   }

                                                   @Override
                                                   public void onError(@NonNull Throwable e) {
                                                       if(!emitter.isDisposed())emitter.onComplete();
                                                   }

                                                   @Override
                                                   public void onComplete() {

                                                   }
                                    });

                            Observable.<String>create(em->{
                                if(socket.isConnected()){
                                 String msg=StringMessageIOProcessors.extract(inputStream,mKey);
                                    Log.d(TAG, "Recieved msg: "+ msg);
                                    em.onNext(msg);
                                }else{
                                    if (!emitter.isDisposed())emitter.onError(new IOException("Socket is closed"));
                                }
                            })
                                    .subscribeOn(Schedulers.newThread())
                                    .subscribe(
                                            new Observer<String>() {
                                                @Override
                                                public void onSubscribe(@NonNull Disposable d) {
                                                    hooks.add(d);

                                                }

                                                @Override
                                                public void onNext(@NonNull String s) {
                                                    mExternalInputHook.onNext(s);
                                                }

                                                @Override
                                                public void onError(@NonNull Throwable e) {
                                                    if (!emitter.isDisposed()){
                                                        emitter.onError(e);
                                                    }
                                                }

                                                @Override
                                                public void onComplete() {

                                                }
                                            }
//                                            (s)-> {
//                                                mExternalInputHook.onNext(s);
//                                            },
//                                            (err)->{emitter.onError(err);}
                                    );


                            mExternalOutputHook
                                    .observeOn(Schedulers.io())
                                    .subscribe(new Observer<String>() {
                                                @Override
                                                public void onSubscribe(@NonNull Disposable d) {
                                                    hooks.add(d);
                                                }

                                                @Override
                                                public void onNext(@NonNull String msg) {
                                                    if(socket.isConnected()){
                                                        Log.d(TAG, "SendingMessage: "+ msg);
                                                        try {
                                                            StringMessageIOProcessors.send(msg, outputStream, mKey);
                                                        } catch (IOException e) {
                                                            if(!emitter.isDisposed())emitter.onError(e);
                                                        }
                                                    }
                                                    else if(!emitter.isDisposed()) {
                                                        emitter.onError(new RuntimeException("Socket is closed"));
                                                }
                                                }

                                                @Override
                                                public void onError(@NonNull Throwable e) {

                                                }

                                                @Override
                                                public void onComplete() {

                                                }






                                    })
                        ;});
                        },
                    (BluetoothSocket::close),

                    //Если что этот параметр нужно будет удалить
                    true
                    )

                    .subscribe(()-> {
                                if(!hooks.isDisposed()){
                                    hooks.clear();
                                }
                        stopConnection();
                        Log.d(TAG, "connection completed");
                        },
                            (error)-> {
                                if(!hooks.isDisposed()){
                                    hooks.clear();
                                }
                        stopConnection();
                        Log.d(TAG, "connection failed ");});
        }




    @Override
    public void stopConnection(){
        mIsConnected.set(false);
            mStopSignalSubject.onNext(1);

    }



}
