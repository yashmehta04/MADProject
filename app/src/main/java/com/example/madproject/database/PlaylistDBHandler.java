package com.example.madproject.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * SQLiteOpenHelper for the playlists database.
 * Manages playlists and playlist_songs tables with foreign key cascade.
 */
public class PlaylistDBHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "playlists.db";
    private static final int DATABASE_VERSION = 2;

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
    public static final String COLUMN_SONG_ARTIST = "song_artist";
    public static final String COLUMN_SONG_ALBUM = "song_album";
    public static final String COLUMN_SONG_DURATION = "song_duration";
    public static final String COLUMN_SONG_ALBUM_ID = "song_album_id";

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
            + COLUMN_SONG_ARTIST + " TEXT, "
            + COLUMN_SONG_ALBUM + " TEXT, "
            + COLUMN_SONG_DURATION + " INTEGER, "
            + COLUMN_SONG_ALBUM_ID + " INTEGER, "
            + "FOREIGN KEY (" + COLUMN_FK_PLAYLIST_ID + ") REFERENCES "
            + TABLE_PLAYLISTS + "(" + COLUMN_PLAYLIST_ID + ") ON DELETE CASCADE"
            + ");";

    public PlaylistDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("PlaylistDBHandler", "Creating playlist database tables");
        db.execSQL(CREATE_PLAYLISTS_TABLE);
        db.execSQL(CREATE_PLAYLIST_SONGS_TABLE);
        Log.d("PlaylistDBHandler", "Database tables created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d("PlaylistDBHandler", "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        // Non-destructive migration - preserve existing data
        if (oldVersion < 2) {
            // Add new columns for version 2
            try {
                db.execSQL("ALTER TABLE " + TABLE_PLAYLISTS + " ADD COLUMN description TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_PLAYLISTS + " ADD COLUMN cover_image TEXT DEFAULT ''");
                db.execSQL("ALTER TABLE " + TABLE_PLAYLIST_SONGS + " ADD COLUMN date_added INTEGER DEFAULT 0");
                Log.d("PlaylistDBHandler", "Added version 2 columns successfully");
            } catch (Exception e) {
                Log.e("PlaylistDBHandler", "Error adding version 2 columns", e);
            }
        }
        
        if (oldVersion < 3) {
            // Add new columns for version 3
            try {
                db.execSQL("ALTER TABLE " + TABLE_PLAYLISTS + " ADD COLUMN is_favorite INTEGER DEFAULT 0");
                db.execSQL("ALTER TABLE " + TABLE_PLAYLIST_SONGS + " ADD COLUMN play_count INTEGER DEFAULT 0");
                Log.d("PlaylistDBHandler", "Added version 3 columns successfully");
            } catch (Exception e) {
                Log.e("PlaylistDBHandler", "Error adding version 3 columns", e);
            }
        }
        
        // Continue with incremental version upgrades as needed
        // Future versions: if (oldVersion < 4) { ... }
        
        Log.d("PlaylistDBHandler", "Database upgrade completed without data loss");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign key support
        db.execSQL("PRAGMA foreign_keys = ON;");
    }
}
