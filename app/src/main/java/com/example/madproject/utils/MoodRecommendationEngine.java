package com.example.madproject.utils;

import android.content.Context;
import android.util.Log;

import com.example.madproject.database.MoodOperations;
import com.example.madproject.models.SongsList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Advanced mood-based recommendation engine using questionnaire responses.
 * Analyzes user's current mood and suggests appropriate songs.
 */
public class MoodRecommendationEngine {
    
    private static final String TAG = "MoodRecommendationEngine";
    
    // Mood questionnaire questions and their impact on mood preferences
    public static class MoodQuestionnaire {
        public final Question[] questions;
        
        public MoodQuestionnaire() {
            this.questions = new Question[] {
                new Question(
                    "How are you feeling right now?",
                    new String[]{"Very Happy", "Happy", "Neutral", "Sad", "Very Sad"},
                    new double[]{0.8, 0.6, 0.4, 0.2, 0.0}, // HAPPY weight
                    new double[]{0.0, 0.1, 0.3, 0.7, 0.9}  // SAD weight
                ),
                new Question(
                    "What's your energy level?",
                    new String[]{"Very High", "High", "Medium", "Low", "Very Low"},
                    new double[]{0.9, 0.7, 0.5, 0.2, 0.0}, // ENERGETIC weight
                    new double[]{0.0, 0.1, 0.3, 0.6, 0.9}  // CALM weight
                ),
                new Question(
                    "What kind of activity are you doing?",
                    new String[]{"Working Out", "Relaxing", "Studying", "Sleeping"},
                    new double[]{0.8, 0.2, 0.1, 0.0}, // ENERGETIC weight
                    new double[]{0.1, 0.6, 0.4, 0.8}  // CALM weight
                ),
                new Question(
                    "What's the weather like today?",
                    new String[]{"Sunny & Bright", "Cloudy", "Rainy", "Stormy", "Snowy"},
                    new double[]{0.7, 0.5, 0.3, 0.2, 0.4}, // HAPPY weight
                    new double[]{0.2, 0.4, 0.6, 0.7, 0.5}  // SAD weight
                ),
                new Question(
                    "What time of day is it?",
                    new String[]{"Morning", "Afternoon", "Evening", "Night", "Late Night"},
                    new double[]{0.7, 0.6, 0.4, 0.2, 0.1}, // ENERGETIC weight
                    new double[]{0.2, 0.3, 0.5, 0.7, 0.8}  // CALM weight
                )
            };
        }
    }
    
    public static class Question {
        public final String question;
        public final String[] options;
        public final double[] energeticWeights;
        public final double[] calmWeights;
        
        public Question(String question, String[] options, double[] energeticWeights, double[] calmWeights) {
            this.question = question;
            this.options = options;
            this.energeticWeights = energeticWeights;
            this.calmWeights = calmWeights;
        }
    }
    
    public static class UserMoodProfile {
        public final double happyScore;
        public final double sadScore;
        public final double calmScore;
        public final double energeticScore;
        public final String primaryMood;
        public final String secondaryMood;
        public final double confidence;
        
        public UserMoodProfile(double happyScore, double sadScore, double calmScore, 
                             double energeticScore) {
            this.happyScore = happyScore;
            this.sadScore = sadScore;
            this.calmScore = calmScore;
            this.energeticScore = energeticScore;
            
            // Determine primary and secondary moods
            String[] moods = {MoodAlgorithm.MOOD_HAPPY, MoodAlgorithm.MOOD_SAD, 
                            MoodAlgorithm.MOOD_CALM, MoodAlgorithm.MOOD_ENERGETIC};
            double[] scores = {happyScore, sadScore, calmScore, energeticScore};
            
            // Sort by score
            Integer[] indices = {0, 1, 2, 3};
            java.util.Arrays.sort(indices, (i, j) -> Double.compare(scores[j], scores[i]));
            
            this.primaryMood = moods[indices[0]];
            this.secondaryMood = moods[indices[1]];
            this.confidence = scores[indices[0]];
        }
        
