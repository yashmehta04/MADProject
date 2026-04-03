package com.example.madproject.utils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for formatting time durations.
 */
public final class TimeFormatter {

    private TimeFormatter() {
        // Private constructor to prevent instantiation
    }

    /**
     * Converts milliseconds to a formatted time string (mm:ss or hh:mm:ss).
     * @param millis Duration in milliseconds
     * @return Formatted time string
     */
    public static String formatTime(long millis) {
        if (millis < 0) {
            return "00:00";
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }
}
