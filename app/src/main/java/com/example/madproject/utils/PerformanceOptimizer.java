package com.example.madproject.utils;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import com.example.madproject.database.MoodOperations;
import com.example.madproject.models.SongsList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive caching and performance optimization system.
 * Provides efficient caching, memory management, and performance monitoring.
 */
public class PerformanceOptimizer {
    
    private static final String TAG = "PerformanceOptimizer";
    
    // Cache sizes
    private static final int MOOD_CACHE_SIZE = 500;
    private static final int AUDIO_ANALYSIS_CACHE_SIZE = 200;
    private static final int GENRE_CACHE_SIZE = 1000;
    
    // Performance thresholds
    private static final long ANALYSIS_TIMEOUT_MS = 10000; // 10 seconds
    private static final int MAX_CONCURRENT_ANALYSES = 3;
    private static final long MEMORY_CLEANUP_INTERVAL_MS = 300000; // 5 minutes
    
    private static volatile PerformanceOptimizer instance;
    private final Context context;
    
    // Caches
    private final LruCache<String, String> moodCache;
    private final LruCache<String, AdvancedAudioAnalyzer.AudioAnalysis> audioAnalysisCache;
    private final LruCache<String, String> genreCache;
    
    // Thread pool for background operations
    private final ExecutorService analysisExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Performance monitoring
    private final Map<String, PerformanceMetrics> performanceMetrics;
    private long lastCleanupTime;
    
    private PerformanceOptimizer(Context context) {
        this.context = context;
        
        // Initialize caches
        this.moodCache = new LruCache<>(MOOD_CACHE_SIZE);
        this.audioAnalysisCache = new LruCache<>(AUDIO_ANALYSIS_CACHE_SIZE);
        this.genreCache = new LruCache<>(GENRE_CACHE_SIZE);
        
        // Initialize thread pools
        this.analysisExecutor = Executors.newFixedThreadPool(MAX_CONCURRENT_ANALYSES);
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        
        // Initialize performance monitoring
        this.performanceMetrics = new ConcurrentHashMap<>();
        this.lastCleanupTime = System.currentTimeMillis();
        
        // Start periodic cleanup
        startPeriodicCleanup();
    }
    
