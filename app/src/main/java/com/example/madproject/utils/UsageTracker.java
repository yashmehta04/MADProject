package com.example.madproject.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Tracks daily app usage time using SharedPreferences.
 * Also stores last played song info for state restoration.
 */
public class UsageTracker {

    private static final String PREFS_NAME = "sonicwave_usage";
    private static final String KEY_SESSION_START = "session_start";
    private static final String KEY_LAST_SONG_TITLE = "last_song_title";
    private static final String KEY_LAST_SONG_ARTIST = "last_song_artist";
    private static final String KEY_LAST_SONG_PATH = "last_song_path";
    private static final String KEY_LAST_SONG_ALBUM_ID = "last_song_album_id";
    private static final String KEY_LAST_SONG_DURATION = "last_song_duration";

    /**
     * Called when app opens or resumes — records the session start time.
     */
    public static void startSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_SESSION_START, System.currentTimeMillis()).apply();
    }

    /**
     * Called when app is paused/destroyed — calculates elapsed time and adds it to today's total.
     */
    public static void endSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long startTime = prefs.getLong(KEY_SESSION_START, 0);
        if (startTime == 0) return;

        long elapsed = System.currentTimeMillis() - startTime;
        int minutes = (int) (elapsed / 60000);
        if (minutes < 1) minutes = 1;

        String todayKey = getTodayKey();
        int existing = prefs.getInt(todayKey, 0);
        prefs.edit()
                .putInt(todayKey, existing + minutes)
                .putLong(KEY_SESSION_START, 0)
                .apply();
    }

    /**
     * Saves the last played song info for state restoration.
     */
    public static void saveLastPlayedSong(Context context, String title, String artist, String path, long albumId, long duration) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_LAST_SONG_TITLE, title)
                .putString(KEY_LAST_SONG_ARTIST, artist)
                .putString(KEY_LAST_SONG_PATH, path)
                .putLong(KEY_LAST_SONG_ALBUM_ID, albumId)
                .putLong(KEY_LAST_SONG_DURATION, duration)
                .apply();
    }

    /**
     * Returns the last played song title (or null).
     */
    public static String getLastSongTitle(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_SONG_TITLE, null);
    }

    public static String getLastSongArtist(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_SONG_ARTIST, null);
    }

    public static String getLastSongPath(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_SONG_PATH, null);
    }

    public static long getLastSongAlbumId(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SONG_ALBUM_ID, -1);
    }

    public static long getLastSongDuration(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SONG_DURATION, 0);
    }

    // ======================== Usage tracking ========================

    public static int[] getWeeklyUsage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int[] usage = new int[7];
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            usage[i] = prefs.getInt(getDayKey(cal.getTime()), 0);
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return usage;
    }

    /**
     * Returns usage for a specific month (array of 31 entries, index = day-1).
     */
    public static int[] getMonthlyUsage(Context context, int year, int month) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int[] usage = new int[daysInMonth];
        for (int i = 0; i < daysInMonth; i++) {
            cal.set(Calendar.DAY_OF_MONTH, i + 1);
            usage[i] = prefs.getInt(getDayKey(cal.getTime()), 0);
        }
        return usage;
    }

    public static String[] getWeeklyLabels() {
        String[] labels = new String[7];
        SimpleDateFormat sdf = new SimpleDateFormat("EEE", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        for (int i = 6; i >= 0; i--) {
            labels[i] = sdf.format(cal.getTime());
            cal.add(Calendar.DAY_OF_YEAR, -1);
        }
        return labels;
    }

    public static int getTodayUsage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(getTodayKey(), 0);
    }

    private static String getTodayKey() {
        return getDayKey(new Date());
    }

    private static String getDayKey(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return "usage_" + sdf.format(date);
    }
}
