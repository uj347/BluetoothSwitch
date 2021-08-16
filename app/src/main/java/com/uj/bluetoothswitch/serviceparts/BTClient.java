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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class BTClient implements IClient {
    private final static String TAG="SENDER";
    private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
    private static final  String PLANSHMAC="74:D2:1D:6B:19:88";
    private  Byte[] mKey;
    private UUID mUuid;
    private final BluetoothAdapter mAdapter=BluetoothAdapter.getDefaultAdapter();
    private final PublishSubject<String> mExternalSenderHook =PublishSubject.create();
    private  final PublishSubject<String> mExternalRecieverHook=PublishSubject.create();
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
        return mExternalRecieverHook;
    }

    @Override
    public void starConnectionToSpecifiedMAC(String MAC){

        if (mIsConnected.compareAndSet(false,true)) {
            Completable.using(
                    ()->{
                        mAdapter.cancelDiscovery();
                        BluetoothDevice deviceToWorkWith=mAdapter.getRemoteDevice(MAC);
                        BluetoothSocket socket =deviceToWorkWith.createRfcommSocketToServiceRecord(mUuid);
                        Log.d(TAG, "trying to Connect Socket on thread: "+Thread.currentThread().getName());
                        Completable
                                .create(e-> {
                            socket.connect();
                                    if (!e.isDisposed()){
                                        if (socket.isConnected()) e.onComplete();
                                        else  e.onError(new RuntimeException("Cannot connect Socket"));
                                    }
                        })
                        .retry(3)
                                .subscribe(
                                        ()-> Log.d(TAG, "Socket is connected"),
                                        (exc)-> Log.d(TAG, "Error in socketConnection: "+exc.getMessage())
                                );

                        Log.d(TAG, "Socket most be connected+ "+ socket
                        .toString());
                        return socket;},


                    (socket)->{return Completable.create(
                        (emitter)-> {
                            InputStream inputStream = socket.getInputStream();
                            OutputStream outputStream = socket.getOutputStream();
                            mStopSignalSubject
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(Void-> {
                                        if(!emitter.isDisposed())emitter.onComplete();
                                    });
                            Observable.<String>create(em->{
                               String msg=StringMessageIOProcessors.extract(inputStream,mKey);
                               if(!em.isDisposed())em.onNext(msg);
                            })
                                    .subscribeOn(Schedulers.io())
                                    .subscribe(mExternalRecieverHook);

                            mExternalSenderHook.subscribeOn(Schedulers.io()).subscribe(
                                    (msg)-> StringMessageIOProcessors.send(msg,outputStream,mKey)
                            );
                           }
                        );
                    },
                    (BluetoothSocket::close)
                    )
                    .subscribeOn(Schedulers.io())
                    //.retry(5)
                    .delay(750, TimeUnit.MILLISECONDS)
                    .subscribe(()-> {Log.d(TAG, "connection completed");},
                            (error)-> Log.d(TAG, "connection failed "));

        }
        };



    @Override
    public void stopConnection(){
        if (mIsConnected.compareAndSet(true,false)) {
            mStopSignalSubject.onNext(1);
        }
    }

    /** Возвращает хук, который ничего не делает, если сендер никуда не подключен */
    @Override
    public Observer<String> getExternalOutputHook(){
        return mExternalSenderHook;

    };

}
