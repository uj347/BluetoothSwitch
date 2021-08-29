package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class StringOutputStream extends DataOutputStream {
    public static final String TAG="StringOutputStream";
    public StringOutputStream(OutputStream out) {

        super(new BufferedOutputStream(out));
    }
    public void writeString(String s)throws IOException {
        super.writeUTF(s);
        Log.d(TAG, "Next step is flushing stream");
        super.flush();
    }
}
