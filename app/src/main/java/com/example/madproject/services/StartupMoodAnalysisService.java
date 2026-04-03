package com.example.madproject.services;

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
import com.example.madproject.utils.SmartFileFilter;
import com.example.madproject.utils.StorageScanner;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Startup mood analysis service that runs when app is first launched.
 * Analyzes all music files with smart filtering and shows progress.
 */
public class StartupMoodAnalysisService {
    
    private static final String TAG = "StartupMoodAnalysis";
    private static final String CHANNEL_ID = "startup_analysis_channel";
    private static final int NOTIFICATION_ID = 2001;
    
    private Context context;
    private NotificationManager notificationManager;
    private HybridMoodAnalyzer hybridAnalyzer;
    private MoodOperations moodOperations;
    
    public StartupMoodAnalysisService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.hybridAnalyzer = HybridMoodAnalyzer.getInstance(context);
        this.moodOperations = new MoodOperations(context);
        
        createNotificationChannel();
    }
    
    /**
     * Start comprehensive mood analysis on app startup
     */
    public void startStartupAnalysis() {
        Log.i(TAG, "Starting comprehensive mood analysis for all songs");
        
        // Run in background thread using ExecutorService for better thread management
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(this::performStartupAnalysis);
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * Perform the complete startup analysis
     */
    private void performStartupAnalysis() {
        try {
            // Step 1: Scan all audio files
            showProgressNotification("Scanning music files...", 0);
            ArrayList<SongsList> allAudioFiles = StorageScanner.scanSongs(context);
            
            Log.i(TAG, "Found " + allAudioFiles.size() + " total audio files");
            
            // Step 2: Filter out non-music files
            showProgressNotification("Filtering music files...", 10);
            ArrayList<SongsList> musicFiles = SmartFileFilter.filterMusicFiles(allAudioFiles);
            
            SmartFileFilter.FilteringStats stats = SmartFileFilter.getFilteringStats(allAudioFiles, musicFiles);
            Log.i(TAG, "Filtering complete: " + stats.getSummary());
            
            if (musicFiles.isEmpty()) {
                showCompletionNotification("No music files found", 0);
                return;
            }
            
            // Step 3: Check which files need analysis
            showProgressNotification("Checking analyzed songs...", 20);
            ArrayList<String> unanalyzedFiles = getUnanalyzedFiles(musicFiles);
            
            Log.i(TAG, "Need to analyze " + unanalyzedFiles.size() + " out of " + musicFiles.size() + " files");
            
            if (unanalyzedFiles.isEmpty()) {
                showCompletionNotification("All songs already analyzed", musicFiles.size());
                return;
            }
            
            // Step 4: Analyze unprocessed files with progress
            analyzeFilesWithProgress(unanalyzedFiles);
            
        } catch (Exception e) {
            Log.e(TAG, "Error during startup analysis", e);
            showErrorNotification("Analysis failed: " + e.getMessage());
        }
    }
    
    /**
     * Get list of files that haven't been analyzed yet
     */
    private ArrayList<String> getUnanalyzedFiles(ArrayList<SongsList> musicFiles) {
        ArrayList<String> unanalyzed = new ArrayList<>();
        
        // Get already analyzed file paths
        java.util.Set<String> analyzedPaths = moodOperations.getAllTaggedPaths();
        
        for (SongsList song : musicFiles) {
            if (!analyzedPaths.contains(song.getPath())) {
                unanalyzed.add(song.getPath());
            }
        }
        
        return unanalyzed;
    }
    
    /**
     * Analyze files with progress updates
     */
    private void analyzeFilesWithProgress(ArrayList<String> filesToAnalyze) {
        int totalFiles = filesToAnalyze.size();
        int processedFiles = 0;
        long startTime = System.currentTimeMillis();
        
        Log.i(TAG, "Starting analysis of " + totalFiles + " files");
        
        for (int i = 0; i < filesToAnalyze.size(); i++) {
            String filePath = filesToAnalyze.get(i);
            
            try {
                // Analyze the file
                HybridMoodAnalyzer.MoodResult result = hybridAnalyzer.analyzeSong(filePath);
                
                // Store in database
                moodOperations.insertMoodTagWithDetails(
                    filePath, 
                    result.mood, 
                    result.confidence, 
                    result.culturalContext
                );
                
                processedFiles++;
                
                // Update progress every 10 files or for the last file
                if (processedFiles % 10 == 0 || processedFiles == totalFiles) {
                    int progress = 30 + (int) ((processedFiles * 60) / totalFiles);
                    showProgressNotification("Analyzing music... (" + processedFiles + "/" + totalFiles + ")", progress);
                    
                    long elapsed = System.currentTimeMillis() - startTime;
                    double avgTime = (double) elapsed / processedFiles / 1000;
                    int remaining = totalFiles - processedFiles;
                    int etaSeconds = (int) (remaining * avgTime);
                    
                    Log.d(TAG, "Progress: " + processedFiles + "/" + totalFiles + 
                              " (" + String.format("%.1f", avgTime) + "s per file, ETA: " + etaSeconds + "s)");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error analyzing file: " + filePath, e);
                processedFiles++;
            }
        }
        
        // Show completion
        long totalTime = System.currentTimeMillis() - startTime;
        double avgTime = (double) totalTime / processedFiles / 1000;
        
        showCompletionNotification(
            "Analysis complete! " + processedFiles + " songs analyzed (" + 
            String.format("%.1f", avgTime) + "s per song)", 
            processedFiles
        );
        
        Log.i(TAG, "Startup analysis complete: " + processedFiles + " files analyzed in " + 
                  (totalTime / 1000) + "s (avg: " + String.format("%.1f", avgTime) + "s per file)");
    }
    
    /**
     * Create notification channel
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Music Analysis",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Analyzing music files for mood detection");
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    /**
     * Show progress notification
     */
    private void showProgressNotification(String message, int progress) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Music Mood Analysis")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setProgress(100, progress, false)
                .setOnlyAlertOnce(true)
                .build();
        
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    /**
     * Show completion notification
     */
    private void showCompletionNotification(String message, int songsAnalyzed) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Music Analysis Complete")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setProgress(0, 0, false)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .build();
        
        notificationManager.notify(NOTIFICATION_ID, notification);
        
        // Auto-dismiss after 5 seconds
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            notificationManager.cancel(NOTIFICATION_ID);
        }, 5000);
    }
    
    /**
     * Show error notification
     */
    private void showErrorNotification(String error) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Analysis Error")
                .setContentText(error)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(error))
                .build();
        
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
    
    /**
     * Check if analysis is needed
     */
    public boolean isAnalysisNeeded() {
        try {
            // Get total music files
            ArrayList<SongsList> allAudioFiles = StorageScanner.scanSongs(context);
            ArrayList<SongsList> musicFiles = SmartFileFilter.filterMusicFiles(allAudioFiles);
            
            if (musicFiles.isEmpty()) {
                return false;
            }
            
            // Get analyzed files
            java.util.Set<String> analyzedPaths = moodOperations.getAllTaggedPaths();
            
            // Need analysis if less than 80% of files are analyzed
            double analysisRatio = (double) analyzedPaths.size() / musicFiles.size();
            
            Log.i(TAG, "Analysis ratio: " + String.format("%.1f", analysisRatio * 100) + 
                      "% (" + analyzedPaths.size() + "/" + musicFiles.size() + ")");
            
            return analysisRatio < 0.8;
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking if analysis is needed", e);
            return false;
        }
    }
    
    /**
     * Get analysis statistics
     */
    public AnalysisStats getAnalysisStats() {
        try {
            ArrayList<SongsList> allAudioFiles = StorageScanner.scanSongs(context);
            ArrayList<SongsList> musicFiles = SmartFileFilter.filterMusicFiles(allAudioFiles);
            java.util.Set<String> analyzedPaths = moodOperations.getAllTaggedPaths();
            
            return new AnalysisStats(
                allAudioFiles.size(),
                musicFiles.size(),
                analyzedPaths.size(),
                musicFiles.size() - analyzedPaths.size()
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting analysis stats", e);
            return new AnalysisStats(0, 0, 0, 0);
        }
    }
    
    /**
     * Analysis statistics
     */
    public static class AnalysisStats {
        public final int totalAudioFiles;
        public final int musicFiles;
        public final int analyzedFiles;
        public final int unanalyzedFiles;
        public final double analysisProgress;
        
        public AnalysisStats(int totalAudioFiles, int musicFiles, int analyzedFiles, int unanalyzedFiles) {
            this.totalAudioFiles = totalAudioFiles;
            this.musicFiles = musicFiles;
            this.analyzedFiles = analyzedFiles;
            this.unanalyzedFiles = unanalyzedFiles;
            this.analysisProgress = musicFiles > 0 ? (double) analyzedFiles / musicFiles : 0;
        }
        
        public String getSummary() {
            return String.format("Analyzed %d/%d music files (%.1f%% complete)", 
                               analyzedFiles, musicFiles, analysisProgress * 100);
        }
    }
}
