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
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

public class SoundProfileManager implements IBluetoothProfileManager {

    private final static String TAG = "SoundProfile_Manager";
    private static final String PHONEMAC = "F8:C3:9E:8B:28:C6";
    private final BluetoothAdapter mAdapter;
    private BluetoothA2dp mA2dpProxy;
    private BluetoothHeadset mHeadsetProxy;
    private ConstructorListener mConstructorListener = new ConstructorListener();
    private reNewSoundProfilesListener mRenewListener = new reNewSoundProfilesListener();
    private Method mA2DPConnectMethod;
    private Method mHeadsetConnectMethod;
    private Method mHeadsetDisconnectMethod;
    private Method mA2DPDisconnectMethod;
    private Subject<Object> proxyRecievedSubject = PublishSubject.create();
    private AtomicBoolean isConstructed = new AtomicBoolean();
    private final Context mContext;
    private final CompositeDisposable mDisposable = new CompositeDisposable();


    public SoundProfileManager(Context context) {
        Log.d(TAG, "SoundProfile: in constructor");
        this.mAdapter=BluetoothAdapter.getDefaultAdapter();
        this.mContext = context;
        mDisposable.add(Completable.create(e -> {
                    mAdapter.getProfileProxy(context, mConstructorListener, BluetoothProfile.A2DP);
                    mAdapter.getProfileProxy(context, mConstructorListener, BluetoothProfile.HEADSET);

                    mDisposable.add(
                            proxyRecievedSubject
                                    .observeOn(Schedulers.newThread())
                                    .distinct()
                                    .take(2)
                                    .subscribe(
                                            (o) -> {
                                                Log.d(TAG, "recieved command in signalSubj: " + o);
                                            },
                                            (err) -> {
                                                Log.d(TAG, "Error occured in signal subject: " + err);
                                            },
                                            () -> {
                                                isConstructed.set(true);
                                                if (!e.isDisposed()) e.onComplete();
                                            })
                    );
                })
                .subscribe(
                                () -> {
                                    Log.d(TAG, " Constructor completable done");
                                },
                                (error) -> {
                                    Log.d(TAG, " error occured in creation: " + error.getMessage());
                                }
                        )
        );
    }

    public boolean isFullyConstruted() {
        return isConstructed.get();
    }

    @Override
    public boolean isConnectedToDevice(String MAC) {
        BluetoothDevice deviceOfIntrest = mAdapter.getRemoteDevice(MAC);
        return mA2dpProxy.getConnectedDevices().contains(deviceOfIntrest)
                || mHeadsetProxy.getConnectedDevices().contains(deviceOfIntrest);

    }
    //TODO Переделть обновление прокси на таймед-реализацию

    @Override
    public boolean isConnectedViaThisProfile() {
        return !mHeadsetProxy.getConnectedDevices().isEmpty()
                || !mA2dpProxy.getConnectedDevices().isEmpty();
    }

    @Override
    public synchronized List<BluetoothDevice> getConnectedDevices() {
        Set<BluetoothDevice> deviceSet = new HashSet<>();
        deviceSet.addAll(mA2dpProxy.getConnectedDevices());
        deviceSet.addAll(mHeadsetProxy.getConnectedDevices());
        List<BluetoothDevice> resultList = new ArrayList<>(deviceSet);
        return resultList;

    }

    @Override
    public synchronized Completable tryConnectToSpecifiedDevice(String MAC) {
        BluetoothDevice deviceOfIntrest = mAdapter.getRemoteDevice(MAC);
        return Completable.create(e -> {
            if (!mAdapter.getBondedDevices().contains(mAdapter.getRemoteDevice(MAC))) {
                Thread.sleep(200);
                deviceOfIntrest.createBond();
                Log.d(TAG, "tryConnectToDevice bonding invoked");
                Thread.sleep(3500);
            }
                invokeConnectA2DP(deviceOfIntrest);
                invokeConnectHeadset(deviceOfIntrest);

            Log.d(TAG, "tryConnectToDevice: connection invoked");
            if (!e.isDisposed()) {
                e.onComplete();
            }
        });


    }

    @Override
    public Completable tryDisconnectFromCurrentDevice() {
        if (!this.isFullyConstruted()) {
            return Completable.error(new RuntimeException("soundManager isn't fully constructed"));
        } else {
            List<BluetoothDevice> currentlyConectedDevices = this.getConnectedDevices();
            return Observable.fromIterable(currentlyConectedDevices).take(1)
                    .concatMapCompletableDelayError(bluetoothDevice -> {
                        return tryDisconnectFromDevice(bluetoothDevice.getAddress());
                    });
        }
    }

    @Override
    public synchronized Completable tryDisconnectFromDevice(String MAC) {
        return Completable.create(e -> {
            invokeDisconnectA2DP(mAdapter.getRemoteDevice(MAC));
            invokeDisconnectHeadset(mAdapter.getRemoteDevice(MAC));
            if (!e.isDisposed()) {
                e.onComplete();
            }
        });
    }

    private synchronized void invokeConnectA2DP(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        if (mA2DPConnectMethod != null) {
            mA2DPConnectMethod.invoke(mA2dpProxy, device);
        }
    }

