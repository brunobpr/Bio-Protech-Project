
/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 * Modified by Bruno Ribeiro at Bio Protech
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.bioprotech;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bio.bioprotech.R;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;


public class MainActivity extends Activity {

    //BLUETOOTH CONNECTION----
    public static final String TAG = "BioProtech";
    public static final int UART_PROFILE_CONNECTED = 20;
    public static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int REQUEST_ENABLE_BT = 2;
    public static int mState = UART_PROFILE_DISCONNECTED;
    public static UartService mService = null;
    //BLUETOOTH CONNECTION----
    //UART service connected/disconnected extracted from UartService.java class offered by Nordic Semiconductors
    public static String FREQUENCY;
    public static boolean IS_MOTOR_VIBRATING = false;protected BluetoothDevice mDevice = null;
    protected BluetoothAdapter mBtAdapter = null;
    private TextView frequencyTF, startButton;
    private ImageView menuButton, sliderView, bluetoothButton, graphButton, settingButton, homeButton, infoButton;
    private FrameLayout clickableArea;
    private SeekBar sliderBar;
    private LinearLayout menu;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        //This is the textview intended to display the current frequency
        frequencyTF = (TextView) findViewById(R.id.frequencyTF);

        //This is the start/stop button, it needs to be set to clickable
        startButton = (TextView) findViewById(R.id.startStop);
        startButton.setClickable(true);

        //Tagging each ImageView with their respective views.
        homeButton = (ImageView) findViewById(R.id.homeButton);
        menuButton = (ImageView) findViewById(R.id.menuButton);
        bluetoothButton = (ImageView) findViewById(R.id.bluetootButton);
        graphButton = (ImageView) findViewById(R.id.graphButton);
        settingButton = (ImageView) findViewById(R.id.settingButton);
        infoButton = (ImageView) findViewById(R.id.infoButton);

        //Making them clickable to simulate a button
        homeButton.setClickable(true);
        menuButton.setClickable(true);
        bluetoothButton.setClickable(true);
        graphButton.setClickable(true);
        settingButton.setClickable(true);
        infoButton.setClickable(true);

        //ClickableArea is the layout added to fill the rest of the screen
        //when the menu is open
        clickableArea = (FrameLayout) findViewById(R.id.clickableArea);
        clickableArea.setVisibility(View.INVISIBLE);
        //sliderBar is the transparent SeekBar under the slider
        sliderBar = (SeekBar) findViewById(R.id.sliderBar);
        //sliderView is the slider-images designed by Bio Protech
        sliderView = (ImageView) findViewById(R.id.slider);

        //Menu is the sidepanel with five buttons
        menu = (LinearLayout) findViewById(R.id.menu);


        service_init();

