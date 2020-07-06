package com.example.bikebluetoothwifi.thread;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import com.example.bikebluetoothwifi.AplicationState;
import com.example.bikebluetoothwifi.general.DataCalculate;
import com.example.bikebluetoothwifi.io.WifiConnection;

import java.lang.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SendDataRunner implements Runnable {

    private Handler m_Handler;
    private Context m_Context;

    //Guardamos el timestamp de inicio del thread
    private long lastReadTime;

    // Thread to read data from bluetooth;
    Thread BluetoothThread = null;
    Thread PositionThread = null;
    Thread WifiSendDataRunner = null;

    //Datos a enviar
    private String strVelocity = new String("0");
    private String strDistance = new String("0");
    private Double totalTrackDistance = new Double(0.0);

    //Set Middle positio
    private Integer middlePos = 0;
    private Integer firstFive = 0;
    private Integer brd_position = 0;
    private Integer position_send = 0;


    //Mutex para syncronizar la lectura escritura
    ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private Integer sendDato = 0;

    public SendDataRunner(Handler handler, Context context)
    {
        m_Handler = handler;
        m_Context = context;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {

            if (BluetoothThread == null)
            {
                BluetoothThread = new Thread(new BluetoothRunner(local_bluetooth_Handler));
                BluetoothThread.start();
            }

            if (PositionThread == null)
            {
                PositionThread = new Thread(new PositionRunner(local_position_Handler, m_Context));
                PositionThread.start();
            }

            if (WifiSendDataRunner == null)
            {
                WifiSendDataRunner = new Thread(new WifiSendDataRunner());
                WifiSendDataRunner.start();
            }

            //Primera lectura
            lastReadTime = System.currentTimeMillis();
            //when it is stoped, it has
            if(!AplicationState.GetInstance().GetIsRunning())
            {
                middlePos = 0;
                brd_position = 0;
                if(!AplicationState.GetInstance().GetMiddlePosition())
                {
                    firstFive = 0;
                    AplicationState.GetInstance().SetMiddlePosition(true);
                }
            }

            while (AplicationState.GetInstance().GetIsRunning());
        }
    }

    private long miliSecondsElapsedTime() {
        long elapsedSeconds= System.currentTimeMillis()-lastReadTime;
        lastReadTime = System.currentTimeMillis();
        return elapsedSeconds;
    }

    private Handler local_bluetooth_Handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (!AplicationState.GetInstance().GetIsRunning())
                return;

            String brdData = (String)msg.obj;

            if (brdData==null || brdData.isEmpty())
                return;
            if(brdData.startsWith("A"))
                brdData = brdData.substring(1);
            Integer brdDatoInt;
            try{
                brdDatoInt = Integer.parseInt(brdData);
            }catch (final NumberFormatException e) {
                return;
            }
            DataCalculate brdDataFinal = new DataCalculate(brdDatoInt);

            strVelocity = brdDataFinal.CalculateVelocity(miliSecondsElapsedTime()).replace(',','.');
            totalTrackDistance+=brdDataFinal.GetDistance();
            strDistance = String.format("%.2f", totalTrackDistance).replace(',','.');

            String sendData = strVelocity + "|" + strDistance + "|" + brd_position.toString();

            //Enviamos el valor a traves del handler.
            Message msg_send = new Message();
            msg_send.obj = new String(sendData + "|" + middlePos + "|" + position_send  );
            msg_send.setTarget(m_Handler);
            msg_send.sendToTarget();
        }
    };

    private Handler local_position_Handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (!AplicationState.GetInstance().GetIsRunning())
                return;

            Integer position = (Integer) msg.obj;
            if (position == null)
                return;
            position_send = position;
            if(AplicationState.GetInstance().GetMiddlePosition() && firstFive <= 5)
            {
                middlePos = position;
                if(firstFive == 5)
                    AplicationState.GetInstance().SetMiddlePosition(false);
                brd_position = 0;
                firstFive++;
            }
            else if (Integer.signum(middlePos)==Integer.signum(position))
            {
                brd_position = position - middlePos;
            }
            else if (Math.abs(middlePos) < 90)
            {
                brd_position = position - middlePos;
            }
            else
            {
                brd_position = position + Integer.signum(middlePos) * 360 - middlePos;
            }

            sendDato++;
            if (sendDato == 5 ) {
                sendDato = 0;
                Message msg_to_Wifi = new Message();
                msg_to_Wifi.obj = strVelocity + "|" + strDistance + "|" + brd_position.toString();
                msg_to_Wifi.setTarget(WifiConnection.GetInstance().GetWifiDataHandler());
                msg_to_Wifi.sendToTarget();
            }
        }
    };
}
