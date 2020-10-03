package com.samsung.smartnotes;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

import static android.content.Intent.getIntent;
import static com.samsung.smartnotes.MainActivity.notesList;


public class FloatingPopupService extends Service {
    private View mFloatingView;
    private WindowManager mWindowManager;
    private static final String TAG = "FloatingPopupService";
    private static final int FOREGROUND_ID = 1338;
    private static final String DEFAULT_CHANNEL_ID = "SERVICE_NOTIFICATION";

    // Int variable to check if service is already running or not.
    public static int isFloatingServiceRunning = 0;

    static TextView objectName;
    static TextView noteText;
    static boolean noteFound = false;
    static int foundNotePosition = -1;
    static String receivedObjectName;


    public FloatingPopupService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        receivedObjectName = intent.getStringExtra("objectName");
        //Log.d(TAG, "receivedObjectName="+receivedObjectName+"<--");
        if (receivedObjectName != null) {
            if (receivedObjectName.equals("null")) {
                //Log.d(TAG, "inside if");
                stopSelf();
            }
            objectName.setText(receivedObjectName);

            if (NoteEditorActivity.isAddingKey) { // Adding key using camera from NoteEditorActivity
                noteText.setText("Add TAG");
                noteText.setVisibility(View.VISIBLE);
            } else { // Default case of receiving Object name from camera
                searchNote(receivedObjectName);
            }
        }
        return START_STICKY;
    }

    public void searchNote(String objectName) {
        if (notesList.size() == 0) {
            Toast.makeText(this, "Initializing DB", Toast.LENGTH_SHORT).show();
            initializeDB();
        }
        //Toast.makeText(this, "NotesList[0] = " + notesList.get(0).getKeys().get(0), Toast.LENGTH_LONG).show();
        for (MainActivity.Note note : notesList) {
            for (String key : note.getKeys()) {
                Log.d(TAG, "Key in note :"+ key.toLowerCase() + "<---->Object name got :" + objectName.toLowerCase() + "<----->");
                if (key.toLowerCase().equals(objectName.toLowerCase())) {
                    noteFound = true;
                    foundNotePosition = notesList.indexOf(note);
                    noteText.setText(note.getText());
                    return;
                }
            }

        }
        noteFound = false;
        foundNotePosition = -1;
        noteText.setText("Create new Note");
    }


    public void initializeDB() {
        // Logic to Retrieve from SharedPrefs
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                , Context.MODE_PRIVATE);

        String jsonList = sharedPreferences.getString("notes", null);
        Type type = new TypeToken<ArrayList<MainActivity.Note>>() {
        }.getType();
        notesList = MainActivity.gson.fromJson(jsonList, type);
        for (MainActivity.Note note : notesList) {
            MainActivity.textList.add(note.getText());
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        isFloatingServiceRunning = 1;

        // Show notification for this foreground service (Required after API 26)
        createNotificationChannel(DEFAULT_CHANNEL_ID, "Running Service", "Foreground service Notification");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, DEFAULT_CHANNEL_ID)
                .setContentTitle("Smart Notes")
                .setContentText("Running in background for object association")
                .setContentIntent(pendingIntent)
                .build();
        startForeground(FOREGROUND_ID, notification);


        // Inflate the floating view created
        mFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_note_widget , null);

        // Provide a parameter instantiator for window to populate my view
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        //Specify the window parameters using instatiator
        params.gravity = Gravity.TOP | Gravity.LEFT;
        //Initially view will be added to top-left corner

        //Bind the view to the window
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(mFloatingView, params);

        // Bind all Elements from XML to Java
        View default_view = mFloatingView.findViewById(R.id.complete_bubble);

        objectName = (TextView) default_view.findViewById(R.id.object_name_text);

        //Dynamically Setting properties for objectName
        objectName.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        objectName.setSingleLine(true);
        objectName.setSelected(true);
        objectName.setMarqueeRepeatLimit(-1);
        // Retrieve and set objectName
        // TODO: objectName.setText(Object Name from Camera)

        ImageView closeButton = (ImageView) default_view.findViewById(R.id.click_to_close);

        ImageView objectCircle = (ImageView) default_view.findViewById(R.id.object_name_circle);

        noteText = (TextView) default_view.findViewById(R.id.note_name_text);
        // Dynamically set NoteText Property
        noteText.setVisibility(View.GONE);
        // Retrieve and set note Description
        //TODO : noteText.setText (Note Text from SmartNotes)

        //OnClickListener for closeButton
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf(); //Self Destruct Service
            }
        });

        //OnClickListener for ObjectCircle
        objectCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Log.d(TAG, "onTouch: I have reached onTouch");
                // Logic to expand or contract Chat Bubble
                if (noteText.getVisibility() == View.GONE){
                    noteText.setVisibility(View.VISIBLE);
                }
                else{
                    noteText.setVisibility(View.GONE);
                }
            }
        });

        //OnClickListener for Text Description (Go to Notes App, Empty for Now)
        noteText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: Either Create New Note [If no note found]
                //  OR Open Existing Note in Note Editor Activity with key filled as Object name
                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                if (NoteEditorActivity.isAddingKey) {  // Adding key using camera from NoteEditorActivity
                    intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", NoteEditorActivity.currentNoteID);
                    intent.putExtra("keyValue", receivedObjectName);
                }
                else { // Default case of receiving Object name from camera
                    intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", foundNotePosition); // Which row was
                    if (foundNotePosition == -1 && receivedObjectName != null) {
                        intent.putExtra("keyValue", receivedObjectName);
                    }
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                stopSelf();
            }
        });

        //Drag Logic for Object Circle on Touch
        objectCircle.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                //Log.d(TAG, "onTouch: I have reached onTouch");
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        // Initial Position of view
                        initialX = params.x;
                        initialY = params.y;

                        // Your Touch Coordinates Location
                        initialTouchX = motionEvent.getRawX();
                        initialTouchY = motionEvent.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        // Get New Coordinates of window
                        params.x = initialX + (int)(motionEvent.getRawX()-initialTouchX);
                        params.y = initialY + (int)(motionEvent.getRawY()-initialTouchY);

                        // Update window layout
                        mWindowManager.updateViewLayout(mFloatingView, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        int Xdiff = (int) (motionEvent.getRawX() - initialTouchX);
                        int Ydiff = (int) (motionEvent.getRawY() - initialTouchY);

                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
                        //So that is click event.
                        if (Xdiff < 10 && Ydiff < 10){
                            if (noteText.getVisibility() == View.VISIBLE){
                                noteText.setVisibility(View.GONE);
                            }
                            else{
                                noteText.setVisibility(View.VISIBLE);
                            }
                        }
                }
                return false;
            }
        });
    }

    private void createNotificationChannel(String channelID, String channelName, String channelDescription) {
        // Create the NotificationChannel, but only on API 26+ because the NotificationChannel lass is new and not in the support library
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelID, channelName, importance);

            //Register the channel with the system; you can't change the importance or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    // On Destroy of service please do the following
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopForeground(true);
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
        isFloatingServiceRunning = 0;
    }
}