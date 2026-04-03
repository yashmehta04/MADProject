package com.example.madproject.utils;

import android.media.MediaMetadataRetriever;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Utility for loading offline lyrics.
 * Strategy 1: Check for .lrc file alongside audio file
 * Strategy 2: Attempt to read embedded lyrics from metadata
 */
public class LyricsLoader {

    /**
     * Attempts to load lyrics for a given audio file path.
     *
     * @param audioPath The full path of the audio file
     * @return Lyrics text, or null if not found
     */
    public static String loadLyrics(String audioPath) {
        if (audioPath == null)
            return null;

        // Strategy 1: Look for .lrc or .txt file next to the audio
        String lrcPath = audioPath.replaceAll("\\.[^.]+$", ".lrc");
        String txtPath = audioPath.replaceAll("\\.[^.]+$", ".txt");

        String result = readFileContent(lrcPath);
        if (result != null)
            return parseLrc(result);

        result = readFileContent(txtPath);
        if (result != null)
            return result;

        // Strategy 2: Check embedded lyrics in metadata
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(audioPath);
            // METADATA_KEY_LYRICS is not a standard key; some formats store it differently
            // We try a generic approach
            String embedded = mmr.extractMetadata(25); // 25 is sometimes LYRICS
            if (embedded != null && !embedded.isEmpty())
                return embedded;
        } catch (Exception ignored) {
        } finally {
            try {
                mmr.release();
            } catch (Exception e) {
                Log.w("LyricsLoader", "Error releasing MediaMetadataRetriever", e);
            }
        }

        return null;
    }

    private static String readFileContent(String path) {
        File file = new File(path);
        if (!file.exists())
            return null;

        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Strips timing tags from LRC format to display plain lyrics.
     * [00:12.34] Some lyrics → Some lyrics
     */
    private static String parseLrc(String lrcContent) {
        StringBuilder sb = new StringBuilder();
        String[] lines = lrcContent.split("\n");
        for (String line : lines) {
            // Remove all [xx:xx.xx] tags
            String cleaned = line.replaceAll("\\[\\d{2}:\\d{2}\\.\\d{2,3}\\]", "").trim();
            if (!cleaned.isEmpty() && !cleaned.startsWith("[")) {
                sb.append(cleaned).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
