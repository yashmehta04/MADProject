package com.example.madproject.utils;

import android.util.Log;

/**
 * Cultural mood adaptation system for Bollywood vs Western music patterns.
 * Adjusts mood predictions based on cultural context and musical characteristics.
 */
public class CulturalMoodAdapter {
    
    private static final String TAG = "CulturalMoodAdapter";
    
    // Cultural context constants
    public static final String CONTEXT_BOLLYWOOD = "BOLLYWOOD";
    public static final String CONTEXT_WESTERN = "WESTERN";
    public static final String CONTEXT_UNIVERSAL = "UNIVERSAL";
    
    /**
     * Mood prediction with cultural adaptation
     */
    public static class MoodPrediction {
        public final String mood;
        public final double confidence;
        public final String culturalContext;
        public final double adaptedConfidence;
        
        public MoodPrediction(String mood, double confidence, String culturalContext, double adaptedConfidence) {
            this.mood = mood;
            this.confidence = confidence;
            this.culturalContext = culturalContext;
            this.adaptedConfidence = adaptedConfidence;
        }
    }
    
    /**
     * Detect cultural context from audio features
     */
    public String detectCulturalContext(AudioFeatureExtractor.AudioFeatures features) {
        // Bollywood music patterns
        if (features.complexRhythm && features.sitarDominant) {
            return CONTEXT_BOLLYWOOD;
        }
        
        // Western music patterns  
        if (features.guitarDominant && !features.complexRhythm) {
            return CONTEXT_WESTERN;
        }
        
        // Universal music (mixed or unclear)
        return CONTEXT_UNIVERSAL;
    }
    
    /**
     * Adapt mood prediction based on cultural context
     */
    public MoodPrediction adaptForCulturalContext(
        MoodPrediction basePrediction, 
        AudioFeatureExtractor.AudioFeatures features
    ) {
        String culturalContext = detectCulturalContext(features);
        String adaptedMood = basePrediction.mood;
        double adaptedConfidence = basePrediction.confidence;
        
        switch (culturalContext) {
            case CONTEXT_BOLLYWOOD:
                adaptedMood = adaptForBollywood(basePrediction.mood, features);
                adaptedConfidence = Math.min(0.95, basePrediction.confidence * 1.1);
                break;
                
            case CONTEXT_WESTERN:
                adaptedMood = adaptForWestern(basePrediction.mood, features);
                adaptedConfidence = Math.min(0.95, basePrediction.confidence * 1.05);
                break;
                
            case CONTEXT_UNIVERSAL:
                // No adaptation needed, use base prediction
                break;
        }
        
        return new MoodPrediction(adaptedMood, basePrediction.confidence, culturalContext, adaptedConfidence);
    }
    
    /**
     * Bollywood-specific mood adaptation
     */
    private String adaptForBollywood(String baseMood, AudioFeatureExtractor.AudioFeatures features) {
        // Bollywood music has different emotional mappings than Western music
        
        switch (baseMood) {
            case MoodAlgorithm.MOOD_SAD:
                // Bollywood "sad" songs are often more energetic than Western sad songs
                if (features.tempo > 100) {
                    return MoodAlgorithm.MOOD_HAPPY; // Actually celebratory/romantic
                }
                break;
                
            case MoodAlgorithm.MOOD_ENERGETIC:
                // Bollywood energetic songs might actually be romantic/dance songs
                if (features.sitarDominant && features.harmonicComplexity > 0.5) {
                    return MoodAlgorithm.MOOD_HAPPY; // Bollywood dance/romantic
                }
                break;
                
            case MoodAlgorithm.MOOD_CALM:
                // Bollywood calm songs are often spiritual/meditative
                if (features.tempo < 80 && features.isMinorKey) {
                    return MoodAlgorithm.MOOD_SAD; // Bollywood devotional/sad
                }
                break;
                
            case MoodAlgorithm.MOOD_HAPPY:
                // Bollywood happy songs are typically faster and more complex
                if (features.tempo < 90 && features.complexRhythm) {
                    return MoodAlgorithm.MOOD_CALM; // Actually romantic/melodic
                }
                break;
        }
        
        return baseMood;
    }
    
    /**
     * Western-specific mood adaptation
     */
    private String adaptForWestern(String baseMood, AudioFeatureExtractor.AudioFeatures features) {
        // Western music follows more conventional emotional mappings
        
        switch (baseMood) {
            case MoodAlgorithm.MOOD_ENERGETIC:
                // Western energetic songs are typically rock/electronic
                if (features.guitarDominant && features.energy > 0.7) {
                    return MoodAlgorithm.MOOD_ENERGETIC; // Confirmed energetic
                }
                break;
                
            case MoodAlgorithm.MOOD_SAD:
                // Western sad songs are typically slower and minor key
                if (features.tempo > 100 || !features.isMinorKey) {
                    return MoodAlgorithm.MOOD_CALM; // Actually calm/melodic
                }
                break;
                
            case MoodAlgorithm.MOOD_HAPPY:
                // Western happy songs are typically major key and upbeat
                if (features.isMinorKey && features.tempo < 100) {
                    return MoodAlgorithm.MOOD_CALM; // Actually melancholic
                }
                break;
                
            case MoodAlgorithm.MOOD_CALM:
                // Western calm songs are typically ambient or acoustic
                if (features.energy > 0.6 && features.tempo > 120) {
                    return MoodAlgorithm.MOOD_HAPPY; // Actually upbeat
                }
                break;
        }
        
        return baseMood;
    }
    
