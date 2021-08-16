package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class A2DPManager implements IBluetoothProfileManager {

private final static String TAG= "A2DP _Manager";
private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
private final BluetoothAdapter sAdapter= BluetoothAdapter.getDefaultAdapter();
private BluetoothA2dp mA2dpProxy;
private A2dpServiceListener mA2dpServiceListener=new A2dpServiceListener();
private Method mConnectMethod;
private Method mUnbindMethod;
private Subject<Object> proxyRecievedSubject= PublishSubject.create();
private AtomicBoolean isConstructed=new AtomicBoolean();


public A2DPManager (Context context){
    Log.d(TAG, "A2DPManager: in constructor");
    Completable.create(e->{
        sAdapter.getProfileProxy(context,mA2dpServiceListener,BluetoothProfile.A2DP);
        proxyRecievedSubject.subscribe((o)->{
            if (!e.isDisposed())e.onComplete();
        });
        })

            .subscribe(
                    ()->{mConnectMethod =mA2dpProxy.getClass()
                            .getMethod("connect", BluetoothDevice.class);
                        mConnectMethod.setAccessible(true);
                        mUnbindMethod = sAdapter.getRemoteDevice(PHONEMAC)
                                .getClass()
                                .getMethod("removeBond", (Class[]) null);
                        mUnbindMethod.setAccessible(true);
                        Log.d(TAG, "A2DPManager: Constructor completable done");
                    },
                    (error)->{
                        Log.d(TAG, "A2DPManager: error occured in creation: "+ error.getMessage());
                    }
            );

}


    @Override
    public boolean isConnected(String MAC) {
    BluetoothDevice deviceOfIntrest=sAdapter.getRemoteDevice(MAC);
    return mA2dpProxy.getConnectedDevices().contains(deviceOfIntrest);
    }

    @Override
    public synchronized List<BluetoothDevice> getConnectedDevices() {
        return mA2dpProxy.getConnectedDevices();
    }

    @Override
    public synchronized Completable tryConnectToDevice(String MAC) {
         BluetoothDevice deviceOfIntrest=sAdapter.getRemoteDevice(MAC);
        return Completable.create(e->{invokeConnect(deviceOfIntrest);
            })
            .subscribeOn(Schedulers.io())
            .retry(3)
            .delay(300, TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized Completable tryUnbindFromDevice(String MAC) {
       return Completable.create(e->{invokeUnbind(sAdapter.getRemoteDevice(MAC));
        })
                .subscribeOn(Schedulers.io())
                .retry(3)
                .delay(300, TimeUnit.MILLISECONDS);

    }

    private synchronized void invokeConnect(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        mConnectMethod.invoke(mA2dpProxy, device);
    }

    private synchronized void invokeUnbind(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        mUnbindMethod.invoke(device, (Object[]) null);
    }



    private class A2dpServiceListener implements BluetoothProfile.ServiceListener{
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile==BluetoothProfile.A2DP){
                Log.d(TAG, "onServiceConnected !!!!!");
                mA2dpProxy=(BluetoothA2dp)proxy;
                proxyRecievedSubject.onNext(1);
                }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile==BluetoothProfile.A2DP) {
                Log.d(TAG, "onServiceDisconnected:!!!!!");
                mA2dpProxy=null;
                            }

        }


    }
}
