package com.example.bikebluetoothwifi.thread;

import android.os.Handler;
import android.os.Message;

import com.example.bikebluetoothwifi.io.WifiConnection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class WifiRunner implements Runnable {

    private String serverMessage;
    private BufferedReader in;

    private Message msg;

    @Override
    public void run() {
        //here you must put your computer's IP address.
        InetAddress serverAddr = null;
        try {
            serverAddr = InetAddress.getByName(WifiConnection.GetInstance().getIp());
            //create a socket to make the connection with the server
            Socket socket = null;

            socket = new Socket(serverAddr, Integer.parseInt(WifiConnection.GetInstance().getPort()));

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            WifiConnection.GetInstance().SetOutStream( new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true) );

            //Enviamos el valor a traves del handler.
            msg = new Message();
            msg.obj = "Connected to Wifi";
            msg.setTarget(WifiConnection.GetInstance().getHandeler());
            msg.sendToTarget();

            //in this while the client listens for the messages sent by the server
            while (!Thread.currentThread().isInterrupted()) {
                serverMessage = in.readLine();

                //Enviamos el valor a traves del handler.
                msg = new Message();
                msg.obj = serverMessage;
                msg.setTarget(WifiConnection.GetInstance().getHandeler());
                msg.sendToTarget();

                serverMessage = null;
            }
        } catch (UnknownHostException e) {
            WifiConnection.GetInstance().sendMessage(e.toString());
        } catch (IOException e) {
            WifiConnection.GetInstance().sendMessage(e.toString());
        }
    }
}
