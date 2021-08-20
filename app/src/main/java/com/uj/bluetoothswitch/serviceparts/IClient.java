package com.uj.bluetoothswitch.serviceparts;

import android.view.inputmethod.InputConnection;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;

public interface IClient extends IConnection {
    void startConnectionToSpecifiedMAC(String MAC);

    void stopConnection();
    boolean isRunning();

}