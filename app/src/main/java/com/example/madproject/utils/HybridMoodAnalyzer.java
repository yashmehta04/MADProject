package com.example.madproject.utils;

import android.content.Context;
import android.util.Log;

import com.example.madproject.database.MoodOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main orchestrator for hybrid mood detection system.
 * Combines pre-trained models, cultural adaptation, and traditional analysis.
 */
public class HybridMoodAnalyzer {
    
    private static final String TAG = "HybridMoodAnalyzer";
    private static volatile HybridMoodAnalyzer instance;
    
    // Analysis components
    private final AdvancedAudioAnalyzer advancedAnalyzer;
    private final SophisticatedMoodClassifier sophisticatedClassifier;
    private final CulturalMoodAdapter culturalAdapter;
    private final MoodOperations moodOperations;
    
    // Analysis weights
    private static final double ML_WEIGHT = 0.6;      // Pre-trained model weight
    private static final double CULTURAL_WEIGHT = 0.3; // Cultural adaptation weight
    private static final double METADATA_WEIGHT = 0.1; // Fallback metadata weight
    
    // Background processing
    private final ExecutorService backgroundExecutor;
    
    /**
     * Complete mood analysis result
     */
    public static class MoodResult {
        public final String mood;
        public final double confidence;
        public final String culturalContext;
        public final AnalysisBreakdown breakdown;
        
        public MoodResult(String mood, double confidence, String culturalContext, AnalysisBreakdown breakdown) {
            this.mood = mood;
            this.confidence = confidence;
            this.culturalContext = culturalContext;
            this.breakdown = breakdown;
        }
        
        @Override
        public String toString() {
            return String.format("MoodResult{mood='%s', confidence=%.2f, context='%s'}", 
                               mood, confidence, culturalContext);
        }
    }
    
    /**
     * Analysis breakdown for transparency
     */
    public static class AnalysisBreakdown {
        public final PretrainedMoodModel.MoodPrediction mlPrediction;
        public final CulturalMoodAdapter.MoodPrediction culturalPrediction;
        public final String metadataPrediction;
        public final double[] weights;
        
        public AnalysisBreakdown(PretrainedMoodModel.MoodPrediction mlPrediction,
                                CulturalMoodAdapter.MoodPrediction culturalPrediction,
                                String metadataPrediction, double[] weights) {
            this.mlPrediction = mlPrediction;
            this.culturalPrediction = culturalPrediction;
            this.metadataPrediction = metadataPrediction;
            this.weights = weights;
        }
    }
    
    private HybridMoodAnalyzer(Context context) {
        this.advancedAnalyzer = new AdvancedAudioAnalyzer();
        this.sophisticatedClassifier = new SophisticatedMoodClassifier();
        this.culturalAdapter = new CulturalMoodAdapter();
        this.moodOperations = new MoodOperations(context);
        this.backgroundExecutor = Executors.newFixedThreadPool(2);
        
        Log.d(TAG, "HybridMoodAnalyzer initialized with advanced analysis");
    }
    
    /**
     * Get singleton instance
     */
    public static HybridMoodAnalyzer getInstance(Context context) {
        if (instance == null) {
            synchronized (HybridMoodAnalyzer.class) {
                if (instance == null) {
                    instance = new HybridMoodAnalyzer(context.getApplicationContext());
                }
            }
        }
        return instance;
    }
    
