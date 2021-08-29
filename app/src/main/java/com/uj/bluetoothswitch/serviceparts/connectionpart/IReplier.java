package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.bluetooth.BluetoothDevice;

import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Completable;

public  interface IReplier <T> {
    Completable waitForInquiry(T objectOfIntrest);
    void stopWaitingForInquiry();


}
