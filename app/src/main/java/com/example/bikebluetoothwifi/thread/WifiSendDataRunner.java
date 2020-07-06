package com.example.bikebluetoothwifi.thread;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import com.example.bikebluetoothwifi.AplicationState;
import com.example.bikebluetoothwifi.io.WifiConnection;

public class WifiSendDataRunner  implements Runnable  {

    private String strDato2Send = null;
    private Handler local_wifi_Handler;
    private HandlerThread mHandlerThread;

    public WifiSendDataRunner()
    {
        mHandlerThread = new HandlerThread("SendDataWifi");
        mHandlerThread.start();
        local_wifi_Handler = new Handler(mHandlerThread.getLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {

                if (!AplicationState.GetInstance().GetIsRunning())
                    return;

                String strDato = (String) msg.obj;
                if (strDato == null)
                    return;
                strDato2Send = strDato;
            }
        };
        WifiConnection.GetInstance().SetWifiDataHandler(local_wifi_Handler);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted())
        {
            while (AplicationState.GetInstance().GetIsRunning())
            {
                if(AplicationState.GetInstance().GetHasTcpConnection() && strDato2Send !=null ) {
                    WifiConnection.GetInstance().sendMessage(strDato2Send);
                    strDato2Send = null;
                }
            }
        }
    }
}
