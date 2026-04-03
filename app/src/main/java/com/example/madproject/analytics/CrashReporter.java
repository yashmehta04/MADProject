package com.example.madproject.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Crash reporting and analytics system for SonicWave Music Player
 * Provides comprehensive error tracking, performance monitoring, and user analytics
 */
public class CrashReporter {

    private static final String TAG = "CrashReporter";
    private static final String PREFS_NAME = "crash_reporter_prefs";
    private static final String CRASH_LOG_FILE = "sonicwave_crashes.log";
    private static final String ANALYTICS_FILE = "sonicwave_analytics.json";
    
    private static volatile CrashReporter instance;
    private final Context context;
    private final SharedPreferences preferences;
    private final ExecutorService analyticsExecutor;
    private final ConcurrentHashMap<String, Object> sessionData;
    
    // Session tracking
    private long sessionStartTime;
    private String sessionId;
    private boolean isReportingEnabled;
    
    private CrashReporter(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.analyticsExecutor = Executors.newSingleThreadExecutor();
        this.sessionData = new ConcurrentHashMap<>();
        this.isReportingEnabled = preferences.getBoolean("crash_reporting_enabled", true);
        
        initializeSession();
        setupGlobalExceptionHandler();
    }
    
    /**
     * Get singleton instance
     */
    public static CrashReporter getInstance(Context context) {
        if (instance == null) {
            synchronized (CrashReporter.class) {
                if (instance == null) {
                    instance = new CrashReporter(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Initialize new session
     */
    private void initializeSession() {
        sessionStartTime = System.currentTimeMillis();
        sessionId = generateSessionId();
        
        // Collect device info
        sessionData.put("device_model", Build.MODEL);
        sessionData.put("device_manufacturer", Build.MANUFACTURER);
        sessionData.put("android_version", Build.VERSION.RELEASE);
        sessionData.put("api_level", Build.VERSION.SDK_INT);
        sessionData.put("app_version", getAppVersion());
        sessionData.put("session_start_time", sessionStartTime);
        
        Log.i(TAG, "Session initialized: " + sessionId);
    }
    
    /**
     * Setup global exception handler
     */
    private void setupGlobalExceptionHandler() {
        if (isReportingEnabled) {
            Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                // Log crash
                logCrash(throwable, "Uncaught Exception", thread.getName());
                
                // Call default handler
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            });
        }
    }
    
    /**
     * Log crash with detailed information
     */
    public void logCrash(Throwable throwable, String context, String threadName) {
        if (!isReportingEnabled) return;
        
        analyticsExecutor.submit(() -> {
            try {
                JSONObject crashReport = new JSONObject();
                crashReport.put("timestamp", System.currentTimeMillis());
                crashReport.put("session_id", sessionId);
                crashReport.put("thread_name", threadName);
                crashReport.put("context", context);
                crashReport.put("error_type", throwable.getClass().getSimpleName());
                crashReport.put("error_message", throwable.getMessage());
                crashReport.put("stack_trace", getStackTrace(throwable));
                
                // Add session data
                crashReport.put("session_data", new JSONObject(sessionData));
                
                // Add device state
                crashReport.put("device_state", getDeviceState());
                
                // Write to crash log file
                writeCrashToFile(crashReport);
                
                // Increment crash count
                incrementCrashCount();
                
                Log.e(TAG, "Crash logged: " + throwable.getMessage());
                
            } catch (Exception e) {
                Log.e(TAG, "Error logging crash", e);
            }
        });
    }
    
    /**
     * Log custom event
     */
    public void logEvent(String eventName, String details) {
        if (!isReportingEnabled) return;
        
        analyticsExecutor.submit(() -> {
            try {
                JSONObject event = new JSONObject();
                event.put("timestamp", System.currentTimeMillis());
                event.put("session_id", sessionId);
                event.put("event_name", eventName);
                event.put("details", details);
                event.put("session_duration", System.currentTimeMillis() - sessionStartTime);
                
                writeEventToFile(event);
                
                Log.d(TAG, "Event logged: " + eventName);
                
            } catch (Exception e) {
                Log.e(TAG, "Error logging event", e);
            }
        });
    }
    
    /**
     * Log performance metric
     */
    public void logPerformanceMetric(String metricName, double value, String unit) {
        if (!isReportingEnabled) return;
        
        analyticsExecutor.submit(() -> {
            try {
                JSONObject metric = new JSONObject();
                metric.put("timestamp", System.currentTimeMillis());
                metric.put("session_id", sessionId);
                metric.put("metric_name", metricName);
                metric.put("value", value);
                metric.put("unit", unit);
                metric.put("session_duration", System.currentTimeMillis() - sessionStartTime);
                
                writeMetricToFile(metric);
                
                Log.d(TAG, "Performance metric logged: " + metricName + " = " + value + " " + unit);
                
            } catch (Exception e) {
                Log.e(TAG, "Error logging performance metric", e);
            }
        });
    }
    
    /**
     * Log user interaction
     */
    public void logUserInteraction(String action, String target, long duration) {
        if (!isReportingEnabled) return;
        
        analyticsExecutor.submit(() -> {
            try {
                JSONObject interaction = new JSONObject();
                interaction.put("timestamp", System.currentTimeMillis());
                interaction.put("session_id", sessionId);
                interaction.put("action", action);
                interaction.put("target", target);
                interaction.put("duration_ms", duration);
                interaction.put("session_duration", System.currentTimeMillis() - sessionStartTime);
                
                writeInteractionToFile(interaction);
                
                Log.d(TAG, "User interaction logged: " + action + " on " + target);
                
            } catch (Exception e) {
                Log.e(TAG, "Error logging user interaction", e);
            }
        });
    }
    
    /**
     * Enable/disable crash reporting
     */
    public void setReportingEnabled(boolean enabled) {
        this.isReportingEnabled = enabled;
        preferences.edit().putBoolean("crash_reporting_enabled", enabled).apply();
        
        Log.i(TAG, "Crash reporting " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if reporting is enabled
     */
    public boolean isReportingEnabled() {
        return isReportingEnabled;
    }
    
    /**
     * Get crash statistics
     */
    public CrashStatistics getCrashStatistics() {
        int totalCrashes = preferences.getInt("total_crashes", 0);
        int sessionsSinceLastCrash = preferences.getInt("sessions_since_last_crash", 0);
        long lastCrashTime = preferences.getLong("last_crash_time", 0);
        
        return new CrashStatistics(totalCrashes, sessionsSinceLastCrash, lastCrashTime);
    }
    
    /**
     * Clear all crash data
     */
    public void clearCrashData() {
        analyticsExecutor.submit(() -> {
            try {
                // Delete crash log file
                File crashFile = new File(context.getFilesDir(), CRASH_LOG_FILE);
                if (crashFile.exists()) {
                    crashFile.delete();
                }
                
                // Delete analytics file
                File analyticsFile = new File(context.getFilesDir(), ANALYTICS_FILE);
                if (analyticsFile.exists()) {
                    analyticsFile.delete();
                }
                
                // Clear preferences
                preferences.edit()
                    .remove("total_crashes")
                    .remove("sessions_since_last_crash")
                    .remove("last_crash_time")
                    .apply();
                
                Log.i(TAG, "Crash data cleared");
                
            } catch (Exception e) {
                Log.e(TAG, "Error clearing crash data", e);
            }
        });
    }
    
    /**
     * End current session
     */
    public void endSession() {
        if (!isReportingEnabled) return;
        
        long sessionDuration = System.currentTimeMillis() - sessionStartTime;
        
        analyticsExecutor.submit(() -> {
            try {
                JSONObject sessionEnd = new JSONObject();
                sessionEnd.put("timestamp", System.currentTimeMillis());
                sessionEnd.put("session_id", sessionId);
                sessionEnd.put("session_duration_ms", sessionDuration);
                sessionEnd.put("session_end_reason", "normal");
                
                writeSessionEndToFile(sessionEnd);
                
                // Increment sessions since last crash
                int sessionsSinceLastCrash = preferences.getInt("sessions_since_last_crash", 0) + 1;
                preferences.edit().putInt("sessions_since_last_crash", sessionsSinceLastCrash).apply();
                
                Log.i(TAG, "Session ended: " + sessionId + " (duration: " + sessionDuration + "ms)");
                
            } catch (Exception e) {
                Log.e(TAG, "Error ending session", e);
            }
        });
    }
    
    // Private helper methods
    
    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString((int) (Math.random() * 0xFFFF));
    }
    
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    private JSONObject getDeviceState() throws JSONException {
        JSONObject state = new JSONObject();
        
        // Memory info
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        state.put("total_memory_mb", totalMemory / (1024 * 1024));
        state.put("used_memory_mb", usedMemory / (1024 * 1024));
        state.put("free_memory_mb", freeMemory / (1024 * 1024));
        
        // Storage info
        File filesDir = context.getFilesDir();
        long totalSpace = filesDir.getTotalSpace();
        long freeSpace = filesDir.getFreeSpace();
        
        state.put("total_storage_mb", totalSpace / (1024 * 1024));
        state.put("free_storage_mb", freeSpace / (1024 * 1024));
        
        return state;
    }
    
    private void writeCrashToFile(JSONObject crashReport) throws IOException, JSONException {
        File crashFile = new File(context.getFilesDir(), CRASH_LOG_FILE);
        try (FileWriter writer = new FileWriter(crashFile, true)) {
            writer.write("=== CRASH REPORT ===\n");
            writer.write("Time: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(crashReport.optLong("timestamp"))));
            writer.write("\n");
            writer.write(crashReport.toString(2)); // Pretty print with 2 spaces indentation
            writer.write("\n\n");
        }
    }
    
    private void writeEventToFile(JSONObject event) throws IOException {
        File analyticsFile = new File(context.getFilesDir(), ANALYTICS_FILE);
        try (FileWriter writer = new FileWriter(analyticsFile, true)) {
            writer.write("EVENT: " + event.toString() + "\n");
        }
    }
    
    private void writeMetricToFile(JSONObject metric) throws IOException {
        File analyticsFile = new File(context.getFilesDir(), ANALYTICS_FILE);
        try (FileWriter writer = new FileWriter(analyticsFile, true)) {
            writer.write("METRIC: " + metric.toString() + "\n");
        }
    }
    
    private void writeInteractionToFile(JSONObject interaction) throws IOException {
        File analyticsFile = new File(context.getFilesDir(), ANALYTICS_FILE);
        try (FileWriter writer = new FileWriter(analyticsFile, true)) {
            writer.write("INTERACTION: " + interaction.toString() + "\n");
        }
    }
    
    private void writeSessionEndToFile(JSONObject sessionEnd) throws IOException {
        File analyticsFile = new File(context.getFilesDir(), ANALYTICS_FILE);
        try (FileWriter writer = new FileWriter(analyticsFile, true)) {
            writer.write("SESSION_END: " + sessionEnd.toString() + "\n");
        }
    }
    
    private void incrementCrashCount() {
        int totalCrashes = preferences.getInt("total_crashes", 0) + 1;
        preferences.edit()
            .putInt("total_crashes", totalCrashes)
            .putLong("last_crash_time", System.currentTimeMillis())
            .putInt("sessions_since_last_crash", 0)
            .apply();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (analyticsExecutor != null && !analyticsExecutor.isShutdown()) {
            analyticsExecutor.shutdown();
            try {
                if (!analyticsExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    analyticsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                analyticsExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Crash statistics data class
     */
    public static class CrashStatistics {
        public final int totalCrashes;
        public final int sessionsSinceLastCrash;
        public final long lastCrashTime;
        
        public CrashStatistics(int totalCrashes, int sessionsSinceLastCrash, long lastCrashTime) {
            this.totalCrashes = totalCrashes;
            this.sessionsSinceLastCrash = sessionsSinceLastCrash;
            this.lastCrashTime = lastCrashTime;
        }
        
        public String getSummary() {
            return "Total Crashes: " + totalCrashes + 
                   ", Sessions Since Last Crash: " + sessionsSinceLastCrash +
                   ", Last Crash: " + (lastCrashTime > 0 ? new Date(lastCrashTime).toString() : "Never");
        }
    }
}
