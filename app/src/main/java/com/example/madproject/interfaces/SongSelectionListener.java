package com.example.madproject.interfaces;

import com.example.madproject.models.SongsList;

import java.util.ArrayList;

/**
 * Interface for Fragment → Activity communication.
 * Fragments call these methods to control playback via MainActivity.
 */
public interface SongSelectionListener {

    /**
     * Called when a song is selected for playback.
     * 
     * @param songsList The list of songs (queue context)
     * @param position  The position of the selected song
     */
    void onSongSelected(ArrayList<SongsList> songsList, int position);

    /**
     * Called when play/pause is toggled.
     */
    void onPlayPauseToggle();

    /**
     * Called to skip to the next song.
     */
    void onNextSong();

    /**
     * Called to go to the previous song.
     */
    void onPreviousSong();

    /**
     * Called to seek to a specific position.
     * 
     * @param position The position in milliseconds
     */
    void onSeekTo(int position);

    /**
     * Called to toggle shuffle mode.
     */
    void onShuffleToggle();

    /**
     * Called to toggle repeat mode.
     */
    void onRepeatToggle();

    /**
     * Check if the player is currently playing.
     * 
     * @return true if playing
     */
    boolean isPlaying();

    /**
     * Get the current playback position.
     * 
     * @return current position in ms
     */
    int getCurrentPosition();

    /**
     * Get the total duration of current song.
     * 
     * @return duration in ms
     */
    int getDuration();

    /**
     * Check if shuffle mode is on.
     */
    boolean isShuffleOn();

    /**
     * Check if repeat mode is on.
     */
    boolean isRepeatOn();

    /**
     * Get the currently playing song.
     */
    SongsList getCurrentSong();

    /**
     * Add a target song to the current playback queue.
     */
    void onAddToQueue(SongsList song);
}
