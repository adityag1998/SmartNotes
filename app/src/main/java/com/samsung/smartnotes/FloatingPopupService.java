package com.samsung.smartnotes;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
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

import androidx.core.app.ActivityCompat;

import static android.content.Intent.getIntent;

public class FloatingPopupService extends Service {
    private View mFloatingView;
    private WindowManager mWindowManager;
    private static final String TAG = "FloatingPopupService";

    // Int variable to check if service is already running or not.
    public static int isFloatingServiceRunning = 0;

    static TextView objectName;
    static TextView noteText;
    static boolean noteFound = false;
    static int foundNotePosition = -1;


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
        String receivedObjectName = intent.getStringExtra("objectName");
        objectName.setText(receivedObjectName);
        searchNote(receivedObjectName);
        return START_STICKY;
    }

    public void searchNote(String objectName) {
        for (MainActivity.Note note : MainActivity.notesList) {
            Log.d(TAG, "Key in note :"+ note.getKey().toLowerCase() + "<---->Object name got :" + objectName.toLowerCase() + "<----->");
            if (note.getKey().toLowerCase().equals(objectName.toLowerCase())) {
                noteFound = true;
                foundNotePosition = MainActivity.notesList.indexOf(note);
                noteText.setText(note.getText());
                return;
            }
        }
        noteFound = false;
        foundNotePosition = -1;
        noteText.setText("Create new Note");
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        isFloatingServiceRunning = 1;

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
        params.gravity = Gravity.TOP | Gravity.RIGHT;
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
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", foundNotePosition); // Which row was
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

    // On Destroy of service please do the following
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) mWindowManager.removeView(mFloatingView);
        isFloatingServiceRunning = 0;
    }
}