    public static PerformanceOptimizer getInstance(Context context) {
        if (instance == null) {
            synchronized (PerformanceOptimizer.class) {
                if (instance == null) {
                    instance = new PerformanceOptimizer(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Get mood tag with caching
     */
    public String getMoodTag(String songPath) {
        // Check cache first
        String cachedMood = moodCache.get(songPath);
        if (cachedMood != null) {
            recordCacheHit("mood");
            return cachedMood;
        }
        
        // Cache miss - fetch from database
        recordCacheMiss("mood");
        MoodOperations moodOps = new MoodOperations(context);
        String mood = moodOps.getMoodTag(songPath);
        
        // Cache the result
        if (mood != null) {
            cacheMoodTag(songPath, mood);
        }
        
        return mood;
    }
    
    /**
     * Cache mood tag
     */
    public void cacheMoodTag(String songPath, String mood) {
        scheduledExecutor.execute(() -> {
            moodCache.put(songPath, mood);
            recordCacheOperation("mood_cache_put");
        });
    }
    
    /**
     * Get audio analysis with caching
     */
    public AdvancedAudioAnalyzer.AudioAnalysis getAudioAnalysis(String songPath) {
        // Check cache first
        AdvancedAudioAnalyzer.AudioAnalysis cachedAnalysis = audioAnalysisCache.get(songPath);
        if (cachedAnalysis != null) {
            recordCacheHit("audio_analysis");
            return cachedAnalysis;
        }
        
        recordCacheMiss("audio_analysis");
        return null; // Will be computed by caller
    }
    
    /**
     * Cache audio analysis
     */
    public void cacheAudioAnalysis(String songPath, AdvancedAudioAnalyzer.AudioAnalysis analysis) {
        scheduledExecutor.execute(() -> {
            audioAnalysisCache.put(songPath, analysis);
            recordCacheOperation("audio_analysis_cache_put");
        });
    }
    
    /**
     * Get genre with caching
     */
    public String getGenre(SongsList song) {
        String cacheKey = generateGenreCacheKey(song);
        
        // Check cache first
        String cachedGenre = genreCache.get(cacheKey);
        if (cachedGenre != null) {
            recordCacheHit("genre");
            return cachedGenre;
        }
        
        recordCacheMiss("genre");
        
        // Cache miss - extract genre
        String genre = GenreMetadataExtractor.extractGenre(song);
        
        // Cache the result
        if (genre != null && !genre.equals("Unknown")) {
            genreCache.put(cacheKey, genre);
        }
        
        return genre;
    }
    
    /**
     * Perform audio analysis with timeout and caching
     */
    public Future<AdvancedAudioAnalyzer.AudioAnalysis> performAudioAnalysis(String songPath) {
        long startTime = System.currentTimeMillis();
        
        return analysisExecutor.submit(() -> {
            try {
                // Check if analysis would exceed timeout
                if (System.currentTimeMillis() - startTime > ANALYSIS_TIMEOUT_MS) {
                    Log.w(TAG, "Analysis timeout for: " + songPath);
                    return null;
                }
                
                // Check cache first
                AdvancedAudioAnalyzer.AudioAnalysis cached = getAudioAnalysis(songPath);
                if (cached != null) {
                    return cached;
                }
                
                // Perform actual analysis
                AdvancedAudioAnalyzer analyzer = new AdvancedAudioAnalyzer();
                AdvancedAudioAnalyzer.AudioAnalysis analysis = analyzer.analyzeAudio(songPath);
                
                // Cache the result
                if (analysis != null) {
                    cacheAudioAnalysis(songPath, analysis);
                }
                
                long duration = System.currentTimeMillis() - startTime;
                recordAnalysisPerformance(songPath, duration, analysis != null);
                
                return analysis;
                
            } catch (Exception e) {
                Log.e(TAG, "Error in audio analysis: " + songPath, e);
                recordAnalysisPerformance(songPath, System.currentTimeMillis() - startTime, false);
                return null;
            }
        });
    }
    
    /**
     * Batch analyze songs with performance optimization
     */
    public Map<String, Future<AdvancedAudioAnalyzer.AudioAnalysis>> batchAnalyzeSongs(List<String> songPaths) {
        Map<String, Future<AdvancedAudioAnalyzer.AudioAnalysis>> futures = new HashMap<>();
        
        // Submit all analyses
        for (String songPath : songPaths) {
            futures.put(songPath, performAudioAnalysis(songPath));
        }
        
        return futures;
    }

    /**
     * Analyze audio features for a batch of songs.
     * This is used by the performance test suite.
     */
    public CompletableFuture<Void> analyzeAudioFeatures(String primaryPath, String[] otherPaths) {
        return CompletableFuture.runAsync(() -> {
            List<String> allPaths = new ArrayList<>();
            if (primaryPath != null) allPaths.add(primaryPath);
            if (otherPaths != null) {
                for (String path : otherPaths) {
                    if (!allPaths.contains(path)) {
                        allPaths.add(path);
                    }
                }
            }
            
            Map<String, Future<AdvancedAudioAnalyzer.AudioAnalysis>> futures = batchAnalyzeSongs(allPaths);
            for (Future<AdvancedAudioAnalyzer.AudioAnalysis> future : futures.values()) {
                try {
                    future.get(); // Wait for each analysis in the batch to complete
                } catch (Exception e) {
                    Log.e(TAG, "Error waiting for batch analysis", e);
                }
            }
        }, analysisExecutor);
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        scheduledExecutor.execute(() -> {
            moodCache.evictAll();
            audioAnalysisCache.evictAll();
            genreCache.evictAll();
            
            Log.i(TAG, "All caches cleared");
            recordCacheOperation("all_caches_cleared");
        });
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
                moodCache.size(),
                audioAnalysisCache.size(),
                genreCache.size(),
                getCacheHitRate("mood"),
                getCacheHitRate("audio_analysis"),
                getCacheHitRate("genre")
        );
    }
    
    /**
     * Get performance statistics
     */
    public PerformanceStats getPerformanceStats() {
        Map<String, PerformanceMetrics> metrics = new HashMap<>(performanceMetrics);
        return new PerformanceStats(metrics);
    }
    
    /**
     * Optimize memory usage
     */
    public void optimizeMemory() {
        scheduledExecutor.execute(() -> {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            double memoryUsage = (double) usedMemory / maxMemory;
            
            Log.d(TAG, String.format("Memory usage: %.1f%% (%d MB used / %d MB max)",
                    memoryUsage * 100,
                    usedMemory / (1024 * 1024),
                    maxMemory / (1024 * 1024)));
            
            // If memory usage is high, clear some caches
            if (memoryUsage > 0.8) {
                Log.w(TAG, "High memory usage, clearing caches");
                audioAnalysisCache.evictAll(); // Clear largest cache first
                genreCache.trimToSize(GENRE_CACHE_SIZE / 2);
            }
            
            recordMemoryUsage(memoryUsage);
        });
    }
    
    /**
     * Preload common moods into cache
     */
    public void preloadCommonMoods(List<String> commonSongPaths) {
        scheduledExecutor.execute(() -> {
            MoodOperations moodOps = new MoodOperations(context);
            int preloaded = 0;
            
            for (String songPath : commonSongPaths) {
                if (moodCache.get(songPath) == null) {
                    String mood = moodOps.getMoodTag(songPath);
                    if (mood != null) {
                        moodCache.put(songPath, mood);
                        preloaded++;
                    }
                    
                    // Limit preloading to avoid blocking
                    if (preloaded >= 50) {
                        break;
                    }
                }
            }
            
            Log.i(TAG, "Preloaded " + preloaded + " mood tags into cache");
            recordCacheOperation("mood_preload_" + preloaded);
        });
    }
    
    // Private helper methods
    
    private String generateGenreCacheKey(SongsList song) {
        return song.getArtist() + "|" + song.getAlbum() + "|" + song.getTitle();
    }
    
    private void recordCacheHit(String cacheType) {
        updateMetrics(cacheType + "_hits", 1);
    }
    
    private void recordCacheMiss(String cacheType) {
        updateMetrics(cacheType + "_misses", 1);
    }
    
    private void recordCacheOperation(String operation) {
        updateMetrics("cache_operations", 1);
    }
    
    private void recordAnalysisPerformance(String songPath, long duration, boolean success) {
        String key = "analysis_performance";
        PerformanceMetrics metrics = performanceMetrics.get(key);
        if (metrics == null) {
            metrics = new PerformanceMetrics();
            performanceMetrics.put(key, metrics);
        }
        
        metrics.addSample(duration, success);
        
        if (duration > 5000) { // 5 seconds
            Log.w(TAG, "Slow analysis detected: " + songPath + " took " + duration + "ms");
        }
    }
    
    private void recordMemoryUsage(double usage) {
        updateMetrics("memory_usage", usage);
    }
    
    private void updateMetrics(String key, double value) {
        PerformanceMetrics metrics = performanceMetrics.get(key);
        if (metrics == null) {
            metrics = new PerformanceMetrics();
            performanceMetrics.put(key, metrics);
        }
        metrics.addValue(value);
    }
    
    private double getCacheHitRate(String cacheType) {
        long hits = getMetricValue(cacheType + "_hits");
        long misses = getMetricValue(cacheType + "_misses");
        long total = hits + misses;
        
        return total > 0 ? (double) hits / total : 0.0;
    }
    
    private long getMetricValue(String key) {
        PerformanceMetrics metrics = performanceMetrics.get(key);
        return metrics != null ? (long) metrics.total : 0;
    }
    
    private void startPeriodicCleanup() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCleanupTime > MEMORY_CLEANUP_INTERVAL_MS) {
                optimizeMemory();
                lastCleanupTime = currentTime;
            }
        }, MEMORY_CLEANUP_INTERVAL_MS, MEMORY_CLEANUP_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    // Data classes
    
    public static class CacheStats {
        public final int moodCacheSize;
        public final int audioAnalysisCacheSize;
        public final int genreCacheSize;
        public final double moodCacheHitRate;
        public final double audioAnalysisCacheHitRate;
        public final double genreCacheHitRate;
        
        public CacheStats(int moodCacheSize, int audioAnalysisCacheSize, int genreCacheSize,
                        double moodCacheHitRate, double audioAnalysisCacheHitRate, double genreCacheHitRate) {
            this.moodCacheSize = moodCacheSize;
            this.audioAnalysisCacheSize = audioAnalysisCacheSize;
            this.genreCacheSize = genreCacheSize;
            this.moodCacheHitRate = moodCacheHitRate;
            this.audioAnalysisCacheHitRate = audioAnalysisCacheHitRate;
            this.genreCacheHitRate = genreCacheHitRate;
        }
        
        public String getSummary() {
            return String.format("Cache Stats:\n" +
                    "  Mood: %d entries (%.1f%% hit rate)\n" +
                    "  Audio Analysis: %d entries (%.1f%% hit rate)\n" +
                    "  Genre: %d entries (%.1f%% hit rate)",
                    moodCacheSize, moodCacheHitRate * 100,
                    audioAnalysisCacheSize, audioAnalysisCacheHitRate * 100,
                    genreCacheSize, genreCacheHitRate * 100);
        }
    }
    
    public static class PerformanceStats {
        public final Map<String, PerformanceMetrics> metrics;
        
        public PerformanceStats(Map<String, PerformanceMetrics> metrics) {
            this.metrics = new HashMap<>(metrics);
        }
        
        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Performance Statistics:\n");
            
            for (Map.Entry<String, PerformanceMetrics> entry : metrics.entrySet()) {
                summary.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue().getSummary()));
            }
            
            return summary.toString();
        }
    }
    
    private static class PerformanceMetrics {
        public double total = 0.0;
        public double count = 0.0;
        public double min = Double.MAX_VALUE;
        public double max = Double.MIN_VALUE;
        
        public void addValue(double value) {
            total += value;
            count++;
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        
        public void addSample(long duration, boolean success) {
            addValue(duration);
            if (!success) {
                addValue(1.0); // Track failures
            }
        }
        
        public double getAverage() {
            return count > 0 ? total / count : 0.0;
        }
        
        public String getSummary() {
            return String.format("avg: %.1f, min: %.1f, max: %.1f, samples: %.0f",
                    getAverage(), min, max, count);
        }
    }
}
