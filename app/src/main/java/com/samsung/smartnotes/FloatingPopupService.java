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
import android.util.Pair;
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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Vector;

import static com.samsung.smartnotes.MainActivity.notesList;


public class FloatingPopupService extends Service {
    private View mFloatingView;
    private WindowManager mWindowManager;
    private static final String TAG = "FloatingPopupService";
    private static final int FOREGROUND_ID = 1338;
    private static final String DEFAULT_CHANNEL_ID = "SERVICE_NOTIFICATION";
    private static final int MINIMUM_MATCHING_TERMS = 2;

    // Int variable to check if service is already running or not.
    public static int isFloatingServiceRunning = 0;

    static TextView inCircleText;
    static LinearLayout optionsParentView;
    static LinearLayout threeParentView;
    static ListView openNoteListView;
    static ListView addKeyListView;
    static TextView createNoteUsingKeyView;
    static TextView createNoteUsingTextView;
    static LinearLayout fullListView;
    static ImageView backButton;
    static ListView createUsingKeyListView;
    static ImageView listIconView;

    static ArrayAdapter<String> createKeyAdapter = null;
    static ArrayAdapter<String> noteAdapter = null;
    static ArrayAdapter<String> addKeyAdapter = null;

    static ArrayList<String> receivedText;
    static ArrayList<String> receivedObjectNames;
    static ArrayList<String> createKeyListViewItems;
    static ArrayList<String> addKeyListViewItems;
    static ArrayList<String> noteListViewItems;
    static ArrayList<Pair<String, Integer>> noteListViewItemsWithIndex = new ArrayList<Pair<String, Integer>>();


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


