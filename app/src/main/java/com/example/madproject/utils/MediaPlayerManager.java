package com.example.madproject.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

/**
 * Singleton helper class to manage MediaPlayer lifecycle.
 * Handles prepare, start, pause, release, and seekTo operations.
 */
public class MediaPlayerManager {

    private static final String TAG = "MediaPlayerManager";
    private static MediaPlayerManager instance;
    private MediaPlayer mediaPlayer;
    private boolean isPrepared = false;

    private MediaPlayerManager() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized MediaPlayerManager getInstance() {
        if (instance == null) {
            instance = new MediaPlayerManager();
        }
        return instance;
    }

    /**
     * Prepares and plays a song from the given path.
     * 
     * @param context The context
     * @param path    The file path of the audio
     */
    public void playSong(Context context, String path) {
        try {
            // Release existing player if any
            releasePlayer();

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(context, Uri.parse(path));
            mediaPlayer.prepare();
            isPrepared = true;
            mediaPlayer.start();

            Log.d(TAG, "Playing: " + path);

        } catch (IOException e) {
            Log.e(TAG, "Error playing song: " + e.getMessage());
            isPrepared = false;
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalState: " + e.getMessage());
            isPrepared = false;
        }
    }

    /**
     * Toggles play/pause state.
     */
    public void togglePlayPause() {
        if (mediaPlayer != null && isPrepared) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }
        }
    }

    /**
     * Pauses playback.
     */
    public void pause() {
        if (mediaPlayer != null && isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    /**
     * Resumes playback.
     */
    public void resume() {
        if (mediaPlayer != null && isPrepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    /**
     * Seeks to the specified position.
     * 
     * @param position Position in milliseconds
     */
    public void seekTo(int position) {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(position);
        }
    }

    /**
     * Checks if the player is currently playing.
     */
    public boolean isPlaying() {
        try {
            return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Gets the current playback position.
     */
    public int getCurrentPosition() {
        try {
            if (mediaPlayer != null && isPrepared) {
                return mediaPlayer.getCurrentPosition();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting position: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Gets the total duration of the current track.
     */
    public int getDuration() {
        try {
            if (mediaPlayer != null && isPrepared) {
                return mediaPlayer.getDuration();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting duration: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Sets looping mode.
     */
    public void setLooping(boolean looping) {
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(looping);
        }
    }

    /**
     * Sets a completion listener.
     */
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(listener);
        }
    }

    /**
     * Checks if the player is prepared and ready.
     */
    public boolean isPrepared() {
        return isPrepared;
    }

    /**
     * Releases the MediaPlayer resources.
     * Must be called when playback is no longer needed.
     */
    public void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping: " + e.getMessage());
            }
            mediaPlayer.release();
            mediaPlayer = null;
            isPrepared = false;
        }
    }
}
