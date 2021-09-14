package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Completable;

public class BTClient implements IClient {
    private final static String TAG = "CLIENT";
    private static final String PHONEMAC = "F8:C3:9E:8B:28:C6";
    private static final String PLANSHMAC = "74:D2:1D:6B:19:88";
    private UUID mUuid;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private final AtomicBoolean mIsConnected = new AtomicBoolean();
    private volatile BluetoothSocket mSocket;


    public BTClient(UUID mUuid, String mName) {
        this.mUuid = mUuid;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (mSocket != null && mSocket.isConnected()) {
            return getSocket().getInputStream();
        } else {
            return null;
        }
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (mSocket != null && mSocket.isConnected()) {
            return getSocket().getOutputStream();
        } else {
            return null;
        }
    }

    private synchronized BluetoothSocket getSocket() {
        return mSocket;
    }

    private synchronized void setSocket(BluetoothSocket socket) {
        this.mSocket = socket;
    }


    @Override
    public Completable startConnectionToSpecifiedMAC(String MAC) {
        BluetoothDevice deviceToConnect = mAdapter.getRemoteDevice(MAC);
        return Completable.create(
                emitter -> {
                    BluetoothSocket tempSocket = null;
                    try {
                        tempSocket = deviceToConnect.createRfcommSocketToServiceRecord(mUuid);
                        mAdapter.cancelDiscovery();
                        tempSocket.connect();
                    } catch (IOException exc) {
                        Log.d(TAG, "atempt to connect throwed exception: " + exc.getMessage());
                        if (!emitter.isDisposed()) {
                            emitter.onError(exc);
                        }
                    }
                    if (tempSocket != null && tempSocket.isConnected()) {
                        setSocket(tempSocket);
                        mIsConnected.set(true);
                        if (!emitter.isDisposed()) {
                            emitter.onComplete();
                        }
                    }

                }
        );
    }


    @Override
    public boolean isConnected() {

        return mIsConnected.get();
    }

    ;

    @Override
    public void stopConnection() throws IOException {
        Log.d(TAG, "stopConnection: is invoked");
        mIsConnected.set(false);
        if (mSocket != null) {
            mSocket.close();
        }
    }


}
