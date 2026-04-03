package com.example.madproject.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper for the favorites database.
 * Manages creation and upgrades of the favorites table.
 */
public class FavoritesDBHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "favorites.db";
    private static final int DATABASE_VERSION = 1;

    // Table and column names
    public static final String TABLE_FAVORITES = "favorites";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SONG_TITLE = "song_title";
    public static final String COLUMN_SONG_PATH = "song_path";
    public static final String COLUMN_ARTIST = "artist";

    // Create table SQL
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_FAVORITES + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_SONG_TITLE + " TEXT NOT NULL, "
            + COLUMN_SONG_PATH + " TEXT NOT NULL UNIQUE, "
            + COLUMN_ARTIST + " TEXT"
            + ");";

    public FavoritesDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FAVORITES);
        onCreate(db);
    }
}