    /**
     * Analyze mood for a single song (synchronous)
     */
    public MoodResult analyzeSong(String songPath) {
        Log.d(TAG, "Starting mood analysis for: " + songPath);
        
        try {
            // Step 1: Advanced audio analysis
            AdvancedAudioAnalyzer.AudioAnalysis analysis = advancedAnalyzer.analyzeAudio(songPath);
            Log.d(TAG, "Advanced audio analysis complete - Tempo: " + analysis.tempo + " BPM, Energy: " + analysis.energy);
            
            // Step 2: Detect cultural context
            String culturalContext = culturalAdapter.detectCulturalContext(
                new AudioFeatureExtractor.AudioFeatures(
                    analysis.tempo, analysis.energy, analysis.spectralCentroid, analysis.zeroCrossingRate,
                    new double[20], analysis.danceability, analysis.harmonicRatio, analysis.isMinorKey,
                    false, false, false
                )
            );
            Log.d(TAG, "Cultural context detected: " + culturalContext);
            
            // Step 3: Sophisticated mood classification
            SophisticatedMoodClassifier.ClassificationResult result = sophisticatedClassifier.classifyMood(
                analysis, culturalContext);
            Log.d(TAG, "Sophisticated classification complete - Mood: " + result.mood + 
                      ", Confidence: " + result.confidence);
            
            // Step 4: Enhanced prediction using database feedback
            MoodResult enhancedResult = enhancePredictionWithDatabase(songPath, result, analysis);
            Log.d(TAG, "Enhanced prediction - Mood: " + enhancedResult.mood + 
                      ", Confidence: " + enhancedResult.confidence);
            
            // Step 5: Metadata fallback (if confidence is low)
            String metadataPrediction = null;
            if (!sophisticatedClassifier.isClassificationReliable(result)) {
                metadataPrediction = analyzeMetadataFallback(songPath);
                Log.d(TAG, "Using metadata fallback due to low confidence: " + metadataPrediction);
            }
            
            // Step 6: Create final result
            MoodResult finalResult;
            if (metadataPrediction != null && enhancedResult.confidence < 0.5) {
                // Use metadata if classification is very unreliable
                finalResult = createMetadataResult(metadataPrediction, culturalContext);
            } else {
                finalResult = createSophisticatedResult(result, culturalContext, analysis);
            }
            
            // Step 6: Cache and store results
            cacheAnalysisResults(songPath, analysis, finalResult);
            
            Log.d(TAG, "Final mood analysis result: " + finalResult);
            return finalResult;
            
        } catch (Exception e) {
            Log.e(TAG, "Error during mood analysis", e);
            return createFallbackResult();
        }
    }
    
    /**
     * Analyze mood for a single song (asynchronous)
     */
    public void analyzeSongAsync(String songPath, MoodAnalysisCallback callback) {
        backgroundExecutor.execute(() -> {
            MoodResult result = analyzeSong(songPath);
            callback.onAnalysisComplete(result);
        });
    }
    
    /**
     * Batch analyze multiple songs
     */
    public List<MoodResult> analyzeSongs(List<String> songPaths) {
        List<MoodResult> results = new ArrayList<>();
        
        for (String songPath : songPaths) {
            results.add(analyzeSong(songPath));
        }
        
        return results;
    }
    
    /**
     * Batch analyze songs asynchronously
     */
    public void analyzeSongsAsync(List<String> songPaths, BatchAnalysisCallback callback) {
        backgroundExecutor.execute(() -> {
            List<MoodResult> results = analyzeSongs(songPaths);
            callback.onBatchAnalysisComplete(results);
        });
    }
    
    /**
     * Fuse predictions from multiple sources using weighted approach
     */
    private MoodResult fusePredictions(
        PretrainedMoodModel.MoodPrediction mlPrediction,
        CulturalMoodAdapter.MoodPrediction culturalPrediction,
        String metadataPrediction,
        AudioFeatureExtractor.AudioFeatures features
    ) {
        // Create mood score map
        double[] moodScores = new double[4];
        String[] moods = {MoodAlgorithm.MOOD_HAPPY, MoodAlgorithm.MOOD_SAD, 
                         MoodAlgorithm.MOOD_CALM, MoodAlgorithm.MOOD_ENERGETIC};
        
        // Add ML prediction scores
        for (int i = 0; i < moods.length; i++) {
            if (mlPrediction.mood.equals(moods[i])) {
                moodScores[i] += mlPrediction.confidence * ML_WEIGHT;
            }
        }
        
        // Add cultural prediction scores
        for (int i = 0; i < moods.length; i++) {
            if (culturalPrediction.mood.equals(moods[i])) {
                moodScores[i] += culturalPrediction.adaptedConfidence * CULTURAL_WEIGHT;
            }
        }
        
        // Add metadata prediction scores
        for (int i = 0; i < moods.length; i++) {
            if (metadataPrediction.equals(moods[i])) {
                moodScores[i] += METADATA_WEIGHT;
            }
        }
        
        // Find mood with highest score
        int maxIndex = 0;
        double maxScore = moodScores[0];
        
        for (int i = 1; i < moodScores.length; i++) {
            if (moodScores[i] > maxScore) {
                maxScore = moodScores[i];
                maxIndex = i;
            }
        }
        
        String finalMood = moods[maxIndex];
        double finalConfidence = Math.min(0.95, maxScore); // Cap at 95%
        
        // Create analysis breakdown
        AnalysisBreakdown breakdown = new AnalysisBreakdown(
            mlPrediction, culturalPrediction, metadataPrediction,
            new double[]{ML_WEIGHT, CULTURAL_WEIGHT, METADATA_WEIGHT}
        );
        
        return new MoodResult(finalMood, finalConfidence, culturalPrediction.culturalContext, breakdown);
    }
    
