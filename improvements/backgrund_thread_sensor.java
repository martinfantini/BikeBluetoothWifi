import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.embedonix.mobilehealth.helpers.AppConstants;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Saeid on 22-4-2014.
 */
final class LogRunnable implements Runnable {

    private Context mContext;
    private SensorManager mSensorManager = null;
    private Sensor mSensor;
    private File mLogFile = null;
    private FileOutputStream mFileStream = null;
    private SensorEventListener mListener;
    private HandlerThread mHandlerThread;

    LogRunnable(Context context) {
        mContext = context;

    }

    /**
     * Sets up folder and file to log the file on it
     */
    private void setupFolderAndFile() {

        File folder = new File(Environment.getExternalStorageDirectory()
                + File.separator + AppConstants.APP_LOG_FOLDER_NAME);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        mLogFile = new File(Environment.getExternalStorageDirectory().toString()
                + File.separator + AppConstants.APP_LOG_FOLDER_NAME
                + File.separator + "test.txt");

        if (!mLogFile.exists()) {
            try {
                mLogFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mFileStream == null) {
            try {
                mFileStream = new FileOutputStream(mLogFile, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        setupFolderAndFile();

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mHandlerThread = new HandlerThread("AccelerometerLogListener");
        mHandlerThread.start();
        Handler handler = new Handler(mHandlerThread.getLooper());


        mListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {


                    String formatted = String.valueOf(System.currentTimeMillis())
                                            + "\t" + String.valueOf(sensorEvent.timestamp)
                                            + "\t" + String.valueOf(sensorEvent.values[0])
                                            + "\t" + String.valueOf(sensorEvent.values[1])
                                            + "\t" + String.valueOf(sensorEvent.values[2])
                                            + "\r\n";

                    //if (mIsServiceStarted && mFileStream != null && mLogFile.exists()) {
                    if (mFileStream != null && mLogFile.exists()) {
                        try {
                            mFileStream.write(formatted.getBytes());
                            long shit = Thread.currentThread().getId();

                            long ass = shit;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        mSensorManager.registerListener(mListener, mSensor, SensorManager.SENSOR_DELAY_FASTEST,
                handler
        );
    }


    public void cleanThread(){

        //Unregister the listener
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(mListener);
        }

        if(mHandlerThread.isAlive())
            mHandlerThread.quitSafely();



        //Flush and close file stream
        if (mFileStream != null) {
            try {
                mFileStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                mFileStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}