package com.example.madproject.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.madproject.database.MoodOperations;
import com.example.madproject.models.SongsList;

import java.util.HashMap;
import java.util.Map;

/**
 * User feedback system for mood correction and learning.
 * Manages user corrections to improve mood detection accuracy over time.
 */
public class UserFeedbackManager {
    
    private static final String TAG = "UserFeedbackManager";
    private static final String PREFS_NAME = "mood_feedback_prefs";
    private static final String FEEDBACK_COUNT_KEY = "feedback_count_";
    private static final String CORRECTION_RATIO_KEY = "correction_ratio_";
    
    private final Context context;
    private final MoodOperations moodOperations;
    private final SharedPreferences preferences;
    
    // Learning data for improving accuracy
    private final Map<String, MoodFeedbackData> feedbackHistory;
    
    public UserFeedbackManager(Context context) {
        this.context = context;
        this.moodOperations = new MoodOperations(context);
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.feedbackHistory = new HashMap<>();
        
        loadFeedbackHistory();
    }
    
    /**
     * Record user feedback for mood correction
     */
    public boolean recordMoodCorrection(String songPath, String predictedMood, String correctedMood, 
                                    double confidence, String reasoning) {
        try {
            Log.i(TAG, "Recording mood correction: " + songPath + 
                      " | Predicted: " + predictedMood + 
                      " | Corrected: " + correctedMood + 
                      " | Confidence: " + confidence);
            
            // Validate inputs
            if (!isValidMood(correctedMood)) {
                Log.e(TAG, "Invalid corrected mood: " + correctedMood);
                return false;
            }
            
            // Update database with user-corrected mood
            boolean dbUpdated = moodOperations.updateMoodTag(songPath, correctedMood, confidence, 
                                                           "User corrected: " + predictedMood + " → " + correctedMood);
            
            if (!dbUpdated) {
                Log.e(TAG, "Failed to update database with corrected mood");
                return false;
            }
            
            // Record feedback for learning
            MoodFeedbackData feedbackData = getOrCreateFeedbackData(songPath, predictedMood);
            feedbackData.addCorrection(correctedMood, confidence, reasoning);
            
            // Update statistics
            updateFeedbackStatistics(predictedMood, correctedMood);
            
            // Save feedback history
            saveFeedbackHistory();
            
            // Apply learning to improve future predictions
            applyLearningFeedback(predictedMood, correctedMood, confidence);
            
            Log.i(TAG, "Mood correction recorded successfully");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Error recording mood correction", e);
            return false;
        }
    }
    
    /**
     * Get feedback data for a song and predicted mood
     */
    private MoodFeedbackData getOrCreateFeedbackData(String songPath, String predictedMood) {
        String key = songPath + "_" + predictedMood;
        
        MoodFeedbackData data = feedbackHistory.get(key);
        if (data == null) {
            data = new MoodFeedbackData(songPath, predictedMood);
            feedbackHistory.put(key, data);
        }
        
        return data;
    }
    
    /**
     * Update feedback statistics
     */
    private void updateFeedbackStatistics(String predictedMood, String correctedMood) {
        // Update feedback count
        int feedbackCount = preferences.getInt(FEEDBACK_COUNT_KEY + predictedMood, 0) + 1;
        preferences.edit()
                .putInt(FEEDBACK_COUNT_KEY + predictedMood, feedbackCount)
                .apply();
        
        // Update correction ratio
        if (!predictedMood.equals(correctedMood)) {
            int corrections = preferences.getInt(CORRECTION_RATIO_KEY + predictedMood, 0) + 1;
            preferences.edit()
                    .putInt(CORRECTION_RATIO_KEY + predictedMood, corrections)
                    .apply();
        }
        
        Log.d(TAG, "Updated statistics for " + predictedMood + 
                  ": " + feedbackCount + " feedbacks, " + 
                  getCorrectionRate(predictedMood) + "% correction rate");
    }
    
    /**
     * Apply learning feedback to improve future predictions
     */
    private void applyLearningFeedback(String predictedMood, String correctedMood, double confidence) {
        // This is where machine learning would be applied
        // For now, we'll implement simple rule-based learning
        
        // If confidence was high but prediction was wrong, reduce weight
        if (confidence > 0.8 && !predictedMood.equals(correctedMood)) {
            Log.i(TAG, "High confidence prediction was wrong, adjusting future weights");
            // In a full ML implementation, this would adjust model weights
        }
        
        // If certain patterns emerge, they can be used to adjust classification rules
        analyzeCorrectionPatterns(predictedMood, correctedMood);
    }
    
