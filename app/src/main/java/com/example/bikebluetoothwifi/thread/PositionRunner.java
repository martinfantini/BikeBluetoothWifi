package com.example.bikebluetoothwifi.thread;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.widget.Toast;

import com.example.bikebluetoothwifi.AplicationState;

import static android.content.Context.SENSOR_SERVICE;

public class PositionRunner implements Runnable {

    private Handler m_Handler;
    private Context m_Context;

    private SensorManager m_SensorManager;
    private Sensor m_RotationSensor;

    //This timer is in microseconds
    private static final int MOVEMENT_SENSOR_DELAY = 500;

    public PositionRunner(Handler handler,Context context)
    {
        m_Handler = handler;
        m_Context = context;
    }

    @Override
    public void run() {
        try {
            m_SensorManager = (SensorManager) m_Context.getSystemService(SENSOR_SERVICE);
            m_RotationSensor = m_SensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        } catch (Exception e) {
            //Toast.makeText(m_Context, "Hardware compatibility issue", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        HandlerThread mHandlerThread = new HandlerThread("AccelerometerLogListener");
        mHandlerThread.start();
        Handler handler_position = new Handler(mHandlerThread.getLooper());

        m_SensorManager.registerListener(m_SensorEventListener, m_RotationSensor, MOVEMENT_SENSOR_DELAY, handler_position);
    }

    private SensorEventListener m_SensorEventListener = new SensorEventListener() {
        float[] orientation = new float[3];
        float[] rMat = new float[9];

        public void onAccuracyChanged(Sensor sensor, int accuracy ) {}

        @Override
        public void onSensorChanged( SensorEvent event ) {
            if( event.sensor.getType() == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR  && AplicationState.GetInstance().GetIsRunning() ){
                // calculate th rotation matrix
                SensorManager.getRotationMatrixFromVector( rMat, event.values );
                SensorManager.getOrientation( rMat, orientation );

                Integer position = (int) Math.toDegrees(orientation[0]);
                //Enviamos el valor a traves del handler.
                Message msg = new Message();
                msg.obj = position;
                msg.setTarget(m_Handler);
                msg.sendToTarget();
            }
        }
    };
}
