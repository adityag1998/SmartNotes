package com.samsung.smartnotes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    static class Note {
        int id;
        ArrayList<String> keyList;
        String text;

        ArrayList<String> topTwoTerms = new ArrayList<>();
        ArrayList<String> blackListTerms = new ArrayList<>();

        public boolean isTermMapsUpdated = false;
        public boolean isTfidfUpdated = false;
        HashMap<String, Integer> termCountMap = new HashMap<>();
        HashMap<String, Double> termFreqMap = new HashMap<>();
        HashMap<String, Double> termTfidfMap = new HashMap<>();


        public Note(int id, ArrayList<String> keyList, String text) {
            this.id = id;
            this.keyList = new ArrayList<String>();
            for ( String key : keyList ) {
                this.keyList.add(key);
            }
            this.text = text;
        }

        public Note(int id, String key, String text) {
            this.id = id;
            this.keyList = new ArrayList<String>();
            this.keyList.add(key);
            this.text = text;
        }

        public Note(int id, String text) {
            this.id = id;
            this.keyList = new ArrayList<String>();
            this.text = text;
        }

        void addKey(String keyValue) {
            this.keyList.add(keyValue);
        }

        void removeKey(String keyValue) {
            this.keyList.remove(keyValue);
        }

        void setText(String textValue) {
            this.text = textValue;
        }

        ArrayList<String> getKeys() {
            return this.keyList;
        }

        String getText() {
            return this.text;
        }


        HashMap<String, Integer> getTermCounts() {
            return this.termCountMap;
        }

        HashMap<String,Double> getTermFreqMap() {
            return this.termFreqMap;
        }

        HashMap<String,Double> getTermTfidfMap() {
            return this.termTfidfMap;
        }

        ArrayList<String> getTopTwoTerms() {
            return this.topTwoTerms;
        }
        ArrayList<String> getBlackListTerms() {
            return this.blackListTerms;
        }

        void setTermCounts(HashMap<String, Integer> inMap) {
            this.termCountMap = new HashMap<String, Integer>(inMap);
        }

        void setTermFreqMap(HashMap<String, Double> inMap) {
            this.termFreqMap = new HashMap<String, Double>(inMap);
        }

        void setTermTfidfMap(HashMap<String, Double> inMap) {
            this.termTfidfMap = new HashMap<String, Double>(inMap);
        }

        void setTopTwoTerms(ArrayList<String> inArr) {
            this.topTwoTerms = new ArrayList<>(inArr);
        }
    }

    static ArrayList<Note> notesList = new ArrayList<Note>();
    static ArrayList<String> textList = new ArrayList<String>();

    static boolean developerOptions = false;
    static Gson gson = new Gson();
    static NoteAdapter mNoteAdapter;
    protected RecyclerView noteRecyclerView;

    // Request Service Code for Overlay Settings
    private static final int POPUP_DRAW_OVER_OTHER_APP_PERMISSION = 1001;

    protected void showToast (String text){
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize the NoteEditorActivity variables
        NoteEditorActivity.isAddingKey = false;
        NoteEditorActivity.currentNoteID = -1;

        if( notesList.size() == 0 ) {
            // Logic to Retrieve from SharedPrefs
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                    , Context.MODE_PRIVATE);

            String jsonList = sharedPreferences.getString("notes", null);
            if (jsonList == null) {
                //Add a sample note
                String exampleNote = "This is an Example Note";
                notesList.add(new Note(0, "example", exampleNote));
                textList.add(exampleNote);
            } else {
                Type type = new TypeToken<ArrayList<Note>>() {
                }.getType();
                notesList = gson.fromJson(jsonList, type);
                for (Note note : notesList) {
                    textList.add(note.getText());
                }
            }
        }

        // Bind ListView with Array Adapter
        noteRecyclerView = findViewById(R.id.recyclerView);
        mNoteAdapter = new NoteAdapter(textList);
        noteRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        noteRecyclerView.setAdapter(mNoteAdapter);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), com.samsung.smartnotes.NoteEditorActivity.class);
                startActivity(intent);
            }
        });

        //TfidfCalculation.updateAllTfidf();
        TfidfCalculation.recalculateAllTfidf();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Add listener to ListView and send local Intent on position of listView
        mNoteAdapter.setmOnItemClickListener(new NoteAdapter.onItemClickListener() {
            @Override
            public void onItemClick(View v, int position) {
                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", position); // Which row was tapped
                startActivity(intent);
            }
        });

        // Add listener to listView and Long Click to delete
        mNoteAdapter.setmOnItemLongClickListener(new NoteAdapter.onItemLongClickListener() {
            @Override
            public boolean onItemLongClick(View v, int position) {
                final int mPosition = position;
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this note?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                notesList.remove(mPosition);
                                textList.remove(mPosition);
                                mNoteAdapter.notifyDataSetChanged();
                                TfidfCalculation.recalculateAllTfidf();

                                //Add Logic to edit stored Data
                                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                                        , Context.MODE_PRIVATE);
                                String jsonList = gson.toJson(notesList);
                                sharedPreferences.edit().putString("notes", jsonList).apply();
                            }
                        })

                        .setNegativeButton("No", null)
                        .show();

                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu){
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item)
    {
        super.onOptionsItemSelected(item);

        if(item.getItemId() == R.id.developerOption)
        {
            if(developerOptions == false) developerOptions = true;
            else developerOptions = false;
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}