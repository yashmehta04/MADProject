package com.example.madproject.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.madproject.R;
import com.example.madproject.activities.MainActivity;
import com.example.madproject.database.MoodOperations;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.HybridMoodAnalyzer;
import com.example.madproject.utils.StorageScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Background service for batch mood analysis of songs.
 * Analyzes all unprocessed songs in the background with progress notifications.
 */
public class MoodAnalysisService extends IntentService {
    
    private static final String TAG = "MoodAnalysisService";
    private static final String CHANNEL_ID = "mood_analysis_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    // Intent actions
    public static final String ACTION_ANALYZE_ALL = "com.example.madproject.ANALYZE_ALL";
    public static final String ACTION_ANALYZE_UNPROCESSED = "com.example.madproject.ANALYZE_UNPROCESSED";
    public static final String EXTRA_SONGS = "extra_songs";
    
    // Progress tracking
    private int totalSongs = 0;
    private int processedSongs = 0;
    private NotificationManager notificationManager;
    private HybridMoodAnalyzer hybridAnalyzer;
    private MoodOperations moodOperations;
    
    public MoodAnalysisService() {
        super("MoodAnalysisService");
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize components
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        hybridAnalyzer = HybridMoodAnalyzer.getInstance(this);
        moodOperations = new MoodOperations(this);
        
        // Create notification channel
        createNotificationChannel();
        
        Log.d(TAG, "MoodAnalysisService created");
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        
        if (action == null) {
            Log.w(TAG, "Received intent with null action");
            return;
        }
        
        try {
            switch (action) {
                case ACTION_ANALYZE_ALL:
                    analyzeAllSongs();
                    break;
                    
                case ACTION_ANALYZE_UNPROCESSED:
                    analyzeUnprocessedSongs();
                    break;
                    
                default:
                    Log.w(TAG, "Unknown action: " + action);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling intent", e);
        }
    }
    
    /**
     * Analyze all songs in the library
     */
    private void analyzeAllSongs() {
        Log.d(TAG, "Starting analysis of all songs");
        
        // Get all songs from storage scanner
        ArrayList<SongsList> allSongs = StorageScanner.scanSongs(this);
        
        if (allSongs.isEmpty()) {
            Log.i(TAG, "No songs found to analyze");
            return;
        }
        
        // Convert to paths and analyze
        List<String> songPaths = new ArrayList<>();
        for (SongsList song : allSongs) {
            songPaths.add(song.getPath());
        }
        
        analyzeSongs(songPaths, "Analyzing All Songs");
    }
    
    /**
     * Analyze only unprocessed songs
     */
    private void analyzeUnprocessedSongs() {
        Log.d(TAG, "Starting analysis of unprocessed songs");
        
        // Get all songs from storage scanner
        ArrayList<SongsList> allSongs = StorageScanner.scanSongs(this);
        
        // Get already processed songs
        Set<String> processedPaths = moodOperations.getAllTaggedPaths();
        
        // Filter to unprocessed songs only
        List<String> unprocessedPaths = new ArrayList<>();
        for (SongsList song : allSongs) {
            if (!processedPaths.contains(song.getPath())) {
                unprocessedPaths.add(song.getPath());
            }
        }
        
        if (unprocessedPaths.isEmpty()) {
            Log.i(TAG, "No unprocessed songs found");
            return;
        }
        
        analyzeSongs(unprocessedPaths, "Analyzing New Songs");
    }
    
    /**
     * Analyze a list of song paths
     */
    private void analyzeSongs(List<String> songPaths, String progressTitle) {
        totalSongs = songPaths.size();
        processedSongs = 0;
        
        Log.d(TAG, "Starting analysis of " + totalSongs + " songs");
        
        // Start progress notification
        startProgressNotification(progressTitle);
        
        // Analyze songs in batches
        for (int i = 0; i < songPaths.size(); i++) {
            String songPath = songPaths.get(i);
            
            try {
                // Check if already analyzed (might have been processed by another process)
                if (hybridAnalyzer.isAnalyzed(songPath)) {
                    processedSongs++;
                    updateProgressNotification(progressTitle);
                    continue;
                }
                
                // Analyze song
                HybridMoodAnalyzer.MoodResult result = hybridAnalyzer.analyzeSong(songPath);
                
                // Store enhanced mood information in database
                moodOperations.insertMoodTagWithDetails(
                    songPath, 
                    result.mood, 
                    result.confidence, 
                    result.culturalContext
                );
                
                processedSongs++;
                updateProgressNotification(progressTitle);
                
                Log.d(TAG, "Analyzed song " + (i + 1) + "/" + totalSongs + ": " + result.mood);
                
                // Add small delay to prevent overwhelming the system
                if (i % 10 == 0) {
                    try {
                        Thread.sleep(100); // 100ms delay every 10 songs
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing song: " + songPath, e);
                processedSongs++;
                updateProgressNotification(progressTitle);
            }
        }
        
        // Complete notification
        completeNotification(progressTitle);
        
        Log.d(TAG, "Analysis complete. Processed " + processedSongs + " out of " + totalSongs + " songs");
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Mood Analysis",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background mood analysis progress");
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Start progress notification
     */
    private void startProgressNotification(String title) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Starting analysis...")
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setProgress(totalSongs, 0, false)
                .build();
        
        startForeground(NOTIFICATION_ID, notification);
    }
    
    /**
     * Update progress notification
     */
    private void updateProgressNotification(String title) {
        int progress = (int) ((processedSongs * 100) / totalSongs);
        
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Analyzed " + processedSongs + " of " + totalSongs + " songs")
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setProgress(totalSongs, processedSongs, false)
                .build();
        
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    /**
     * Complete notification
     */
    private void completeNotification(String title) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText("Analysis complete! Processed " + processedSongs + " songs")
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .build();
        
        notificationManager.notify(NOTIFICATION_ID, notification);
        
        // Stop foreground service
        stopForeground(false);
    }
    
    /**
     * Static method to start analysis of all songs
     */
    public static void analyzeAllSongs(Context context) {
        Intent intent = new Intent(context, MoodAnalysisService.class);
        intent.setAction(ACTION_ANALYZE_ALL);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    /**
     * Static method to start analysis of unprocessed songs
     */
    public static void analyzeUnprocessedSongs(Context context) {
        Intent intent = new Intent(context, MoodAnalysisService.class);
        intent.setAction(ACTION_ANALYZE_UNPROCESSED);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
    
    /**
     * Check if analysis is currently running
     */
    public static boolean isAnalysisRunning(Context context) {
        // This is a simplified check - in a real implementation, 
        // you might use SharedPreferences or another mechanism
        return false; // Placeholder
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Cleanup resources
        if (hybridAnalyzer != null) {
            hybridAnalyzer.cleanup();
        }
        
        Log.d(TAG, "MoodAnalysisService destroyed");
    }
}
