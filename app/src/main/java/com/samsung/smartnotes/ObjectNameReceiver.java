package com.samsung.smartnotes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class ObjectNameReceiver extends BroadcastReceiver {
    private static final String TAG = "ObjectNameReceiver";
    private static final String KEY = "com.example.dummycamera.objectName";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Toast.makeText(context, "I heard you Object Name Broadcast", Toast.LENGTH_LONG).show();
        StringBuilder sb = new StringBuilder();
        sb.append("Action: " + intent.getAction() + "\n");
        sb.append("Key: " + KEY + "\n");
        sb.append("Value: " + intent.getStringExtra(KEY) + "\n");
        String log = sb.toString();
        Toast.makeText(context, log, Toast.LENGTH_LONG).show();
        Log.d(TAG, log);
    }
}