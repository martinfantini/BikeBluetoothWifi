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

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView textVelocity;
    private TextView textDistance;
    private Chronometer chrTime;
    private TextView textInclination;

    private Button startBtn;
    private Button stopBtn;

    private boolean isRunning = false;

    //Timer para leer los datos desde la plaqueta.
    private Timer timerData = null;
    private int Time_Defoult = 5;

    private boolean cancelTimer = false;

    // Thread to read data from bluetooth;
    Thread bluetoothThread = null;

    private Double totalTrackDistance = new Double(0.0);

    private long lastReadTime;

    //To register movement sensors
    private SensorManager mSensorManager;
    private Sensor mRotationSensor;
    private static final int SENSOR_DELAY = 5000; // 5ms
    private Integer position = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WifiConnection.GetInstance().setHandeler( wifiHandler );

        textVelocity = (TextView) findViewById(R.id.show_velocity);
        textDistance = (TextView) findViewById(R.id.show_distance);
        chrTime = (Chronometer) findViewById(R.id.show_time);
            // chrTime.setFormat("HH:MM:SS");
        chrTime.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                chrTime = chronometer;
            }
        });
        textInclination = (TextView) findViewById(R.id.show_inclination);

        startBtn = (Button) findViewById(R.id.start_running);
        stopBtn = (Button) findViewById(R.id.stop_running);

        findViewById(R.id.bluetooth_config).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent k = new Intent(MainActivity.this, BluetoothActivity.class);
                startActivity(k);
                finish();
            }
        });

        findViewById(R.id.wifi_config).setOnClickListener(new View.OnClickListener() {
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
                if(!isRunning){
                    if (!WifiConnection.GetInstance().IsWifiConnected()){
                        Toast.makeText(v.getContext().getApplicationContext(),"Wifi is not connected",Toast.LENGTH_LONG).show();
                        return;
                    }

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
            }
        });

        try {
            mSensorManager = (SensorManager) getSystemService(MainActivity.SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(mSensorEventListener, mRotationSensor, SENSOR_DELAY);
        } catch (Exception e) {
            Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
        }
    }

    private long miliSecondsElapsedTime() {
        long elapsedSeconds= System.currentTimeMillis()-lastReadTime;
        lastReadTime = System.currentTimeMillis();
        return elapsedSeconds;
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
            else if (wifiData.equals("Start") && !isRunning)
            {
                if (!BluetoothConnection.GetInstance().IsBluetoothConnected()){
                    Toast.makeText(MainActivity.this.getApplicationContext(),"Bluetooth is not connected",Toast.LENGTH_LONG).show();
                    return;
                }
                StartRunning();
                return;
            }
            else if (wifiData.equals("Stop"))
            {
                Toast.makeText(MainActivity.this.getApplicationContext(),"Wifi Stop Running",Toast.LENGTH_LONG).show();
                StopRunning();
                return;
            }
        }
    };

    private Handler localHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (!isRunning)
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
            totalTrackDistance+=brdDataFinal.GetDistance();

            //Show data
            textDistance.setText(String.format("%.2f", totalTrackDistance) + " m");
            String velocity = brdDataFinal.CalculateVelocity(miliSecondsElapsedTime());
            textVelocity.setText( velocity + " Km/h");

            WifiConnection.GetInstance().sendMessage( velocity + "|" + String.format("%.2f",totalTrackDistance) + "|" + position );
        }
    };

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        float[] orientation = new float[3];
        float[] rMat = new float[9];

        public void onAccuracyChanged( Sensor sensor, int accuracy ) {}

        @Override
        public void onSensorChanged( SensorEvent event ) {
            if( event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ){
                // calculate th rotation matrix
                SensorManager.getRotationMatrixFromVector( rMat, event.values );
                SensorManager.getOrientation( rMat, orientation );

                if(isRunning) {
                    position = (int) Math.toDegrees(orientation[0]);
                    if (position < -15)
                        textInclination.setText("Right X: " + position);
                    else if (-15 < position && position < 15)
                        textInclination.setText("Center X: " + position);
                    else if (position > 15)
                        textInclination.setText("Left X: " + position);
                }
            }
        }
    };

    private void StartRunning() {
        if(!isRunning) {
            //indication that the module is runing
            isRunning = true;

            //Start Thread to read data from Bluetooth
            chrTime.start();
            lastReadTime = System.currentTimeMillis();
            if (bluetoothThread == null) {
                bluetoothThread = new Thread(new BluetoothRunner(localHandler));
                bluetoothThread.start();
            }
            //if(!bluetoothThread.isAlive() || bluetoothThread.getState() )
            //if( bluetoothThread.getState() != Thread.State.NEW )
            //    bluetoothThread.start();
        }
    }

    private void StopRunning()
    {
        if(isRunning) {
            isRunning = false;
            textVelocity.setText("0 Km/h");
            textDistance.setText("0 m");
            textInclination.setText("Center 0");
            chrTime.stop();
            //if(bluetoothThread.isAlive())
            //bluetoothThread.interrupt();
        }
    }


}