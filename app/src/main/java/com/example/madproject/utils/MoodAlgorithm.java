package com.example.madproject.utils;

import android.content.Context;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.example.madproject.database.MoodOperations;
import com.example.madproject.models.SongsList;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Offline mood classification algorithm for local songs.
 * Uses a multi-factor heuristic scoring system combining:
 * 1. Genre metadata (primary signal, +3 weight)
 * 2. Title keyword analysis (fallback signal, +2 weight) 
 * 3. Duration analysis (support signal, +1 weight)
 *
 * Mood categories: HAPPY, SAD, CALM, ENERGETIC
 * 
 * <p>This class provides thread-safe mood classification for audio files
 * using a combination of metadata analysis and heuristic rules. It operates
 * entirely offline without requiring network connectivity.</p>
 * 
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * // Classify a single song
 * String mood = MoodAlgorithm.classifySong("rock", "Happy Song", 180000);
 * 
 * // Tag multiple songs in background
 * ArrayList<SongsList> songs = getSongsList();
 * MoodAlgorithm.tagSongsInBackground(context, songs, () -> {
 *     Log.d("MainActivity", "Mood tagging complete");
 * });
 * }</pre>
 * 
 * @since 1.0
 * @author SonicWave Team
 * @version 2.1.0
 */
public final class MoodAlgorithm {

    private static final String TAG = "MoodAlgorithm";

    // Mood constants
    public static final String MOOD_HAPPY = "HAPPY";
    public static final String MOOD_SAD = "SAD";
    public static final String MOOD_CALM = "CALM";
    public static final String MOOD_ENERGETIC = "ENERGETIC";

    private MoodAlgorithm() {
        // Private constructor — utility class
    }

