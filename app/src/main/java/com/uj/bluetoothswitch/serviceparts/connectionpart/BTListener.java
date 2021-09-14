package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.uj.bluetoothswitch.BluetoothSwitcherApp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.rxjava3.core.Completable;

public class BTListener implements IListener {
    private final static String TAG = "BTLISTENER";
    private static final String PHONEMAC = "F8:C3:9E:8B:28:C6";
    private static final String PLANSHMAC = "74:D2:1D:6B:19:88";
    private final String mName;
    private UUID mUuid;
    private final BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothServerSocket mServerSocket;
    private volatile BluetoothSocket mSocket;
    private final AtomicBoolean mIsConnected = new AtomicBoolean();

    public BTListener(UUID mUuid, String mName) {
        this.mUuid = mUuid;
        this.mName = mName;
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
    public Completable startListening() {
        return Completable.create(
                emitter -> {
                    BluetoothSocket tempSocket = null;
                    BluetoothServerSocket serverSocket = null;
                    try {
                        serverSocket = mAdapter.listenUsingRfcommWithServiceRecord(BluetoothSwitcherApp.APP_NAME, mUuid);
                        mAdapter.cancelDiscovery();
                        Log.d(TAG, "Before incoming connection accepted");
                        tempSocket = serverSocket.accept();
                        Log.d(TAG, "Incomming connection accepted with socket : " + tempSocket.toString());
                        serverSocket.close();
                    } catch (IOException exc) {
                        Log.d(TAG, "Error occured in accepting incomin connection: " + exc.getMessage());
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
    public void stopConnection() throws IOException {
        Log.d(TAG, "stopConnection invoked");
        mIsConnected.set(false);
        if (mServerSocket != null) {
            mServerSocket.close();
        }
        if (mSocket != null) {
            mSocket.close();

        }
    }


    @Override
    public boolean isConnected() {

        return mIsConnected.get();
    }

    ;


};






