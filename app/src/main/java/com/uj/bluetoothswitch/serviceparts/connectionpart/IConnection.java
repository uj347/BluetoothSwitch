package com.uj.bluetoothswitch.serviceparts.connectionpart;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;

public interface IConnection {

    InputStream getInputStream() throws IOException;

    OutputStream getOutputStream() throws IOException;
}