    /**
     * Analyze correction patterns for insights
     */
    private void analyzeCorrectionPatterns(String predictedMood, String correctedMood) {
        // Simple pattern analysis - in full implementation, this would be more sophisticated
        if (predictedMood.equals(MoodAlgorithm.MOOD_HAPPY) && correctedMood.equals(MoodAlgorithm.MOOD_CALM)) {
            Log.d(TAG, "Pattern detected: HAPPY often corrected to CALM");
        } else if (predictedMood.equals(MoodAlgorithm.MOOD_SAD) && correctedMood.equals(MoodAlgorithm.MOOD_CALM)) {
            Log.d(TAG, "Pattern detected: SAD often corrected to CALM");
        }
        // Add more pattern analysis as needed
    }
    
    /**
     * Get correction rate for a mood
     */
    public double getCorrectionRate(String mood) {
        int feedbackCount = preferences.getInt(FEEDBACK_COUNT_KEY + mood, 0);
        int corrections = preferences.getInt(CORRECTION_RATIO_KEY + mood, 0);
        
        if (feedbackCount == 0) return 0.0;
        return (double) corrections / feedbackCount * 100;
    }
    
    /**
     * Check if mood prediction should be trusted based on feedback history
     */
    public boolean shouldTrustPrediction(String predictedMood, double confidence) {
        double correctionRate = getCorrectionRate(predictedMood);
        
        // If correction rate is high (>30%), be more cautious
        if (correctionRate > 30.0) {
            return confidence > 0.9; // Require higher confidence
        }
        
        // If correction rate is moderate (15-30%), require good confidence
        if (correctionRate > 15.0) {
            return confidence > 0.7;
        }
        
        // If correction rate is low (<15%), normal confidence is fine
        return confidence > 0.6;
    }
    
    /**
     * Get suggested correction based on feedback patterns
     */
    public String getSuggestedCorrection(String predictedMood, SongsList song) {
        // Simple suggestion logic based on common corrections
        double correctionRate = getCorrectionRate(predictedMood);
        
        if (correctionRate > 40.0) {
            // High correction rate - suggest most common correction
            String mostCommonCorrection = getMostCommonCorrection(predictedMood);
            if (mostCommonCorrection != null) {
                return mostCommonCorrection;
            }
        }
        
        // Check song characteristics for better suggestion
        return getAlternativeMoodSuggestion(predictedMood, song);
    }
    
