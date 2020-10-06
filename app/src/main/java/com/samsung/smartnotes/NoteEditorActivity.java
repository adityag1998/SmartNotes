package com.samsung.smartnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class NoteEditorActivity extends AppCompatActivity {

    public static boolean isAddingKey = false;
    public static int currentNoteID = -1;
    static Button cameraButton;
    static Button addButton;

    int noteID;
    String keyValue;
    public List<String> keyList;
    KeyAdapter mAdapter;


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
        keyValue = intent.getStringExtra("keyValue");

        //Initialize keyList Array
        keyList = new ArrayList<>();

        //Bind editText view in Java
        EditText editText = (EditText) findViewById(R.id.editText);
        final EditText editKey = (EditText) findViewById(R.id.editKey);
        cameraButton = (Button) findViewById(R.id.cameraButton);
        addButton = (Button) findViewById(R.id.addButton);
        TextView tfidfView = (TextView) findViewById(R.id.textViewTfidf);

        //Bind keyList to recycler view, we have to bind it after initialization because we cannot bind null keyList to KeyAdapter
        RecyclerView recyclerView = findViewById(R.id.keyRecyclerView);
        mAdapter = new KeyAdapter(keyList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);


        // If Note is existing edit the note
        if (noteID != -1){
            editText.setText(MainActivity.textList.get(noteID));
            addAllKeysToList(MainActivity.notesList.get(noteID).getKeys());
            mAdapter.notifyDataSetChanged();
            if(isAddingKey && keyValue != null) {
                if(keyList.contains(keyValue)) { // TAG already present
                    Toast.makeText(this, "TAG already present", Toast.LENGTH_LONG).show();
                } else {
                    keyList.add(keyValue);
                    mAdapter.notifyDataSetChanged();
                    MainActivity.notesList.get(noteID).addKey(keyValue);
                }
            }
            isAddingKey = false;
            currentNoteID = -1;
        }
        else // create a new note
        {
            if(keyValue != null) {  // check if new note is created from camera intent
                keyList.add(keyValue);
                mAdapter.notifyDataSetChanged();
                MainActivity.notesList.add(new MainActivity.Note(MainActivity.textList.size(), keyValue, ""));
            } else {
                MainActivity.notesList.add(new MainActivity.Note(MainActivity.textList.size(), ""));
            }
            isAddingKey = false;
            currentNoteID = -1;

            MainActivity.textList.add("");                // as initially, the note is empty
            noteID = MainActivity.notesList.size() - 1;
        }

        if(MainActivity.notesList.get(noteID).getTermTfidfMap() != null) {
            tfidfView.setText(MainActivity.notesList.get(noteID).getTermTfidfMap().toString());
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
                MainActivity.notesList.get(noteID).isTfidfUpdated = false;
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
                Intent cameraIntent = getPackageManager().getLaunchIntentForPackage("com.example.dummycamera");
                if(cameraIntent != null) {
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
                for (String key : keyList) {
                    if (key.toLowerCase().equals(newKey.toLowerCase())) {
                        Toast.makeText(getBaseContext(), "TAG already present", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                MainActivity.notesList.get(noteID).addKey(newKey);
                keyList.add(newKey);
                mAdapter.notifyDataSetChanged();
                editKey.setText("");
            }
        });



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


    @Override
    protected void onDestroy() {
        if(MainActivity.notesList.get(noteID).getKeys().size() == 0 && MainActivity.textList.get(noteID) == "") {
            // delete created note
            MainActivity.textList.remove(noteID);
            MainActivity.notesList.remove(noteID);
        } else {
            // Calculate Tfidf for this note.
            TfidfCalculation.updateNoteTfidf(MainActivity.notesList.get(noteID));
        }

        // Add Logic to store Data
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                , Context.MODE_PRIVATE);
        String jsonList = MainActivity.gson.toJson(MainActivity.notesList);
        sharedPreferences.edit().putString("notes", jsonList).apply();

        if(MainActivity.arrayAdapter != null) {
            MainActivity.arrayAdapter.notifyDataSetChanged();
        }

        super.onDestroy();
    }

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
            holder.key.setText(keyVal);
        }

        @Override
        public int getItemCount() {
            return keyList.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView key;
        MyViewHolder(View view) {
            super(view);
            key = view.findViewById(R.id.keyValue);

            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    new AlertDialog.Builder(NoteEditorActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Delete")
                            .setMessage("Are you sure you want to delete this note?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    int position = getAdapterPosition();
                                    keyList.remove(position);
                                    MainActivity.notesList.get(noteID).getKeys().remove(position);
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