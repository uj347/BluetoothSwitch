package com.uj.bluetoothswitch.serviceparts.connectionpart;

import java.io.IOException;

import io.reactivex.rxjava3.core.Completable;

public interface IClient extends IConnection {

    public static final String NO_CONNECTION = "*347*no cOnNeCtIoN*347*";

    Completable startConnectionToSpecifiedMAC(String MAC);

    void stopConnection() throws IOException;

    boolean isConnected();


}