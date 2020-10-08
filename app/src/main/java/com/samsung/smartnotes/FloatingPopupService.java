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
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;

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
    static LinearLayout optionsParentView;
    static LinearLayout threeParentView;
    static ListView openNoteListView;
    static TextView createNoteKey;
    static TextView createNoteText;
    static LinearLayout fullListView;
    static ListView keyListView;
    static ArrayAdapter<String> keyAdapter = null;
    static ArrayAdapter<String> noteAdapter = null;

    static boolean noteFound = false;
    static int foundNotePosition = -1;
    static String receivedText;
    static ArrayList<String> receivedObjectNames;
    static ArrayList<String> keyListViewItems;
    static ArrayList<String> noteListViewItems;

    HashMap<String, Integer> notesWithIndex = new HashMap<>();


    public FloatingPopupService() {
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


    public void searchNotesAndUpdate(ArrayList<String> objectNameList) {
        notesWithIndex.clear();
        noteListViewItems.clear();
        if (notesList.size() == 0) {
            Toast.makeText(this, "Initializing DB", Toast.LENGTH_SHORT).show();
            initializeDB();
        }
//        //Toast.makeText(this, "NotesList[0] = " + notesList.get(0).getKeys().get(0), Toast.LENGTH_LONG).show();
        for (String objectName : objectNameList) {
            for (MainActivity.Note note : notesList) {
                for (String key : note.getKeys()) {
                    if (key.equals(objectName.toLowerCase())) {
                        noteListViewItems.add(objectName + ": " + note.getText());
                        notesWithIndex.put(objectName + ": " + note.getText(), notesList.indexOf(note));
                    }
                }
                for (String key : note.getTopTwoTerms()) {
                    if(key.equals(objectName.toLowerCase())) {
                        if (!note.getKeys().contains(key) && !note.getBlackListTerms().contains(key)) {
                            noteListViewItems.add(objectName + ": " + note.getText());
                            notesWithIndex.put(objectName + ": " + note.getText(), notesList.indexOf(note));
                        }
                    }
                }
            }
        }
        if(noteListViewItems.size() > 0) {
            noteAdapter.notifyDataSetChanged();
            threeParentView.setVisibility(View.VISIBLE);
            openNoteListView.setVisibility(View.VISIBLE);
        }
    }

    public void createNoteWithKeyUpdate(ArrayList<String> objectNamesList) {
        keyListViewItems.clear();
        for(String objects : receivedObjectNames) keyListViewItems.add(objects);
        keyAdapter.notifyDataSetChanged();
        threeParentView.setVisibility(View.VISIBLE);
        createNoteKey.setVisibility(View.VISIBLE);
    }


    public void removeAllViews() {
        fullListView.setVisibility(View.GONE);
        createNoteText.setVisibility(View.GONE);
        createNoteKey.setVisibility(View.GONE);
        openNoteListView.setVisibility(View.GONE);
        threeParentView.setVisibility(View.GONE);
        optionsParentView.setVisibility(View.GONE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        // throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        receivedObjectNames = intent.getStringArrayListExtra("objectNames");
        receivedText = intent.getStringExtra("text");

        removeAllViews();
        boolean nothingtoShow = true;

        if(NoteEditorActivity.isAddingKey) {
            if (receivedObjectNames != null && receivedObjectNames.size() > 0){
                nothingtoShow = false;
                createNoteWithKeyUpdate(receivedObjectNames);
            }
        } else {
            if (receivedObjectNames != null && receivedObjectNames.size() > 0) {
                nothingtoShow = false;
                searchNotesAndUpdate(receivedObjectNames);
                createNoteWithKeyUpdate(receivedObjectNames);
            }

            if (receivedText != null && receivedText.length() > 0) {
                nothingtoShow = false;
                threeParentView.setVisibility(View.VISIBLE);
                createNoteText.setVisibility(View.VISIBLE);
            }
        }

        // TODO: Implement this thing in ObjectNameReceiver (that will be more efficient).
        if(nothingtoShow)
            stopSelf();
        //Log.d(TAG, "receivedObjectName="+receivedObjectName+"<--");
//        if (receivedObjectName != null) {
//            if (receivedObjectName.equals("null")) {
//                //Log.d(TAG, "inside if");
//                stopSelf();
//            }
//            objectName.setText(receivedObjectName);
//
//            if (NoteEditorActivity.isAddingKey) { // Adding key using camera from NoteEditorActivity
//                noteText.setText("Add TAG");
//                noteText.setVisibility(View.VISIBLE);
//            } else { // Default case of receiving Object name from camera
//                searchNote(receivedObjectName);
//            }
//        }
        return START_STICKY;
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

        final Notification notification =
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

        //Specify the window parameters using instantiator
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

        ImageView closeButton = (ImageView) default_view.findViewById(R.id.click_to_close);
        ImageView objectCircle = (ImageView) default_view.findViewById(R.id.object_name_circle);
        optionsParentView = (LinearLayout) default_view.findViewById(R.id.optionsParentView);
        threeParentView = (LinearLayout) default_view.findViewById(R.id.threeParentView);
        openNoteListView = (ListView) default_view.findViewById(R.id.openNoteList);
        createNoteKey = (TextView) default_view.findViewById(R.id.keyNote);
        createNoteText = (TextView) default_view.findViewById(R.id.textNote);
        fullListView = (LinearLayout) default_view.findViewById(R.id.fullListView);
        ImageView backButton = (ImageView) default_view.findViewById(R.id.click_to_back);
        keyListView = (ListView) default_view.findViewById(R.id.floatingListView);

        // Initialize listViews
        noteListViewItems = new ArrayList<>();
        noteAdapter = new ArrayAdapter<>(this, R.layout.list_white_text, R.id.list_content, noteListViewItems);
        openNoteListView.setAdapter(noteAdapter);

        keyListViewItems = new ArrayList<>();
        keyAdapter = new ArrayAdapter<String>(this, R.layout.list_white_text, R.id.list_content, keyListViewItems);
        keyListView.setAdapter(keyAdapter);


        // Remove all views
        removeAllViews();

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
                if (optionsParentView.getVisibility() == View.GONE){
                    optionsParentView.setVisibility(View.VISIBLE);
                }
                else{
                    optionsParentView.setVisibility(View.GONE);
                }
            }
        });

        createNoteKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threeParentView.setVisibility(View.GONE);
                fullListView.setVisibility(View.VISIBLE);
            }
        });

        createNoteText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO : Create new note with text and no tag.
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullListView.setVisibility(View.GONE);
                threeParentView.setVisibility(View.VISIBLE);
            }
        });



        //OnClickListener for Text Description (Go to Notes App, Empty for Now)