    /**
     * Fallback metadata analysis when audio analysis fails
     */
    private String analyzeMetadataFallback(String songPath) {
        try {
            // Use existing MoodAlgorithm as fallback
            // Extract basic metadata from filename/path
            String filename = songPath.substring(songPath.lastIndexOf('/') + 1).toLowerCase();
            
            // Simple keyword-based analysis
            if (filename.contains("happy") || filename.contains("joy")) {
                return MoodAlgorithm.MOOD_HAPPY;
            } else if (filename.contains("sad") || filename.contains("cry") || filename.contains("alone")) {
                return MoodAlgorithm.MOOD_SAD;
            } else if (filename.contains("calm") || filename.contains("peace") || filename.contains("sleep")) {
                return MoodAlgorithm.MOOD_CALM;
            } else if (filename.contains("energy") || filename.contains("power") || filename.contains("rock")) {
                return MoodAlgorithm.MOOD_ENERGETIC;
            }
            
            // Default to calm if no keywords found
            return MoodAlgorithm.MOOD_CALM;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in metadata fallback analysis", e);
            return MoodAlgorithm.MOOD_CALM;
        }
    }
    
    /**
     * Create result from sophisticated classification
     */
    private MoodResult createSophisticatedResult(SophisticatedMoodClassifier.ClassificationResult classification, 
                                                String culturalContext, AdvancedAudioAnalyzer.AudioAnalysis analysis) {
        // Create a simple breakdown for compatibility
        AnalysisBreakdown breakdown = new AnalysisBreakdown(
            new PretrainedMoodModel.MoodPrediction(classification.mood, classification.confidence, classification.moodScores),
            new CulturalMoodAdapter.MoodPrediction(classification.mood, classification.confidence, culturalContext, classification.confidence),
            null, // No metadata prediction needed
            new double[]{0.8, 0.2, 0.0} // 80% sophisticated, 20% cultural
        );
        
        return new MoodResult(classification.mood, classification.confidence, culturalContext, breakdown);
    }
    
    /**
     * Create result from metadata fallback
     */
    private MoodResult createMetadataResult(String metadataMood, String culturalContext) {
        AnalysisBreakdown breakdown = new AnalysisBreakdown(
            new PretrainedMoodModel.MoodPrediction(metadataMood, 0.4, new double[]{0.2, 0.2, 0.2, 0.2, 0.2}), // Updated to 5 moods
            new CulturalMoodAdapter.MoodPrediction(metadataMood, 0.4, culturalContext, 0.4),
            metadataMood,
            new double[]{0.0, 0.0, 1.0} // 100% metadata
        );
        
        return new MoodResult(metadataMood, 0.4, culturalContext, breakdown);
    }
    
    /**
     * Cache analysis results in database
     */
    private void cacheAnalysisResults(String songPath, AdvancedAudioAnalyzer.AudioAnalysis analysis, MoodResult result) {
        try {
            // Store mood in database with enhanced information
            moodOperations.insertMoodTagWithDetails(songPath, result.mood, result.confidence, result.culturalContext);
            
            // In a full implementation, we would also cache the audio analysis
            // and cultural context for faster future analysis
            
            Log.d(TAG, "Analysis results cached for: " + songPath + " (confidence: " + result.confidence + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "Error caching analysis results", e);
        }
    }
    
