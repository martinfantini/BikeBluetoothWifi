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
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

    private TextView textVelocity;
    private TextView textDistance;
    private Chronometer chrTime;
    private TextView textInclination;

    private Button startBtn;
    private Button stopBtn;
    private Button wifiConfig;
    private Button blueConfig;

    private boolean isRunning = false;

    // Thread to read data from bluetooth;
    Thread bluetoothThread = null;

    private Double totalTrackDistance = new Double(0.0);

    private long lastReadTime;

    //To register movement sensors
    private SensorManager mSensorManager =  null;
    private Sensor mRotationSensor;
    private Integer position = 0;

    //Set Middle position
    private Integer middlePos;
    private boolean calcMiddlePos = false;

    private static final int MOVEMENT_SENSOR_DELAY = 300;

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
        /*if(WifiConnection.GetInstance().IsWifiConnected() && !isRunning)*/
            startBtn.setEnabled(true);

        stopBtn = (Button) findViewById(R.id.stop_running);
        if(isRunning)
            stopBtn.setEnabled(true);

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

        if (BluetoothConnection.GetInstance().IsBluetoothConnected())
            wifiConfig.setEnabled(true);


        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isRunning){
                    if (!BluetoothConnection.GetInstance().IsBluetoothConnected()){
                        Toast.makeText(v.getContext().getApplicationContext(),"Bluetooth is not connected",Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (!WifiConnection.GetInstance().IsWifiConnected()){
                        Toast.makeText(v.getContext().getApplicationContext(),"Wifi is not connected",Toast.LENGTH_LONG).show();
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
                Toast.makeText(MainActivity.this.getApplicationContext(), "Wifi Start Running", Toast.LENGTH_LONG).show();

                if (!BluetoothConnection.GetInstance().IsBluetoothConnected()) {
                    Toast.makeText(MainActivity.this.getApplicationContext(), "Bluetooth is not connected", Toast.LENGTH_LONG).show();
                    return;
                }
                StartRunning();
                return;
            }
            else if (wifiData.equals("Stop") && isRunning)
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
            String strDistance = String.format("%.2f", totalTrackDistance);
            textDistance.setText( strDistance + " m");
            String velocity = brdDataFinal.CalculateVelocity(miliSecondsElapsedTime());
            textVelocity.setText( velocity + " Km/h");

            if( Math.abs(position) < 5 )
                textInclination.setText("Center X: " + position );
            else
            {
                textInclination.setText( (Math.signum(position)==1?"Right":"Left") + " X: " + position );
            }

            WifiConnection.GetInstance().sendMessage( velocity.replace(',','.') + "|" + strDistance.replace(',','.') + "|" + position );
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

                if(isRunning)
                {
                    position = (int) Math.toDegrees(orientation[0]) - middlePos;
                    if(calcMiddlePos)
                    {
                        middlePos = position;
                        calcMiddlePos = false;
                        position = 0;
                    }
                }
            }
        }
    };

    private void StartRunning() {
        if(!isRunning) {
            if (mSensorManager == null)
            {
                try {
                    mSensorManager = (SensorManager) getSystemService(MainActivity.SENSOR_SERVICE);
                    mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                    mSensorManager.registerListener(mSensorEventListener, mRotationSensor, MOVEMENT_SENSOR_DELAY);
                } catch (Exception e) {
                    Toast.makeText(this, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            else
            {
                mSensorManager.registerListener(mSensorEventListener, mRotationSensor, MOVEMENT_SENSOR_DELAY);
            }

            //indication that the module is runing
            calcMiddlePos = isRunning = true;
            middlePos = 0;

            //Start Thread to read data from Bluetooth
            chrTime.start();
            lastReadTime = System.currentTimeMillis();
            if (bluetoothThread == null)
            {
                bluetoothThread = new Thread(new BluetoothRunner(localHandler));
                bluetoothThread.start();
            }
            stopBtn.setEnabled(true);
        }
    }

    private void StopRunning()
    {
        if(isRunning)
        {
            calcMiddlePos = isRunning = false;
            textVelocity.setText("0 Km/h");
            textInclination.setText("Center 0");
            chrTime.stop();
            stopBtn.setEnabled(false);
            if(mSensorManager != null)
                mSensorManager.unregisterListener(mSensorEventListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

}