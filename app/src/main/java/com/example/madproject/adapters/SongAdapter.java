package com.example.madproject.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.database.MoodOperations;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.TimeFormatter;

import java.util.ArrayList;

/**
 * RecyclerView Adapter for displaying songs in a list.
 * Supports search filtering by title, artist, album and mood.
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> implements Filterable {

    private final Context context;
    private ArrayList<SongsList> songsList;
    private ArrayList<SongsList> songsListFull; // Complete list for filtering
    private MoodOperations moodOperations;
    private java.util.concurrent.ConcurrentHashMap<String, String> moodCache; // Thread-safe mood cache
    private java.util.concurrent.ExecutorService moodLoaderExecutor; // Sequential mood loading
    private final OnSongClickListener listener;

    /**
     * Interface for song item click events.
     */
    public interface OnSongClickListener {
        void onSongClick(ArrayList<SongsList> songs, int position);
    }

    public SongAdapter(Context context, ArrayList<SongsList> songsList, OnSongClickListener listener) {
        this.context = context;
        this.songsList = songsList;
        this.songsListFull = new ArrayList<>(songsList);
        this.listener = listener;
        // Initialize MoodOperations lazily to prevent startup crashes
        this.moodOperations = null;
        // Initialize thread-safe mood cache for performance
        this.moodCache = new java.util.concurrent.ConcurrentHashMap<>();
        // Initialize sequential executor for mood loading
        this.moodLoaderExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
        // Preload mood tags
        preloadMoodTags();
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        SongsList song = songsList.get(position);

        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvDuration.setText(TimeFormatter.formatTime(song.getDuration()));

        // Load album art
        try {
            Uri albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), song.getAlbumId());
            holder.ivAlbumArt.setImageURI(albumArtUri);
            if (holder.ivAlbumArt.getDrawable() == null) {
                holder.ivAlbumArt.setImageResource(R.drawable.ic_music_note);
            }
        } catch (Exception e) {
            holder.ivAlbumArt.setImageResource(R.drawable.ic_music_note);
        }

        // Click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(songsList, holder.getAdapterPosition());
            }
        });

        // Play button click
        holder.btnPlay.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSongClick(songsList, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return songsList != null ? songsList.size() : 0;
    }

    /**
     * Updates the adapter data.
     */
    public void updateData(ArrayList<SongsList> newSongs) {
        this.songsList = newSongs;
        this.songsListFull = new ArrayList<>(newSongs);
        // Refresh mood cache when data updates
        preloadMoodTags();
        notifyDataSetChanged();
    }
    
    /**
     * Preloads mood tags for all songs to improve filtering performance
     */
    private void preloadMoodTags() {
        if (moodLoaderExecutor == null) return;
        
        // Clear cache on main thread
        synchronized (this) {
            if (moodCache != null) {
                moodCache.clear();
            }
        }
        
        // Copy fields to locals for thread safety
        final MoodOperations localMoodOps;
        final java.util.concurrent.ConcurrentHashMap<String, String> localMoodCache;
        final java.util.List<SongsList> localSongs;
        
        synchronized (this) {
            if (moodOperations == null) {
                try {
                    moodOperations = new MoodOperations(context);
                } catch (Exception e) {
                    return; // Skip mood caching if MoodOperations fails
                }
            }
            localMoodOps = moodOperations;
            localMoodCache = moodCache;
            localSongs = new ArrayList<>(songsListFull);
        }
        
        // Load mood tags using sequential executor
        moodLoaderExecutor.submit(() -> {
            try {
                if (localMoodOps == null || localMoodCache == null || localSongs == null) {
                    return;
                }
                
                for (SongsList song : localSongs) {
                    if (song != null && song.getPath() != null) {
                        String mood = localMoodOps.getMoodTag(song.getPath());
                        if (mood != null) {
                            localMoodCache.put(song.getPath(), mood);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore errors in mood loading
            }
        });
    }

    // ======================== Search Filter ========================

    @Override
    public Filter getFilter() {
        return songFilter;
    }

    private final Filter songFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            ArrayList<SongsList> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(songsListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (SongsList song : songsListFull) {
                    // Filter by title, artist, album, or mood with null-safe checks
                    String title = java.util.Objects.toString(song.getTitle(), "");
                    String artist = java.util.Objects.toString(song.getArtist(), "");
                    String album = java.util.Objects.toString(song.getAlbum(), "");
                    
                    boolean matchesTitle = title.toLowerCase().contains(filterPattern);
                    boolean matchesArtist = artist.toLowerCase().contains(filterPattern);
                    boolean matchesAlbum = album.toLowerCase().contains(filterPattern);
                    
                    // Get mood from cache instead of database query with null safety
                    String songMood = null;
                    final java.util.concurrent.ConcurrentHashMap<String, String> localMoodCache = moodCache;
                    if (song.getPath() != null && localMoodCache != null) {
                        songMood = localMoodCache.get(song.getPath());
                    }
                    boolean matchesMood = songMood != null && 
                                         songMood.toLowerCase().contains(filterPattern);
                    
                    if (matchesTitle || matchesArtist || matchesAlbum || matchesMood) {
                        filteredList.add(song);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            songsList = (ArrayList<SongsList>) results.values;
            notifyDataSetChanged();
        }
    };
    
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Cleanup MoodOperations and cache
        if (moodOperations != null) {
            // MoodOperations doesn't have close() method, but we can clear references
            moodOperations = null;
        }
        if (moodCache != null) {
            moodCache.clear();
            moodCache = null;
        }
        // Shutdown mood loading executor
        if (moodLoaderExecutor != null) {
            moodLoaderExecutor.shutdownNow();
            moodLoaderExecutor = null;
        }
    }

    // ======================== ViewHolder ========================

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumArt;
        TextView tvTitle;
        TextView tvArtist;
        TextView tvDuration;
        ImageButton btnPlay;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
            tvTitle = itemView.findViewById(R.id.tv_song_title);
            tvArtist = itemView.findViewById(R.id.tv_song_artist);
            tvDuration = itemView.findViewById(R.id.tv_song_duration);
            btnPlay = itemView.findViewById(R.id.btn_play_song);
        }
    }
}
