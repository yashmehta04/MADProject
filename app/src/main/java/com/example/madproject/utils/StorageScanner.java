package com.example.madproject.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.example.madproject.models.SongsList;

import java.util.ArrayList;

/**
 * Utility class to scan device storage for audio files
 * using ContentResolver and MediaStore.
 */
public final class StorageScanner {

    private StorageScanner() {
        // Private constructor
    }

    /**
     * Scans the device for audio files and returns a list of SongsList objects.
     * 
     * @param context The application context
     * @return ArrayList of SongsList containing all found audio files
     */
    public static ArrayList<SongsList> scanSongs(Context context) {
        ArrayList<SongsList> songsList = new ArrayList<>();
        java.util.HashSet<String> uniqueKeys = new java.util.HashSet<>();

        // Content URI for external audio files
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        // Columns to retrieve
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };

        // Filter: only music files with duration > 10 seconds
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
                + MediaStore.Audio.Media.DURATION + " > 10000";

        // Sort by title ascending
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, selection, null, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            // Get column indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            do {
                long id = cursor.getLong(idColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                String path = cursor.getString(pathColumn);
                long duration = cursor.getLong(durationColumn);
                String album = cursor.getString(albumColumn);
                long albumId = cursor.getLong(albumIdColumn);

                // Handle unknown artist
                if (artist == null || artist.equals("<unknown>")) {
                    artist = "Unknown Artist";
                }

                // Handle unknown album
                if (album == null || album.equals("<unknown>")) {
                    album = "Unknown Album";
                }

                // Duplicate Detection Signature
                String duplicateKey = (title + "_" + artist + "_" + duration).toLowerCase();

                if (!uniqueKeys.contains(duplicateKey)) {
                    uniqueKeys.add(duplicateKey);
                    SongsList song = new SongsList(id, title, artist, path, duration, album, albumId);
                    songsList.add(song);
                }

            } while (cursor.moveToNext());

            cursor.close();
        }

        return songsList;
    }

    /**
     * Gets the album art URI for a given album ID.
     * 
     * @param albumId The album ID
     * @return URI pointing to the album art
     */
    public static Uri getAlbumArtUri(long albumId) {
        return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), albumId);
    }
}
