package com.example.madproject.utils;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.example.madproject.models.SongsList;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Advanced genre metadata extraction from audio files.
 * Extracts genre information from file metadata and intelligent filename analysis.
 */
public class GenreMetadataExtractor {
    
    private static final String TAG = "GenreMetadataExtractor";
    
    // Common genre mappings and patterns
    private static final Map<String, String> GENRE_MAPPINGS = new HashMap<>();
    private static final Map<String, String> FILENAME_PATTERNS = new HashMap<>();
    
    static {
        // Standard genre mappings
        GENRE_MAPPINGS.put("rock", "Rock");
        GENRE_MAPPINGS.put("pop", "Pop");
        GENRE_MAPPINGS.put("jazz", "Jazz");
        GENRE_MAPPINGS.put("blues", "Blues");
        GENRE_MAPPINGS.put("classical", "Classical");
        GENRE_MAPPINGS.put("electronic", "Electronic");
        GENRE_MAPPINGS.put("edm", "Electronic");
        GENRE_MAPPINGS.put("house", "Electronic");
        GENRE_MAPPINGS.put("techno", "Electronic");
        GENRE_MAPPINGS.put("trance", "Electronic");
        GENRE_MAPPINGS.put("dubstep", "Electronic");
        GENRE_MAPPINGS.put("hip hop", "Hip Hop");
        GENRE_MAPPINGS.put("hip-hop", "Hip Hop");
        GENRE_MAPPINGS.put("rap", "Hip Hop");
        GENRE_MAPPINGS.put("r&b", "R&B");
        GENRE_MAPPINGS.put("rnb", "R&B");
        GENRE_MAPPINGS.put("soul", "R&B");
        GENRE_MAPPINGS.put("funk", "Funk");
        GENRE_MAPPINGS.put("disco", "Disco");
        GENRE_MAPPINGS.put("country", "Country");
        GENRE_MAPPINGS.put("folk", "Folk");
        GENRE_MAPPINGS.put("indie", "Indie");
        GENRE_MAPPINGS.put("alternative", "Alternative");
        GENRE_MAPPINGS.put("punk", "Punk");
        GENRE_MAPPINGS.put("metal", "Metal");
        GENRE_MAPPINGS.put("heavy metal", "Metal");
        GENRE_MAPPINGS.put("reggae", "Reggae");
        GENRE_MAPPINGS.put("latin", "Latin");
        GENRE_MAPPINGS.put("salsa", "Latin");
        GENRE_MAPPINGS.put("bachata", "Latin");
        GENRE_MAPPINGS.put("bollywood", "Bollywood");
        GENRE_MAPPINGS.put("hindi", "Bollywood");
        GENRE_MAPPINGS.put("indian", "Bollywood");
        GENRE_MAPPINGS.put("punjabi", "Bollywood");
        GENRE_MAPPINGS.put("devotional", "Devotional");
        GENRE_MAPPINGS.put("bhakti", "Devotional");
        GENRE_MAPPINGS.put("ghazal", "Ghazal");
        GENRE_MAPPINGS.put("qawwali", "Qawwali");
        GENRE_MAPPINGS.put("classical indian", "Classical Indian");
        GENRE_MAPPINGS.put("instrumental", "Instrumental");
        GENRE_MAPPINGS.put("ambient", "Ambient");
        GENRE_MAPPINGS.put("world", "World Music");
        
        // Filename patterns for genre detection
        FILENAME_PATTERNS.put("rock", "Rock");
        FILENAME_PATTERNS.put("pop", "Pop");
        FILENAME_PATTERNS.put("jazz", "Jazz");
        FILENAME_PATTERNS.put("blues", "Blues");
        FILENAME_PATTERNS.put("classical", "Classical");
        FILENAME_PATTERNS.put("electro", "Electronic");
        FILENAME_PATTERNS.put("edm", "Electronic");
        FILENAME_PATTERNS.put("house", "Electronic");
        FILENAME_PATTERNS.put("techno", "Electronic");
        FILENAME_PATTERNS.put("trance", "Electronic");
        FILENAME_PATTERNS.put("dubstep", "Electronic");
        FILENAME_PATTERNS.put("hip", "Hip Hop");
        FILENAME_PATTERNS.put("rap", "Hip Hop");
        FILENAME_PATTERNS.put("r&b", "R&B");
        FILENAME_PATTERNS.put("rnb", "R&B");
        FILENAME_PATTERNS.put("soul", "R&B");
        FILENAME_PATTERNS.put("funk", "Funk");
        FILENAME_PATTERNS.put("disco", "Disco");
        FILENAME_PATTERNS.put("country", "Country");
        FILENAME_PATTERNS.put("folk", "Folk");
        FILENAME_PATTERNS.put("indie", "Indie");
        FILENAME_PATTERNS.put("alt", "Alternative");
        FILENAME_PATTERNS.put("punk", "Punk");
        FILENAME_PATTERNS.put("metal", "Metal");
        FILENAME_PATTERNS.put("reggae", "Reggae");
        FILENAME_PATTERNS.put("latin", "Latin");
        FILENAME_PATTERNS.put("salsa", "Latin");
        FILENAME_PATTERNS.put("bachata", "Latin");
        FILENAME_PATTERNS.put("bollywood", "Bollywood");
        FILENAME_PATTERNS.put("hindi", "Bollywood");
        FILENAME_PATTERNS.put("punjabi", "Bollywood");
        FILENAME_PATTERNS.put("devotional", "Devotional");
        FILENAME_PATTERNS.put("bhakti", "Devotional");
        FILENAME_PATTERNS.put("ghazal", "Ghazal");
        FILENAME_PATTERNS.put("qawwali", "Qawwali");
        FILENAME_PATTERNS.put("instrumental", "Instrumental");
        FILENAME_PATTERNS.put("ambient", "Ambient");
        FILENAME_PATTERNS.put("world", "World Music");
    }
    
