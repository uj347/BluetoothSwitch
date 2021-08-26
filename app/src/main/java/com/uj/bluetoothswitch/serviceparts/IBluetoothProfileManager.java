package com.uj.bluetoothswitch.serviceparts;

import android.bluetooth.BluetoothDevice;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;

public interface IBluetoothProfileManager {

    boolean isConnectedToDevice(String MAC);
    List<BluetoothDevice> getConnectedDevices();
     Completable tryConnectToDevice(String MAC);
    Completable tryDisconnectFromDevice(String MacToUnbind);
    boolean isConnectedViaThisProfile();


}
