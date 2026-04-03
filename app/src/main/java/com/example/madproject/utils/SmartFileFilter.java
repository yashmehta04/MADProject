package com.example.madproject.utils;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.example.madproject.models.SongsList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Smart file filtering system to exclude unwanted audio files.
 * Filters out WhatsApp recordings, voice memos, and other non-music files.
 */
public class SmartFileFilter {
    
    private static final String TAG = "SmartFileFilter";
    
    // File patterns to exclude
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
        "whatsapp",
        "recording",
        "voice memo",
        "voice note",
        "audio recording",
        "call recording",
        "screen recording",
        "memo",
        "recordings"
    );
    
    // File extensions to exclude
    private static final List<String> EXCLUDED_EXTENSIONS = Arrays.asList(
        ".m4a", // Often used for voice recordings
        ".3gp", // Often used for voice recordings
        ".amr", // Voice recording format
        ".aac"  // Can be voice recordings
    );
    
    // Minimum duration for music files (30 seconds)
    private static final long MIN_DURATION_MS = 30000;
    
    // Maximum file size for voice recordings (10MB)
    private static final long MAX_VOICE_FILE_SIZE = 10 * 1024 * 1024;
    
    // Suspicious metadata patterns
    private static final List<String> SUSPICIOUS_TITLES = Arrays.asList(
        "audio",
        "recording",
        "voice",
        "memo",
        "note",
        "call",
        "whatsapp",
        "voice message"
    );
    
    /**
     * Filter list of songs to exclude non-music files
     */
    public static ArrayList<SongsList> filterMusicFiles(ArrayList<SongsList> allSongs) {
        ArrayList<SongsList> filteredSongs = new ArrayList<>();
        int excludedCount = 0;
        
        for (SongsList song : allSongs) {
            if (isValidMusicFile(song)) {
                filteredSongs.add(song);
            } else {
                excludedCount++;
                Log.d(TAG, "Excluded non-music file: " + song.getTitle() + " (" + song.getPath() + ")");
            }
        }
        
        Log.i(TAG, "File filtering complete: " + filteredSongs.size() + " music files kept, " + 
                  excludedCount + " files excluded");
        
        return filteredSongs;
    }
    
    /**
     * Check if a file is a valid music file
     */
    private static boolean isValidMusicFile(SongsList song) {
        String filePath = song.getPath().toLowerCase();
        String fileName = getFileName(filePath).toLowerCase();
        String title = song.getTitle().toLowerCase();
        
        // Check 1: Path-based exclusion
        if (containsExcludedPath(filePath)) {
            Log.d(TAG, "Excluded by path: " + filePath);
            return false;
        }
        
        // Check 2: File name-based exclusion
        if (containsExcludedKeywords(fileName)) {
            Log.d(TAG, "Excluded by filename: " + fileName);
            return false;
        }
        
        // Check 3: Title-based exclusion
        if (containsExcludedKeywords(title)) {
            Log.d(TAG, "Excluded by title: " + title);
            return false;
        }
        
        // Check 4: Duration check (too short = likely voice memo)
        if (song.getDuration() < MIN_DURATION_MS) {
            Log.d(TAG, "Excluded by duration: " + song.getDuration() + "ms (< " + MIN_DURATION_MS + "ms)");
            return false;
        }
        
        // Check 5: Metadata analysis
        if (isLikelyVoiceRecording(song)) {
            Log.d(TAG, "Excluded by metadata analysis: " + song.getTitle());
            return false;
        }
        
        // Check 6: File extension check (with metadata confirmation)
        if (hasSuspiciousExtension(filePath) && !hasMusicMetadata(song)) {
            Log.d(TAG, "Excluded by extension + metadata: " + filePath);
            return false;
        }
        
        // Passed all checks - likely a music file
        return true;
    }
    
    /**
     * Check if file path contains excluded keywords
     */
    private static boolean containsExcludedPath(String filePath) {
        for (String excludedPath : EXCLUDED_PATHS) {
            if (filePath.contains(excludedPath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if file name contains excluded keywords
     */
    private static boolean containsExcludedKeywords(String fileName) {
        for (String keyword : EXCLUDED_PATHS) {
            if (fileName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if file has suspicious extension
     */
    private static boolean hasSuspiciousExtension(String filePath) {
        for (String ext : EXCLUDED_EXTENSIONS) {
            if (filePath.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Extract file name from path
     */
    private static String getFileName(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
            return filePath.substring(lastSlash + 1);
        }
        return filePath;
    }
    
    /**
     * Advanced metadata analysis to detect voice recordings
     */
    private static boolean isLikelyVoiceRecording(SongsList song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(song.getPath());
            
            // Check for missing or suspicious metadata
            String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
            
            // Voice recordings typically have missing metadata
            int metadataScore = 0;
            
            if (artist == null || artist.trim().isEmpty() || 
                artist.toLowerCase().contains("unknown") ||
                artist.toLowerCase().contains("various")) {
                metadataScore++;
            }
            
            if (album == null || album.trim().isEmpty() || 
                album.toLowerCase().contains("unknown") ||
                album.toLowerCase().contains("various")) {
                metadataScore++;
            }
            
            if (genre == null || genre.trim().isEmpty() || 
                genre.toLowerCase().contains("speech") ||
                genre.toLowerCase().contains("voice")) {
                metadataScore++;
            }
            
            if (year == null || year.trim().isEmpty()) {
                metadataScore++;
            }
            
            // Check title patterns
            String title = song.getTitle().toLowerCase();
            for (String suspiciousTitle : SUSPICIOUS_TITLES) {
                if (title.contains(suspiciousTitle)) {
                    metadataScore += 2; // Higher weight for title patterns
                    break;
                }
            }
            
            // High score indicates likely voice recording
            boolean isVoiceRecording = metadataScore >= 3;
            
            if (isVoiceRecording) {
                Log.d(TAG, "Voice recording detected (score: " + metadataScore + "): " + song.getTitle());
            }
            
            return isVoiceRecording;
            
        } catch (Exception e) {
            Log.w(TAG, "Error analyzing metadata for: " + song.getPath(), e);
            // If we can't analyze metadata, assume it's music (safer)
            return false;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing MediaMetadataRetriever", e);
            }
        }
    }
    
    /**
     * Get filtering statistics
     */
    public static FilteringStats getFilteringStats(ArrayList<SongsList> originalFiles, 
                                                  ArrayList<SongsList> filteredFiles) {
        int originalCount = originalFiles.size();
        int filteredCount = filteredFiles.size();
        int excludedCount = originalCount - filteredCount;
        
        return new FilteringStats(originalCount, filteredCount, excludedCount);
    }
    
    /**
     * Filtering statistics
     */
    public static class FilteringStats {
        public final int originalCount;
        public final int filteredCount;
        public final int excludedCount;
        public final double exclusionRate;
        
        public FilteringStats(int originalCount, int filteredCount, int excludedCount) {
            this.originalCount = originalCount;
            this.filteredCount = filteredCount;
            this.excludedCount = excludedCount;
            this.exclusionRate = originalCount > 0 ? (double) excludedCount / originalCount : 0;
        }
        
        public String getSummary() {
            return String.format("Filtered %d/%d files (%.1f%% excluded)", 
                               filteredCount, originalCount, exclusionRate * 100);
        }
    }
    
    /**
     * Test filtering on a single file (for debugging)
     */
    public static boolean testFile(String filePath) {
        // Create a dummy SongsList for testing
        SongsList testSong = new SongsList(
            0, 
            getFileName(filePath), 
            "Unknown Artist", 
            filePath, 
            180000, // 3 minutes
            "Unknown Album", 
            0
        );
        
        return isValidMusicFile(testSong);
    }
}
