package com.bioprotech;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.bio.bioprotech.R;

import java.util.ArrayList;

import static com.bioprotech.MainActivity.UART_PROFILE_CONNECTED;

public class SettingsActivity extends Activity {

    // ArrayList to store device name and mac address
    final ArrayList<String> devicesInfo = new ArrayList<>();
    private ImageView menuButton, graphButton, settingButton, homeButton, infoButton;
    private FrameLayout clickableArea;
    private LinearLayout menu;
    private Button scanButton;
    private TextView selectTF;
    //Declaring Bluetooth Adapter and BroadcastReceiver
    private BluetoothAdapter mBluetoothAdapter;
    private BroadcastReceiver mReceiver;
    // ListView to display the bluetooth devices
    private ListView bluetoothList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        homeButton = (ImageView) findViewById(R.id.homeButton);
        menuButton = (ImageView) findViewById(R.id.menuButton);
        graphButton = (ImageView) findViewById(R.id.graphButton);
        settingButton = (ImageView) findViewById(R.id.settingButton);
        infoButton = (ImageView) findViewById(R.id.infoButton);


        //Making them clickable to simulate a button
        homeButton.setClickable(true);
        menuButton.setClickable(true);
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
                Intent intent = new Intent(SettingsActivity.this, GraphActivity.class);
                if (MainActivity.mState == UART_PROFILE_CONNECTED) {
                    //The GRAPH text will be interpreted as a request for the accelerometer data
                    MainActivity.sendMessage("G");
                } else {
                    //if not connected, show a Toast message
                    showMessage("Park Med is not connected!");
                }
                //Start the new Activity
                SettingsActivity.this.finish();
                startActivity(intent);
            }
        });


        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The settings button and the clickableArea have the same function
                //We can simulate a click in the clickableArea instead of copying the code
                clickableArea.performClick();
            }
        });

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Once info button is pressed, build a new intent and start the new activity
                Intent intent = new Intent(SettingsActivity.this, InfoActivity.class);
                SettingsActivity.this.finish();
                startActivity(intent);
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The Home Button will close the Graph Activity
                //and return the user to the Main Activity
                //which is running in the background
                SettingsActivity.this.finish();
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
                    clickableArea.setClickable(false);
                    //Enable all the buttons of the main activity outside the menu
                }
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        scanButton = (Button) findViewById(R.id.scan);

        final ArrayList<String> devicesMACAddress = new ArrayList<>();
        selectTF = (TextView) findViewById(R.id.selectTF);


        //https://stackoverflow.com/questions/3170805/how-to-scan-for-available-bluetooth-devices-in-range-in-android
        //https://code.tutsplus.com/tutorials/create-a-bluetooth-scanner-with-androids-bluetooth-api--cms-24084
        //https://www.tutorialspoint.com/android/android_list_view.htm
        //https://developer.android.com/training/data-storage/shared-preferences
        scanButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // Setting the bluetooth adapter to start discovery
                showMessage("Scanning...");
                // Instantiating a new BroadcastReceiver
                mBluetoothAdapter.startDiscovery();
                mReceiver = new BroadcastReceiver() {
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        // if the broadcast find a new device
                        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                            // create a new BluetoothDevice object
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            // Check if the device found is named Park Med
                            if (device.getName() != null) {
                                if (device.getName().equals("Park Med")) {
                                    // Display the message 'Select one of the devices available:'
                                    selectTF.setVisibility(View.VISIBLE);
                                    // If so, add name and address to the devicesInfo array
                                    devicesInfo.add(device.getName() + "\n" + device.getAddress());
                                    // and add the address to the devicesMACAddress array
                                    devicesMACAddress.add(device.getAddress());
                                }
                            }
                            // Link the bluetoothList to the ListView
                            bluetoothList = (ListView) findViewById(R.id.bluetooth_list);
                            // Create an ArrayAdapter and set it to the bluetoothList
                            // The arguments passed are the current activty, the layout of the item and the arraylist of devices
                            ArrayAdapter adapter = new ArrayAdapter<String>(SettingsActivity.this, R.layout.bluetooth_item, devicesInfo);
                            // Set the adapter to the ListView
                            bluetoothList.setAdapter(adapter);
                            // senOnItemClickListener handles clicks for each item using their position
                            bluetoothList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
                                    //Text with name and mac address of the device in the position
                                    String selectedDevice = devicesInfo.get(position);
                                    //MAC address of the device in the given position
                                    String mac = devicesMACAddress.get(position);
                                    //Create a private SharedPreference named DEVICE
                                    SharedPreferences sharedPref = getSharedPreferences("DEVICE", Context.MODE_PRIVATE);
                                    // Uses the editor tool to edit the SharedPreference
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putString("Mac", mac); //Put a new String 'Mac' with the mac address value
                                    editor.commit(); //Save
                                    //Display a Toast message
                                    showMessage("Device saved: " + selectedDevice);
                                }
                            });
                        }

                    }
                };

                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(mReceiver, filter);
            }
        });


    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void onBackPressed() {
        if (((LinearLayout) findViewById(R.id.menu)).getVisibility() == View.VISIBLE) {
            ((LinearLayout) findViewById(R.id.menu)).setVisibility(View.INVISIBLE);
        } else {

            finish();
        }
    }

}
