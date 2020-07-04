package com.example.bikebluetoothwifi.thread;

import android.os.Message;

import com.example.bikebluetoothwifi.AplicationState;
import com.example.bikebluetoothwifi.io.BluetoothConnection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import android.os.Handler;
import android.os.Message;

//Clase que lle los datos que van llegando desde el bluetooth
public class BluetoothRunner implements Runnable {

    private Handler mHandler;

    public BluetoothRunner(Handler handler)
    {
        this.mHandler = handler;
    }

    private final byte delimiter = 10; //This is the ASCII code for a newline character

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] readBuffer = new byte[1024];
                int readBufferPosition = 0;
                int bytesAvailable = BluetoothConnection.GetInstance().GetInputStream().available();
                if (bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    BluetoothConnection.GetInstance().GetInputStream().read(packetBytes);
                    for (int i = 0; i < bytesAvailable; i++) {
                        byte b = packetBytes[i];
                        if (b == delimiter) {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            //Removemos el primero
                            System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                            final String data = new String(encodedBytes, "US-ASCII");
                            readBufferPosition = 0;

                            if (AplicationState.GetInstance().GetIsRunning())
                            {
                                //Enviamos el valor a traves del handler.
                                Message msg = new Message();
                                msg.obj = data;
                                msg.setTarget(mHandler);
                                msg.sendToTarget();
                            }
                        } else {
                            readBuffer[readBufferPosition++] = b;
                        }
                    }
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
