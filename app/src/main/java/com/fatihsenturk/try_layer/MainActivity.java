package com.fatihsenturk.try_layer;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.layer.sdk.LayerClient;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String LAYER_APP_ID = "layer:///apps/staging/84b37518-d4d8-11e5-bfa3-44c9010027fe";
    public static final String GCM_PROJECT_NUMBER = "00000";

    private LayerClient layerClient;
    private ConversationViewController conversationView;
    private MyConnectionListener connectionListener;
    private MyAuthenticationListener authenticationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(layerClient == null)
            setContentView(R.layout.activity_loading);

        if(connectionListener == null)
            connectionListener = new MyConnectionListener(this);

        if(authenticationListener == null)
            authenticationListener = new MyAuthenticationListener(this);

    }


    @Override
    protected void onResume() {
        super.onResume();

        loadLayerClient();

        if(layerClient != null && conversationView != null)
            layerClient.registerTypingIndicator(conversationView);
    }

    private void loadLayerClient() {

        if (isValidAppID()){
            if(layerClient == null){

                //Used for debugging purposes ONLY. DO NOT include this option in Production Builds.
                LayerClient.enableLogging(this.getApplicationContext());

                // Initializes a LayerClient object with the Google Project Number
                LayerClient.Options options = new LayerClient.Options();

                //Sets the GCM sender id allowing for push notifications
                options.googleCloudMessagingSenderId(GCM_PROJECT_NUMBER);

                //By default, only unread messages are synced after a user is authenticated, but you
                // can change that behavior to all messages or just the last message in a conversation
                options.historicSyncPolicy(LayerClient.Options.HistoricSyncPolicy.ALL_MESSAGES);


                layerClient = LayerClient.newInstance(this, LAYER_APP_ID, options);

                //Register the connection and authentication listeners
                layerClient.registerConnectionListener(connectionListener);
                layerClient.registerAuthenticationListener(authenticationListener);
            }


            if (!layerClient.isConnected()) {

                //If Layer is not connected, make sure we connect in order to send/receive messages.
                // MyConnectionListener.java handles the callbacks associated with Connection, and
                // will start the Authentication process once the connection is established
                layerClient.connect();

            } else if (!layerClient.isAuthenticated()) {

                //If the client is already connected, try to authenticate a user on this device.
                // MyAuthenticationListener.java handles the callbacks associated with Authentication
                // and will start the Conversation View once the user is authenticated
                layerClient.authenticate();

            } else {

                // If the client is to Layer and the user is authenticated, start the Conversation
                // View. This will be called when the app moves from the background to the foreground,
                // for example.
                onUserAuthenticated();
            }



        }


    }

    public static String getUserID() {
        if(Build.FINGERPRINT.startsWith("generic"))
            return "Simulator";

        return "Device";
    }


    public static List<String> getAllParticipants(){
        return Arrays.asList("Device", "Simulator", "Dashboard");
    }


    public void onUserAuthenticated(){

        if(conversationView == null) {

            conversationView = new ConversationViewController(this, layerClient);

            if (layerClient != null) {
                layerClient.registerTypingIndicator(conversationView);
            }
        }
    }

    private boolean isValidAppID() {
        if(LAYER_APP_ID.equalsIgnoreCase("LAYER_APP_ID")) {

            // Instantiate an AlertDialog.Builder with its constructor
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            // Chain together various setter methods to set the dialog characteristics
            builder.setMessage("To correctly use this project you need to replace LAYER_APP_ID in MainActivity.java (line 39) with your App ID from developer.layer.com.")
                    .setTitle(":-(");

            // Get the AlertDialog from create() and then show() it
            AlertDialog dialog = builder.create();
            dialog.show();

            return false;
        }

        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