        public String getMoodSummary() {
            return String.format("Primary: %s (%.1f%%), Secondary: %s (%.1f%%)", 
                               primaryMood, confidence * 100, 
                               secondaryMood, scores[indices[1]] * 100);
        }
        
        private double[] scores;
        private Integer[] indices;
    }
    
    public static class RecommendationResult {
        public final ArrayList<SongsList> recommendedSongs;
        public final UserMoodProfile userMoodProfile;
        public final String reasoning;
        public final int totalSongs;
        
        public RecommendationResult(ArrayList<SongsList> recommendedSongs, 
                                 UserMoodProfile userMoodProfile, 
                                 String reasoning) {
            this.recommendedSongs = recommendedSongs;
            this.userMoodProfile = userMoodProfile;
            this.reasoning = reasoning;
            this.totalSongs = recommendedSongs.size();
        }
    }
    
    private final Context context;
    private final MoodOperations moodOperations;
    private final MoodQuestionnaire questionnaire;
    private final Random random;
    
    public MoodRecommendationEngine(Context context) {
        this.context = context;
        this.moodOperations = new MoodOperations(context);
        this.questionnaire = new MoodQuestionnaire();
        this.random = new Random();
    }
    
    /**
     * Generate recommendations based on questionnaire responses
     */
    public RecommendationResult generateRecommendations(int[] responses, ArrayList<SongsList> allSongs) {
        Log.i(TAG, "Generating recommendations based on questionnaire responses");
        
        // Step 1: Analyze user's mood profile from responses
        UserMoodProfile moodProfile = analyzeUserMood(responses);
        
        Log.i(TAG, "User mood profile: " + moodProfile.primaryMood + 
                  " (confidence: " + String.format("%.2f", moodProfile.confidence) + ")");
        
        // Step 2: Get songs matching the user's mood preferences
        ArrayList<SongsList> moodMatchingSongs = getMoodMatchingSongs(moodProfile, allSongs);
        
        // Step 3: Apply smart filtering and ranking
        ArrayList<SongsList> recommendedSongs = rankAndFilterSongs(moodMatchingSongs, moodProfile);
        
        // Step 4: Generate reasoning
        String reasoning = generateReasoning(moodProfile, recommendedSongs);
        
        Log.i(TAG, "Recommendation complete: " + recommendedSongs.size() + " songs recommended");
        
        return new RecommendationResult(recommendedSongs, moodProfile, reasoning);
    }
    
    /**
     * Analyze user's mood from questionnaire responses
     */
    private UserMoodProfile analyzeUserMood(int[] responses) {
        double happyScore = 0.5; // Base scores
        double sadScore = 0.5;
        double calmScore = 0.5;
        double energeticScore = 0.5;
        
        // Process each question response
        for (int i = 0; i < questionnaire.questions.length && i < responses.length; i++) {
            Question question = questionnaire.questions[i];
            int responseIndex = responses[i];
            
            if (responseIndex >= 0 && responseIndex < question.options.length) {
                // Apply weights based on question type
                switch (i) {
                    case 0: // Feeling question - affects HAPPY/SAD
                        happyScore += (question.energeticWeights[responseIndex] - 0.5) * 0.3;
                        sadScore += (question.calmWeights[responseIndex] - 0.5) * 0.3;
                        break;
                        
                    case 1: // Energy question - affects ENERGETIC/CALM
                        energeticScore += (question.energeticWeights[responseIndex] - 0.5) * 0.4;
                        calmScore += (question.calmWeights[responseIndex] - 0.5) * 0.4;
                        break;
                        
                    case 2: // Activity question - affects ENERGETIC/CALM
                        energeticScore += (question.energeticWeights[responseIndex] - 0.5) * 0.4;
                        calmScore += (question.calmWeights[responseIndex] - 0.5) * 0.3;
                        if (responseIndex == 0) { // Working Out
                            energeticScore += 0.3;
                        }
                        break;
                        
                    case 3: // Weather question - affects HAPPY/SAD
                        happyScore += (question.energeticWeights[responseIndex] - 0.5) * 0.2;
                        sadScore += (question.calmWeights[responseIndex] - 0.5) * 0.2;
                        break;
                        
                    case 4: // Time question - affects ENERGETIC/CALM
                        energeticScore += (question.energeticWeights[responseIndex] - 0.5) * 0.3;
                        calmScore += (question.calmWeights[responseIndex] - 0.5) * 0.3;
                        if (responseIndex == 4) { // Late night
                            calmScore += 0.2;
                        }
                        break;
                }
            }
        }
        
        // Normalize scores to 0-1 range
        happyScore = Math.max(0, Math.min(1, happyScore));
        sadScore = Math.max(0, Math.min(1, sadScore));
        calmScore = Math.max(0, Math.min(1, calmScore));
        energeticScore = Math.max(0, Math.min(1, energeticScore));
        
        return new UserMoodProfile(happyScore, sadScore, calmScore, energeticScore);
    }
    