//        noteText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // TODO: Either Create New Note [If no note found]
//                //  OR Open Existing Note in Note Editor Activity with key filled as Object name
//                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
//                if (NoteEditorActivity.isAddingKey) {  // Adding key using camera from NoteEditorActivity
//                    intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", NoteEditorActivity.currentNoteID);
//                    intent.putExtra("keyValue", receivedObjectName);
//                }
//                else { // Default case of receiving Object name from camera
//                    intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", foundNotePosition); // Which row was
//                    if (foundNotePosition == -1 && receivedObjectName != null) {
//                        intent.putExtra("keyValue", receivedObjectName);
//                    }
//                }
//                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//                startActivity(intent);
//                stopSelf();
//            }
//        });

        openNoteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int notePosition = notesWithIndex.get(noteListViewItems.get(position));

                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", notePosition);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                stopSelf();
            }
        });

        keyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                if(NoteEditorActivity.isAddingKey) {
                    intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", /*Adding key*/NoteEditorActivity.currentNoteID);
                } else {
                    intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", /*New note*/-1);
                }
                intent.putExtra("keyValue", keyListViewItems.get(position));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                stopSelf();
            }
        });

        createNoteText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", /*New note*/-1);
                intent.putExtra("textValue", receivedText);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_HISTORY);
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
                            if (optionsParentView.getVisibility() == View.VISIBLE){
                                optionsParentView.setVisibility(View.GONE);
                            }
                            else{
                                optionsParentView.setVisibility(View.VISIBLE);
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