package com.example.madproject.utils;

import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import com.example.madproject.services.PlaybackService;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutionException;

public class ExoPlayerManager {
    private static volatile ExoPlayerManager instance;
    private MediaController mediaController;
    private boolean isPrepared = false;
    private Player.Listener playerListener;

    private ExoPlayerManager() {
    }

    public static ExoPlayerManager getInstance() {
        if (instance == null) {
            synchronized (ExoPlayerManager.class) {
                if (instance == null) {
                    instance = new ExoPlayerManager();
                }
            }
        }
        return instance;
    }

    public void initialize(Context context, Runnable onConnected) {
        if (mediaController != null) {
            if (onConnected != null)
                onConnected.run();
            return;
        }

        SessionToken sessionToken = new SessionToken(context, new ComponentName(context, PlaybackService.class));
        ListenableFuture<MediaController> controllerFuture = new MediaController.Builder(context, sessionToken)
                .buildAsync();

        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                if (playerListener != null) {
                    mediaController.addListener(playerListener);
                }
                if (onConnected != null) {
                    onConnected.run();
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, MoreExecutors.directExecutor());
    }

    public void playSong(Context context, String path, String title, String artist, String album,
            String artworkUriStr) {
        if (mediaController == null)
            return;

        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(android.net.Uri.parse(artworkUriStr))
                .build();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(path))
                .setMediaMetadata(metadata)
                .build();

        mediaController.setMediaItem(mediaItem);
        mediaController.prepare();
        mediaController.play();
        isPrepared = true;
    }

    public void togglePlayPause() {
        if (mediaController != null) {
            if (mediaController.isPlaying())
                mediaController.pause();
            else
                mediaController.play();
        }
    }

    public void pause() {
        if (mediaController != null && mediaController.isPlaying()) {
            mediaController.pause();
        }
    }

    public void resume() {
        if (mediaController != null && !mediaController.isPlaying()) {
            mediaController.play();
        }
    }

    public void seekTo(int position) {
        if (mediaController != null) {
            mediaController.seekTo(position);
        }
    }

    public boolean isPlaying() {
        return mediaController != null && mediaController.isPlaying();
    }

    public int getCurrentPosition() {
        return mediaController != null ? (int) mediaController.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return mediaController != null ? (int) mediaController.getDuration() : 0;
    }

    public void setLooping(boolean looping) {
        if (mediaController != null) {
            mediaController.setRepeatMode(looping ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        }
    }

    public void setPlayerListener(Player.Listener listener) {
        this.playerListener = listener;
        if (mediaController != null) {
            mediaController.addListener(listener);
        }
    }

    public void releasePlayer() {
        // Handled by Service lifecycle
    }
}
