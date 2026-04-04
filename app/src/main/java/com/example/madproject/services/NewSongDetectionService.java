package com.example.madproject.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.madproject.R;
import com.example.madproject.database.MoodOperations;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.MoodAlgorithm;
import com.example.madproject.utils.StorageScanner;

import java.util.ArrayList;

/**
 * Service to automatically detect and process newly downloaded songs.
 * Listens for media scan broadcasts and automatically tags new songs with moods.
 */
public class NewSongDetectionService extends Service {
    
    private static final String TAG = "NewSongDetectionService";
    private static final long SCAN_DELAY_MS = 3000; // 3 seconds after download
    
    private Handler mainHandler;
    private BroadcastReceiver mediaScanReceiver;
    private boolean isReceiverRegistered = false;
    private java.util.concurrent.atomic.AtomicBoolean isScanning = new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile boolean isServiceAlive = false;
    private Thread scanThread;
    
    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        setupMediaScanReceiver();
        isServiceAlive = true;
        Log.d(TAG, "NewSongDetectionService started");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Create notification channel for Android O+
        createNotificationChannel();
        
        // Start foreground service
        startForeground(1, createNotification());
        
        // Check if service was started with a specific action
        if (intent != null && "SCAN_NEW_SONGS".equals(intent.getAction())) {
            Log.d(TAG, "Manual scan triggered");
            scanAndTagNewSongs();
        }
        
        return START_NOT_STICKY; // Don't restart automatically
    }
    
    /**
     * Create notification channel for Android O+
     */
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                "new_song_detection",
                "New Song Detection",
                android.app.NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Detects and processes new songs");
            android.app.NotificationManager notificationManager = getSystemService(android.app.NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Create foreground service notification
     */
    private android.app.Notification createNotification() {
        android.app.Notification.Builder builder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            builder = new android.app.Notification.Builder(this, "new_song_detection");
        } else {
            builder = new android.app.Notification.Builder(this);
        }
        
        builder.setContentTitle("SonicWave")
               .setContentText("Monitoring for new songs...")
               .setSmallIcon(R.drawable.ic_music_note)
               .setPriority(android.app.Notification.PRIORITY_LOW);
        
        return builder.build();
    }
    
    /**
     * Sets up broadcast receiver to detect when media scanner finishes
     */
    private void setupMediaScanReceiver() {
        mediaScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_MEDIA_SCANNER_FINISHED.equals(intent.getAction())) {
                    Log.d(TAG, "Media scan finished, checking for new songs");
                    
                    // Delay scan to ensure all files are properly indexed
                    mainHandler.postDelayed(() -> {
                        scanAndTagNewSongs();
                    }, SCAN_DELAY_MS);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        // Remove ACTION_MEDIA_SCANNER_SCAN_FILE as it's not handled
        filter.addDataScheme("file");
        
        registerReceiver(mediaScanReceiver, filter);
        isReceiverRegistered = true;
        
        Log.d(TAG, "Media scan receiver registered");
    }
    
    /**
     * Scans for new songs and automatically tags them with moods
     */
    private void scanAndTagNewSongs() {
        // Check if service is still alive
        if (!isServiceAlive) {
            Log.d(TAG, "Service not alive, skipping scan");
            return;
        }
        
        // Atomic check-then-set - only proceed if not already scanning
        if (!isScanning.compareAndSet(false, true)) {
            Log.d(TAG, "Scan already in progress, skipping");
            return;
        }
        
        scanThread = new Thread(() -> {
            try {
                Log.d(TAG, "Scanning for new songs...");
                
                // Get current songs from database (already tagged)
                MoodOperations moodOps = new MoodOperations(this);
                java.util.Set<String> taggedPaths = moodOps.getAllTaggedPaths();
                
                // Scan device for all songs
                ArrayList<SongsList> allSongs = StorageScanner.scanSongs(this);
                
                // Find new songs (not in database)
                ArrayList<SongsList> newSongs = new ArrayList<>();
                for (SongsList song : allSongs) {
                    if (song != null && song.getPath() != null && !taggedPaths.contains(song.getPath())) {
                        newSongs.add(song);
                    }
                }
                
                if (!newSongs.isEmpty()) {
                    Log.i(TAG, "Found " + newSongs.size() + " new songs. Tagging with moods...");
                    
                    // Tag new songs with moods
                    MoodAlgorithm.tagSongsInBackground(this, newSongs, () -> {
                        mainHandler.post(() -> {
                            if (!isServiceAlive) {
                                Log.d(TAG, "Service not alive, skipping UI updates");
                                return;
                            }
                            
                            String message = "Processed " + newSongs.size() + " new songs with mood tags";
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                            Log.i(TAG, message);
                            
                            // Notify MainActivity about new songs AFTER tagging completes
                            Intent updateIntent = new Intent("com.example.madproject.NEW_SONGS_DETECTED");
                            updateIntent.putExtra("new_songs_count", newSongs.size());
                            sendBroadcast(updateIntent);
                            
                            // Reset scanning flag only when all work completes
                            isScanning.set(false);
                        });
                    });
                    
                } else {
                    Log.d(TAG, "No new songs found");
                    // Reset scanning flag when no new songs found
                    isScanning.set(false);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error scanning for new songs", e);
                mainHandler.post(() -> {
                    if (isServiceAlive) {
                        Toast.makeText(this, "Error scanning for new songs", Toast.LENGTH_SHORT).show();
                    }
                });
                // Reset scanning flag on error
                isScanning.set(false);
            }
        });
        
        // Start the thread
        scanThread.start();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Set service as not alive
        isServiceAlive = false;
        
        // Cancel any ongoing scan thread
        if (scanThread != null && scanThread.isAlive()) {
            scanThread.interrupt();
            try {
                scanThread.join(1000); // Wait up to 1 second for thread to finish
            } catch (InterruptedException e) {
                Log.w(TAG, "Interrupted while waiting for scan thread to finish", e);
            }
        }
        
        // Clear pending handler callbacks
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        
        // Unregister receiver
        if (isReceiverRegistered && mediaScanReceiver != null) {
            unregisterReceiver(mediaScanReceiver);
            isReceiverRegistered = false;
        }
        
        Log.d(TAG, "NewSongDetectionService stopped");
    }
    
    /**
     * Static method to trigger manual scan
     */
    public static void triggerManualScan(Context context) {
        Intent intent = new Intent(context, NewSongDetectionService.class);
        intent.setAction("SCAN_NEW_SONGS");
        ContextCompat.startForegroundService(context, intent);
    }
}
