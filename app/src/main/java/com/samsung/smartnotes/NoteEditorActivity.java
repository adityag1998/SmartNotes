package com.samsung.smartnotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

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
            editText.setText(MainActivity.textList.get(noteID));
            editKey.setText(MainActivity.notesList.get(noteID).getKey());
        }
        else
        {
            MainActivity.notesList.add(new MainActivity.Note(MainActivity.textList.size(), "", ""));
            MainActivity.textList.add("");                // as initially, the note is empty
            noteID = MainActivity.notesList.size() - 1;
        }

        //Change Note when text has been edited
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.textList.set(noteID, String.valueOf(s));
                MainActivity.notesList.get(noteID).setText(String.valueOf(s));
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
                MainActivity.notesList.get(noteID).setKey(String.valueOf(s));
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }

    @Override
    protected void onDestroy() {
        if(MainActivity.notesList.get(noteID).getKey() == "" && MainActivity.textList.get(noteID) == "") {
            // delete created note
            MainActivity.textList.remove(noteID);
            MainActivity.notesList.remove(noteID);
        } else {
            //Add Logic to store Data
            SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                    , Context.MODE_PRIVATE);
            String jsonList = MainActivity.gson.toJson(MainActivity.notesList);
            sharedPreferences.edit().putString("notes", jsonList).apply();
        }
        MainActivity.arrayAdapter.notifyDataSetChanged();

        super.onDestroy();
    }

}