package com.example.madproject.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.madproject.models.SongsList;
import com.example.madproject.utils.MoodAlgorithm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Operations class for the mood tags database.
 * Provides insert, query-by-mood, and lookup operations.
 */
public class MoodOperations {

    private static final String TAG = "MoodOperations";
    private final MoodDBHandler dbHandler;

    public MoodOperations(Context context) {
        dbHandler = new MoodDBHandler(context);
    }

    /**
     * Inserts or replaces a mood tag with enhanced information.
     *
     * @param songPath       The file path of the song (unique key)
     * @param moodTag        The mood tag (HAPPY, SAD, CALM, ENERGETIC)
     * @param confidence      Confidence score (0.0-1.0)
     * @param culturalContext Cultural context (BOLLYWOOD, WESTERN, UNIVERSAL)
     */
    public void insertMoodTagWithDetails(String songPath, String moodTag, 
                                       double confidence, String culturalContext) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MoodDBHandler.COLUMN_SONG_PATH, songPath);
        values.put(MoodDBHandler.COLUMN_MOOD_TAG, moodTag);
        values.put(MoodDBHandler.COLUMN_CONFIDENCE_SCORE, confidence);
        values.put(MoodDBHandler.COLUMN_CULTURAL_CONTEXT, culturalContext);
        values.put(MoodDBHandler.COLUMN_ANALYSIS_TIMESTAMP, System.currentTimeMillis());
        
        db.insertWithOnConflict(MoodDBHandler.TABLE_MOOD_TAGS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    /**
     * Gets detailed mood information for a specific song.
     *
     * @param songPath The file path of the song
     * @return DetailedMoodInfo object or null if not found
     */
    public DetailedMoodInfo getDetailedMoodInfo(String songPath) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        DetailedMoodInfo info = null;
        
        try (Cursor cursor = db.query(MoodDBHandler.TABLE_MOOD_TAGS,
                new String[]{
                    MoodDBHandler.COLUMN_MOOD_TAG,
                    MoodDBHandler.COLUMN_CONFIDENCE_SCORE,
                    MoodDBHandler.COLUMN_CULTURAL_CONTEXT,
                    MoodDBHandler.COLUMN_ANALYSIS_TIMESTAMP
                },
                MoodDBHandler.COLUMN_SONG_PATH + " = ?",
                new String[]{songPath},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                String mood = cursor.getString(cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_MOOD_TAG));
                double confidence = cursor.getDouble(cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_CONFIDENCE_SCORE));
                String culturalContext = cursor.getString(cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_CULTURAL_CONTEXT));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_ANALYSIS_TIMESTAMP));
                
                info = new DetailedMoodInfo(mood, confidence, culturalContext, timestamp);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying detailed mood info", e);
        } finally {
            db.close();
        }
        return info;
    }

    /**
     * Gets mood tag with confidence score.
     *
     * @param songPath The file path of the song
     * @return MoodWithConfidence object or null if not found
     */
    public MoodWithConfidence getMoodWithConfidence(String songPath) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        MoodWithConfidence info = null;
        try (Cursor cursor = db.query(MoodDBHandler.TABLE_MOOD_TAGS,
                new String[]{
                    MoodDBHandler.COLUMN_MOOD_TAG,
                    MoodDBHandler.COLUMN_CONFIDENCE_SCORE
                },
                MoodDBHandler.COLUMN_SONG_PATH + " = ?",
                new String[]{songPath},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                String mood = cursor.getString(cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_MOOD_TAG));
                double confidence = cursor.getDouble(cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_CONFIDENCE_SCORE));
                
                info = new MoodWithConfidence(mood, confidence);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying mood with confidence", e);
        } finally {
            db.close();
        }
        return info;
    }

    /**
     * Batch inserts mood tags efficiently using a transaction.
     *
     * @param entries List of path-mood pairs
     */
    public void insertMoodTagsBatch(ArrayList<String[]> entries) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.beginTransaction();
        try {
            for (String[] entry : entries) {
                ContentValues values = new ContentValues();
                values.put(MoodDBHandler.COLUMN_SONG_PATH, entry[0]);
                values.put(MoodDBHandler.COLUMN_MOOD_TAG, entry[1]);
                db.insertWithOnConflict(MoodDBHandler.TABLE_MOOD_TAGS, null, values,
                        SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    /**
     * Gets the mood tag for a specific song.
     *
     * @param songPath The file path of the song
     * @return The mood tag, or null if not found
     */
    public String getMoodTag(String songPath) {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        Cursor cursor = db.query(MoodDBHandler.TABLE_MOOD_TAGS,
                new String[]{MoodDBHandler.COLUMN_MOOD_TAG},
                MoodDBHandler.COLUMN_SONG_PATH + " = ?",
                new String[]{songPath},
                null, null, null);

        String mood = null;
        if (cursor != null && cursor.moveToFirst()) {
            mood = cursor.getString(cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_MOOD_TAG));
            cursor.close();
        }
        db.close();
        return mood;
    }

    /**
     * Gets all song paths that already have mood tags (for scan optimization).
     *
     * @return Set of file paths that are already tagged
     */
    public Set<String> getAllTaggedPaths() {
        Set<String> paths = new HashSet<>();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        try (Cursor cursor = db.query(MoodDBHandler.TABLE_MOOD_TAGS,
                new String[]{MoodDBHandler.COLUMN_SONG_PATH},
                null, null, null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                int pathIndex = cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_SONG_PATH);
                do {
                    paths.add(cursor.getString(pathIndex));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying tagged paths", e);
        } finally {
            db.close();
        }
        return paths;
    }

    /**
     * Gets all song paths that match a specific mood.
     *
     * @param mood The mood tag to filter by
     * @return Set of file paths that match the mood
     */
    public Set<String> getPathsByMood(String mood) {
        Set<String> paths = new HashSet<>();
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        try (Cursor cursor = db.query(MoodDBHandler.TABLE_MOOD_TAGS,
                new String[]{MoodDBHandler.COLUMN_SONG_PATH},
                MoodDBHandler.COLUMN_MOOD_TAG + " = ?",
                new String[]{mood},
                null, null, null)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    paths.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying paths for mood", e);
        } finally {
            db.close();
        }
        return paths;
    }

    /**
     * Finds all songs from the provided list that match a given mood tag.
     * Uses randomized order via shuffling.
     *
     * @param allSongs Full list of songs from the library
     * @param moodTag  The mood to filter by (HAPPY, SAD, CALM, ENERGETIC)
     * @param limit    Maximum number of songs to return
     * @return ArrayList of matching SongsList objects
     */
    public ArrayList<SongsList> getSongsByMood(ArrayList<SongsList> allSongs, String moodTag, int limit) {
        // Input validation
        if (allSongs == null || allSongs.isEmpty()) {
            Log.w(TAG, "Empty song list provided");
            return new ArrayList<>();
        }
        
        if (moodTag == null || moodTag.trim().isEmpty()) {
            Log.w(TAG, "Invalid mood tag provided");
            return new ArrayList<>();
        }
        
        // Validate mood tag against allowed values to prevent SQL injection
        String normalizedMoodTag = moodTag.trim().toUpperCase();
        if (!isValidMoodTag(normalizedMoodTag)) {
            Log.w(TAG, "Invalid mood tag: " + moodTag + ", using default HAPPY");
            normalizedMoodTag = "HAPPY";
        }
        
        // Validate limit
        int safeLimit = Math.max(0, Math.min(limit, allSongs.size()));
        
        // First get all paths matching the mood
        Set<String> matchingPaths = new HashSet<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        
        try {
            db = dbHandler.getReadableDatabase();
            cursor = db.query(MoodDBHandler.TABLE_MOOD_TAGS,
                    new String[]{MoodDBHandler.COLUMN_SONG_PATH},
                    MoodDBHandler.COLUMN_MOOD_TAG + " = ?",
                    new String[]{normalizedMoodTag},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int pathIndex = cursor.getColumnIndexOrThrow(MoodDBHandler.COLUMN_SONG_PATH);
                do {
                    String path = cursor.getString(pathIndex);
                    if (path != null && !path.trim().isEmpty()) {
                        matchingPaths.add(path);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying mood database", e);
            return new ArrayList<>();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (db != null) {
                db.close();
            }
        }

        // Match against the full library to get complete SongsList objects
        ArrayList<SongsList> result = new ArrayList<>();
        for (SongsList song : allSongs) {
            if (song != null && song.getPath() != null && matchingPaths.contains(song.getPath())) {
                result.add(song);
            }
        }

        // Shuffle for non-repetitive recommendations
        java.util.Collections.shuffle(result);

        // Apply limit
        if (result.size() > safeLimit) {
            return new ArrayList<>(result.subList(0, safeLimit));
        }
        
        return result;
    }
    
    /**
     * Validates mood tag against allowed values to prevent SQL injection
     */
    private boolean isValidMoodTag(String moodTag) {
        return MoodAlgorithm.MOOD_HAPPY.equals(moodTag) ||
               MoodAlgorithm.MOOD_SAD.equals(moodTag) ||
               MoodAlgorithm.MOOD_CALM.equals(moodTag) ||
               MoodAlgorithm.MOOD_ENERGETIC.equals(moodTag);
    }

    /**
     * Gets the count of songs tagged with each mood.
     *
     * @return An array: [happyCount, sadCount, calmCount, energeticCount]
     */
    public int[] getMoodCounts() {
        int[] counts = new int[4]; // HAPPY, SAD, CALM, ENERGETIC
        SQLiteDatabase db = dbHandler.getReadableDatabase();

        String[] moods = {"HAPPY", "SAD", "CALM", "ENERGETIC"};
        for (int i = 0; i < moods.length; i++) {
            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + MoodDBHandler.TABLE_MOOD_TAGS
                            + " WHERE " + MoodDBHandler.COLUMN_MOOD_TAG + " = ?",
                    new String[]{moods[i]});
            if (cursor != null && cursor.moveToFirst()) {
                counts[i] = cursor.getInt(0);
                cursor.close();
            }
        }
        db.close();
        return counts;
    }
    
    /**
     * Data class for detailed mood information
     */
    public static class DetailedMoodInfo {
        public final String mood;
        public final double confidence;
        public final String culturalContext;
        public final long analysisTimestamp;
        
        public DetailedMoodInfo(String mood, double confidence, String culturalContext, long analysisTimestamp) {
            this.mood = mood;
            this.confidence = confidence;
            this.culturalContext = culturalContext;
            this.analysisTimestamp = analysisTimestamp;
        }
        
        public String getFormattedTimestamp() {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            return sdf.format(new java.util.Date(analysisTimestamp));
        }
    }
    
    /**
     * Delete mood tag for a specific song.
     *
     * @param songPath The file path of the song
     */
    public void deleteMoodTag(String songPath) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        db.delete(MoodDBHandler.TABLE_MOOD_TAGS, 
                 MoodDBHandler.COLUMN_SONG_PATH + " = ?", 
                 new String[]{songPath});
        db.close();
    }
    
    /**
     * Update mood tag for a specific song (for user corrections).
     *
     * @param songPath The file path of the song
     * @param newMood The corrected mood
     * @param confidence The confidence score
     * @param reasoning The analysis reasoning
     * @return true if update was successful
     */
    public boolean updateMoodTag(String songPath, String newMood, double confidence, String reasoning) {
        SQLiteDatabase db = dbHandler.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(MoodDBHandler.COLUMN_MOOD_TAG, newMood);
        values.put(MoodDBHandler.COLUMN_CONFIDENCE_SCORE, confidence);
        values.put(MoodDBHandler.COLUMN_CULTURAL_CONTEXT, "User corrected");
        values.put(MoodDBHandler.COLUMN_ANALYSIS_TIMESTAMP, System.currentTimeMillis());
        values.put(MoodDBHandler.COLUMN_AUDIO_FEATURES, reasoning);
        
        int rowsAffected = db.update(MoodDBHandler.TABLE_MOOD_TAGS, values,
                MoodDBHandler.COLUMN_SONG_PATH + " = ?",
                new String[]{songPath});
        
        db.close();
        
        boolean success = rowsAffected > 0;
        if (success) {
            Log.i(TAG, "Updated mood tag for: " + songPath + 
                      " -> " + newMood + " (confidence: " + confidence + ")");
        } else {
            Log.e(TAG, "Failed to update mood tag for: " + songPath);
        }
        
        return success;
    }
    
    /**
     * Data class for mood with confidence
     */
    public static class MoodWithConfidence {
        public final String mood;
        public final double confidence;
        
        public MoodWithConfidence(String mood, double confidence) {
            this.mood = mood;
            this.confidence = confidence;
        }
    }
}