    /**
     * Extract genre from song metadata and filename
     */
    public static String extractGenre(SongsList song) {
        try {
            // Method 1: Extract from metadata
            String metadataGenre = extractFromMetadata(song);
            if (metadataGenre != null && !metadataGenre.trim().isEmpty()) {
                String normalizedGenre = normalizeGenre(metadataGenre);
                Log.d(TAG, "Genre from metadata: " + song.getTitle() + " -> " + normalizedGenre);
                return normalizedGenre;
            }
            
            // Method 2: Extract from filename
            String filenameGenre = extractFromFilename(song);
            if (filenameGenre != null && !filenameGenre.trim().isEmpty()) {
                Log.d(TAG, "Genre from filename: " + song.getTitle() + " -> " + filenameGenre);
                return filenameGenre;
            }
            
            // Method 3: Extract from artist name patterns
            String artistGenre = extractFromArtist(song);
            if (artistGenre != null && !artistGenre.trim().isEmpty()) {
                Log.d(TAG, "Genre from artist: " + song.getTitle() + " -> " + artistGenre);
                return artistGenre;
            }
            
            // Method 4: Default to "Unknown"
            Log.d(TAG, "No genre found for: " + song.getTitle() + " -> Unknown");
            return "Unknown";
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting genre for: " + song.getTitle(), e);
            return "Unknown";
        }
    }
    
