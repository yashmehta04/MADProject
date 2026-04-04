package com.example.madproject.database;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Unit tests for MoodOperations class
 * Tests database operations and mood management
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MoodOperationsTest {

    private MoodOperations moodOperations;
    private MoodDBHandler dbHandler;

    @Before
    public void setUp() {
        // Initialize database for testing
        dbHandler = new MoodDBHandler(RuntimeEnvironment.getApplication());
        moodOperations = new MoodOperations(RuntimeEnvironment.getApplication());
    }

    @Test
    public void testInsertAndGetMoodTag() {
        String songPath = "/test/song.mp3";
        String mood = "HAPPY";
        double confidence = 0.85;
        
        // Insert mood tag
        moodOperations.insertMoodTag(songPath, mood, confidence, "Test reasoning");
        
        // Retrieve mood tag
        MoodOperations.DetailedMoodInfo info = moodOperations.getDetailedMoodInfo(songPath);
        
        assertNotNull("Mood info should not be null", info);
        assertEquals("Mood should match", mood, info.mood);
        assertEquals("Confidence should match", confidence, info.confidence, 0.01);
        assertEquals("Reasoning should match", "Test reasoning", info.reasoning);
    }

    @Test
    public void testGetMoodTag() {
        String songPath = "/test/song2.mp3";
        String mood = "SAD";
        
        // Insert mood tag
        moodOperations.insertMoodTag(songPath, mood, 0.9, "Test reasoning");
        
        // Get mood tag
        String retrievedMood = moodOperations.getMoodTag(songPath);
        
        assertEquals("Retrieved mood should match", mood, retrievedMood);
    }

    @Test
    public void testGetMoodTagNotFound() {
        String nonExistentSong = "/test/nonexistent.mp3";
        
        // Get mood tag for non-existent song
        String mood = moodOperations.getMoodTag(nonExistentSong);
        
        assertNull("Mood should be null for non-existent song", mood);
    }

    @Test
    public void testUpdateMoodTag() {
        String songPath = "/test/song3.mp3";
        String originalMood = "CALM";
        String updatedMood = "ENERGETIC";
        
        // Insert original mood
        moodOperations.insertMoodTag(songPath, originalMood, 0.7, "Original reasoning");
        
        // Update mood
        boolean updateSuccess = moodOperations.updateMoodTag(songPath, updatedMood, 0.95, "Updated reasoning");
        assertTrue("Update should succeed", updateSuccess);
        
        // Verify update
        MoodOperations.DetailedMoodInfo info = moodOperations.getDetailedMoodInfo(songPath);
        assertEquals("Mood should be updated", updatedMood, info.mood);
        assertEquals("Confidence should be updated", 0.95, info.confidence, 0.01);
        assertEquals("Reasoning should be updated", "Updated reasoning", info.reasoning);
    }

    @Test
    public void testDeleteMoodTag() {
        String songPath = "/test/song4.mp3";
        String mood = "HAPPY";
        
        // Insert mood tag
        moodOperations.insertMoodTag(songPath, mood, 0.8, "Test reasoning");
        
        // Verify insertion
        assertNotNull("Mood should exist before deletion", moodOperations.getMoodTag(songPath));
        
        // Delete mood tag
        moodOperations.deleteMoodTag(songPath);
        
        // Verify deletion
        assertNull("Mood should be null after deletion", moodOperations.getMoodTag(songPath));
    }

    @Test
    public void testGetSongsByMood() {
        // Insert test songs with different moods
        String[] songPaths = {
            "/test/happy1.mp3",
            "/test/happy2.mp3", 
            "/test/sad1.mp3",
            "/test/sad2.mp3"
        };
        
        moodOperations.insertMoodTag(songPaths[0], "HAPPY", 0.9, "Test");
        moodOperations.insertMoodTag(songPaths[1], "HAPPY", 0.85, "Test");
        moodOperations.insertMoodTag(songPaths[2], "SAD", 0.8, "Test");
        moodOperations.insertMoodTag(songPaths[3], "SAD", 0.75, "Test");
        
        // Create mock song list
        ArrayList<com.example.madproject.models.SongsList> allSongs = new ArrayList<>();
        for (int i = 0; i < songPaths.length; i++) {
            com.example.madproject.models.SongsList song = new com.example.madproject.models.SongsList();
            song.setPath(songPaths[i]);
            song.setTitle("Test Song " + (i + 1));
            allSongs.add(song);
        }
        
        // Get happy songs
        ArrayList<com.example.madproject.models.SongsList> happySongs = 
            moodOperations.getSongsByMood(allSongs, "HAPPY", 10);
        
        assertEquals("Should find 2 happy songs", 2, happySongs.size());
        
        // Get sad songs
        ArrayList<com.example.madproject.models.SongsList> sadSongs = 
            moodOperations.getSongsByMood(allSongs, "SAD", 10);
        
        assertEquals("Should find 2 sad songs", 2, sadSongs.size());
        
        // Get songs with limit
        ArrayList<com.example.madproject.models.SongsList> limitedHappySongs = 
            moodOperations.getSongsByMood(allSongs, "HAPPY", 1);
        
        assertEquals("Should respect limit", 1, limitedHappySongs.size());
    }

    @Test
    public void testGetMoodCounts() {
        // Insert test songs with different moods
        moodOperations.insertMoodTag("/test/happy1.mp3", "HAPPY", 0.9, "Test");
        moodOperations.insertMoodTag("/test/happy2.mp3", "HAPPY", 0.85, "Test");
        moodOperations.insertMoodTag("/test/sad1.mp3", "SAD", 0.8, "Test");
        moodOperations.insertMoodTag("/test/calm1.mp3", "CALM", 0.7, "Test");
        moodOperations.insertMoodTag("/test/energetic1.mp3", "ENERGETIC", 0.75, "Test");
        
        // Get mood counts
        int[] counts = moodOperations.getMoodCounts();
        
        assertNotNull("Counts array should not be null", counts);
        assertEquals("Should have 4 mood categories", 4, counts.length);
        assertEquals("Happy count should be 2", 2, counts[0]);
        assertEquals("Sad count should be 1", 1, counts[1]);
        assertEquals("Calm count should be 1", 1, counts[2]);
        assertEquals("Energetic count should be 1", 1, counts[3]);
    }

    @Test
    public void testGetAllTaggedPaths() {
        // Insert test songs
        String[] songPaths = {"/test/song1.mp3", "/test/song2.mp3", "/test/song3.mp3"};
        
        for (String path : songPaths) {
            moodOperations.insertMoodTag(path, "HAPPY", 0.8, "Test");
        }
        
        // Get all tagged paths
        java.util.Set<String> taggedPaths = moodOperations.getAllTaggedPaths();
        
        assertNotNull("Tagged paths set should not be null", taggedPaths);
        assertEquals("Should have 3 tagged paths", 3, taggedPaths.size());
        
        for (String path : songPaths) {
            assertTrue("Should contain path: " + path, taggedPaths.contains(path));
        }
    }

    @Test
    public void testMoodWithConfidence() {
        String songPath = "/test/confidence_test.mp3";
        String mood = "HAPPY";
        double confidence = 0.92;
        
        // Insert mood tag
        moodOperations.insertMoodTag(songPath, mood, confidence, "Test");
        
        // Get mood with confidence
        MoodOperations.MoodWithConfidence result = moodOperations.getMoodWithConfidence(songPath);
        
        assertNotNull("Result should not be null", result);
        assertEquals("Mood should match", mood, result.mood);
        assertEquals("Confidence should match", confidence, result.confidence, 0.01);
    }

    @Test
    public void testEdgeCases() {
        // Test with null inputs
        try {
            moodOperations.insertMoodTag(null, "HAPPY", 0.8, "Test");
            fail("Should handle null song path gracefully");
        } catch (Exception e) {
            // Expected behavior
        }
        
        // Test with empty strings
        moodOperations.insertMoodTag("", "HAPPY", 0.8, "Test");
        String mood = moodOperations.getMoodTag("");
        assertNotNull("Should handle empty path", mood);
        
        // Test with invalid confidence values
        moodOperations.insertMoodTag("/test/invalid_conf.mp3", "HAPPY", -0.5, "Test");
        MoodOperations.DetailedMoodInfo info = moodOperations.getDetailedMoodInfo("/test/invalid_conf.mp3");
        assertNotNull("Should handle negative confidence", info);
        
        moodOperations.insertMoodTag("/test/high_conf.mp3", "HAPPY", 1.5, "Test");
        info = moodOperations.getDetailedMoodInfo("/test/high_conf.mp3");
        assertNotNull("Should handle confidence > 1", info);
    }
}
