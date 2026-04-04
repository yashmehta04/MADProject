package com.example.madproject.utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Unit tests for MoodAlgorithm class
 * Tests mood classification logic and edge cases
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class MoodAlgorithmTest {

    @Before
    public void setUp() {
        // Setup test environment
    }

    @Test
    public void testMoodConstants() {
        assertEquals("HAPPY", MoodAlgorithm.MOOD_HAPPY);
        assertEquals("SAD", MoodAlgorithm.MOOD_SAD);
        assertEquals("CALM", MoodAlgorithm.MOOD_CALM);
        assertEquals("ENERGETIC", MoodAlgorithm.MOOD_ENERGETIC);
    }

    @Test
    public void testGetMoodDisplayName() {
        assertEquals("Happy", MoodAlgorithm.getMoodDisplayName("HAPPY"));
        assertEquals("Sad", MoodAlgorithm.getMoodDisplayName("SAD"));
        assertEquals("Calm", MoodAlgorithm.getMoodDisplayName("CALM"));
        assertEquals("Energetic", MoodAlgorithm.getMoodDisplayName("ENERGETIC"));
        assertEquals("Unknown", MoodAlgorithm.getMoodDisplayName("INVALID_MOOD"));
        assertEquals("Unknown", MoodAlgorithm.getMoodDisplayName(null));
    }

    @Test
    public void testGetMoodColor() {
        // Test that mood colors are returned (exact values depend on implementation)
        assertNotNull(MoodAlgorithm.getMoodColor("HAPPY"));
        assertNotNull(MoodAlgorithm.getMoodColor("SAD"));
        assertNotNull(MoodAlgorithm.getMoodColor("CALM"));
        assertNotNull(MoodAlgorithm.getMoodColor("ENERGETIC"));
        assertNotNull(MoodAlgorithm.getMoodColor("INVALID_MOOD"));
        assertNotNull(MoodAlgorithm.getMoodColor(null));
    }

    @Test
    public void testIsValidMood() {
        assertTrue(MoodAlgorithm.isValidMood("HAPPY"));
        assertTrue(MoodAlgorithm.isValidMood("SAD"));
        assertTrue(MoodAlgorithm.isValidMood("CALM"));
        assertTrue(MoodAlgorithm.isValidMood("ENERGETIC"));
        
        assertFalse(MoodAlgorithm.isValidMood("INVALID_MOOD"));
        assertFalse(MoodAlgorithm.isValidMood(""));
        assertFalse(MoodAlgorithm.isValidMood(null));
        assertFalse(MoodAlgorithm.isValidMood("happy")); // Case sensitive
    }

    @Test
    public void testClassifySongEdgeCases() {
        // Test with null inputs
        assertEquals("CALM", MoodAlgorithm.classifySong(null, null, 0));
        assertEquals("CALM", MoodAlgorithm.classifySong("", "", -1));
        
        // Test with invalid duration
        assertEquals("CALM", MoodAlgorithm.classifySong("rock", "Test Song", -100));
        assertEquals("CALM", MoodAlgorithm.classifySong("rock", "Test Song", 1000000)); // Very long song
        
        // Test with empty title
        assertEquals("CALM", MoodAlgorithm.classifySong("rock", "", 180000));
    }

    @Test
    public void testClassifySongByGenre() {
        // Test different genres
        assertEquals("HAPPY", MoodAlgorithm.classifySong("pop", "Happy Song", 180000));
        assertEquals("SAD", MoodAlgorithm.classifySong("blues", "Sad Song", 120000));
        assertEquals("CALM", MoodAlgorithm.classifySong("classical", "Calm Song", 300000));
        assertEquals("ENERGETIC", MoodAlgorithm.classifySong("rock", "Rock Song", 200000));
        assertEquals("HAPPY", MoodAlgorithm.classifySong("dance", "Party Song", 140000));
        
        // Test unknown genre
        assertEquals("CALM", MoodAlgorithm.classifySong("unknown", "Test Song", 180000));
    }

    @Test
    public void testClassifySongByTitle() {
        // Test title-based classification
        assertEquals("HAPPY", MoodAlgorithm.classifySong("", "Happy Birthday", 180000));
        assertEquals("SAD", MoodAlgorithm.classifyGenre("", "Sad Song", 120000));
        assertEquals("CALM", MoodAlgorithm.classifyGenre("", "Peaceful", 180000));
        assertEquals("ENERGETIC", MoodAlgorithm.classifyGenre("", "Energy", 180000));
        assertEquals("HAPPY", MoodAlgorithm.classifyGenre("", "Party Time", 180000));
    }

    @Test
    public void testClassifySongByDuration() {
        // Test duration-based classification
        assertEquals("CALM", MoodAlgorithm.classifySong("", "Long Song", 600000)); // 10 minutes
        assertEquals("ENERGETIC", MoodAlgorithm.classifySong("", "Short Song", 60000)); // 1 minute
        assertEquals("HAPPY", MoodAlgorithm.classifySong("", "Medium Song", 180000)); // 3 minutes
    }
}