    /**
     * Extract genre from file metadata
     */
    private static String extractFromMetadata(SongsList song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(song.getPath());
            
            // Try different metadata keys
            String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
            
            if (genre != null && !genre.trim().isEmpty() && !genre.equals("Unknown")) {
                return normalizeGenre(genre);
            }
            
            return null;
            
        } catch (Exception e) {
            Log.w(TAG, "Error reading metadata for: " + song.getPath(), e);
            return null;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing MediaMetadataRetriever", e);
            }
        }
    }
    
    /**
     * Extract genre from filename patterns
     */
    private static String extractFromFilename(SongsList song) {
        String filename = getFileName(song.getPath()).toLowerCase();
        String title = song.getTitle().toLowerCase();
        
        // Check filename for genre patterns
        for (Map.Entry<String, String> pattern : FILENAME_PATTERNS.entrySet()) {
            String keyword = pattern.getKey();
            String genre = pattern.getValue();
            
            // Use word boundaries for more accurate matching
            String patternString = "\\b" + Pattern.quote(keyword) + "\\b";
            
            if (filename.matches(".*" + patternString + ".*") || title.matches(".*" + patternString + ".*")) {
                return genre;
            }
        }
        
        return null;
    }
    
    /**
     * Extract genre from artist name patterns
     */
    private static String extractFromArtist(SongsList song) {
        String artist = song.getArtist().toLowerCase();
        
        // Artist-based genre detection
        if (artist.contains("dj") || artist.contains("producer") || artist.contains("remix")) {
            return "Electronic";
        }
        
        if (artist.contains("band") || artist.contains("orchestra") || artist.contains("symphony")) {
            return "Classical";
        }
        
        if (artist.contains("singer") || artist.contains("playback") || artist.contains("singer")) {
            return "Bollywood";
        }
        
        // Check for typical artist names in specific genres
        String[] rockIndicators = {"beatles", "rolling", "led zeppelin", "pink floyd", "queen"};
        String[] electronicIndicators = {"deadmau5", "skrillex", "diplo", "calvin harris", "tiesto"};
        String[] bollywoodIndicators = {"arjit", "atif", "shreya", "sonu", "alka"};
        
        for (String indicator : rockIndicators) {
            if (artist.contains(indicator)) {
                return "Rock";
            }
        }
        
        for (String indicator : electronicIndicators) {
            if (artist.contains(indicator)) {
                return "Electronic";
            }
        }
        
        for (String indicator : bollywoodIndicators) {
            if (artist.contains(indicator)) {
                return "Bollywood";
            }
        }
        
        return null;
    }
    
    /**
     * Normalize genre string
     */
    private static String normalizeGenre(String genre) {
        if (genre == null) return null;
        
        String normalized = genre.trim().toLowerCase();
        
        // Remove common prefixes/suffixes
        normalized = normalized.replaceAll("^(the |a )", "");
        normalized = normalized.replaceAll("( music| songs| mix| remix| version| original)$", "");
        
        // Apply genre mappings
        for (Map.Entry<String, String> mapping : GENRE_MAPPINGS.entrySet()) {
            String key = mapping.getKey();
            String value = mapping.getValue();
            
            if (normalized.equals(key) || normalized.contains(key)) {
                return value;
            }
        }
        
        // Capitalize first letter of each word
        String[] words = normalized.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Extract file name from path
     */
    private static String getFileName(String filePath) {
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < filePath.length() - 1) {
            String fileName = filePath.substring(lastSlash + 1);
            // Remove extension
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                fileName = fileName.substring(0, lastDot);
            }
            return fileName;
        }
        return filePath;
    }
    
    /**
     * Get genre statistics for a collection of songs
     */
    public static GenreStats getGenreStats(java.util.List<SongsList> songs) {
        Map<String, Integer> genreCounts = new HashMap<>();
        
        for (SongsList song : songs) {
            String genre = extractGenre(song);
            genreCounts.put(genre, genreCounts.getOrDefault(genre, 0) + 1);
        }
        
        return new GenreStats(genreCounts, songs.size());
    }
    
    /**
     * Genre statistics
     */
    public static class GenreStats {
        public final Map<String, Integer> genreCounts;
        public final int totalSongs;
        public final String mostCommonGenre;
        public final int genreVariety;
        
        public GenreStats(Map<String, Integer> genreCounts, int totalSongs) {
            this.genreCounts = genreCounts;
            this.totalSongs = totalSongs;
            this.genreVariety = genreCounts.size();
            
            // Find most common genre
            String mostCommon = "Unknown";
            int maxCount = 0;
            
            for (Map.Entry<String, Integer> entry : genreCounts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    mostCommon = entry.getKey();
                }
            }
            
            this.mostCommonGenre = mostCommon;
        }
        
        public String getSummary() {
            return String.format("%d genres, most common: %s (%d songs)", 
                               genreVariety, mostCommonGenre, 
                               genreCounts.getOrDefault(mostCommonGenre, 0));
        }
        
        public double getGenrePercentage(String genre) {
            if (totalSongs == 0) return 0.0;
            return (double) genreCounts.getOrDefault(genre, 0) / totalSongs * 100;
        }
    }
    
    /**
     * Validate genre extraction
     */
    public static boolean isValidGenre(String genre) {
        if (genre == null || genre.trim().isEmpty()) {
            return false;
        }
        
        String normalized = genre.toLowerCase().trim();
        
        // Check against known genres
        for (String knownGenre : GENRE_MAPPINGS.values()) {
            if (knownGenre.toLowerCase().equals(normalized)) {
                return true;
            }
        }
        
        // Allow custom genres but exclude obvious non-genres
        String[] nonGenres = {"unknown", "various", "multiple", "mixed", "other", "none"};
        
        for (String nonGenre : nonGenres) {
            if (normalized.equals(nonGenre)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get all supported genres
     */
    public static String[] getSupportedGenres() {
        return GENRE_MAPPINGS.values().toArray(new String[0]);
    }
    
    /**
     * Batch extract genres for multiple songs
     */
    public static Map<String, String> batchExtractGenres(java.util.List<SongsList> songs) {
        Map<String, String> results = new HashMap<>();
        
        for (SongsList song : songs) {
            String genre = extractGenre(song);
            results.put(song.getPath(), genre);
        }
        
        return results;
    }
}
