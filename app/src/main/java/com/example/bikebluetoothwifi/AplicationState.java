package com.example.bikebluetoothwifi;

import android.content.Context;

public class AplicationState {

    private boolean isRunning = false;
    private boolean calculateMiddlePosition = false;
    private boolean hasTcpConnection = false;

    private static AplicationState AplicationStateInstance = null;

    public static AplicationState GetInstance()
    {
        if ( AplicationStateInstance==null )
            AplicationStateInstance = new AplicationState();
        return AplicationStateInstance;
    }

    public boolean GetIsRunning()
    {
        return isRunning;
    }

    public void SetIsRunning(boolean running)
    {
        isRunning = running;
    }

    public void SetMiddlePosition(boolean middlePosition)
    {
        calculateMiddlePosition = middlePosition;
    }

    public boolean GetMiddlePosition()
    {
        return calculateMiddlePosition;
    }

    public void SetHasTcpConnection(boolean tcpConnection)
    {
        hasTcpConnection = tcpConnection;
    }

    public boolean GetHasTcpConnection()
    {
        return hasTcpConnection;
    }
}
