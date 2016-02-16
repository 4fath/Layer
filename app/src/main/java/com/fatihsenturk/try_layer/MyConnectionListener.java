package com.fatihsenturk.try_layer;

import android.app.Activity;
import android.util.Log;

import com.layer.sdk.LayerClient;
import com.layer.sdk.exceptions.LayerException;
import com.layer.sdk.listeners.LayerConnectionListener;

/**
 * Created by TOSHIBA on 16.2.2016. Şubat
 * Dont worry !
 */
public class MyConnectionListener implements LayerConnectionListener {

    private static final String TAG = MyConnectionListener.class.getSimpleName();

    private MainActivity main_activity;

    public MyConnectionListener(MainActivity activity) {
        main_activity = activity;
    }

    @Override
    public void onConnectionConnected(LayerClient layerClient) {
        Log.v(TAG, "Connected to Layer");

        //If the user is already authenticated (and this connection was being established after
        // the app was disconnected from the network), then start the conversation view.
        //Otherwise, start the authentication process, which effectively "logs in" a user
        if (layerClient.isAuthenticated())
            main_activity.onUserAuthenticated();
        else
            layerClient.authenticate();

    }

    @Override
    public void onConnectionDisconnected(LayerClient layerClient) {
        Log.v(TAG, "Connection to Layer closed");
    }

    @Override
    public void onConnectionError(LayerClient layerClient, LayerException e) {
        Log.v(TAG, "Error connecting to layer: " + e.toString());
    }
}
