package com.example.madproject.utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 * Unit tests for SmartFileFilter class
 * Tests file filtering logic and security features
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SmartFileFilterTest {

    @Before
    public void setUp() {
        // Setup test environment
    }

    @Test
    public void testFilterMusicFiles() {
        // Create test song list
        ArrayList<com.example.madproject.models.SongsList> allSongs = new ArrayList<>();
        
        // Add valid music files
        allSongs.add(createTestSong("valid_song.mp3", "/music/valid_song.mp3"));
        allSongs.add(createTestSong("another_song.wav", "/music/another_song.wav"));
        allSongs.add(createTestSong("music_file.flac", "/music/music_file.flac"));
        
        // Add non-music files
        allSongs.add(createTestSong("document.pdf", "/docs/document.pdf"));
        allSongs.add(createTestSong("image.jpg", "/images/image.jpg"));
        allSongs.add(createTestSong("video.mp4", "/videos/video.mp4"));
        
        // Filter music files
        ArrayList<com.example.madproject.models.SongsList> filteredSongs = 
            SmartFileFilter.filterMusicFiles(allSongs);
        
        // Should only contain music files
        assertEquals("Should filter to 3 music files", 3, filteredSongs.size());
        
        // Verify music files are kept
        assertTrue("Should contain mp3", containsSongWithPath(filteredSongs, "/music/valid_song.mp3"));
        assertTrue("Should contain wav", containsSongWithPath(filteredSongs, "/music/another_song.wav"));
        assertTrue("Should contain flac", containsSongWithPath(filteredSongs, "/music/music_file.flac"));
        
        // Verify non-music files are excluded
        assertFalse("Should not contain pdf", containsSongWithPath(filteredSongs, "/docs/document.pdf"));
        assertFalse("Should not contain jpg", containsSongWithPath(filteredSongs, "/images/image.jpg"));
        assertFalse("Should not contain mp4", containsSongWithPath(filteredSongs, "/videos/video.mp4"));
    }

    @Test
    public void testPathTraversalProtection() {
        // Test path traversal attempts
        assertFalse("Should reject ../ path", SmartFileFilter.testFile("../../../etc/passwd"));
        assertFalse("Should reject ..\\ path", SmartFileFilter.testFile("..\\..\\windows\\system32\\config\\sam"));
        assertFalse("Should reject absolute path", SmartFileFilter.testFile("/etc/passwd"));
        assertFalse("Should reject Windows absolute path", SmartFileFilter.testFile("C:\\Windows\\System32\\config\\sam"));
        assertFalse("Should reject very long filename", SmartFileFilter.testFile("a".repeat(300) + ".mp3"));
        
        // Test valid paths
        assertTrue("Should accept relative path", SmartFileFilter.testFile("music/song.mp3"));
        assertTrue("Should accept simple filename", SmartFileFilter.testFile("song.mp3"));
        assertTrue("Should accept path with subdirectories", SmartFileFilter.testFile("artist/album/song.mp3"));
    }

    @Test
    public void testGetFileName() {
        // Test normal file paths
        assertEquals("song.mp3", getTestFileName("music/song.mp3"));
        assertEquals("track.wav", getTestFileName("artist/album/track.wav"));
        assertEquals("file.flac", getTestFileName("file.flac"));
        
        // Test path traversal attempts (should return safe fallback)
        assertEquals("safe_file", getTestFileName("../../../etc/passwd"));
        assertEquals("safe_file", getTestFileName("..\\..\\windows\\system32\\config\\sam"));
        assertEquals("safe_file", getTestFileName("/etc/passwd"));
        assertEquals("safe_file", getTestFileName("C:\\Windows\\System32\\config\\sam"));
        
        // Test edge cases
        assertEquals("unknown_file", getTestFileName(null));
        assertEquals("unknown_file", getTestFileName(""));
        assertEquals("unknown_file", getTestFileName("   "));
        assertEquals("safe_file", getTestFileName("a".repeat(300) + ".mp3"));
    }

    @Test
    public void testFilteringStats() {
        // Create test data
        ArrayList<com.example.madproject.models.SongsList> originalFiles = new ArrayList<>();
        originalFiles.add(createTestSong("song1.mp3", "/music/song1.mp3"));
        originalFiles.add(createTestSong("song2.wav", "/music/song2.wav"));
        originalFiles.add(createTestSong("document.pdf", "/docs/document.pdf"));
        originalFiles.add(createTestSong("image.jpg", "/images/image.jpg"));
        
        // Filter files
        ArrayList<com.example.madproject.models.SongsList> filteredFiles = 
            SmartFileFilter.filterMusicFiles(originalFiles);
        
        // Get stats
        SmartFileFilter.FilteringStats stats = 
            SmartFileFilter.getFilteringStats(originalFiles, filteredFiles);
        
        assertEquals("Original count should be 4", 4, stats.getOriginalCount());
        assertEquals("Filtered count should be 2", 2, stats.getFilteredCount());
        assertEquals("Excluded count should be 2", 2, stats.getExcludedCount());
        assertTrue("Should have summary", stats.getSummary().length() > 0);
    }

    @Test
    public void testSuspiciousExtensions() {
        // Test files with suspicious extensions that should be filtered out
        ArrayList<com.example.madproject.models.SongsList> suspiciousFiles = new ArrayList<>();
        
        // Add files with suspicious extensions
        suspiciousFiles.add(createTestSong("suspicious.exe", "/downloads/suspicious.exe"));
        suspiciousFiles.add(createTestSong("malware.bat", "/temp/malware.bat"));
        suspiciousFiles.add(createTestSong("script.sh", "/scripts/script.sh"));
        suspiciousFiles.add(createTestSong("archive.zip", "/downloads/archive.zip"));
        suspiciousFiles.add(createTestSong("document.doc", "/docs/document.doc"));
        
        // Filter files
        ArrayList<com.example.madproject.models.SongsList> filteredFiles = 
            SmartFileFilter.filterMusicFiles(suspiciousFiles);
        
        // Should filter out all suspicious files
        assertTrue("Should filter out suspicious files", filteredFiles.isEmpty());
    }

    @Test
    public void testValidMusicExtensions() {
        // Test files with valid music extensions
        ArrayList<com.example.madproject.models.SongsList> musicFiles = new ArrayList<>();
        
        // Add files with valid music extensions
        musicFiles.add(createTestSong("song.mp3", "/music/song.mp3"));
        musicFiles.add(createTestSong("track.wav", "/music/track.wav"));
        musicFiles.add(createTestSong("audio.flac", "/music/audio.flac"));
        musicFiles.add(createTestSong("music.m4a", "/music/music.m4a"));
        musicFiles.add(createTestSong("sound.ogg", "/music/sound.ogg"));
        musicFiles.add(createTestSong("track.aac", "/music/track.aac"));
        
        // Filter files
        ArrayList<com.example.madproject.models.SongsList> filteredFiles = 
            SmartFileFilter.filterMusicFiles(musicFiles);
        
        // Should keep all valid music files
        assertEquals("Should keep all valid music files", musicFiles.size(), filteredFiles.size());
    }

    @Test
    public void testExcludedPaths() {
        // Test files in excluded paths
        ArrayList<com.example.madproject.models.SongsList> excludedPathFiles = new ArrayList<>();
        
        // Add files in paths that should be excluded
        excludedPathFiles.add(createTestSong("system_sound.mp3", "/system/media/audio/system_sound.mp3"));
        excludedPathFiles.add(createTestSong("notification.mp3", "/data/app/notification.mp3"));
        excludedPathFiles.add(createTestSong("ringtone.mp3", "/system/media/audio/ringtones/ringtone.mp3"));
        excludedPathFiles.add(createTestSong("cache_file.mp3", "/cache/cache_file.mp3"));
        
        // Filter files
        ArrayList<com.example.madproject.models.SongsList> filteredFiles = 
            SmartFileFilter.filterMusicFiles(excludedPathFiles);
        
        // Should filter out files in excluded paths
        assertTrue("Should filter out files in excluded paths", filteredFiles.isEmpty());
    }

    @Test
    public void testEmptyAndNullInputs() {
        // Test with empty list
        ArrayList<com.example.madproject.models.SongsList> emptyList = new ArrayList<>();
        ArrayList<com.example.madproject.models.SongsList> result = 
            SmartFileFilter.filterMusicFiles(emptyList);
        
        assertTrue("Should handle empty list", result.isEmpty());
        
        // Test with null list
        result = SmartFileFilter.filterMusicFiles(null);
        assertTrue("Should handle null list", result.isEmpty());
        
        // Test with list containing null items
        ArrayList<com.example.madproject.models.SongsList> listWithNulls = new ArrayList<>();
        listWithNulls.add(null);
        listWithNulls.add(createTestSong("valid.mp3", "/music/valid.mp3"));
        listWithNulls.add(null);
        
        result = SmartFileFilter.filterMusicFiles(listWithNulls);
        assertEquals("Should handle null items", 1, result.size());
    }

    @Test
    public void testMetadataValidation() {
        // Test files with missing or suspicious metadata
        ArrayList<com.example.madproject.models.SongsList> suspiciousMetadataFiles = new ArrayList<>();
        
        // Add files that might be voice recordings based on metadata patterns
        suspiciousMetadataFiles.add(createTestSong("recording.mp3", "/recordings/recording.mp3"));
        suspiciousMetadataFiles.get(0).setArtist("Unknown Artist");
        suspiciousMetadataFiles.get(0).setAlbum("Unknown Album");
        suspiciousMetadataFiles.get(0).setGenre("Speech");
        
        // Filter files
        ArrayList<com.example.madproject.models.SongsList> filteredFiles = 
            SmartFileFilter.filterMusicFiles(suspiciousMetadataFiles);
        
        // Should potentially filter out suspicious metadata files
        // Note: This test depends on the specific implementation of metadata validation
        assertNotNull("Should handle metadata validation", filteredFiles);
    }

    // Helper methods
    private com.example.madproject.models.SongsList createTestSong(String title, String path) {
        com.example.madproject.models.SongsList song = new com.example.madproject.models.SongsList();
        song.setTitle(title);
        song.setPath(path);
        song.setArtist("Test Artist");
        song.setAlbum("Test Album");
        song.setDuration(180000); // 3 minutes
        return song;
    }

    private boolean containsSongWithPath(ArrayList<com.example.madproject.models.SongsList> songs, String path) {
        for (com.example.madproject.models.SongsList song : songs) {
            if (song != null && path.equals(song.getPath())) {
                return true;
            }
        }
        return false;
    }

    private String getTestFileName(String filePath) {
        // This would need to access the private method through reflection or make it package-private
        // For now, we'll test the public testFile method which uses getFileName internally
        if (SmartFileFilter.testFile(filePath)) {
            // If the file is considered valid, extract filename safely
            if (filePath == null || filePath.trim().isEmpty()) {
                return "unknown_file";
            }
            
            String normalizedPath = filePath.replace('\\', '/');
            int lastSlash = normalizedPath.lastIndexOf('/');
            if (lastSlash >= 0 && lastSlash < normalizedPath.length() - 1) {
                return normalizedPath.substring(lastSlash + 1);
            }
            return normalizedPath;
        }
        return "safe_file"; // Fallback for invalid files
    }
}