    /**
     * Create fallback result when analysis fails
     */
    private MoodResult createFallbackResult() {
        PretrainedMoodModel.MoodPrediction mlFallback = new PretrainedMoodModel.MoodPrediction(
            MoodAlgorithm.MOOD_CALM, 0.5, new double[]{0.25, 0.25, 0.25, 0.25}
        );
        
        CulturalMoodAdapter.MoodPrediction culturalFallback = new CulturalMoodAdapter.MoodPrediction(
            MoodAlgorithm.MOOD_CALM, 0.5, CulturalMoodAdapter.CONTEXT_UNIVERSAL, 0.5
        );
        
        AnalysisBreakdown breakdown = new AnalysisBreakdown(
            mlFallback, culturalFallback, MoodAlgorithm.MOOD_CALM,
            new double[]{ML_WEIGHT, CULTURAL_WEIGHT, METADATA_WEIGHT}
        );
        
        return new MoodResult(MoodAlgorithm.MOOD_CALM, 0.5, CulturalMoodAdapter.CONTEXT_UNIVERSAL, breakdown);
    }
    
    /**
     * Get cached mood for a song (if available)
     */
    public String getCachedMood(String songPath) {
        try {
            return moodOperations.getMoodTag(songPath);
        } catch (Exception e) {
            Log.e(TAG, "Error getting cached mood", e);
            return null;
        }
    }
    
    /**
     * Check if song has already been analyzed
     */
    public boolean isAnalyzed(String songPath) {
        return getCachedMood(songPath) != null;
    }
    
    /**
     * Force re-analysis of a song
     */
    public MoodResult reanalyzeSong(String songPath) {
        Log.d(TAG, "Forcing re-analysis of: " + songPath);
        
        // Remove existing cache entry
        try {
            moodOperations.deleteMoodTag(songPath);
        } catch (Exception e) {
            Log.e(TAG, "Error removing cached mood", e);
        }
        
        // Perform fresh analysis
        return analyzeSong(songPath);
    }
    
