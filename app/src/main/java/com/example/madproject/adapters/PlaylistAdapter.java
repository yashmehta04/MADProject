package com.example.madproject.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.Playlist;

import java.util.ArrayList;

/**
 * RecyclerView Adapter for displaying playlists.
 */
public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder> {

    private final Context context;
    private ArrayList<Playlist> playlists;
    private final OnPlaylistClickListener listener;

    /**
     * Interface for playlist click events.
     */
    public interface OnPlaylistClickListener {
        void onPlaylistClick(Playlist playlist);

        void onPlaylistDelete(Playlist playlist, int position);
    }

    public PlaylistAdapter(Context context, ArrayList<Playlist> playlists, OnPlaylistClickListener listener) {
        this.context = context;
        this.playlists = playlists;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PlaylistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false);
        return new PlaylistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaylistViewHolder holder, int position) {
        Playlist playlist = playlists.get(position);

        holder.tvPlaylistName.setText(playlist.getName());

        int songCount = playlist.getSongCount();
        String countText = songCount + (songCount == 1 ? " song" : " songs");
        holder.tvSongCount.setText(countText);

        // Click to open/play playlist
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistClick(playlist);
            }
        });

        // Delete button
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPlaylistDelete(playlist, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return playlists != null ? playlists.size() : 0;
    }

    /**
     * Updates the adapter data.
     */
    public void updateData(ArrayList<Playlist> newPlaylists) {
        this.playlists = newPlaylists;
        notifyDataSetChanged();
    }

    // ======================== ViewHolder ========================

    static class PlaylistViewHolder extends RecyclerView.ViewHolder {
        TextView tvPlaylistName;
        TextView tvSongCount;
        ImageButton btnDelete;

        PlaylistViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPlaylistName = itemView.findViewById(R.id.tv_playlist_name);
            tvSongCount = itemView.findViewById(R.id.tv_song_count);
            btnDelete = itemView.findViewById(R.id.btn_delete_playlist);
        }
    }
}
