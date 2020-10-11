package com.samsung.smartnotes;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;


public class ObjectNameReceiver extends BroadcastReceiver {
    private static final String TAG = "ObjectNameReceiver";
    private static final String KEY = "com.example.dummycamera.objectName";
    protected static final String KEY1 = "com.samsung.navicam.objectList";
    protected static final String KEY2 = "com.samsung.navicam.blockWiseTextList";

    // Request Service Code for Overlay Settings
    private static final int POPUP_DRAW_OVER_OTHER_APP_PERMISSION = 1001;

    private static String receivedAction;
    private static String receivedValue;

    FloatingPopupService mService;
    boolean mBound = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving an Intent broadcast.
//        Toast.makeText(context, "I heard you Object Name Broadcast", Toast.LENGTH_LONG).show();
//        StringBuilder sb = new StringBuilder();
//        sb.append("Action: " + intent.getAction() + "\n");
//        sb.append("Key: " + KEY + "\n");
//        sb.append("Value: " + intent.getStringExtra(KEY) + "\n");
//        String log = sb.toString();
//        Toast.makeText(context, log, Toast.LENGTH_LONG).show();
//        Log.d(TAG, log);

        receivedAction = intent.getAction();
        // receivedValue = intent.getStringExtra(KEY);
        Bundle bundle = intent.getExtras();

        ArrayList<String> objectList = (ArrayList<String>) bundle.getSerializable(KEY1);
        ArrayList<String> text = (ArrayList<String>) bundle.getSerializable(KEY2);


//        Intent overlayIntent = new Intent();
//        overlayIntent.setClassName("com.samsung.smartnotes", "com.samsung.smartnotes.OverlayPermissionActivity" );
//        overlayIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
//        overlayIntent.putExtra("objectName", receivedValue);
//        context.startActivity(overlayIntent);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)){
            Intent overlayPermissionIntent = new Intent(context.getApplicationContext(), OverlayPermissionActivity.class);
            overlayPermissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(overlayPermissionIntent);
        } else {
            Intent floatingServiceIntent = new Intent(context, FloatingPopupService.class);
            floatingServiceIntent.putExtra("objectNames", objectList);
            floatingServiceIntent.putExtra("text", text);
            context.startForegroundService(floatingServiceIntent);
//            Toast.makeText(context, "I passed the Object Name to OverlayPermissionActivity", Toast.LENGTH_LONG).show();
        }

    }
}