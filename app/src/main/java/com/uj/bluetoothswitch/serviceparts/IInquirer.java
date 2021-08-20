package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothDevice;

public interface  IInquirer<T> {

void makeInquiry( String whatAboutMAC,T ... devicesToConnect);
void stopInqueries();


}
