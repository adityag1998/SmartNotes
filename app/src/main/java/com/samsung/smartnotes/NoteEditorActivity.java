package com.samsung.smartnotes;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;


import java.util.HashSet;

public class NoteEditorActivity extends AppCompatActivity {

    int noteID;

    protected void showToast (String text){
        Toast.makeText(NoteEditorActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        //Logic to Receive Intent
        Intent intent = getIntent();
        noteID = intent.getIntExtra("com.samsung.smartnotes.MainActivity.noteID" , -1);

        //Bind editText view in Java
        EditText editText = (EditText) findViewById(R.id.editText);
        EditText editKey = (EditText) findViewById(R.id.editKey);

        //If Note is existent edit the note
        if (noteID != -1){
            editText.setText(MainActivity.notesList.get(noteID));
            editKey.setText(MainActivity.notes.get(noteID).getKey());
        }
        else
        {
            MainActivity.notes.add(new MainActivity.Note(MainActivity.notesList.size(), "", ""));
            MainActivity.notesList.add("");                // as initially, the note is empty
            noteID = MainActivity.notes.size() - 1;
        }

        //Change Note when text has been edited
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.notesList.set(noteID, String.valueOf(s));
                MainActivity.notes.get(noteID).setText(String.valueOf(s));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        editKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.notes.get(noteID).setKey(String.valueOf(s));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    protected void onDestroy() {
        if(MainActivity.notes.get(noteID).getKey() == "" && MainActivity.notesList.get(noteID) == "") {
            // delete created note
            MainActivity.notesList.remove(noteID);
            MainActivity.notes.remove(noteID);
        } else {
            //Add Logic to store Data
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                    , Context.MODE_PRIVATE);
            String jsonList = MainActivity.gson.toJson(MainActivity.notes);
            sharedPreferences.edit().putString("notes", jsonList).apply();
        }
        MainActivity.arrayAdapter.notifyDataSetChanged();

        super.onDestroy();
    }

}