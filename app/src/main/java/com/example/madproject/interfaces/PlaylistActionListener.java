package com.example.madproject.interfaces;

import com.example.madproject.models.Playlist;
import com.example.madproject.models.SongsList;

import java.util.ArrayList;

/**
 * Interface for playlist-related actions from fragments.
 */
public interface PlaylistActionListener {

    /**
     * Called when a playlist is selected for playback.
     * @param playlist The selected playlist
     */
    void onPlaylistSelected(Playlist playlist);

    /**
     * Called when a playlist is deleted.
     * @param playlistId The ID of the playlist to delete
     */
    void onPlaylistDeleted(int playlistId);

    /**
     * Returns the full list of songs on the device (for song picker).
     */
    ArrayList<SongsList> getAllDeviceSongs();
}
