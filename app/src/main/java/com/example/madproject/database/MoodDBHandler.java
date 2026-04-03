package com.example.madproject.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLiteOpenHelper for the mood tags database.
 * Maps song_id to a mood_tag (HAPPY, SAD, CALM, ENERGETIC).
 */
public class MoodDBHandler extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "mood_tags.db";
    private static final int DATABASE_VERSION = 2; // Incremented for new columns

    // Mood tags table
    public static final String TABLE_MOOD_TAGS = "mood_tags";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_SONG_PATH = "song_path";
    public static final String COLUMN_MOOD_TAG = "mood_tag";
    public static final String COLUMN_CONFIDENCE_SCORE = "confidence_score";
    public static final String COLUMN_CULTURAL_CONTEXT = "cultural_context";
    public static final String COLUMN_AUDIO_FEATURES = "audio_features";
    public static final String COLUMN_ANALYSIS_TIMESTAMP = "analysis_timestamp";

    // Create mood tags table SQL
    private static final String CREATE_MOOD_TAGS_TABLE = "CREATE TABLE " + TABLE_MOOD_TAGS + " ("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_SONG_PATH + " TEXT NOT NULL UNIQUE, "
            + COLUMN_MOOD_TAG + " TEXT NOT NULL, "
            + COLUMN_CONFIDENCE_SCORE + " REAL DEFAULT 0.0, "
            + COLUMN_CULTURAL_CONTEXT + " TEXT DEFAULT 'UNIVERSAL', "
            + COLUMN_AUDIO_FEATURES + " TEXT, "
            + COLUMN_ANALYSIS_TIMESTAMP + " INTEGER DEFAULT 0"
            + ");";

    public MoodDBHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MOOD_TAGS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add new columns for version 2
            db.execSQL("ALTER TABLE " + TABLE_MOOD_TAGS + " ADD COLUMN " + COLUMN_CONFIDENCE_SCORE + " REAL DEFAULT 0.0");
            db.execSQL("ALTER TABLE " + TABLE_MOOD_TAGS + " ADD COLUMN " + COLUMN_CULTURAL_CONTEXT + " TEXT DEFAULT 'UNIVERSAL'");
            db.execSQL("ALTER TABLE " + TABLE_MOOD_TAGS + " ADD COLUMN " + COLUMN_AUDIO_FEATURES + " TEXT");
            db.execSQL("ALTER TABLE " + TABLE_MOOD_TAGS + " ADD COLUMN " + COLUMN_ANALYSIS_TIMESTAMP + " INTEGER DEFAULT 0");
        } else {
            // For future versions, drop and recreate
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOOD_TAGS);
            onCreate(db);
        }
    }
}
