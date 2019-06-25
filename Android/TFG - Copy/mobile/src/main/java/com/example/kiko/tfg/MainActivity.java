package com.example.kiko.tfg;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.os.Handler;
import android.os.Message;
import android.os.Environment;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

/**
 * this is the main activity on the device/phone  (just to kept everything straight)
 *
 * The device will setup a listener to receive messages from the wear device and display them to the screen.
 *
 * It also setups up a button, so it can send a message to the wear device, the wear device will auto
 * response to the message.   This code does not auto response, otherwise we would get caught in a loop.
 *
 * This is all down via the datalayer in the googleApiClient that requires om.google.android.gms:play-services-wearable
 * in the gradle (both wear and mobile).  Also the applicationId MUST be the same in both files as well
 * both use a the "/message_path" to send/receive messages.
 *
 * debuging over bluetooth.
 * https://developer.android.com/training/wearables/apps/debugging.html
 */

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener {

    String datapath = "/message_path";
    Button mybutton;
    Button startButton;
    ToggleButton startRecording;
    public TextView redLogger;
    Boolean isOn=false;
    TextView logger;
    protected Handler handler;
    String TAG = "Mobile MainActivity";
    int num = 1;
    final String path = Environment.getExternalStorageDirectory().getPath();
    final File file = new File(path + File.separator +"Datos", "datos.txt");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //get the widgets
        startRecording = findViewById(R.id.isOn);
        mybutton = findViewById(R.id.sendbtn);
        mybutton.setOnClickListener(this);
        logger = findViewById(R.id.logger);
        startButton = findViewById(R.id.startbtn);
        redLogger = findViewById(R.id.redNeuronal);

        startRecording.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) isOn = true;
                else isOn = false;
            }
        });

        //message handler for the send thread.
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Bundle stuff = msg.getData();
                logthis(stuff.getString("logthis"));
                return true;
            }
        });
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
        // B
    }

    /*
    * Method used to get the permissions to write into external SD, where we save the data.
    */
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
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to write your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    /*
     * simple method to add the log TextView.
     */
    public void logthis(String newinfo) {
        if (newinfo.compareTo("") != 0) {
            logger.append("\n" + newinfo);
        }
    }

    //setup a broadcast receiver to receive the messages from the wear device via the listenerService.
    public class MessageReceiver extends BroadcastReceiver {
        float[][] break_data = { { 1, 2, 3 } };
        float[][] output = { {-1, -1, -1 , -1} };
        int i = 0;
        int[] results = {-1, -1, -1 , -1, -1, -1, -1 , -1, -1};
        float[] resultado = {0,0,0,0};
        float[] resultado2 = {0,0,0};
        float[] resultado3 = {0,0};
        float[] resultado4 = {0};
        int index = 0;
        int instantResult = 0;
        @Override
        public void onReceive(Context context, final Intent intent) {
            String message = intent.getStringExtra("message");
            Log.v(TAG, "Main activity received message: " + message);
            // Display message in UI
            logthis(message);
            /*
            * New thread to write the data from the clock into a txt. Then this txt will be used to
            * train a Neural Network to detect the type of movement.
            * Important: add the true into the filewriter to allow appending the data instead of overwritting
            * Also note the "/n" to get a newline.
            * */
            if(isOn){
                Thread writerThread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            String message = intent.getStringExtra("message");
                            break_data[0][0] = Float.parseFloat(message.split(",")[0]);// ((message_break[0] + 3.036879)/7.213168); //
                            break_data[0][1] = Float.parseFloat(message.split(",")[1]);// ((message_break[1] + 3.869599)/8.843972); //
                            break_data[0][2] = Float.parseFloat(message.split(",")[2]);// ((message_break[2] - 4.779501)/6.318493); //
                            try {
                                Interpreter interpreter = new Interpreter(loadModelFile(getAssets(), "PossibleBF.tflite").asReadOnlyBuffer());
                                interpreter.run(break_data, output);
                                if (i < 8) {
                                    resultado[0] = resultado[0] + output[0][3];
                                    resultado[1] = resultado[1] + output[0][2];
                                    resultado[2] = resultado[2] + output[0][1];
                                    resultado[3] = resultado[3] + output[0][0];
                                    if(Math.abs(break_data[0][0]) > 65 || Math.abs(break_data[0][1]) > 65 || Math.abs(break_data[0][2]) > 65) {
                                        resultado[3] = resultado[3] + 2;
                                    }
                                    float maxValue = getMax(resultado);
                                    for(int x = 0; x<4; x++){
                                        if(maxValue == resultado[x]) {
                                            instantResult = x;
                                        }
                                    }
                                    i++;
                                } else {
                                    i = 0;
                                    HashMap<Float, Integer> hmapResults = new HashMap<>();
                                    for (int x = 0; x<4; x++){
                                        hmapResults.put(resultado[x], x);
                                    }
                                    Map<Float, Integer> map = new TreeMap<>(hmapResults);
                                   float maxValue = getMax(resultado);
                                    resultado2 = arraySectioning(resultado, maxValue);
                                    float maxValue2 = getMax(resultado2);
                                    resultado3 = arraySectioning(resultado2, maxValue2);
                                    float maxValue3 = getMax(resultado3);
                                    resultado4 = arraySectioning(resultado3, maxValue3);
                                    float maxValue4 = getMax(resultado4);
                                    if (map.get(maxValue) == 1 && map.get(maxValue2) == 2 && (maxValue-maxValue2) < 1) {
                                        redLogger.setText("RED NEURONAL: CORRIENDO");
                                        index = 2;
                                    } else if(map.get(maxValue) == 0) {
                                        redLogger.setText("RED NEURONAL: REPOSO");
                                        index = 0;
                                    }else if (map.get(maxValue) == 1) {
                                        redLogger.setText("RED NEURONAL: ANDANDO");
                                        index = 1;
                                    } else if (map.get(maxValue) == 2){
                                        redLogger.setText("RED NEURONAL: CORRIENDO");
                                        index = 2;
                                    } else if (map.get(maxValue) == 3) {
                                        index = 3;
                                        redLogger.setText("RED NEURONAL: CAYENDO");
                                    }
                                    resultado[0] = 0;
                                    resultado[1] = 0;
                                    resultado[2] = 0;
                                    resultado[3] = 0;
                                }

                            } catch (Exception e) {
                                Log.e("OPERATOR", "Interpreter Failed");
                            }

                            BufferedWriter out = new BufferedWriter(new FileWriter(file, true));
                            out.append(message +',' + instantResult + ',' + index + " \n");
                            out.flush();
                            out.close();
                        } catch (Exception e) {
                            Log.e("Exception", "File write failed: " + e.toString());
                        }
                    }
                };
                writerThread.start();
            }
        }
    }
    public static float[] arraySectioning(float[] arrayToSec, float arg) {
        float[] newArr = new float[arrayToSec.length-1];
        for(int i = 0; i < arrayToSec.length; i++){
            if(arrayToSec[i] == arg){
                for(int index = 0; index < i; index++){
                    newArr[index] = arrayToSec[index];
                }
                for(int j = i; j < arrayToSec.length - 1; j++){
                    newArr[j] = arrayToSec[j+1];
                }
                break;
            }
        }
        return newArr;
    }
    public static float getMax(float[] inputArray){
        float maxValue = inputArray[0];
        for(int i=1;i < inputArray.length;i++){
            if(inputArray[i] > maxValue){
                maxValue = inputArray[i];
            }
        }
        return maxValue;
    }

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
    //button listener
    @Override
    public void onClick(View v) {
        String message = "Hello wearable " + num;
        //Requires a new thread to avoid blocking the UI
        new SendThread(datapath, message).start();
        num++;
    }

    //method to create up a bundle to send to a handler via the thread below.
    public void sendmessage(String logthis) {
        Bundle b = new Bundle();
        b.putString("logthis", logthis);
        Message msg = handler.obtainMessage();
        msg.setData(b);
        msg.arg1 = 1;
        msg.what = 1; //so the empty message is not used!
        handler.sendMessage(msg);

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
        //since there is (should only?) be one, no problem.
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
                        sendmessage("SendThread: message send to " + node.getDisplayName());
                        Log.v(TAG, "SendThread: message send to " + node.getDisplayName());

                    } catch (ExecutionException exception) {
                        sendmessage("SendThread: message failed to" + node.getDisplayName());
                        Log.e(TAG, "Send Task failed: " + exception);

                    } catch (InterruptedException exception) {
                        Log.e(TAG, "Send Interrupt occurred: " + exception);
                    }

                }

            } catch (ExecutionException exception) {
                sendmessage("Node Task failed: " + exception);
                Log.e(TAG, "Node Task failed: " + exception);

            } catch (InterruptedException exception) {
                Log.e(TAG, "Node Interrupt occurred: " + exception);
            }

        }
    }
}