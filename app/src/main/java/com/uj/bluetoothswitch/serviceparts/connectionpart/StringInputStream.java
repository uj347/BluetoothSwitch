package com.uj.bluetoothswitch.serviceparts.connectionpart;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UTFDataFormatException;
import java.util.zip.DataFormatException;

public class StringInputStream extends DataInputStream {
    public static final String TAG = "StringInputStream";

    public StringInputStream(InputStream in) {
        super(new BufferedInputStream(in));
    }

    public String readString() throws IOException {
        try {
            return super.readUTF();
        } catch (EOFException e) {
            Log.d(TAG, "EOF Exception");
            throw new IOException(e);
        } catch (UTFDataFormatException e) {
            Log.d(TAG, "UTFDataFormat exc");
            throw new IOException(e);
        } catch (IOException e) {
            Log.d(TAG, "General IO exception");
            throw new IOException(e);
        }
    }
}
