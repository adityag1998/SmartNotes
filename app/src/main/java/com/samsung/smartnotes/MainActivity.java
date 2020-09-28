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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {

    static class Note {
        int id;
        String key;
        String text;

        public Note(int id, String key, String text) {
            this.id = id;
            this.key = key;
            this.text = text;
        }

        void setKey(String keyValue) {
            this.key = keyValue;
        }

        void setText(String textValue) {
            this.text = textValue;
        }

        String getKey() {
            return this.key;
        }

        String getText() {
            return this.text;
        }
    }

    static ArrayList<Note> notesList = new ArrayList<Note>();
    static ArrayList<String> textList = new ArrayList<String>();

    static Gson gson = new Gson();
    static ArrayAdapter<String> arrayAdapter;
    protected ListView listView;

    protected void showToast (String text){
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, textList);
        listView.setAdapter(arrayAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Add listener to ListView and send local Intent on position of listView
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), NoteEditorActivity.class);
                intent.putExtra("com.samsung.smartnotes.MainActivity.noteID", position); // Which row was tapped
                startActivity(intent);
            }
        });

        // Add listener to listView and Long Click to delete
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
            {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this note?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                notesList.remove(position);
                                textList.remove(position);
                                arrayAdapter.notifyDataSetChanged();

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

        if(item.getItemId() == R.id.createNewNote)
        {
            Intent intent = new Intent(getApplicationContext(), com.samsung.smartnotes.NoteEditorActivity.class);
            startActivity(intent);
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}