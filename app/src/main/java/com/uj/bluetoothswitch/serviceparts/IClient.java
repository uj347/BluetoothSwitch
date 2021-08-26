package com.uj.bluetoothswitch.serviceparts;

import android.view.inputmethod.InputConnection;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;

public interface IClient extends IConnection {

    public static final String NO_CONNECTION="*347*no cOnNeCtIoN*347*";

    void startConnectionToSpecifiedMAC(String MAC);

    void stopConnection();
    boolean isRunning();

}