    /**
     * Get most common correction for a predicted mood
     */
    private String getMostCommonCorrection(String predictedMood) {
        Map<String, Integer> correctionCounts = new HashMap<>();
        
        for (MoodFeedbackData data : feedbackHistory.values()) {
            if (data.predictedMood.equals(predictedMood)) {
                for (String correction : data.correctionCounts.keySet()) {
                    correctionCounts.put(correction, 
                        correctionCounts.getOrDefault(correction, 0) + data.correctionCounts.get(correction));
                }
            }
        }
        
        String mostCommon = null;
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : correctionCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostCommon = entry.getKey();
            }
        }
        
        return mostCommon;
    }
    
    /**
     * Get alternative mood suggestion based on song characteristics
     */
    private String getAlternativeMoodSuggestion(String predictedMood, SongsList song) {
        // Simple heuristic based on song metadata
        String title = song.getTitle().toLowerCase();
        String artist = song.getArtist().toLowerCase();
        
        // Check title/artist for mood indicators
        if (title.contains("sad") || title.contains("cry") || title.contains("breakup")) {
            return MoodAlgorithm.MOOD_SAD;
        }
        
        if (title.contains("happy") || title.contains("joy") || title.contains("celebrate")) {
            return MoodAlgorithm.MOOD_HAPPY;
        }
        
        if (title.contains("party") || title.contains("dance") || title.contains("club")) {
            return MoodAlgorithm.MOOD_PARTY;
        }
        
        if (artist.contains("dj") || artist.contains("remix")) {
            return MoodAlgorithm.MOOD_PARTY;
        }
        
        // Default fallback
        switch (predictedMood) {
            case MoodAlgorithm.MOOD_HAPPY:
                return MoodAlgorithm.MOOD_CALM;
            case MoodAlgorithm.MOOD_SAD:
                return MoodAlgorithm.MOOD_CALM;
            case MoodAlgorithm.MOOD_ENERGETIC:
                return MoodAlgorithm.MOOD_HAPPY;
            case MoodAlgorithm.MOOD_CALM:
                return MoodAlgorithm.MOOD_HAPPY;
            case MoodAlgorithm.MOOD_PARTY:
                return MoodAlgorithm.MOOD_ENERGETIC;
            default:
                return MoodAlgorithm.MOOD_CALM;
        }
    }
    
    /**
     * Validate mood string
     */
    private boolean isValidMood(String mood) {
        return mood.equals(MoodAlgorithm.MOOD_HAPPY) ||
               mood.equals(MoodAlgorithm.MOOD_SAD) ||
               mood.equals(MoodAlgorithm.MOOD_CALM) ||
               mood.equals(MoodAlgorithm.MOOD_ENERGETIC) ||
               mood.equals(MoodAlgorithm.MOOD_PARTY);
    }
    
    /**
     * Load feedback history from preferences
     */
    private void loadFeedbackHistory() {
        // In a full implementation, this would load from database
        // For now, we'll keep it in memory
        Log.d(TAG, "Feedback history loaded");
    }
    
    /**
     * Save feedback history to preferences
     */
    private void saveFeedbackHistory() {
        // In a full implementation, this would save to database
        // For now, we'll just save statistics
        preferences.edit().apply();
        Log.d(TAG, "Feedback history saved");
    }
    
    /**
     * Get feedback statistics summary
     */
    public FeedbackStats getFeedbackStats() {
        Map<String, Integer> totalFeedbacks = new HashMap<>();
        Map<String, Integer> totalCorrections = new HashMap<>();
        
        String[] moods = {MoodAlgorithm.MOOD_HAPPY, MoodAlgorithm.MOOD_SAD, 
                         MoodAlgorithm.MOOD_CALM, MoodAlgorithm.MOOD_ENERGETIC, MoodAlgorithm.MOOD_PARTY};
        
        for (String mood : moods) {
            totalFeedbacks.put(mood, preferences.getInt(FEEDBACK_COUNT_KEY + mood, 0));
            totalCorrections.put(mood, preferences.getInt(CORRECTION_RATIO_KEY + mood, 0));
        }
        
        return new FeedbackStats(totalFeedbacks, totalCorrections);
    }
    
    /**
     * Clear all feedback data
     */
    public void clearFeedbackData() {
        preferences.edit().clear().apply();
        feedbackHistory.clear();
        Log.i(TAG, "All feedback data cleared");
    }
    
    /**
     * Mood feedback data container
     */
    private static class MoodFeedbackData {
        public final String songPath;
        public final String predictedMood;
        public final Map<String, Integer> correctionCounts;
        public int totalCorrections;
        
        public MoodFeedbackData(String songPath, String predictedMood) {
            this.songPath = songPath;
            this.predictedMood = predictedMood;
            this.correctionCounts = new HashMap<>();
            this.totalCorrections = 0;
        }
        
        public void addCorrection(String correctedMood, double confidence, String reasoning) {
            correctionCounts.put(correctedMood, 
                correctionCounts.getOrDefault(correctedMood, 0) + 1);
            totalCorrections++;
            
            Log.d(TAG, "Correction added: " + correctedMood + 
                      " (count: " + correctionCounts.get(correctedMood) + ")");
        }
    }
    
    /**
     * Feedback statistics container
     */
    public static class FeedbackStats {
        public final Map<String, Integer> totalFeedbacks;
        public final Map<String, Integer> totalCorrections;
        
        public FeedbackStats(Map<String, Integer> totalFeedbacks, 
                         Map<String, Integer> totalCorrections) {
            this.totalFeedbacks = totalFeedbacks;
            this.totalCorrections = totalCorrections;
        }
        
        public String getSummary() {
            StringBuilder summary = new StringBuilder();
            summary.append("Feedback Statistics:\n");
            
            for (String mood : totalFeedbacks.keySet()) {
                int total = totalFeedbacks.get(mood);
                int corrections = totalCorrections.getOrDefault(mood, 0);
                double rate = total > 0 ? (double) corrections / total * 100 : 0;
                
                summary.append(String.format("  %s: %d feedbacks, %d corrections (%.1f%%)\n",
                           mood, total, corrections, rate));
            }
            
            return summary.toString();
        }
    }
}
