package com.example.madproject.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.SongsList;

import java.util.ArrayList;
import java.util.Collections;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    private final Context context;
    private ArrayList<SongsList> queue;
    private final OnQueueActionListener listener;
    private ItemTouchHelper touchHelper;

    public interface OnQueueActionListener {
        void onQueueItemRemoved(int position);

        void onQueueReordered(ArrayList<SongsList> newQueue);

        void onQueueItemClick(int position);
    }

    public QueueAdapter(Context context, ArrayList<SongsList> queue, OnQueueActionListener listener) {
        this.context = context;
        this.queue = queue;
        this.listener = listener;
    }

    public void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_queue, parent, false);
        return new QueueViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        SongsList song = queue.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());

        holder.btnRemove.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                queue.remove(pos);
                notifyItemRemoved(pos);
                if (listener != null)
                    listener.onQueueItemRemoved(pos);
            }
        });

        holder.ivDragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && touchHelper != null) {
                touchHelper.startDrag(holder);
            }
            return false;
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onQueueItemClick(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return queue != null ? queue.size() : 0;
    }

    public void onItemMove(int from, int to) {
        Collections.swap(queue, from, to);
        notifyItemMoved(from, to);
        if (listener != null)
            listener.onQueueReordered(queue);
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDragHandle;
        TextView tvTitle, tvArtist;
        ImageButton btnRemove;

        QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDragHandle = itemView.findViewById(R.id.iv_drag_handle);
            tvTitle = itemView.findViewById(R.id.tv_queue_song_title);
            tvArtist = itemView.findViewById(R.id.tv_queue_song_artist);
            btnRemove = itemView.findViewById(R.id.btn_remove_queue);
        }
    }
}
