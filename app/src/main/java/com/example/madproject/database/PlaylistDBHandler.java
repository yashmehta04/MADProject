package com.example.madproject.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper for the playlists database.
 * Manages playlists and playlist_songs tables with foreign key cascade.
 */
public class PlaylistDBHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "playlists.db";
    private static final int DATABASE_VERSION = 1;

    // Playlists table
    public static final String TABLE_PLAYLISTS = "playlists";
    public static final String COLUMN_PLAYLIST_ID = "id";
    public static final String COLUMN_PLAYLIST_NAME = "playlist_name";

    // Playlist songs table
    public static final String TABLE_PLAYLIST_SONGS = "playlist_songs";
    public static final String COLUMN_SONG_ID = "id";
    public static final String COLUMN_FK_PLAYLIST_ID = "playlist_id";
    public static final String COLUMN_SONG_PATH = "song_path";
    public static final String COLUMN_SONG_TITLE = "song_title";

    // Create playlists table SQL
    private static final String CREATE_PLAYLISTS_TABLE = "CREATE TABLE " + TABLE_PLAYLISTS + " ("
            + COLUMN_PLAYLIST_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_PLAYLIST_NAME + " TEXT NOT NULL UNIQUE"
            + ");";

    // Create playlist songs table SQL with foreign key
    private static final String CREATE_PLAYLIST_SONGS_TABLE = "CREATE TABLE " + TABLE_PLAYLIST_SONGS + " ("
            + COLUMN_SONG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_FK_PLAYLIST_ID + " INTEGER NOT NULL, "
            + COLUMN_SONG_PATH + " TEXT NOT NULL, "
            + COLUMN_SONG_TITLE + " TEXT, "
            + "FOREIGN KEY (" + COLUMN_FK_PLAYLIST_ID + ") REFERENCES "
            + TABLE_PLAYLISTS + "(" + COLUMN_PLAYLIST_ID + ") ON DELETE CASCADE"
            + ");";

    public PlaylistDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_PLAYLISTS_TABLE);
        db.execSQL(CREATE_PLAYLIST_SONGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign key support
        db.execSQL("PRAGMA foreign_keys = ON;");
    }
}
