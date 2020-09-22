package com.samsung.smartnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

import java.util.ArrayList;
import java.util.HashSet;

public class MainActivity extends AppCompatActivity {

    static ArrayList<String> notes = new ArrayList<String>();
    static ArrayAdapter<String> arrayAdapter;
    protected ListView listView;

    protected void showToast (String text){
        Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Logic to Retrieve from SharedPrefs
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                , Context.MODE_PRIVATE);
        HashSet<String> hash = (HashSet<String>) sharedPreferences.getStringSet("notes" , null);
        if (hash == null){
            //Add a sample note
            notes.add("This is an Example Note");
        }
        else {
            notes = new ArrayList<>(hash);
        }

        // Bind ListView with Array Adapter
        listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, notes);
        listView.setAdapter(arrayAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Logic to know Receivable Intent from Dummy Camera App
        Intent intent = getIntent();
        String receivable = intent.getStringExtra("com.samsung.smartnotes.MainActivity.objectName");
        if (receivable != null){
            showToast("I just received a " + receivable);
        }

        //Add listener to ListView and send local Intent on position of listView
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), com.samsung.smartnotes.NoteEditorActivity.class);
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
                                notes.remove(position);
                                arrayAdapter.notifyDataSetChanged();

                                //Add Logic to edit stored Data
                                SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("com.samsung.smartnotes.notes"
                                        , Context.MODE_PRIVATE);
                                HashSet<String> set = new HashSet<>(com.samsung.smartnotes.MainActivity.notes);
                                sharedPreferences.edit().putStringSet("notes" , set).apply();
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

}