    /**
     * Get songs that match the user's mood profile
     */
    private ArrayList<SongsList> getMoodMatchingSongs(UserMoodProfile moodProfile, ArrayList<SongsList> allSongs) {
        ArrayList<SongsList> matchingSongs = new ArrayList<>();
        
        // Get songs for each mood with weighting
        Map<String, Double> moodWeights = new HashMap<>();
        moodWeights.put(MoodAlgorithm.MOOD_HAPPY, moodProfile.happyScore);
        moodWeights.put(MoodAlgorithm.MOOD_SAD, moodProfile.sadScore);
        moodWeights.put(MoodAlgorithm.MOOD_CALM, moodProfile.calmScore);
        moodWeights.put(MoodAlgorithm.MOOD_ENERGETIC, moodProfile.energeticScore);
        
        // Collect songs with mood matching scores
        for (SongsList song : allSongs) {
            String songMood = moodOperations.getMoodTag(song.getPath());
            
            if (songMood != null && moodWeights.containsKey(songMood)) {
                double moodWeight = moodWeights.get(songMood);
                
                // Only include songs with decent mood match
                if (moodWeight >= 0.3) {
                    song.tempMoodScore = moodWeight; // Temporary score storage
                    matchingSongs.add(song);
                }
            }
        }
        
        Log.i(TAG, "Found " + matchingSongs.size() + " songs matching mood profile");
        return matchingSongs;
    }
    
    /**
     * Rank and filter songs based on multiple factors
     */
    private ArrayList<SongsList> rankAndFilterSongs(ArrayList<SongsList> songs, UserMoodProfile moodProfile) {
        if (songs.isEmpty()) {
            return songs;
        }
        
        // Sort songs by mood match score
        Collections.sort(songs, new Comparator<SongsList>() {
            @Override
            public int compare(SongsList s1, SongsList s2) {
                return Double.compare(s2.tempMoodScore, s1.tempMoodScore);
            }
        });
        
        // Apply diversity and quality filters
        ArrayList<SongsList> filteredSongs = new ArrayList<>();
        int maxRecommendations = Math.min(20, songs.size()); // Limit to 20 recommendations
        
        // Add top matches with some diversity
        Set<String> addedArtists = new HashSet<>();
        Set<String> addedGenres = new HashSet<>();
        
        for (int i = 0; i < maxRecommendations && i < songs.size(); i++) {
            SongsList song = songs.get(i);
            
            // Skip if we already have too many songs from this artist
            if (addedArtists.size() < 5 || !addedArtists.contains(song.getArtist())) {
                // Extract genre for diversity
                String genre = GenreMetadataExtractor.extractGenre(song);
                
                // Add some diversity in genres
                if (addedGenres.size() < 8 || !addedGenres.contains(genre)) {
                    filteredSongs.add(song);
                    addedArtists.add(song.getArtist());
                    if (!genre.equals("Unknown")) {
                        addedGenres.add(genre);
                    }
                }
            }
        }
        
        // Shuffle slightly for variety while keeping high-scoring songs upfront
        if (filteredSongs.size() > 5) {
            // Keep top 5 in order, shuffle the rest
            List<SongsList> topSongs = filteredSongs.subList(0, 5);
            List<SongsList> remainingSongs = new ArrayList<>(filteredSongs.subList(5, filteredSongs.size()));
            Collections.shuffle(remainingSongs);
            
            filteredSongs = new ArrayList<>();
            filteredSongs.addAll(topSongs);
            filteredSongs.addAll(remainingSongs);
        }
        
        return filteredSongs;
    }
    
