package com.uj.bluetoothswitch.serviceparts.soundprofilepart;

import android.bluetooth.BluetoothDevice;

import java.util.List;

import io.reactivex.rxjava3.core.Completable;

public interface IBluetoothProfileManager {

    boolean isConnectedToDevice(String MAC);
    List<BluetoothDevice> getConnectedDevices();
     Completable tryConnectToSpecifiedDevice(String MAC);
    Completable tryDisconnectFromDevice(String MacToUnbind);
    boolean isConnectedViaThisProfile();
    Completable tryDisconnectFromCurrentDevice();
    void disposeResources();



}
