package com.example.madproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.madproject.models.Playlist;
import com.example.madproject.models.SongsList;

import java.util.ArrayList;

/**
 * Operations class for the playlists database.
 * Provides CRUD operations for playlists and playlist songs.
 */
public class PlaylistOperations {
    private static final String TAG = "PlaylistOperations";

    private final PlaylistDBHandler dbHandler;

    public PlaylistOperations(Context context) {
        dbHandler = new PlaylistDBHandler(context);
    }

    // ======================== Playlist CRUD ========================

    /**
     * Creates a new playlist.
     * 
     * @param name Playlist name
     * @return The new playlist ID, or -1 if failed
     */
    public long createPlaylist(String name) {
        // Check if playlist with same name already exists
        if (getPlaylistByName(name) != null) {
            Log.w(TAG, "Playlist with name '" + name + "' already exists");
            return -1;
        }
        
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PlaylistDBHandler.COLUMN_PLAYLIST_NAME, name);

        long id = db.insert(PlaylistDBHandler.TABLE_PLAYLISTS, null, values);
        db.close();
        return id;
    }

    /**
     * Creates a playlist and adds songs to it in one operation.
     */
    public long createPlaylistWithSongs(String name, ArrayList<SongsList> songs) {
        long playlistId = createPlaylist(name);
        if (playlistId != -1) {
            for (SongsList song : songs) {
                addSongToPlaylist((int) playlistId, song);
            }
        }
        return playlistId;
    }

    /**
     * Gets all playlists.
     * 
     * @return ArrayList of Playlist objects
     */
    public ArrayList<Playlist> getAllPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<>();
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        try (Cursor cursor = db.query(PlaylistDBHandler.TABLE_PLAYLISTS,
                null, null, null, null, null,
                PlaylistDBHandler.COLUMN_PLAYLIST_NAME + " ASC")) {

            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_PLAYLIST_ID);
                int nameIndex = cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_PLAYLIST_NAME);

                do {
                    int id = cursor.getInt(idIndex);
                    String name = cursor.getString(nameIndex);
                    Playlist playlist = new Playlist(id, name);

                    // Load songs for this playlist
                    playlist.setSongs(getPlaylistSongs(db, id));
                    playlists.add(playlist);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching playlists", e);
        } finally {
            db.close();
        }
        return playlists;
    }

    /**
     * Gets a playlist by name.
     * 
     * @param name Playlist name
     * @return Playlist object or null if not found
     */
    public Playlist getPlaylistByName(String name) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Playlist playlist = null;
        
        try (Cursor cursor = db.query(PlaylistDBHandler.TABLE_PLAYLISTS,
                null, 
                PlaylistDBHandler.COLUMN_PLAYLIST_NAME + " = ?",
                new String[] { name },
                null, null, null)) {
            
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_PLAYLIST_ID);
                int nameIndex = cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_PLAYLIST_NAME);
                
                int id = cursor.getInt(idIndex);
                String playlistName = cursor.getString(nameIndex);
                playlist = new Playlist(id, playlistName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching playlist by name", e);
        } finally {
            db.close();
        }
        return playlist;
    }

    /**
     * Deletes a playlist and all its songs (cascade).
     * 
     * @param playlistId The playlist ID
     * @return true if deleted successfully
     */
    public boolean deletePlaylist(int playlistId) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        int result = db.delete(PlaylistDBHandler.TABLE_PLAYLISTS,
                PlaylistDBHandler.COLUMN_PLAYLIST_ID + " = ?",
                new String[] { String.valueOf(playlistId) });
        db.close();
        return result > 0;
    }

    // ======================== Playlist Songs CRUD ========================

    /**
     * Adds a song to a playlist.
     * 
     * @param playlistId The playlist ID
     * @param song       The song to add
     * @return true if added successfully
     */
    public boolean addSongToPlaylist(int playlistId, SongsList song) {
        Log.d(TAG, "Adding song to playlist: " + song.getTitle() + " to playlist ID: " + playlistId);
        
        // Check if song already exists in this playlist
        if (isSongInPlaylist(playlistId, song.getPath())) {
            Log.w(TAG, "Song already exists in playlist: " + song.getTitle());
            return false;
        }

        SQLiteDatabase db = dbHandler.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(PlaylistDBHandler.COLUMN_FK_PLAYLIST_ID, playlistId);
        values.put(PlaylistDBHandler.COLUMN_SONG_PATH, song.getPath());
        values.put(PlaylistDBHandler.COLUMN_SONG_TITLE, song.getTitle());
        values.put(PlaylistDBHandler.COLUMN_SONG_ARTIST, song.getArtist());
        values.put(PlaylistDBHandler.COLUMN_SONG_ALBUM, song.getAlbum());
        values.put(PlaylistDBHandler.COLUMN_SONG_DURATION, song.getDuration());
        values.put(PlaylistDBHandler.COLUMN_SONG_ALBUM_ID, song.getAlbumId());
        
        Log.d(TAG, "Inserting values: " + values.toString());
        
        long result = db.insert(PlaylistDBHandler.TABLE_PLAYLIST_SONGS, null, values);
        db.close();
        
        boolean success = result != -1;
        Log.d(TAG, "Insert result: " + result + ", success: " + success);
        
        return success;
    }

    /**
     * Removes a song from a playlist.
     * 
     * @param playlistId The playlist ID
     * @param songPath   The song file path
     * @return true if removed
     */
    public boolean removeSongFromPlaylist(int playlistId, String songPath) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        int result = db.delete(PlaylistDBHandler.TABLE_PLAYLIST_SONGS,
                PlaylistDBHandler.COLUMN_FK_PLAYLIST_ID + " = ? AND "
                        + PlaylistDBHandler.COLUMN_SONG_PATH + " = ?",
                new String[] { String.valueOf(playlistId), songPath });
        db.close();
        return result > 0;
    }

    /**
     * Checks if a song is already in a playlist.
     */
    public boolean isSongInPlaylist(int playlistId, String songPath) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Cursor cursor = db.query(PlaylistDBHandler.TABLE_PLAYLIST_SONGS,
                new String[] { PlaylistDBHandler.COLUMN_SONG_ID },
                PlaylistDBHandler.COLUMN_FK_PLAYLIST_ID + " = ? AND "
                        + PlaylistDBHandler.COLUMN_SONG_PATH + " = ?",
                new String[] { String.valueOf(playlistId), songPath },
                null, null, null);

        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }
        db.close();
        return exists;
    }

    /**
     * Gets all songs for a specific playlist.
     * 
     * @param db         An open database instance
     * @param playlistId The playlist ID
     * @return ArrayList of SongsList
     */
    private ArrayList<SongsList> getPlaylistSongs(SQLiteDatabase db, int playlistId) {
        ArrayList<SongsList> songs = new ArrayList<>();
        try (Cursor cursor = db.query(PlaylistDBHandler.TABLE_PLAYLIST_SONGS,
                null,
                PlaylistDBHandler.COLUMN_FK_PLAYLIST_ID + " = ?",
                new String[]{String.valueOf(playlistId)},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_SONG_TITLE));
                    String path = cursor.getString(cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_SONG_PATH));
                    String artist = cursor.getString(cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_SONG_ARTIST));
                    String album = cursor.getString(cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_SONG_ALBUM));
                    long duration = cursor.getLong(cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_SONG_DURATION));
                    long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(PlaylistDBHandler.COLUMN_SONG_ALBUM_ID));
                    
                    // Use the 7-parameter constructor: id, title, artist, path, duration, album, albumId
                    // We'll use 0 for id since it's not stored in playlist_songs table
                    songs.add(new SongsList(0, title, artist, path, duration, album, albumId));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching playlist songs", e);
        }
        return songs;
    }

    /**
     * Gets all songs for a playlist (opens its own DB connection).
     * 
     * @param playlistId The playlist ID
     * @return ArrayList of SongsList
     */
    public ArrayList<SongsList> getPlaylistSongsById(int playlistId) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        ArrayList<SongsList> songs = getPlaylistSongs(db, playlistId);
        db.close();
        return songs;
    }
}
