package com.samsung.smartnotes;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;


public class OverlayPermissionActivity extends Activity {

    // Request Service Code for Overlay Settings
    private static final int POPUP_DRAW_OVER_OTHER_APP_PERMISSION = 1001;

    private static String TAG = "ObjectPermissionActivity: ";

    private void startFloatingService() {
        // TODO : implement starting of floating service and also pass the intent received from the ObjectName Receiver.
        startService(new Intent(OverlayPermissionActivity.this, FloatingPopupService.class));
        finish(); // Destroy Activity just activate it's service
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overlay_permission);

        //Check if the application has draw over other apps permission or not?
        //This permission is by default available for API<23. But for API > 23
        //I will ask for the permission in runtime.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            //Fire Intent to ask permission at runtime
            Intent settingIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            settingIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivityForResult(settingIntent, POPUP_DRAW_OVER_OTHER_APP_PERMISSION);
        } else {
            finish();
        }
    }

    // For Android P or greater, it is not mandatory for settings to return RESULT_OK on settings change
    // That's why explicit check is made for settings.canDrawOverlays additionally
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Check if the permission is granted or not.
        if (requestCode == POPUP_DRAW_OVER_OTHER_APP_PERMISSION) {
            if (resultCode != RESULT_OK && !Settings.canDrawOverlays(this)) { //Permission is not available
                Toast.makeText(this,
                        "Draw over other app permission not available. Closing the application",
                        Toast.LENGTH_SHORT).show();
            }
            finish(); // Destroy Activity without doing anything
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

}