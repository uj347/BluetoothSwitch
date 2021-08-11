package com.uj.bluetoothswitch.disposables;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

public class StringMessageIOProcessors {
static final String TAG="Message processor";
    public static String extract (InputStream inputStream, Byte [] keyPattern) throws IOException {
      String resultingMessage="";
      ChannelGate patternGate=ChannelGate.getSingleModeChannelGate(keyPattern);
        DataInputStream dataInputStream =new DataInputStream( patternGate.singleMatch(inputStream));
        int lengthOfMeassage=dataInputStream.readInt();
        ReadableByteChannel inputChannel= Channels.newChannel(inputStream);
        ByteBuffer buff=ByteBuffer.allocate(2048);
int readed=0;
        //TODO 
        while(readed<lengthOfMeassage){
        readed+=inputChannel.read(buff);
        }
        buff.flip();
        buff.limit(lengthOfMeassage);
        CharBuffer charBuff=Charset.forName("UTF8").decode(buff);
        resultingMessage=charBuff.toString();
        Log.d(TAG, "extract: message recieved from processor:"+resultingMessage);
        return resultingMessage;
    }


    public static void send(String msg, OutputStream outputStream, Byte[] keyPattern) throws IOException{
        BufferedOutputStream outBStream= new BufferedOutputStream(outputStream);
        for (Byte bByte:
             keyPattern) {
          outBStream.write(bByte);
        }
        outBStream.flush();
        DataOutputStream dataOutputStream=new DataOutputStream(outBStream);
        byte [] msgByteRepresentation=msg.getBytes("UTF8");
        int msgLength=msgByteRepresentation.length;
        dataOutputStream.writeInt(msgLength);
        dataOutputStream.write(msgByteRepresentation);
        dataOutputStream.flush();
        Log.d(TAG, "sent message from processor, legth:"+msgLength);




    }


}