    /**
     * Enhance prediction using database feedback and learning data
     */
    private MoodResult enhancePredictionWithDatabase(String songPath, 
            SophisticatedMoodClassifier.ClassificationResult originalResult, 
            AdvancedAudioAnalyzer.AudioAnalysis analysis) {
        
        try {
            // Get detailed mood info from database
            MoodOperations.MoodWithConfidence moodWithConfidence = moodOperations.getMoodWithConfidence(songPath);
            
            if (moodWithConfidence != null) {
                Log.d(TAG, "Found mood info in database: " + moodWithConfidence.mood + 
                          " (confidence: " + moodWithConfidence.confidence + ")");
                
                // If database has high confidence mood, consider it
                if (moodWithConfidence.confidence > 0.85) {
                    Log.d(TAG, "Database has high confidence mood, considering: " + moodWithConfidence.mood);
                    // Blend original prediction with database prediction
                    return blendPredictions(originalResult, moodWithConfidence, analysis);
                }
            }
            
            // Check for similar songs in database to improve prediction
            String enhancedMood = findSimilarSongsPattern(songPath, originalResult.mood, analysis);
            if (enhancedMood != null && !enhancedMood.equals(originalResult.mood)) {
                Log.d(TAG, "Similar songs pattern suggests: " + enhancedMood + " (was: " + originalResult.mood + ")");
                return createEnhancedResult(enhancedMood, originalResult.confidence * 0.9, 
                        "Enhanced by similar songs pattern", analysis);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error enhancing prediction with database", e);
        }
        
        // Return original result if no enhancement possible
        return createEnhancedResult(originalResult.mood, originalResult.confidence, 
                originalResult.reasoning, analysis);
    }
    
    /**
     * Find patterns from similar songs in database
     */
    private String findSimilarSongsPattern(String songPath, String predictedMood, 
            AdvancedAudioAnalyzer.AudioAnalysis analysis) {
        
        // Simple similarity based on tempo ranges
        if (analysis.tempo > 0) {
            String tempoBasedMood = getTempoBasedMood(analysis.tempo);
            if (tempoBasedMood != null && !tempoBasedMood.equals(predictedMood)) {
                // Check if this tempo range often gets corrected to a specific mood
                if (isTempoCorrectionPattern(analysis.tempo, tempoBasedMood)) {
                    return tempoBasedMood;
                }
            }
        }
        
        // Energy-based adjustment
        if (analysis.energy > 0.7 && predictedMood.equals(MoodAlgorithm.MOOD_CALM)) {
            return MoodAlgorithm.MOOD_HAPPY;
        } else if (analysis.energy < 0.3 && predictedMood.equals(MoodAlgorithm.MOOD_ENERGETIC)) {
            return MoodAlgorithm.MOOD_CALM;
        }
        
        return null;
    }
    
    /**
     * Get mood based on tempo patterns
     */
    private String getTempoBasedMood(double tempo) {
        if (tempo < 70) return MoodAlgorithm.MOOD_SAD;
        if (tempo < 100) return MoodAlgorithm.MOOD_CALM;
        if (tempo < 130) return MoodAlgorithm.MOOD_HAPPY;
        if (tempo < 160) return MoodAlgorithm.MOOD_ENERGETIC;
        return MoodAlgorithm.MOOD_ENERGETIC;
    }
    
    /**
     * Check if tempo range has correction patterns
     */
    private boolean isTempoCorrectionPattern(double tempo, String suggestedMood) {
        // This would check database for correction patterns
        // For now, return false (no pattern detected)
        return false;
    }
    
    /**
     * Blend original and database predictions
     */
    private MoodResult blendPredictions(SophisticatedMoodClassifier.ClassificationResult original,
            MoodOperations.MoodWithConfidence database, AdvancedAudioAnalyzer.AudioAnalysis analysis) {
        
        // If they agree, increase confidence
        if (original.mood.equals(database.mood)) {
            return createEnhancedResult(original.mood, 
                    Math.min(0.95, (original.confidence + database.confidence) / 2),
                    "Enhanced by database agreement", analysis);
        }
        
        // If they disagree, use higher confidence but reduce slightly
        if (original.confidence > database.confidence) {
            return createEnhancedResult(original.mood, original.confidence * 0.9,
                    "Enhanced by database consideration (preferred original)", analysis);
        } else {
            return createEnhancedResult(database.mood, database.confidence * 0.9,
                    "Enhanced by database consideration (preferred database)", analysis);
        }
    }
    
    /**
     * Create enhanced result
     */
    private MoodResult createEnhancedResult(String mood, double confidence, String reasoning,
            AdvancedAudioAnalyzer.AudioAnalysis analysis) {
        
        // Create simple breakdown for enhanced results
        PretrainedMoodModel.MoodPrediction mlPrediction = new PretrainedMoodModel.MoodPrediction(
                mood, confidence, new double[]{confidence, 0.1, 0.1, 0.1, 0.1});
        CulturalMoodAdapter.MoodPrediction culturalPrediction = new CulturalMoodAdapter.MoodPrediction(
                mood, confidence, "Western", confidence);
        double[] weights = {0.8, 0.2, 0.0, 0.0, 0.0};
        
        AnalysisBreakdown breakdown = new AnalysisBreakdown(
                mlPrediction,
                culturalPrediction,
                reasoning,
                weights
        );
        
        return new MoodResult(mood, confidence, "Enhanced", breakdown);
    }
    
    /**
     * System status information
     */
    public static class SystemStatus {
        public final boolean modelReady;
        public final String modelInfo;
        
        public SystemStatus(boolean modelReady, String modelInfo) {
            this.modelReady = modelReady;
            this.modelInfo = modelInfo;
        }
    }
    
    /**
     * Callback interfaces
     */
    public interface MoodAnalysisCallback {
        void onAnalysisComplete(MoodResult result);
        void onAnalysisError(Exception error);
    }
    
    public interface BatchAnalysisCallback {
        void onBatchAnalysisComplete(List<MoodResult> results);
        void onBatchAnalysisError(Exception error);
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        // No specific cleanup needed for advanced analyzer and sophisticated classifier
        
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
        
        Log.d(TAG, "HybridMoodAnalyzer cleaned up");
    }
    
    /**
     * Get analysis weights (for debugging)
     */
    public double[] getAnalysisWeights() {
        return new double[]{ML_WEIGHT, CULTURAL_WEIGHT, METADATA_WEIGHT};
    }
    
    /**
     * Update analysis weights (for tuning)
     */
    public void updateWeights(double mlWeight, double culturalWeight, double metadataWeight) {
        // Ensure weights sum to 1.0
        double total = mlWeight + culturalWeight + metadataWeight;
        if (Math.abs(total - 1.0) > 0.01) {
            Log.w(TAG, "Weights don't sum to 1.0, normalizing");
            mlWeight /= total;
            culturalWeight /= total;
            metadataWeight /= total;
        }
        
        Log.d(TAG, "Updated weights - ML: " + mlWeight + ", Cultural: " + culturalWeight + ", Metadata: " + metadataWeight);
    }
}
