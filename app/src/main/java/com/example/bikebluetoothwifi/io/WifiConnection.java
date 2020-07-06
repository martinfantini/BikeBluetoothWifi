package com.example.bikebluetoothwifi.io;

import android.os.Handler;
import android.widget.Toast;

import com.example.bikebluetoothwifi.thread.WifiRunner;

import java.io.PrintWriter;

//Sigletone to save all information of the Wifi/TCP connection.

public class WifiConnection {

    private String strLastError = "Not connected";

    private String strIp;
    private String strPort;

    private PrintWriter outStream = null;

    private Handler mHandler;
    private Thread wifiRunnerThread;

    private static WifiConnection WifiConnectionInstance = null;

    private Handler wifiSendDataHandler;

    public static WifiConnection GetInstance()
    {
        if ( WifiConnectionInstance==null )
            WifiConnectionInstance = new WifiConnection();
        return WifiConnectionInstance;
    }

    public void setIp(String strIp)
    {
        this.strIp = strIp;
    }
    public String getIp(){ return this.strIp; }

    public void setPort(String strPort)
    {
        this.strPort = strPort;
    }
    public String getPort() {return this.strPort; }

    public String getLastError()
    {
        return strLastError;
    }
    public void getLastError(String LastError)
    {
        this.strLastError = LastError;
    }

    public void SetOutStream(PrintWriter outStream)
    {
        this.outStream = outStream;
    }

    public void setHandeler(Handler handler)
    {
        this.mHandler = handler;
    }
    public Handler getHandeler()
    {
        return this.mHandler;
    }

    public boolean IsWifiConnected()
    {
        return (outStream != null );
    }

    public void sendMessage(String message){
        if (outStream != null && !outStream.checkError()) {
            outStream.println(message);
            outStream.flush();
        }
    }

    public Thread getWifiRunnerThread()
    {
        return this.wifiRunnerThread;
    }

    public void setWifiRunnerThread(Thread wifiRunnerThread)
    {
        this.wifiRunnerThread = wifiRunnerThread;
    }

    public void SetWifiDataHandler(Handler dataHandler)
    {
        wifiSendDataHandler  = dataHandler;
    }

    public Handler GetWifiDataHandler()
    {
        return wifiSendDataHandler;
    }
}
