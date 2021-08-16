package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothDevice;

public interface  IInquirer<T> {

boolean makeInquiry( String whatAboutMAC,T ... devicesToConnect);


}
