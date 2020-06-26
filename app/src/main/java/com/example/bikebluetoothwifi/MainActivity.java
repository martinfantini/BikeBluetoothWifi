package com.example.bikebluetoothwifi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bikebluetoothwifi.general.DataCalculate;
import com.example.bikebluetoothwifi.io.BluetoothConnection;
import com.example.bikebluetoothwifi.io.WifiConnection;
import com.example.bikebluetoothwifi.thread.BluetoothRunner;
import com.example.bikebluetoothwifi.thread.PositionRunner;
import com.example.bikebluetoothwifi.thread.SendDataRunner;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private TextView textVelocity;
    private TextView textDistance;
    private Chronometer chrTime;
    private TextView textInclination;
    private TextView textCenter;
    private TextView textMeasure;

    private Button startBtn;
    private Button stopBtn;
    private Button wifiConfig;
    private Button blueConfig;

    private Thread SendDataThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiConnection.GetInstance().setHandeler( wifiHandler );

        textVelocity = (TextView) findViewById(R.id.show_velocity);
        textDistance = (TextView) findViewById(R.id.show_distance);
        chrTime = (Chronometer) findViewById(R.id.show_time);
        chrTime.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                chrTime = chronometer;
            }
        });
        textInclination = (TextView) findViewById(R.id.show_inclination);
        textCenter = (TextView) findViewById(R.id.show_center);
        textMeasure  = (TextView) findViewById(R.id.show_measure);

        startBtn = (Button) findViewById(R.id.start_running);

        stopBtn = (Button) findViewById(R.id.stop_running);

        blueConfig = (Button) findViewById(R.id.bluetooth_config);
        blueConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent k = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivity(k);
                finish();
            }
        });

        wifiConfig = (Button) findViewById(R.id.wifi_config);
        wifiConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent k = new Intent(MainActivity.this, WifiActivity.class);
                startActivity(k);
                finish();
            }
        });

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!AplicationState.GetInstance().GetIsRunning()){

                    if (!BluetoothConnection.GetInstance().IsBluetoothConnected()){
                        Toast.makeText(v.getContext().getApplicationContext(),"Bluetooth is not connected",Toast.LENGTH_LONG).show();
                        return;
                    }

                    StartRunning();
                }
            }
        });

        stopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                StopRunning();

                if(AplicationState.GetInstance().GetHasTcpConnection())
                {
                    WifiConnection.GetInstance().sendMessage( "Pause" );
                }
                else
                    wifiConfig.setEnabled(true);
            }
        });

        if (BluetoothConnection.GetInstance().IsBluetoothConnected() )
        {
            blueConfig.setEnabled(false);

            if(AplicationState.GetInstance().GetIsRunning() && !AplicationState.GetInstance().GetHasTcpConnection())
                wifiConfig.setEnabled(false);
            else if(!AplicationState.GetInstance().GetIsRunning())
            {
                startBtn.setEnabled(true);
                wifiConfig.setEnabled(!AplicationState.GetInstance().GetHasTcpConnection());
            }
        }
    }

    private Handler wifiHandler = new Handler() {
        @Override
        public void handleMessage (@NonNull Message msg){
            //nothing to do now
            String wifiData = (String) msg.obj;
            if (wifiData==null || wifiData.isEmpty())
                return;

            if (wifiData.equals("Connected")) {
                Toast.makeText(MainActivity.this, "Wifi is connected", Toast.LENGTH_LONG).show();
                return;
            }
            else if (wifiData.equals("Start") && !AplicationState.GetInstance().GetIsRunning())
            {
                Toast.makeText(MainActivity.this.getApplicationContext(), "Wifi Start Running", Toast.LENGTH_LONG).show();
                StartRunning();
                return;
            }
            else if (wifiData.equals("Stop") && AplicationState.GetInstance().GetIsRunning())
            {
                Toast.makeText(MainActivity.this.getApplicationContext(),"Wifi Stop Running",Toast.LENGTH_LONG).show();
                StopRunning();
                return;
            }
        }
    };

    private Handler dataHandler = new Handler() {
        @Override
        public void handleMessage (@NonNull Message msg) {
            //nothing to do now
            String baseData =(String) msg.obj;

            if (baseData == null || baseData.isEmpty() )
                return;

            String[] array_datos = new String(baseData).split("\\|");
            if (array_datos.length != 5)
                return;

            textVelocity.setText(array_datos[0] + " Km/h");
            textDistance.setText(array_datos[1] + " m");
            textInclination.setText(array_datos[2]);
            textCenter.setText(array_datos[3]);
            textMeasure.setText( array_datos[4]);
        }
    };

    private void StartRunning() {
        if(!AplicationState.GetInstance().GetIsRunning()) {

            //indication that the module is runing
            AplicationState.GetInstance().SetIsRunning(true);
            AplicationState.GetInstance().SetMiddlePosition(true);

            if (SendDataThread == null)
            {
                SendDataThread = new Thread(new SendDataRunner(dataHandler, this));
                SendDataThread.start();
            }

            //Start Thread to read data from Bluetooth
            chrTime.start();

            stopBtn.setEnabled(true);
            startBtn.setEnabled(false);
            wifiConfig.setEnabled(false);
            blueConfig.setEnabled(false);
        }
    }

    private void StopRunning()
    {
        if(AplicationState.GetInstance().GetIsRunning())
        {
            //indication that the module is runing
            AplicationState.GetInstance().SetIsRunning(false);
            AplicationState.GetInstance().SetMiddlePosition(false);

            textVelocity.setText("0 Km/h");
            textInclination.setText("0");
            chrTime.stop();

            stopBtn.setEnabled(false);
            startBtn.setEnabled(true);
            wifiConfig.setEnabled(false);
            blueConfig.setEnabled(false);
        }
    }
}