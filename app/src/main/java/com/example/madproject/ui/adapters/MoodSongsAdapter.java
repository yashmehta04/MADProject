package com.example.madproject.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.SongsList;

import java.util.List;

/**
 * Glassmorphism adapter for mood songs with smooth animations
 */
public class MoodSongsAdapter extends RecyclerView.Adapter<MoodSongsAdapter.MoodSongViewHolder> {

    private List<SongsList> moodSongs;
    private OnSongClickListener listener;

    public interface OnSongClickListener {
        void onSongClicked(SongsList song);
    }

    public MoodSongsAdapter(List<SongsList> moodSongs, OnSongClickListener listener) {
        this.moodSongs = moodSongs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MoodSongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song_glass, parent, false);
        return new MoodSongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodSongViewHolder holder, int position) {
        SongsList song = moodSongs.get(position);
        
        holder.tvSongTitle.setText(song.getTitle());
        holder.tvSongArtist.setText(song.getArtist());
        
        // Set click listener with glassmorphism feedback
        holder.itemView.setOnClickListener(v -> {
            // Add scale animation
            holder.itemView.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        holder.itemView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
            
            if (listener != null) {
                listener.onSongClicked(song);
            }
        });
        
        // Animate entrance
        animateItemEntrance(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return moodSongs.size();
    }

    /**
     * Animate item entrance with staggered effect
     */
    private void animateItemEntrance(View itemView, int position) {
        itemView.setTranslationY(50f);
        itemView.setAlpha(0f);
        
        itemView.postDelayed(() -> {
            itemView.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }, position * 50);
    }

    static class MoodSongViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbumArt;
        TextView tvSongTitle;
        TextView tvSongArtist;
        TextView tvMoodTag;
        ImageView btnPlay;

        MoodSongViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAlbumArt = itemView.findViewById(R.id.iv_album_art);
            tvSongTitle = itemView.findViewById(R.id.tv_song_title);
            tvSongArtist = itemView.findViewById(R.id.tv_song_artist);
            tvMoodTag = itemView.findViewById(R.id.tv_mood_tag);
            btnPlay = itemView.findViewById(R.id.btn_play);
        }
    }
}
