package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.bluetooth.BluetoothDevice;

import io.reactivex.rxjava3.core.Completable;

public interface  IInquirer<T> {

Completable makeInquiries(String whatAboutMAC, T ... devicesToConnect);
void stopInqueries();


}