    /**
     * Get cultural-specific mood weights for fusion
     */
    public double[] getCulturalMoodWeights(String culturalContext) {
        switch (culturalContext) {
            case CONTEXT_BOLLYWOOD:
                // Bollywood music tends to be more emotionally expressive
                return new double[] {0.4, 0.2, 0.2, 0.2}; // HAPPY, SAD, CALM, ENERGETIC
                
            case CONTEXT_WESTERN:
                // Western music follows more balanced emotional distribution
                return new double[] {0.25, 0.25, 0.25, 0.25};
                
            case CONTEXT_UNIVERSAL:
            default:
                // Universal music uses equal weights
                return new double[] {0.25, 0.25, 0.25, 0.25};
        }
    }
    
    /**
     * Get cultural-specific tempo ranges
     */
    public double[] getCulturalTempoRanges(String culturalContext) {
        switch (culturalContext) {
            case CONTEXT_BOLLYWOOD:
                // Bollywood music has wider tempo range
                return new double[] {70, 160}; // min, max BPM
                
            case CONTEXT_WESTERN:
                // Western music has more conventional tempo ranges
                return new double[] {60, 140};
                
            case CONTEXT_UNIVERSAL:
            default:
                return new double[] {60, 180}; // Full range
        }
    }
    
    /**
     * Check if tempo is within cultural expectations
     */
    public boolean isTempoCulturallyAppropriate(double tempo, String mood, String culturalContext) {
        double[] ranges = getCulturalTempoRanges(culturalContext);
        
        switch (culturalContext) {
            case CONTEXT_BOLLYWOOD:
                switch (mood) {
                    case MoodAlgorithm.MOOD_HAPPY:
                        return tempo >= 90 && tempo <= 160; // Bollywood happy songs are fast
                    case MoodAlgorithm.MOOD_SAD:
                        return tempo >= 70 && tempo <= 110; // Bollywood sad songs are moderate
                    case MoodAlgorithm.MOOD_CALM:
                        return tempo >= 80 && tempo <= 120; // Bollywood calm songs are moderate
                    case MoodAlgorithm.MOOD_ENERGETIC:
                        return tempo >= 120 && tempo <= 160; // Bollywood energetic songs are fast
                }
                break;
                
            case CONTEXT_WESTERN:
                switch (mood) {
                    case MoodAlgorithm.MOOD_HAPPY:
                        return tempo >= 100 && tempo <= 140; // Western happy songs are upbeat
                    case MoodAlgorithm.MOOD_SAD:
                        return tempo >= 60 && tempo <= 90; // Western sad songs are slow
                    case MoodAlgorithm.MOOD_CALM:
                        return tempo >= 70 && tempo <= 100; // Western calm songs are moderate
                    case MoodAlgorithm.MOOD_ENERGETIC:
                        return tempo >= 120 && tempo <= 180; // Western energetic songs are fast
                }
                break;
                
            case CONTEXT_UNIVERSAL:
            default:
                return tempo >= ranges[0] && tempo <= ranges[1];
        }
        
        return true; // Default to appropriate
    }
    
    /**
     * Get cultural confidence adjustment factor
     */
    public double getCulturalConfidenceAdjustment(String culturalContext, String mood) {
        switch (culturalContext) {
            case CONTEXT_BOLLYWOOD:
                // Bollywood music has strong cultural patterns
                switch (mood) {
                    case MoodAlgorithm.MOOD_HAPPY:
                        return 1.2; // High confidence in Bollywood happy detection
                    case MoodAlgorithm.MOOD_SAD:
                        return 1.1; // Good confidence in Bollywood sad detection
                    case MoodAlgorithm.MOOD_CALM:
                        return 0.9; // Lower confidence (can be ambiguous)
                    case MoodAlgorithm.MOOD_ENERGETIC:
                        return 1.15; // High confidence in Bollywood energetic detection
                }
                break;
                
            case CONTEXT_WESTERN:
                // Western music follows well-established patterns
                switch (mood) {
                    case MoodAlgorithm.MOOD_HAPPY:
                        return 1.1; // Good confidence
                    case MoodAlgorithm.MOOD_SAD:
                        return 1.15; // High confidence in Western sad detection
                    case MoodAlgorithm.MOOD_CALM:
                        return 1.05; // Good confidence
                    case MoodAlgorithm.MOOD_ENERGETIC:
                        return 1.1; // Good confidence
                }
                break;
                
            case CONTEXT_UNIVERSAL:
            default:
                return 1.0; // No adjustment
        }
        
        return 1.0;
    }
    
    /**
     * Log cultural analysis for debugging
     */
    public void logCulturalAnalysis(AudioFeatureExtractor.AudioFeatures features, String detectedContext, MoodPrediction original, MoodPrediction adapted) {
        Log.d(TAG, "=== Cultural Analysis ===");
        Log.d(TAG, "Detected Context: " + detectedContext);
        Log.d(TAG, "Tempo: " + features.tempo + " BPM");
        Log.d(TAG, "Complex Rhythm: " + features.complexRhythm);
        Log.d(TAG, "Sitar Dominant: " + features.sitarDominant);
        Log.d(TAG, "Guitar Dominant: " + features.guitarDominant);
        Log.d(TAG, "Original Mood: " + original.mood + " (confidence: " + original.confidence + ")");
        Log.d(TAG, "Adapted Mood: " + adapted.mood + " (confidence: " + adapted.adaptedConfidence + ")");
        Log.d(TAG, "========================");
    }
}
