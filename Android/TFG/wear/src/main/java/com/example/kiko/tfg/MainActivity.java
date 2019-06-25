package com.example.kiko.tfg;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Wear Device code  so I can kept this straight.
 *
 * This will receive messages (from a device/phone) via the datalayer (through the listener code)
 * and display them to the wear device.  There is also a button to send a message
 * to the device/phone as well.
 *
 * if the wear device receives a message from the phone/device it will then send a message back
 * via the button on the wear device, it can also send a message to the device/phone as well.
 *    There is no auto response from the phone/device otherwise we would get caught in a loop!
 *
 * debuging over bluetooth.
 * https://developer.android.com/training/wearables/apps/debugging.html
 */


public class MainActivity extends WearableActivity implements SensorEventListener {

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
    private SensorManager mSensorManager;
    private Sensor mAcceSensor;
    private final static String TAG = "Wear MainActivity";
    private TextView mTextView;
    private Button myButton;
    private ToggleButton activateSensors;
    private int num = 1;
    public boolean isOn;
    private String datapath = "/message_path";
    private Date startDate = new Date();
    private Date nowDate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        mTextView =  findViewById(R.id.text);

        //send a message from the wear.  This one will not have response.
        myButton =  findViewById(R.id.wrbutton);
        activateSensors = findViewById(R.id.toggleButton);
        activateSensors.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) isOn = true;
                else isOn = false;
            }
        });

        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Hello device " + num;
                //Requires a new thread to avoid blocking the UI
                new SendThread(datapath, message).start();

                  num++;
            }
        });

        // Register the local broadcast receiver to receive messages from the listener.
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mAcceSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAcceSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this, mAcceSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        nowDate = new Date();
        if(sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            String msg = Float.toString(sensorEvent.values[0]) +","+ Float.toString(sensorEvent.values[1]) +","+ Float.toString(sensorEvent.values[2]);
            if (isOn && (nowDate.getTime() -startDate.getTime()) >= 40){
                    startDate = nowDate;
                    String timeStamp = dateFormat.format(nowDate);
                    msg = msg +","+timeStamp;
                    new SendThread(datapath, msg).start();
            }
        }
    }

/*    public static String getCurrentTimeStamp(){
        try {

            String currentDateTime = dateFormat.format(new Date()); // Find todays date
            return currentDateTime;
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }*/


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.v(TAG, "Main activity received message: " + message);
            // Display message in UI
            mTextView.setText(message);
            //here, send a message back.
            message = "Hello device " + num;
            //Requires a new thread to avoid blocking the UI
            new SendThread(datapath, message).start();
            num++;
        }
    }

    //This actually sends the message to the wearable device.
    class SendThread extends Thread {
        String path;
        String message;

        //constructor
        SendThread(String p, String msg) {
            path = p;
            message = msg;
        }

        //sends the message via the thread.  this will send to all wearables connected, but
        //since there is (should only?) be one, so no problem.
        public void run() {
            //first get all the nodes, ie connected wearable devices.
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                List<Node> nodes = Tasks.await(nodeListTask);

                //Now send the message to each device.
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), path, message.getBytes());

                    try {
                        // Block on a task and get the result synchronously (because this is on a background
                        // thread).
                        Integer result = Tasks.await(sendMessageTask);
                        Log.v(TAG, "SendThread: message send to " + node.getDisplayName());

                    } catch (ExecutionException exception) {
                        Log.e(TAG, "Task failed: " + exception);

                    } catch (InterruptedException exception) {
                        Log.e(TAG, "Interrupt occurred: " + exception);
                    }

                }

            } catch (ExecutionException exception) {
                Log.e(TAG, "Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Interrupt occurred: " + exception);
            }
        }
    }
}

