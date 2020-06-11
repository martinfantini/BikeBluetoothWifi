package com.example.bikebluetoothwifi.io;

import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnection {

    private InputStream inputBluetoothStream= null;
    private OutputStream outputBluetoothStream = null;

    private static BluetoothConnection BluetoothConnectionInstance = null;

    public static BluetoothConnection GetInstance()
    {
        if ( BluetoothConnectionInstance==null )
            BluetoothConnectionInstance = new BluetoothConnection();
        return BluetoothConnectionInstance;
    }
    public void SetInputStream(InputStream inputStream){
        inputBluetoothStream = inputStream;
    }

    public void SetOutputStream(OutputStream outputStream){
        outputBluetoothStream = outputStream;
    }

    public InputStream GetInputStream() {
        return inputBluetoothStream;
    }

    public OutputStream GetOutputStream(){
        return outputBluetoothStream;
    }

    public boolean IsBluetoothConnected()
    {
        if (inputBluetoothStream != null && outputBluetoothStream!=null )
            return true;
        return false;
    }
}