    /**
     * Scans all untagged songs in the background and inserts mood tags into SQLite.
     * Only processes songs whose paths are NOT already in the MoodTable (optimization).
     *
     * @param context  Application context
     * @param allSongs Full list of scanned songs
     * @param callback Optional callback when scanning is complete
     */
    public static void tagSongsInBackground(Context context, ArrayList<SongsList> allSongs,
                                             Runnable callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                MoodOperations moodOps = new MoodOperations(context);

                // Optimization: get all already-tagged paths
                Set<String> taggedPaths = moodOps.getAllTaggedPaths();
                Log.d(TAG, "Already tagged: " + taggedPaths.size() + " songs");

                // Filter to only untagged songs
                ArrayList<SongsList> untagged = new ArrayList<>();
                for (SongsList song : allSongs) {
                    if (song.getPath() != null && !taggedPaths.contains(song.getPath())) {
                        untagged.add(song);
                    }
                }

                Log.d(TAG, "New songs to tag: " + untagged.size());

                if (untagged.isEmpty()) {
                    if (callback != null) callback.run();
                    return;
                }

                // Batch process and insert
                ArrayList<String[]> batchEntries = new ArrayList<>();
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                
                try {
                    for (SongsList song : untagged) {
                        String genre = extractGenre(retriever, song.getPath());
                        String mood = classifySong(genre, song.getTitle(), song.getDuration());
                        batchEntries.add(new String[]{song.getPath(), mood});
                    }
                } finally {
                    try {
                        retriever.release();
                    } catch (Exception e) {
                        Log.w(TAG, "Error releasing retriever", e);
                    }
                }

                // Batch insert into SQLite (single transaction)
                moodOps.insertMoodTagsBatch(batchEntries);
                Log.d(TAG, "Mood tagging complete. Tagged " + batchEntries.size() + " songs.");

                if (callback != null) callback.run();

            } catch (Exception e) {
                Log.e(TAG, "Error during mood tagging", e);
                if (callback != null) callback.run();
            }
        });
        try {
            executor.shutdown();
            // Wait for tasks to complete, but don't block indefinitely
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                Log.w(TAG, "Executor did not terminate within 30 seconds, forcing shutdown");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Log.w(TAG, "Interrupted while waiting for executor termination", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Extracts the genre from a song's ID3 tag using MediaMetadataRetriever.
     *
     * @param retriever Reusable retriever instance
     * @param filePath  Absolute path to the audio file
     * @return Genre string or null if not available
     */
    private static String extractGenre(MediaMetadataRetriever retriever, String filePath) {
        try {
            retriever.setDataSource(filePath);
            return retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        } catch (Exception e) {
            // File might be inaccessible or corrupt
            return null;
        }
    }

    /**
     * Classifies a song's mood using multi-factor scoring.
     *
     * Weight system:
     * - Genre match: +3 (most reliable signal)
     * - Title keyword match: +2 (fallback when no genre)
     * - Duration analysis: +1 (supporting signal)
     *
     * @param genre    Genre from ID3 tag (can be null)
     * @param title    Song title
     * @param duration Song duration in milliseconds
     * @return The classified mood: HAPPY, SAD, CALM, or ENERGETIC
     */
    public static String classifySong(String genre, String title, long duration) {
        int happyScore = 0;
        int sadScore = 0;
        int calmScore = 0;
        int energeticScore = 0;

        // ==================== Factor 1: Genre (weight +3) ====================
        if (genre != null && !genre.isEmpty()) {
            String g = genre.toLowerCase().trim();

            // HAPPY genres
            if (g.contains("pop") || g.contains("dance") || g.contains("edm")
                    || g.contains("disco") || g.contains("funk") || g.contains("reggae")
                    || g.contains("ska") || g.contains("latin") || g.contains("k-pop")
                    || g.contains("bollywood") || g.contains("filmi")) {
                happyScore += 3;
            }

            // ENERGETIC genres
            if (g.contains("rock") || g.contains("metal") || g.contains("punk")
                    || g.contains("hard") || g.contains("grunge") || g.contains("industrial")
                    || g.contains("drum") || g.contains("bass") || g.contains("dubstep")
                    || g.contains("trap") || g.contains("hip-hop") || g.contains("hip hop")
                    || g.contains("rap")) {
                energeticScore += 3;
            }

            // SAD genres
            if (g.contains("blues") || g.contains("soul") || g.contains("slow")
                    || g.contains("ballad") || g.contains("emo") || g.contains("gothic")
                    || g.contains("country")) {
                sadScore += 3;
            }

            // CALM genres
            if (g.contains("classical") || g.contains("acoustic") || g.contains("ambient")
                    || g.contains("jazz") || g.contains("lounge") || g.contains("chill")
                    || g.contains("new age") || g.contains("meditation")
                    || g.contains("instrumental") || g.contains("lo-fi") || g.contains("lofi")
                    || g.contains("easy listening") || g.contains("folk")) {
                calmScore += 3;
            }
        }

        // ==================== Factor 2: Title keywords (weight +2) ====================
        if (title != null && !title.isEmpty()) {
            String t = title.toLowerCase().trim();

            // HAPPY keywords
            if (t.contains("happy") || t.contains("joy") || t.contains("love")
                    || t.contains("dance") || t.contains("fun")
                    || t.contains("celebrate") || t.contains("sunshine") || t.contains("smile")
                    || t.contains("beautiful") || t.contains("good") || t.contains("wonderful")
                    || t.contains("alive") || t.contains("summer")) {
                happyScore += 2;
            }

            // SAD keywords
            if (t.contains("sad") || t.contains("alone") || t.contains("lonely")
                    || t.contains("cry") || t.contains("tears") || t.contains("broken")
                    || t.contains("pain") || t.contains("hurt") || t.contains("miss you")
                    || t.contains("goodbye") || t.contains("lost") || t.contains("sorry")
                    || t.contains("rain") || t.contains("heartbreak") || t.contains("gone")) {
                sadScore += 2;
            }

            // CALM keywords
            if (t.contains("peace") || t.contains("calm") || t.contains("quiet")
                    || t.contains("gentle") || t.contains("soft") || t.contains("dream")
                    || t.contains("sleep") || t.contains("night") || t.contains("breeze")
                    || t.contains("ocean") || t.contains("river") || t.contains("moon")
                    || t.contains("whisper") || t.contains("lullaby")) {
                calmScore += 2;
            }

            // ENERGETIC keywords
            if (t.contains("fire") || t.contains("rage") || t.contains("power")
                    || t.contains("fight") || t.contains("run") || t.contains("wild")
                    || t.contains("rock") || t.contains("thunder") || t.contains("storm")
                    || t.contains("energy") || t.contains("beast") || t.contains("burn")
                    || t.contains("scream") || t.contains("warrior")) {
                energeticScore += 2;
            }
        }

        // ==================== Factor 3: Duration (weight +1) ====================
        long durationSec = duration / 1000;
        if (durationSec > 0) {
            if (durationSec < 150) {
                // Short songs (<2.5 min) tend to be energetic or happy
                energeticScore += 1;
            } else if (durationSec > 360) {
                // Long songs (>6 min) tend to be calm or ambient
                calmScore += 1;
            } else if (durationSec >= 180 && durationSec <= 270) {
                // Standard pop length (3–4.5 min) → happy
                happyScore += 1;
            }
        }

        // ==================== Determine Winner ====================
        int maxScore = Math.max(Math.max(happyScore, sadScore),
                Math.max(calmScore, energeticScore));

        // If all scores are 0 (no genre + no keyword matches), default to CALM
        if (maxScore == 0) {
            return MOOD_CALM;
        }

        // Return the mood with the highest score (tie-breaking order: HAPPY > ENERGETIC > SAD > CALM)
        if (happyScore == maxScore) return MOOD_HAPPY;
        if (energeticScore == maxScore) return MOOD_ENERGETIC;
        if (sadScore == maxScore) return MOOD_SAD;
        return MOOD_CALM;
    }
    
    /**
     * Check if mood is valid
     */
    public static boolean isValidMood(String mood) {
        if (mood == null) return false;
        
        switch (mood) {
            case MOOD_HAPPY:
            case MOOD_SAD:
            case MOOD_CALM:
            case MOOD_ENERGETIC:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Classify song by genre (alias for classifySong)
     */
    public static String classifyGenre(String genre, String title, int duration) {
        return classifySong(genre, title, duration);
    }

    /**
     * Returns a display-friendly emoji + label for a mood.
     */
    public static String getMoodDisplayName(String mood) {
        switch (mood) {
            case MOOD_HAPPY:
                return "😊 Happy";
            case MOOD_SAD:
                return "😢 Sad";
            case MOOD_CALM:
                return "😌 Calm";
            case MOOD_ENERGETIC:
                return "🔥 Energetic";
            default:
                return "🎵 Mixed";
        }
    }

    /**
     * Returns a color resource hint for each mood (used in UI theming).
     */
    public static int getMoodColor(String mood) {
        switch (mood) {
            case MOOD_HAPPY:
                return 0xFFFFD700; // Gold
            case MOOD_SAD:
                return 0xFF6495ED; // Cornflower Blue
            case MOOD_CALM:
                return 0xFF7CFC00; // Lawn Green
            case MOOD_ENERGETIC:
                return 0xFFFF4500; // Orange Red
            default:
                return 0xFF2196F3; // Blue
        }
    }
}
