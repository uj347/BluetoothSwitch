package com.uj.bluetoothswitch.serviceparts.connectionpart;

import java.io.IOException;

import io.reactivex.rxjava3.core.Completable;

public interface IListener extends IConnection{
    public static final String NO_CONNECTION="*347*no cOnNeCtIoN*347*";

    Completable startListening();

    void stopConnection() throws IOException;

    boolean isConnected();


}
