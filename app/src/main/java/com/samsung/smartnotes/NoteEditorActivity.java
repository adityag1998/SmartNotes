package com.samsung.smartnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import static com.samsung.smartnotes.MainActivity.notesList;


public class NoteEditorActivity extends AppCompatActivity {

    public static final String TAG = "NoteEditorActivityLogs";
    public static final double defaultLatitude = -90;
    public static final double defaultLongitude = -45;

    public static boolean isAddingKey = false;
    public static int currentNoteID = -1;
    static Button cameraButton;
    static Button addButton;
    static RecyclerView recyclerView;

    static ArrayList<String> topTwoKeys;
    static List<String> keysAdded;

    static FusedLocationProviderClient fusedLocationClient;
    static double currentLatitude = defaultLatitude;
    static double currentLongitude = defaultLongitude;

    int noteID;
    String keyValueFromService;
    ArrayList<String> textListFromService;
    ArrayList<String> keyValueListFromService;
    public List<String> keyList;
    KeyAdapter mAdapter;


    protected void showToast (String text){
        Toast.makeText(NoteEditorActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//        LocationRequest mLocationRequest = LocationRequest.create();
//        mLocationRequest.setInterval(60000);
//        mLocationRequest.setFastestInterval(5000);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        LocationCallback mLocationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                if (locationResult == null) {
//                    return;
//                }
//                for (Location location : locationResult.getLocations()) {
//                    if (location != null) {
//                        // UI updates.
//                    }
//                }
//            }
//        };
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, "Please give location permission from Samsung Notes -> menu -> Grant location permission", Toast.LENGTH_LONG).show();
//        }
//        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());

        //Logic to Receive Intent
        Intent intent = getIntent();
        noteID = intent.getIntExtra("com.samsung.smartnotes.MainActivity.noteID" , -1);
        keyValueFromService = intent.getStringExtra("keyValue");
        textListFromService = intent.getStringArrayListExtra("textValue");
        keyValueListFromService = intent.getStringArrayListExtra("keyValueList");


        //Initialize keyList Array
        keyList = new ArrayList<>();

        //Bind editText view in Java
        EditText editText = (EditText) findViewById(R.id.editText);
        final EditText editKey = (EditText) findViewById(R.id.editKey);
        cameraButton = (Button) findViewById(R.id.cameraButton);
        addButton = (Button) findViewById(R.id.addButton);
        final TextView tfidfView = (TextView) findViewById(R.id.textViewTfidf);

        if(!MainActivity.developerOptions)
            tfidfView.setVisibility(View.GONE);

        //Bind keyList to recycler view, we have to bind it after initialization because we cannot bind null keyList to KeyAdapter
        recyclerView = findViewById(R.id.keyRecyclerView);
        mAdapter = new KeyAdapter(keyList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mAdapter);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setAddDuration(1000);
        itemAnimator.setRemoveDuration(1000);
        recyclerView.setItemAnimator(itemAnimator);


        // If Note is existing edit the note
        if (noteID != -1){
            editText.setText(MainActivity.textList.get(noteID));
            addAllKeysToList(notesList.get(noteID).getKeys());
            mAdapter.notifyDataSetChanged();
            if(isAddingKey && keyValueFromService != null) {
                addProvidedKeyToList(keyValueFromService);
            }
            isAddingKey = false;
            currentNoteID = -1;

            if(!notesList.get(noteID).isTfidfUpdated) {
                TfidfCalculation.recalculateAllTfidf();
            }
        }

        else // create a new note
        {
            // getLocation
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Kindly provide location permission in Samsung Notes -> Menu -> Grant Location Permission", Toast.LENGTH_LONG).show();
            } else {
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    currentLatitude = location.getLatitude();
                                    currentLongitude = location.getLongitude();
                                } else {
                                    System.out.println("Location not received");
                                }
                            }
                        });
            }

            if(keyValueFromService != null) {  // check if new note is created from camera intent
                keyList.add(keyValueFromService.toLowerCase());
                mAdapter.notifyDataSetChanged();
                notesList.add(new MainActivity.Note(MainActivity.textList.size(), keyValueFromService.toLowerCase(), "", currentLatitude, currentLongitude));
                MainActivity.textList.add("");
            } else if (textListFromService != null && textListFromService.size()>0) {
                String processedText = processTextList(textListFromService);
                editText.setText(processedText);

                MainActivity.Note newNote = new MainActivity.Note(MainActivity.textList.size(), processedText, currentLatitude, currentLongitude);
                if(keyValueListFromService != null) {
                    for(String receivedKey : keyValueListFromService) {
                        keyList.add(receivedKey.toLowerCase());
                        mAdapter.notifyDataSetChanged();
                        newNote.addKey(receivedKey.toLowerCase());
                    }
                }
                notesList.add(newNote);
                MainActivity.textList.add(processedText);
            } else {
                notesList.add(new MainActivity.Note(MainActivity.textList.size(), "", currentLatitude, currentLongitude));
                MainActivity.textList.add("");
            }
            isAddingKey = false;
            currentNoteID = -1;

                           // as initially, the note is empty
            noteID = notesList.size() - 1;
        }

        if(notesList.get(noteID).getTermTfidfMap() != null) {
            tfidfView.setText(notesList.get(noteID).getTermTfidfMap().toString());
        }

        // Add smart Keys
        addSmartKeys();
        mAdapter.notifyDataSetChanged();
        System.out.println("Location of Current Note : lat=" + notesList.get(noteID).getLatitude() + "  long="+ notesList.get(noteID).getLongitude());


        //Change Note when text has been edited
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.textList.set(noteID, String.valueOf(s));
                notesList.get(noteID).setText(String.valueOf(s));
                notesList.get(noteID).isTfidfUpdated = false;

                for(int i=start; i<(start+count) ; ++i) {
                    if(s.charAt(i) == ' '){
                        // showToast("updating Tfidf");
                        notesList.get(noteID).isTermMapsUpdated = false;
                        TfidfCalculation.recalculateAllTfidf();
                        checkAndRemoveBlacklistedTerms();
                        refreshSmartKeysInKeyList();
                        mAdapter.notifyDataSetChanged();
                        tfidfView.setText(notesList.get(noteID).getTermTfidfMap().toString());
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isAddingKey = true;
                currentNoteID = noteID;
                Intent cameraIntent = getPackageManager().getLaunchIntentForPackage("com.samsung.navicam");
                if(cameraIntent != null) {
                    cameraIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    startActivity(cameraIntent);
                    finish();
                } else {
                    Toast.makeText(getBaseContext(), "Camera app not found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newKey = String.valueOf(editKey.getText());
                addProvidedKeyToList(newKey);

                editKey.setText("");
            }
        });

    }

    public static String processTextList(ArrayList<String> inputList) {
        StringBuilder sb = new StringBuilder();
        for (String line : inputList) {
            if(line.length() > 0) {
                sb.append(line);
                if(!line.equals(inputList.get(inputList.size() - 1)))
                    sb.append("\n\n");
            }
        }
        return sb.toString();
    }

    public void addProvidedKeyToList(String key) {
        key = key.toLowerCase();
        if(key.replaceAll(" ", "").length() == 0)
            return;
        if(notesList.get(noteID).getKeys().contains(key)) {
            Toast.makeText(this, "TAG already present", Toast.LENGTH_SHORT).show();
            return;
        }

        while(keyList.size() != notesList.get(noteID).getKeys().size()) {
            keyList.remove(notesList.get(noteID).getKeys().size());
        }

        if(notesList.get(noteID).getBlackListTerms().contains(key)){
            notesList.get(noteID).getBlackListTerms().remove(key);
        }

        TfidfCalculation.updateTopTwoTerms(notesList.get(noteID));

        // now add the new key
        keyList.add(key);
        //mAdapter.notifyDataSetChanged();
        notesList.get(noteID).addKey(key);

        // now add smartKeys to the list
        addSmartKeys();
        recyclerView.invalidate();
        mAdapter.notifyDataSetChanged();
    }

    public void addSmartKeys() {
        Log.d(TAG, "TopTwoTerms list is : " + notesList.get(noteID).getTopTwoTerms().toString() );
        for(String keys : notesList.get(noteID).getTopTwoTerms()) {
            if(!keyList.contains(keys)){
                keyList.add(keys);
                // mAdapter.notifyDataSetChanged();
            }
        }
        // mAdapter.notifyDataSetChanged();
    }

    public void refreshSmartKeysInKeyList() {
        while(keyList.size() != notesList.get(noteID).getKeys().size()) {
            keyList.remove(notesList.get(noteID).getKeys().size());
        }
        TfidfCalculation.updateTopTwoTerms(notesList.get(noteID));
        addSmartKeys();
    }

    public void addAllKeysToList(ArrayList<String> inputKeyList) {
        if(keyList == null) {
            keyList = new ArrayList<>();
        }
        for (String key : inputKeyList) {
            keyList.add(key);
        }
        //mAdapter.notifyDataSetChanged();
    }

    public void checkAndRemoveBlacklistedTerms() {
        ArrayList<String> tempList = new ArrayList<>();

        for (String term : notesList.get(noteID).getBlackListTerms()) {
            tempList.add(term);
        }

        for (String term : tempList) {
            if(!notesList.get(noteID).getTermCounts().keySet().contains(term)) {
                notesList.get(noteID).getBlackListTerms().remove(term);
            }
        }
    }

    @Override
    protected void onDestroy() {
        System.out.println("Keys size :" + Integer.toString(notesList.get(noteID).getKeys().size()) + "----text=" + MainActivity.textList.get(noteID) + "---->");
        if(notesList.get(noteID).getKeys().size() == 0 && MainActivity.textList.get(noteID).equals("")) {
            // delete created note
            MainActivity.textList.remove(noteID);
            notesList.remove(noteID);
        } else {
            // Calculate Tfidf for this note.
            notesList.get(noteID).isTermMapsUpdated = false;
            notesList.get(noteID).isTfidfUpdated = false;

            // TODO: We can optimize the code here to check if the terms array has changed or not to do the calculations again.
            TfidfCalculation.recalculateAllTfidf();

            // Check and remove all blacklisted items if removed from note.
            checkAndRemoveBlacklistedTerms();
        }

        // Add Logic to store Data
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                , Context.MODE_PRIVATE);
        String jsonList = MainActivity.gson.toJson(notesList);
        sharedPreferences.edit().putString("notes", jsonList).apply();

        if(MainActivity.mNoteAdapter != null) {
            MainActivity.mNoteAdapter.notifyDataSetChanged();
        }

        super.onDestroy();
    }

