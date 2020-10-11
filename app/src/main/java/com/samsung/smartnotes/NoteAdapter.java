package com.samsung.smartnotes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private ArrayList<String> noteTextList;
    private onItemClickListener mOnItemClickListener;
    private onItemLongClickListener mOnItemLongClickListener;

    public NoteAdapter(ArrayList<String> noteTextList) {
        this.noteTextList = noteTextList;
    }

    public interface onItemClickListener {
        public void onItemClick(View v, int position);
    }

    public interface onItemLongClickListener {
        public boolean onItemLongClick(View v, int position);
    }

    public void setmOnItemClickListener(onItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    public void setmOnItemLongClickListener(onItemLongClickListener monItemLongClickListener) {
        this.mOnItemLongClickListener = monItemLongClickListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.main_notes_list, parent, false);
        return new NoteViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, final int position) {
        holder.textView.setText(noteTextList.get(position));

        holder.textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickListener.onItemClick(v, position);
            }
        });

        holder.textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return mOnItemLongClickListener.onItemLongClick(v, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return noteTextList.size();
    }


    class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        NoteViewHolder(View view){
            super(view);
            textView = view.findViewById(R.id.noteTextView);
        }
    }
}
