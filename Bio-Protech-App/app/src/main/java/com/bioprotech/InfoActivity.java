package com.bioprotech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bio.bioprotech.R;

import static com.bioprotech.MainActivity.UART_PROFILE_CONNECTED;

public class InfoActivity extends Activity {
    private ImageView menuButton, graphButton, settingButton, homeButton, infoButton;
    private FrameLayout clickableArea;
    private LinearLayout menu;
    private WebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        //https://abhiandroid.com/androidstudio/add-local-html-file-android-studio.html
        // Linking to the WebView
        webView = (WebView) findViewById(R.id.webView);
        // Loading the file manual.html
        webView.loadUrl("file:///android_asset/manual.html");

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
                Intent intent = new Intent(InfoActivity.this, GraphActivity.class);
                if (MainActivity.mState == UART_PROFILE_CONNECTED) {
                    //The GRAPH text will be interpreted as a request for the accelerometer data
                    MainActivity.sendMessage("G");
                } else {
                    //if not connected, show a Toast message
                    showMessage("Park Med is not connected!");
                }
                InfoActivity.this.finish();
                //Start the new Activity
                startActivity(intent);
            }
        });


        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Once info button is pressed, build a new intent and start the new activity
                Intent intent = new Intent(InfoActivity.this, SettingsActivity.class);
                InfoActivity.this.finish();
                startActivity(intent);
            }
        });

        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The info button and the clickableArea have the same function
                //We can simulate a click in the clickableArea instead of copying the code
                clickableArea.performClick();
            }
        });

        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //The Home Button will close the Graph Activity
                //and return the user to the Main Activity
                //which is running in the background
                InfoActivity.this.finish();
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
                }
            }
        });

    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void onBackPressed() {
        if ((findViewById(R.id.menu)).getVisibility() == View.VISIBLE) {
            (findViewById(R.id.menu)).setVisibility(View.INVISIBLE);
        } else {
            finish();
        }
    }


}