    public void searchNotesForKeyAndUpdate(ArrayList<String> objectNameList) {
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
                        noteListViewItemsWithIndex.add(new Pair(objectName + ": " + note.getText(), notesList.indexOf(note)));
                    }
                }
                for (String key : note.getTopTwoTerms()) {
                    if(key.equals(objectName.toLowerCase())) {
                        if (!note.getKeys().contains(key) && !note.getBlackListTerms().contains(key)) {
                            noteListViewItems.add(objectName + ": " + note.getText());
                            noteListViewItemsWithIndex.add(new Pair(objectName + ": " + note.getText(), notesList.indexOf(note)));
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

    public void checkLastTermAndSort(Vector<Pair<Integer, Integer>> v) {
        for(int i = v.size() - 1; i > 0; --i) {
            if(v.get(i).second > v.get(i-1).second) {
                Pair<Integer, Integer> temp = v.get(i-1);
                v.set(i-1, v.get(i));
                v.set(i, temp);
            }
        }
    }

    public void checkAndInsert(Vector<Pair<Integer, Integer>> v, int position, int numMatchingTerms) {
        if(v.size() < 5) {
            v.add(new Pair(position, numMatchingTerms));
            checkLastTermAndSort(v);
            return;
        }

        if(v.size() == 5) {
            if(v.get(v.size() - 1).second < numMatchingTerms) {
                v.set(v.size() - 1, new Pair(position, numMatchingTerms));
                checkLastTermAndSort(v);
            }
        }
    }

    public void searchNotesForTextAndUpdate(ArrayList<String> textList) {
        if (notesList.size() == 0) {
            Toast.makeText(this, "Initializing DB", Toast.LENGTH_SHORT).show();
            initializeDB();
        }
        ArrayList<String> terms = TfidfCalculation.getAllTerms(textList);

        Vector<Pair<Integer, Integer>> topFive = new Vector<Pair<Integer, Integer>>();

        for(MainActivity.Note note : notesList) {
            int numMatchingTerms = 0;
            for(String term : terms) {
                if(note.getTermCounts().keySet().contains(term)) numMatchingTerms++;
            }
            checkAndInsert(topFive, notesList.indexOf(note), numMatchingTerms);
        }

        for(Pair<Integer, Integer> item : topFive) {
            if(item.second >= MINIMUM_MATCHING_TERMS) {
                noteListViewItems.add("Text: " + notesList.get(item.first).getText());
                noteListViewItemsWithIndex.add(new Pair("Text: " + notesList.get(item.first).getText(), item.first));
            }
        }
        if(noteListViewItems.size() > 0) {
            noteAdapter.notifyDataSetChanged();
            threeParentView.setVisibility(View.VISIBLE);
            openNoteListView.setVisibility(View.VISIBLE);
        }
    }

    public void createNoteWithKeyUpdate(ArrayList<String> objectNamesList) {
        if(NoteEditorActivity.isAddingKey){
            inCircleText.setText("Add TAG");
            addKeyListViewItems.clear();
            for (String objects : receivedObjectNames) addKeyListViewItems.add(objects);
            addKeyAdapter.notifyDataSetChanged();
            threeParentView.setVisibility(View.VISIBLE);
            addKeyListView.setVisibility(View.VISIBLE);
        } else {
            createKeyListViewItems.clear();
            for (String objects : receivedObjectNames) createKeyListViewItems.add(objects);
            createKeyAdapter.notifyDataSetChanged();
            threeParentView.setVisibility(View.VISIBLE);
            createNoteUsingKeyView.setVisibility(View.VISIBLE);
        }
    }


    public void removeAllViews() {
        fullListView.setVisibility(View.GONE);  // contains both the back button and create using key list
        createNoteUsingTextView.setVisibility(View.GONE);
        createNoteUsingKeyView.setVisibility(View.GONE);
        openNoteListView.setVisibility(View.GONE);
        addKeyListView.setVisibility(View.GONE);
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
        receivedText = intent.getStringArrayListExtra("text");

        if(optionsParentView.getVisibility() == View.VISIBLE) {
            return START_STICKY;
        }

        if ((receivedObjectNames == null || receivedObjectNames.size() == 0) && (receivedText == null || receivedText.size() == 0))
            stopSelf();

        StringBuilder circleText = new StringBuilder();
        if(receivedObjectNames != null && receivedObjectNames.size() > 0)
            for(String objects : receivedObjectNames) if(objects.length()>0) {
                circleText.append(objects);
                if(!objects.equals(receivedObjectNames.get(receivedObjectNames.size() -1)))
                    circleText.append(", ");
            }
        if(receivedText != null && receivedText.size() > 0) {
            if (circleText.length() > 0)
                circleText.append(", ");
            circleText.append("TEXT");
        }

        inCircleText.setText(circleText);
        removeAllViews();
        noteListViewItemsWithIndex.clear();
        noteListViewItems.clear();
        if (NoteEditorActivity.isAddingKey) {
            if (receivedObjectNames != null && receivedObjectNames.size() > 0) {
                createNoteWithKeyUpdate(receivedObjectNames);
            }
        } else {
            if (receivedObjectNames != null && receivedObjectNames.size() > 0) {
                searchNotesForKeyAndUpdate(receivedObjectNames);
                createNoteWithKeyUpdate(receivedObjectNames);
            }
            if (receivedText != null && receivedText.size() > 0) {
                searchNotesForTextAndUpdate(receivedText);
                threeParentView.setVisibility(View.VISIBLE);
                createNoteUsingTextView.setVisibility(View.VISIBLE);
            }
        }

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

        inCircleText = (TextView) default_view.findViewById(R.id.object_name_text);

        //Dynamically Setting properties for objectName
        inCircleText.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        inCircleText.setSingleLine(true);
        inCircleText.setSelected(true);
        inCircleText.setMarqueeRepeatLimit(-1);

        ImageView closeButton = (ImageView) default_view.findViewById(R.id.click_to_close);
        ImageView objectCircle = (ImageView) default_view.findViewById(R.id.object_name_circle);
        optionsParentView = (LinearLayout) default_view.findViewById(R.id.optionsParentView);
        threeParentView = (LinearLayout) default_view.findViewById(R.id.threeParentView);
        openNoteListView = (ListView) default_view.findViewById(R.id.openNoteList);
        addKeyListView = (ListView) default_view.findViewById(R.id.addKeyList);
        createNoteUsingKeyView = (TextView) default_view.findViewById(R.id.keyNote);
        createNoteUsingTextView = (TextView) default_view.findViewById(R.id.textNote);
        fullListView = (LinearLayout) default_view.findViewById(R.id.fullListView);
        backButton = (ImageView) default_view.findViewById(R.id.click_to_back);
        createUsingKeyListView = (ListView) default_view.findViewById(R.id.floatingListView);

        // Initialize listViews
        noteListViewItems = new ArrayList<>();
        noteAdapter = new ArrayAdapter<>(this, R.layout.list_open_note, R.id.list_content, noteListViewItems);
        openNoteListView.setAdapter(noteAdapter);

        addKeyListViewItems = new ArrayList<>();
        addKeyAdapter = new ArrayAdapter<>(this, R.layout.list_add_tag, R.id.list_content, addKeyListViewItems);
        addKeyListView.setAdapter(addKeyAdapter);

        createKeyListViewItems = new ArrayList<>();
        createKeyAdapter = new ArrayAdapter<String>(this, R.layout.list_create_new_note, R.id.list_content, createKeyListViewItems);
        createUsingKeyListView.setAdapter(createKeyAdapter);

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

        createNoteUsingKeyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                threeParentView.setVisibility(View.GONE);
                fullListView.setVisibility(View.VISIBLE);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fullListView.setVisibility(View.GONE);
                threeParentView.setVisibility(View.VISIBLE);
            }
        });

        openNoteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int notePosition = noteListViewItemsWithIndex.get(position).second;

                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", notePosition);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                stopSelf();
            }
        });

        createUsingKeyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", /*New note*/-1);
                intent.putExtra("keyValue", createKeyListViewItems.get(position));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                stopSelf();
            }
        });

        createNoteUsingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", /*New note*/-1);
                intent.putExtra("textValue", receivedText);
                intent.putExtra("keyValueList", receivedObjectNames);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
                stopSelf();
            }
        });

        addKeyListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", /*Adding key*/NoteEditorActivity.currentNoteID);
                intent.putExtra("keyValue", addKeyListViewItems.get(position));
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