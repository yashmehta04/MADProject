package com.example.madproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.madproject.models.SongsList;

import java.util.ArrayList;

/**
 * Operations class for the favorites database.
 * Provides CRUD operations for favorite songs.
 */
public class FavoritesOperations {

    private final FavoritesDBHandler dbHandler;

    public FavoritesOperations(Context context) {
        dbHandler = new FavoritesDBHandler(context);
    }

    /**
     * Adds a song to favorites.
     * 
     * @param song The song to add
     * @return true if added successfully
     */
    public boolean addFavorite(SongsList song) {
        if (isFavorite(song.getPath())) {
            return false; // Already exists
        }

        SQLiteDatabase db = dbHandler.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(FavoritesDBHandler.COLUMN_SONG_TITLE, song.getTitle());
        values.put(FavoritesDBHandler.COLUMN_SONG_PATH, song.getPath());
        values.put(FavoritesDBHandler.COLUMN_ARTIST, song.getArtist());

        long result = db.insert(FavoritesDBHandler.TABLE_FAVORITES, null, values);
        db.close();
        return result != -1;
    }

    /**
     * Removes a song from favorites by path.
     * 
     * @param path The file path of the song
     * @return true if removed successfully
     */
    public boolean removeFavorite(String path) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        int result = db.delete(FavoritesDBHandler.TABLE_FAVORITES,
                FavoritesDBHandler.COLUMN_SONG_PATH + " = ?",
                new String[] { path });
        db.close();
        return result > 0;
    }

    /**
     * Checks if a song is in favorites.
     * 
     * @param path The file path of the song
     * @return true if the song is a favorite
     */
    public boolean isFavorite(String path) {
        boolean exists = false;
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        try (Cursor cursor = db.query(FavoritesDBHandler.TABLE_FAVORITES,
                new String[]{FavoritesDBHandler.COLUMN_ID},
                FavoritesDBHandler.COLUMN_SONG_PATH + " = ?",
                new String[]{path},
                null, null, null)) {

            exists = cursor != null && cursor.getCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
        return exists;
    }

    /**
     * Gets all favorite songs.
     * 
     * @return ArrayList of SongsList containing favorite songs
     */
    public ArrayList<SongsList> getAllFavorites() {
        ArrayList<SongsList> favorites = new ArrayList<>();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        try (Cursor cursor = db.query(FavoritesDBHandler.TABLE_FAVORITES,
                null, null, null, null, null,
                FavoritesDBHandler.COLUMN_SONG_TITLE + " ASC")) {

            if (cursor != null && cursor.moveToFirst()) {
                int titleIndex = cursor.getColumnIndexOrThrow(FavoritesDBHandler.COLUMN_SONG_TITLE);
                int pathIndex = cursor.getColumnIndexOrThrow(FavoritesDBHandler.COLUMN_SONG_PATH);
                int artistIndex = cursor.getColumnIndexOrThrow(FavoritesDBHandler.COLUMN_ARTIST);

                do {
                    SongsList song = new SongsList();
                    song.setTitle(cursor.getString(titleIndex));
                    song.setPath(cursor.getString(pathIndex));
                    song.setArtist(cursor.getString(artistIndex));
                    favorites.add(song);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.close();
        }
        return favorites;
    }

    /**
     * Clears all favorites.
     */
    public void clearAllFavorites() {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(FavoritesDBHandler.TABLE_FAVORITES, null, null);
        db.close();
    }
}
