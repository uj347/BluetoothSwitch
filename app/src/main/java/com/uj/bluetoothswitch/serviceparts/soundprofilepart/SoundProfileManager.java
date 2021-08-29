package com.uj.bluetoothswitch.serviceparts.soundprofilepart;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class SoundProfileManager implements IBluetoothProfileManager {

private final static String TAG= "SoundProfile_Manager";
private static final String PHONEMAC="F8:C3:9E:8B:28:C6";
private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
private BluetoothA2dp mA2dpProxy;
private BluetoothHeadset mHeadsetProxy;
private A2dpServiceListener mServiceListener =new A2dpServiceListener();
private Method mA2DPConnectMethod;
private Method mHeadsetConnectMethod;
private Method mHeadsetDisconnectMethod;
private Method mUnbindMethod;
private Method mA2DPDisconnectMethod;
private Subject<Object> proxyRecievedSubject= PublishSubject.create();
private AtomicBoolean isConstructed=new AtomicBoolean();


public SoundProfileManager(Context context){
    Log.d(TAG, "SoundProfile: in constructor");
    Completable.create(e->{
        mAdapter.getProfileProxy(context, mServiceListener,BluetoothProfile.A2DP);
        mAdapter.getProfileProxy(context, mServiceListener,BluetoothProfile.HEADSET);

        proxyRecievedSubject
                .observeOn(Schedulers.newThread())
                .distinct()
                .take(2)
                .subscribe(
                        (o)->{
                    Log.d(TAG, "recieved command in signalSubj: "+ o);
                        },
                        (err)->{
                            Log.d(TAG, "Error occured in signal subject: "+ err);
                        },
                        ()-> {
                            isConstructed.set(true);
                            if (!e.isDisposed()) e.onComplete();
                        });
        })

            .subscribe(
                    ()->{


                        mA2DPConnectMethod =mA2dpProxy.getClass()
                            .getMethod("connect", BluetoothDevice.class);
                        mA2DPConnectMethod.setAccessible(true);

                        mA2DPDisconnectMethod =mA2dpProxy.getClass()
                                .getMethod("disconnect", BluetoothDevice.class);
                        mA2DPDisconnectMethod.setAccessible(true);
                        //disconnectSink

                        mHeadsetConnectMethod =mHeadsetProxy.getClass()
                                .getMethod("connect", BluetoothDevice.class);
                        mHeadsetConnectMethod.setAccessible(true);


                        mHeadsetDisconnectMethod =mHeadsetProxy.getClass()
                                .getMethod("disconnect", BluetoothDevice.class);
                        mHeadsetDisconnectMethod.setAccessible(true);


                        mUnbindMethod = mAdapter.getRemoteDevice(PHONEMAC)
                                .getClass()
                                .getMethod("removeBond", (Class[]) null);
                        mUnbindMethod.setAccessible(true);




                        Log.d(TAG, " Constructor completable done");
                    },
                    (error)->{
                        Log.d(TAG, " error occured in creation: "+ error.getMessage());
                    }
            );

}

public boolean isFullyConstruted(){
    return isConstructed.get();
}

    @Override
    public boolean isConnectedToDevice(String MAC) {
    BluetoothDevice deviceOfIntrest= mAdapter.getRemoteDevice(MAC);
    return mA2dpProxy.getConnectedDevices().contains(deviceOfIntrest)
            ||mHeadsetProxy.getConnectedDevices().contains(deviceOfIntrest);

}

    @Override
    public boolean isConnectedViaThisProfile() {
       return !mHeadsetProxy.getConnectedDevices().isEmpty()
               &&!mA2dpProxy.getConnectedDevices().isEmpty();
    }

    @Override
    public synchronized List<BluetoothDevice> getConnectedDevices() {
    Set<BluetoothDevice> deviceSet=new HashSet<>();
    deviceSet.addAll(mA2dpProxy.getConnectedDevices());
    deviceSet.addAll(mHeadsetProxy.getConnectedDevices());
    List<BluetoothDevice>resultList=new ArrayList<>(deviceSet);
    return resultList;

    }

    @Override
    public synchronized Completable tryConnectToDevice(String MAC) {
         BluetoothDevice deviceOfIntrest= mAdapter.getRemoteDevice(MAC);
        return Completable.create(e->{
           if (!mAdapter.getBondedDevices().contains(mAdapter.getRemoteDevice(MAC))) {
               Thread.sleep(200);
               deviceOfIntrest.createBond();
               Log.d(TAG, "tryConnectToDevice bonding invoked");
               Thread.sleep(3500);
           }
           Thread.sleep(300);
            invokeConnectA2DP(deviceOfIntrest);
            invokeConnectHeadset(deviceOfIntrest);
            Log.d(TAG, "tryConnectToDevice: connection invoked");
            if(!e.isDisposed()){e.onComplete();}
            });

        }

    @Override
    public synchronized Completable tryDisconnectFromDevice(String MAC) {
       return Completable.create(e->{
           invokeDisconnectA2DP(mAdapter.getRemoteDevice(MAC));
           invokeDisconnectHeadset(mAdapter.getRemoteDevice(MAC));
//           Thread.sleep(200);
//           invokeUnbind(sAdapter.getRemoteDevice(MAC));
           if(!e.isDisposed()){e.onComplete();}
        });
                //.subscribeOn(Schedulers.io());
//                .delay(50, TimeUnit.MILLISECONDS);

    }

    private synchronized void invokeConnectA2DP(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        if (mA2DPConnectMethod !=null) {
            mA2DPConnectMethod.invoke(mA2dpProxy, device);
        }
    }

    private synchronized void invokeDisconnectA2DP(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        if(mA2DPDisconnectMethod !=null) {
            mA2DPDisconnectMethod.invoke(mA2dpProxy, device);
        }
    }

    private synchronized void invokeUnbind(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
       if(mUnbindMethod!=null) {
           mUnbindMethod.invoke(device, (Object[]) null);
       }
    }

    private synchronized void invokeConnectHeadset(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        if (mHeadsetConnectMethod !=null) {
            mHeadsetConnectMethod.invoke(mHeadsetProxy, device);
        }
    }

    private synchronized void invokeDisconnectHeadset(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        if(mHeadsetDisconnectMethod !=null) {
            mHeadsetDisconnectMethod.invoke(mHeadsetProxy, device);
        }
    }



    private class A2dpServiceListener implements BluetoothProfile.ServiceListener{
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile==BluetoothProfile.A2DP){
                Log.d(TAG, "onServiceConnected !!!!!");
                mA2dpProxy=(BluetoothA2dp)proxy;
                proxyRecievedSubject.onNext(1);
                }
            if (profile==BluetoothProfile.HEADSET){
                Log.d(TAG, "onServiceConnected !!!!!");
                mHeadsetProxy =(BluetoothHeadset) proxy;
                proxyRecievedSubject.onNext(2);
            }

        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile==BluetoothProfile.A2DP) {
                Log.d(TAG, "onServiceDisconnected:!!!!!");
                mA2dpProxy=null;
                            }
            if (profile==BluetoothProfile.HEADSET) {
                Log.d(TAG, "onServiceDisconnected:!!!!!");
                mHeadsetProxy =null;
            }
        }


    }
}
