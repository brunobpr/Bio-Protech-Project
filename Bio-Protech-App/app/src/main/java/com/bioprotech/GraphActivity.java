/*
 *    Copyright 2015 AndroidPlot.com
 *    Modified by Bruno Ribeiro at Bio Protech
 *
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.bioprotech;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.Plot;
import com.androidplot.util.PixelUtils;
import com.androidplot.util.Redrawer;
import com.androidplot.xy.BarFormatter;
import com.androidplot.xy.BarRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.bio.bioprotech.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import au.com.bytecode.opencsv.CSVWriter;

public class GraphActivity extends MainActivity {
    //-------------------------------------------------ANDROIDPLOT---
    //History_Size represents the length of the graph
    private static final int HISTORY_SIZE = 120;
    //Layouts, buttons and Views
    private ImageView menuButton, bluetoothButton, graphButton, settingButton, homeButton, infoButton;
    private TextView frequencyTF;
    private FrameLayout clickableArea;
    private LinearLayout menu;
    private String frequency;
    //https://github.com/halfhp/androidplot/blob/master/docs/dynamicdata.md
    //------------------------------SAVE THE GRAPH------------
    private Switch save;
    private boolean isSavingGraph = false;
    private ArrayList<String> frequencyArray;
    private double timer;
    // XYPlot is the x and y axis
    // SimpleXYSeries is the graph plotted into x and y
    // FrequencyBar will be displayed as a dynamic Bar Chart
    private XYPlot FrequencyBar = null;
    private SimpleXYSeries FrequencyBarPlot = null;
    //FrequencyLine will be displayed as a dynamic line.
    private SimpleXYSeries FrequencyLinePlot = null;
    // The BroadcastReceiver is the class responsible for bluetooth communication
    public final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //The action ACTIO_DATA_AVAILABLE
            // is triggered when there is new stream coming from the bluetooth
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                //Add the extra data to an array of bytes
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            //Convert it to a string with UTF-8 format
                            String text = new String(txValue, "UTF-8");
                            //Parse it to a integer
                            float tremor = Float.valueOf(text);
                            // Update the bar with the newes data:
                            FrequencyBarPlot.setModel(Arrays.asList(
                                    new Number[]{tremor}),
                                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);

                            // If the x-axis is already full, remove the first Y value of the line
                            if (FrequencyLinePlot.size() > HISTORY_SIZE) {
                                FrequencyLinePlot.removeFirst();
                            }
                            // And the the newest data to the last position of the line
                            FrequencyLinePlot.addLast(null, tremor);
                            // Verify if the data needs to be saved.
                            if (isSavingGraph) {
                                // If so, add it to the array
                                frequencyArray.add(String.valueOf(tremor));
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
        }
    };
    private XYPlot FrequencyLine = null;
    //-------------------------------------------------ANDROIDPLOT---
    //The Redrawer allows us to create a dynamic graph
    private Redrawer redrawer;

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);


        if (MainActivity.FREQUENCY != null && MainActivity.IS_MOTOR_VIBRATING) {
            //Set to the TextView
            ((TextView) findViewById(R.id.frequencyTF)).setText("Frequency: " + MainActivity.FREQUENCY);
            frequency = MainActivity.FREQUENCY;
        } else {
            ((TextView) findViewById(R.id.frequencyTF)).setText("Frequency: ");
        }

        frequencyArray = new ArrayList<String>();
        service_init();
        //Tagging each ImageView with their respective views.
        homeButton = (ImageView) findViewById(R.id.homeButton);
        menuButton = (ImageView) findViewById(R.id.menuButton);
        bluetoothButton = (ImageView) findViewById(R.id.bluetootButton);
        graphButton = (ImageView) findViewById(R.id.graphButton);
        settingButton = (ImageView) findViewById(R.id.settingButton);
        infoButton = (ImageView) findViewById(R.id.infoButton);
        save = (Switch) findViewById(R.id.saveSwitch);


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

        //Menu is the sidepanel with five buttons
        menu = (LinearLayout) findViewById(R.id.menu);


        if (MainActivity.mState == UART_PROFILE_CONNECTED) {
            menuButton.setImageResource(R.drawable.logoconnected);
        } else {
            menuButton.setImageResource(R.drawable.logodisconnected);
        }


        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (save.isChecked()) {
                    //Start saving
                    isSavingGraph = true;
                } else {
                    //Stop saving
                    isSavingGraph = false;
                }
            }
        });

        //http://www.codebind.com/android-tutorials-and-examples/ndroid-studio-save-file-internal-storage-read-write/
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (save.isChecked()) {
                    isSavingGraph = true;
                } else {
                    isSavingGraph = false;
                    showMessage("Saving");
                    //Get access to the directory of the smartphone and try to get the /Bio Protech folder
                    File directory = new File(Environment.getExternalStorageDirectory() + "/BioProtech", "");
                    //If the folder doesn't exist create one
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    //Create a date format as 31-05-2020 114034
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HHmmss");
                    //Create the file name with the timestamp and the frequency of the vibrations
                    //Eg. 120Hz-dd-MM-yyyy HH:mm:ss.csv
                    String fileName = frequency + "Hz-" + dateFormat.format(new Date()) + ".csv";
                    //The file class receives the directory folder and the file name
                    File file = new File(directory, fileName);
                    try {
                        //Start the new file
                        file.createNewFile();
                        //Use a CSVWriter Library to convert the plain text file
                        //to a CSV format
                        CSVWriter csvWrite = new CSVWriter(new FileWriter(file));
                        //Start the counter
                        timer = 0;
                        //The first row of the file works as a table header
                        String title[] = {"Frequency", "Time"};
                        //Write the header to the file
                        csvWrite.writeNext(title);
                        for (String item : frequencyArray) {
                            //Each row contains a frequency and the timer counter
                            String row[] = {item, String.valueOf(timer)};
                            csvWrite.writeNext(row); // Write the newest row to the file
                            timer += 0.1; //increment the timer by 0.1
                        }
                        csvWrite.close();
                    } catch (IOException e) {
                        Log.e("MainActivity", e.getMessage(), e);
                    }
                }
            }
        });

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

                }
            }
        });

        graphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The graph button and the clickableArea have the same function
                //We can simulate a click in the clickableArea instead of copying the code
                clickableArea.performClick();
            }
        });


        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check if is disconnected
                if (mState == UART_PROFILE_DISCONNECTED) {
                    //Show a message to ensure that the device is on
                    showMessage("Make sure Park Med is on!");
                    //Create a new blutooth device using the bluetooth adapter.
                    //For now, the MAC address "C4:51:8C:F2:2A:29" will be hardcoded
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice("C4:51:8C:F2:2A:29");
                    //Connect to "C4:51:8C:F2:2A:29"
                    mService.connect("C4:51:8C:F2:2A:29");
                } else {
                    //If is connected
                    if (mDevice != null) {
                        //Disconnect
                        showMessage("Disconnecting...");
                        mService.disconnect();
                    }
                }
            }
        });

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Once settings button is pressed, build a new intent and start the new activity
                Intent intent = new Intent(GraphActivity.this, SettingsActivity.class);
                GraphActivity.this.finish();
                startActivity(intent);
            }
        });

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Once info button is pressed, build a new intent and start the new activity
                Intent intent = new Intent(GraphActivity.this, InfoActivity.class);
                startActivity(intent);
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The Home Button will close the Graph Activity
                //and return the user to the Main Activity
                //which is running in the background
                GraphActivity.this.finish();
            }
        });


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
                }
            }
        });


        //Tag FrequencyBar and FrequencyLine to the same view
        //So it will be visible as only one graph
        FrequencyBar = (XYPlot) findViewById(R.id.plot);
        FrequencyLine = (XYPlot) findViewById(R.id.plot);
        //Instantiate a new SimpleXYSeries,
        //The title is blank because it's already defined in the XML file
        FrequencyBarPlot = new SimpleXYSeries("");
        FrequencyLinePlot = new SimpleXYSeries("");

        //The method setDomainBoundaries is used to define the limits of the X axis
        FrequencyBar.setDomainBoundaries(0, 1, BoundaryMode.FIXED);
        FrequencyLine.setDomainBoundaries(0, HISTORY_SIZE, BoundaryMode.FIXED);
        //The method setRangeBoundaries is used to define the limits of the Y axis
        FrequencyBar.setRangeBoundaries(0, 40, BoundaryMode.AUTO);
        FrequencyLine.setRangeBoundaries(0, 40, BoundaryMode.AUTO);
        //The addSeries method is responsible for linking the XY axis to the type of graph needed
        FrequencyBar.addSeries(FrequencyBarPlot,
                // The BarFormatter creates a bar chart
                new BarFormatter(
                        Color.rgb(0, 188, 212),
                        Color.rgb(0, 0, 0)
                ));
        FrequencyLine.addSeries(FrequencyLinePlot,
                // The LineAndPointFormatter creates a line
                new LineAndPointFormatter(
                        Color.rgb(0, 188, 212),
                        null, null, null));


        //The setDomainStepMode defines how many divisions for the x axis
        //StepMode.SUBDIVIDE divides the axis in n parts
        FrequencyLine.setDomainStepMode(StepMode.SUBDIVIDE);
        //The setDomainStepValue is the n number of divisions
        FrequencyLine.setDomainStepValue(0);
        //The setDomainStepMode defines how many divisions for the y axis
        FrequencyLine.setRangeStepValue(9);
        //The setRangeLabel is the label seen on the Y axis
        FrequencyLine.setRangeLabel("Frequency");
        // Do not display the decimal value for the labels
        FrequencyBar.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).
                setFormat(new DecimalFormat("#"));


        // The useImplicitXVals allows us to plot the line without an x value
        FrequencyLinePlot.useImplicitXVals();
        // Initialise the Redrawer object
        redrawer = new Redrawer(
                Arrays.asList(new Plot[]{FrequencyLine, FrequencyBar}),
                1000, false);


        // Get a reference to the BarRenderer so we can make some changes to it:
        BarRenderer barRenderer = FrequencyBar.getRenderer(BarRenderer.class);
        if (barRenderer != null) {
            // Make the bar a thicker than the default so they can be seen better:
            barRenderer.setBarGroupWidth(
                    BarRenderer.BarGroupWidthMode.FIXED_WIDTH, PixelUtils.dpToPix(30));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        redrawer.start();
    }

    @Override
    public void onPause() {
        redrawer.pause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        redrawer.finish();
        super.onDestroy();
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (((LinearLayout) findViewById(R.id.menu)).getVisibility() == View.VISIBLE) {
            clickableArea.performClick();
        } else {
            GraphActivity.this.finish();

        }
    }


}