    private synchronized void invokeDisconnectA2DP(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException{
        if (mA2DPDisconnectMethod != null) {
            mA2DPDisconnectMethod.invoke(mA2dpProxy, device);
        }
    }


    private synchronized void invokeConnectHeadset(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        if (mHeadsetConnectMethod != null) {
            mHeadsetConnectMethod.invoke(mHeadsetProxy, device);
        }
    }

    private synchronized void invokeDisconnectHeadset(BluetoothDevice device) throws InvocationTargetException, IllegalAccessException {
        if (mHeadsetDisconnectMethod != null) {
           mHeadsetDisconnectMethod.invoke(mHeadsetProxy, device);
        }

    }


    @Override
    public void disposeResources() {
        Log.d(TAG, "Disposing Sound profile manager resources");
        mDisposable.clear();
        if (mA2dpProxy != null) {
            mAdapter.closeProfileProxy(BluetoothProfile.A2DP, mA2dpProxy);
        }
        if (mHeadsetProxy != null) {
            mAdapter.closeProfileProxy(BluetoothProfile.HEADSET, mHeadsetProxy);
        }
    }

    //TODO
    private void renewA2DPProxy()throws InterruptedException {
        Log.d(TAG, "renewHEADSETProxy: Invoking renewal Of headset");
       if (mA2dpProxy!=null) {
               mAdapter.closeProfileProxy(BluetoothProfile.A2DP,mA2dpProxy);
       }
        mAdapter.getProfileProxy(mContext, mRenewListener, BluetoothProfile.A2DP);
        Thread.sleep(50);
    }

    //TODO
    private void renewHEADSETProxy()throws InterruptedException {
        Log.d(TAG, "renewHEADSETProxy: Invoking renewal Of headset");
        if (mHeadsetProxy!=null) {
            mAdapter.closeProfileProxy(BluetoothProfile.HEADSET,mHeadsetProxy);

        }
        mAdapter.getProfileProxy(mContext, mRenewListener, BluetoothProfile.HEADSET);
        Thread.sleep(50);
    }

    private void extractMethodsFromProfileProxy(int profile, BluetoothProfile proxy) throws NoSuchMethodException {
        if (profile == BluetoothProfile.A2DP) {
            mA2DPConnectMethod = mA2dpProxy.getClass()
                    .getMethod("connect", BluetoothDevice.class);
            mA2DPConnectMethod.setAccessible(true);

            mA2DPDisconnectMethod = mA2dpProxy.getClass()
                    .getMethod("disconnect", BluetoothDevice.class);
            mA2DPDisconnectMethod.setAccessible(true);

        }
        if (profile == BluetoothProfile.HEADSET) {

            mHeadsetConnectMethod = mHeadsetProxy.getClass()
                    .getMethod("connect", BluetoothDevice.class);
            mHeadsetConnectMethod.setAccessible(true);


            mHeadsetDisconnectMethod = mHeadsetProxy.getClass()
                    .getMethod("disconnect", BluetoothDevice.class);
            mHeadsetDisconnectMethod.setAccessible(true);

        }
    }

    private void setProxyMethodsToNull(int profile,String context) {
        Log.d(TAG, "setProxyMethodsToNull from : "+ context);
        if (profile == BluetoothProfile.A2DP) {
            mA2DPConnectMethod = null;
            mA2DPDisconnectMethod = null;
        }
        if (profile == BluetoothProfile.HEADSET) {
            mHeadsetConnectMethod = null;
            mHeadsetDisconnectMethod = null;
        }
    }
    private void setProxiesToNull(int profile, String context){
        Log.d(TAG, "setProxiesToNull invoked from context: "+context);
        if (profile == BluetoothProfile.A2DP) {
            mA2dpProxy = null;
        }
        if (profile == BluetoothProfile.HEADSET) {
            mHeadsetProxy = null;
        }
    }





    private class reNewSoundProfilesListener implements BluetoothProfile.ServiceListener {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.d(TAG, "onServiceConnected in renewListener");
            try {
                extractMethodsFromProfileProxy(profile,proxy);
            } catch (NoSuchMethodException e) {
                Log.d(TAG, "onServiceConnected: Extraction of proxy methods failed");
            }
            if (profile == BluetoothProfile.A2DP) {
                Log.d(TAG, "A2DP renewed");
                mA2dpProxy = (BluetoothA2dp) proxy;
            }
            if (profile == BluetoothProfile.HEADSET) {
                Log.d(TAG, "Headset renewed");
                mHeadsetProxy = (BluetoothHeadset) proxy;
            }


        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.d(TAG, "onServiceDisconnected in renewListener");
            setProxyMethodsToNull(profile, "Renew listener");
            setProxiesToNull(profile,"Renew listener");
        }
    }

    private class ConstructorListener implements BluetoothProfile.ServiceListener {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                Log.d(TAG, "onServiceConnected !!!!!");
                mA2dpProxy = (BluetoothA2dp) proxy;
                proxyRecievedSubject.onNext(1);
            }
            if (profile == BluetoothProfile.HEADSET) {
                Log.d(TAG, "onServiceConnected !!!!!");
                mHeadsetProxy = (BluetoothHeadset) proxy;
                proxyRecievedSubject.onNext(2);
            }
            try {
                extractMethodsFromProfileProxy(profile,proxy);
            } catch (NoSuchMethodException e) {
                Log.d(TAG, "onServiceConnected: Extraction of proxy methods failed");
            }

        }

        @Override
        public void onServiceDisconnected(int profile) {
            setProxyMethodsToNull(profile, "Constructor listener");
            setProxiesToNull(profile,"Constructor listener");
        }


    }
}