    /**
     * Generate reasoning for recommendations
     */
    private String generateReasoning(UserMoodProfile moodProfile, ArrayList<SongsList> songs) {
        StringBuilder reasoning = new StringBuilder();
        
        reasoning.append("Based on your responses, you're feeling ").append(moodProfile.primaryMood);
        
        if (moodProfile.confidence > 0.7) {
            reasoning.append(" (strong preference)");
        } else if (moodProfile.confidence > 0.4) {
            reasoning.append(" (moderate preference)");
        } else {
            reasoning.append(" (mixed feelings)");
        }
        
        reasoning.append(". I've selected ").append(songs.size()).append(" songs that match your mood.");
        
        // Add mood-specific reasoning
        switch (moodProfile.primaryMood) {
            case MoodAlgorithm.MOOD_HAPPY:
                reasoning.append(" These upbeat tracks should lift your spirits!");
                break;
            case MoodAlgorithm.MOOD_SAD:
                reasoning.append(" These melancholic tunes match your contemplative mood.");
                break;
            case MoodAlgorithm.MOOD_CALM:
                reasoning.append(" These relaxing tracks are perfect for unwinding.");
                break;
            case MoodAlgorithm.MOOD_ENERGETIC:
                reasoning.append(" These high-energy tracks will keep you motivated!");
                break;
        }
        
        return reasoning.toString();
    }
    
    /**
     * Get questionnaire for display
     */
    public MoodQuestionnaire getQuestionnaire() {
        return questionnaire;
    }
    
    /**
     * Get mood-based recommendations for a specific mood
     */
    public ArrayList<SongsList> getMoodBasedRecommendations(String targetMood, ArrayList<SongsList> allSongs, int limit) {
        ArrayList<SongsList> recommendations = new ArrayList<>();
        
        for (SongsList song : allSongs) {
            String songMood = moodOperations.getMoodTag(song.getPath());
            
            if (targetMood.equals(songMood)) {
                recommendations.add(song);
                if (recommendations.size() >= limit) {
                    break;
                }
            }
        }
        
        // Shuffle for variety
        Collections.shuffle(recommendations);
        
        return recommendations;
    }
    
    /**
     * Get mixed mood recommendations (for users with mixed feelings)
     */
    public ArrayList<SongsList> getMixedMoodRecommendations(ArrayList<SongsList> allSongs, int limit) {
        ArrayList<SongsList> recommendations = new ArrayList<>();
        
        // Get equal distribution from all moods
        String[] moods = {MoodAlgorithm.MOOD_HAPPY, MoodAlgorithm.MOOD_SAD, 
                         MoodAlgorithm.MOOD_CALM, MoodAlgorithm.MOOD_ENERGETIC};
        int perMood = Math.max(1, limit / moods.length);
        
        for (String mood : moods) {
            ArrayList<SongsList> moodSongs = getMoodBasedRecommendations(mood, allSongs, perMood);
            recommendations.addAll(moodSongs);
        }
        
        // Shuffle and limit
        Collections.shuffle(recommendations);
        if (recommendations.size() > limit) {
            recommendations = new ArrayList<>(recommendations.subList(0, limit));
        }
        
        return recommendations;
    }
    
    /**
     * Update user preferences based on song skips/likes
     */
    public void updatePreferencesFromFeedback(String songPath, boolean liked) {
        // In a full implementation, this would learn from user behavior
        // For now, just log the feedback
        String songMood = moodOperations.getMoodTag(songPath);
        Log.i(TAG, "User feedback: " + (liked ? "Liked" : "Skipped") + " song with mood: " + songMood);
    }
}
