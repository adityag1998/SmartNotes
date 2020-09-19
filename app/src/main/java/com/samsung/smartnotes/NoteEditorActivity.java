package com.samsung.smartnotes;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class NoteEditorActivity extends AppCompatActivity {

    int noteID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        //Bind editText view in Java
        EditText editText = (EditText) findViewById(R.id.editText);
        Intent intent = getIntent();
        noteID = intent.getIntExtra("com.samsung.smartnotes.MainActivity.noteID" , -1);

        //If Note is existent edit the note
        if (noteID != -1){
            editText.setText(MainActivity.notes.get(noteID));
        }
        else
        {
            MainActivity.notes.add("");                // as initially, the note is empty
            noteID = MainActivity.notes.size() - 1;
            MainActivity.arrayAdapter.notifyDataSetChanged();
        }

        //Change Note when text has been edited
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.notes.set(noteID, String.valueOf(s));
                MainActivity.arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

}