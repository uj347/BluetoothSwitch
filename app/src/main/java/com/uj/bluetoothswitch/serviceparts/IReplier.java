package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothDevice;

import java.util.function.Consumer;

public  interface IReplier <T> {
    void waitForInquiry(T objectOfIntrest);
    void stopWaitingForInquiry();


}
