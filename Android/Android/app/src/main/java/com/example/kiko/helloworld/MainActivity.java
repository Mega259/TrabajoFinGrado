package com.example.kiko.helloworld;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends Activity implements SensorEventListener {

    private static final String TAG = "MainActivity";
    private TextView mTextViewStepCount;
    private TextView mTextViewStepDetect;
    private TextView mTextViewHeart;
    private TextView mTextViewGyro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //AlwaysOn
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        //check permissions
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, 1);

        mTextViewHeart = findViewById(R.id.heart);
        mTextViewStepCount = findViewById(R.id.step_count);
        mTextViewStepDetect = findViewById(R.id.step_detect);
        mTextViewGyro = findViewById(R.id.gyro);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    getStepCount();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void getStepCount(){
        SensorManager mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        Sensor mStepCountSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor mStepDetectSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        Sensor mGyroSensor = mSensorManager.getDefaultSensor((Sensor.TYPE_GYROSCOPE));

        mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mStepCountSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mStepDetectSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private String currentTimeStr(){
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss") ;
        return df.format(c.getTime());
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy){
        Log.d(TAG, "onAccuracyChange - accuracy: " + accuracy);
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            String msg = "Heart rate: " + event.values[0];
            mTextViewHeart.setText(msg);
            Log.d(TAG, msg);
        }
        else if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            String msg = "Count: " + (int)event.values[0];
            mTextViewStepCount.setText(msg);
            Log.d(TAG, msg);
        }
        else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            String msg = "Detected at " + currentTimeStr();
            mTextViewStepDetect.setText(msg);
            Log.d(TAG, msg);
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            String msg = "X: " + event.values[0] + " Y: " + event.values[1] + " Z: " + event.values[2];
            mTextViewGyro.setText(msg);

        }
        else
            Log.d(TAG, "Unknown sensor type");
    }
}