        // MENU BUTTON
        //The menu button is responsible for opening the MenuView
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check if the menu is invisible
                if (menu.getVisibility() == View.INVISIBLE) {
                    //Make the menu visible
                    menu.setVisibility(View.VISIBLE);
                    //Enable the clicklableArea
                    clickableArea.setVisibility(View.VISIBLE);
                    clickableArea.setClickable(true);
                    //Disable all the buttons of the main activity outside the menu
                    startButton.setClickable(false);
                    sliderBar.setVisibility(View.INVISIBLE);
                }
            }
        });

        graphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Build a new Intent with the MainActivity as context and GraphActivity as destination
                Intent intent = new Intent(MainActivity.this, GraphActivity.class);
                if (mState == UART_PROFILE_CONNECTED) {
                    //The GRAPH text will be interpreted as a request for the accelerometer data
                    sendMessage("G");
                } else {
                    //if not connected, show a Toast message
                    showMessage("Park Med is not connected!");
                }
                //Getting the value from the frequency text field, parsing to a string
                //And put it into the intent
                MainActivity.FREQUENCY = frequencyTF.getText().toString();
                //Start the new Activity
                startActivity(intent);
            }
        });


        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check if is disconnected
                if (mState == UART_PROFILE_DISCONNECTED) {
                    // Create a new SharedPreferences to retrieve the mac address
                    SharedPreferences sharedPref = getSharedPreferences("DEVICE", Context.MODE_PRIVATE);
                    String macAddress = sharedPref.getString("Mac", "");
                    //Connect to the saved Mac Address
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress);
                    mService.connect(macAddress);
                } else {
                    //If is connected
                    if (mDevice != null) {
                        mService.disconnect();
                    }
                }
            }
        });

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Once settings button is pressed, build a new intent and start the new activity
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Once info button is pressed, build a new intent and start the new activity
                Intent intent = new Intent(MainActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The home button and the clickableArea have the same function
                //We can simulate a click in the clickableArea instead of copying the dode
                clickableArea.performClick();
            }
        });

        sliderBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int frequency = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //First check if the menu is not open
                if (menu.getVisibility() == View.INVISIBLE) {
                    frequency = progress;

                    //Update the progress (frequency) to the frequencyTF
                    frequencyTF.setText(progress + "Hz");
                    //Anything bellow 20hz will 'deactivate' all the segments
                    if (progress <= 20) {
                        sliderView.setImageResource(R.drawable.slider0);
                    }
                    //This is the first segment starting from the bottom
                    //Anything between 20hz and 40Hz will only activate the first segment
                    else if (progress <= 40 && progress >= 20) {
                        sliderView.setImageResource(R.drawable.slider1);
                    }
                    //This is the second segment starting from the bottom
                    //Anything between 40hz and 60Hz will only activate the first two segments
                    else if (progress <= 60 && progress >= 40) {
                        sliderView.setImageResource(R.drawable.slider2);
                    }
                    //This is the third segment starting from the bottom
                    //Anything between 60hz and 50Hz will only activate the first three segments
                    else if (progress <= 80 && progress >= 60) {
                        sliderView.setImageResource(R.drawable.slider3);
                    }
                    //This is the fourth segment starting from the bottom
                    //Anything between 80hz and 100Hz will only activate the first four segments
                    else if (progress <= 100 && progress >= 80) {
                        sliderView.setImageResource(R.drawable.slider4);
                    }
                    //This is the fifth segment starting from the bottom
                    //Anything between 100hz and 120Hz will only activate the first five segments
                    else if (progress <= 120 && progress >= 100) {
                        sliderView.setImageResource(R.drawable.slider5);
                    }
                    //This is the sixth segment starting from the bottom
                    //Anything between 120hz and 140Hz will only activate the first six segments
                    else if (progress <= 140 && progress >= 120) {
                        sliderView.setImageResource(R.drawable.slider6);
                    }
                    //This is the seventh segment starting from the bottom
                    //Anything between 80hz and 100Hz will only activate the first seven segments
                    else if (progress <= 160 && progress >= 140) {
                        sliderView.setImageResource(R.drawable.slider7);
                    }
                    //This is the eighth segment starting from the bottom
                    //Anything between 80hz and 100Hz will activate all eight segments
                    else if (progress <= 180 && progress >= 160) {
                        sliderView.setImageResource(R.drawable.slider8);
                    }
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            //When the user releases the finger from the slider this method is invoked
            public void onStopTrackingTouch(SeekBar seekBar) {
                //First check if the app is connected to a park med device
                if (mState == UART_PROFILE_CONNECTED) {
                    //Converting the integer value to a String
                    //The F letter will help the Park Med device to identify that the new message is a frequency
                    sendMessage(frequency + "F");
                } else {
                    //if not connected, show a Toast message
                    showMessage("Park Med is not connected!");
                }
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //First check if the app is connected to a park med device
                if (mState == UART_PROFILE_CONNECTED) {
                    //Create an empty string
                    String message = "";
                    //First check if the button is currently "START"
                    if (startButton.getText().equals("START")) {
                        //Set the message value to "START"
                        sendMessage("START");
                        //Change the text of the startButton to STOP
                        startButton.setText("STOP");
                        //Change status of the motors
                        IS_MOTOR_VIBRATING = true;
                    } else {
                        //Set the message value to "STOP"
                        sendMessage("STOP");
                        //Change the text of the startButton to START
                        startButton.setText("START");
                        IS_MOTOR_VIBRATING = false;
                    }
                } else {
                    //if not connected, show a Toast message
                    showMessage("Park Med is not connected!");
                }
            }
        });


        //The clickableArea will have the opposite function of the menu button
        clickableArea.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //first check if the menu is visible
                if (menu.getVisibility() == View.VISIBLE) {
                    //Then make the menu invisible
                    menu.setVisibility(View.INVISIBLE);
                    //Make itself non clickable and invisble
                    clickableArea.setVisibility(View.INVISIBLE);
                    clickableArea.setClickable(false);
                    //Enable all the buttons of the main activity outside the menu
                    startButton.setClickable(true);
                    sliderBar.setVisibility(View.VISIBLE);
                }
            }
        });


    }

    //BroadcastReceiver extracted from UartService.java class offered by Nordic Semiconductors
    //This method handles different actions regarding the bluetooth connectivity
    public final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final Intent mIntent = intent;
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        mState = UART_PROFILE_CONNECTED;
                        menuButton.setImageResource(R.drawable.logoconnected);
                        bluetoothButton.setImageResource(R.drawable.button_bluetoothdisconnect);
                        clickableArea.performClick();
                    }
                });
            }
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        mState = UART_PROFILE_DISCONNECTED;
                        menuButton.setImageResource(R.drawable.logodisconnected);
                        bluetoothButton.setImageResource(R.drawable.button_bluetoothconnect);
                        startButton.setText("START");
                        IS_MOTOR_VIBRATING = false;
                        mService.close();
                    }
                });
            }
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            // System.out.println(text);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device is not available. Try again.");
                mService.disconnect();
            }


        }
    };


    public ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };


    public static void sendMessage(String text) {
        final String message = text;
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] value;
                try {
                    //Send a PKG string to prepare Park Med to the command
                    value = "PKG".getBytes("UTF-8");
                    mService.writeRXCharacteristic(value);
                    //Sleep for 1 second
                    Thread.sleep(1500);
                    //Convert the message to a array of bytes
                    value = message.getBytes();
                    //Send it over bluetooth
                    mService.writeRXCharacteristic(value);
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }

   /* private Handler mHandler = new Handler() {
        @Override
        //Handler events that received from UART service
        public void handleMessage(Message msg) {
        }
    };*/

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        // unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (((LinearLayout) findViewById(R.id.menu)).getVisibility() == View.VISIBLE) {
            clickableArea.performClick();
        } else {
            if (mState == UART_PROFILE_CONNECTED) {
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
                showMessage("Bio Protech running in background.\n             Disconnect to exit");
            } else {
                new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.popup_title)
                        .setMessage(R.string.popup_message)
                        .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.popup_no, null)
                        .show();
            }
        }
    }


}