//    public void reloadAllRecyclerViewData() {
//        mAdapter.getData().clear();
//        mAdapter.getData().addAll(keyList);
//        mAdapter.notifyDataSetChanged();
//    }

    public class KeyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private List<String> keyList;

        public KeyAdapter(List<String> keyList) {
            this.keyList = keyList;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.key_list, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            String keyVal = keyList.get(position);
            if(position >= notesList.get(noteID).getKeys().size()) {
                holder.keyParent.setBackgroundColor(Color.parseColor("#ffde59"));
            } else {
                holder.keyParent.setBackgroundColor(Color.parseColor("#ffffff"));
            }
            holder.key.setText(keyVal);
        }

        @Override
        public int getItemCount() {
            return keyList.size();
        }

        public List<String> getData() {
            return this.keyList;
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView key;
        LinearLayout keyParent;
        MyViewHolder(View view) {
            super(view);
            key = view.findViewById(R.id.keyValue);
            keyParent = view.findViewById(R.id.keyParent);

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    final int position = getAdapterPosition();

                    new AlertDialog.Builder(NoteEditorActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Delete")
                            .setMessage("Are you sure you want to delete this TAG?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    if(position >= notesList.get(noteID).getKeys().size()) {
                                        notesList.get(noteID).getBlackListTerms().add(keyList.get(position));
                                        notesList.get(noteID).getTopTwoTerms().remove(keyList.get(position));
                                    } else {
                                        notesList.get(noteID).getKeys().remove(position);
                                        keyList.remove(position);
                                    }
                                    refreshSmartKeysInKeyList();
                                    mAdapter.notifyDataSetChanged();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();

                    return true;
                }
            });
        }
    }

}