package com.example.madproject.utils;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import com.example.madproject.models.SongsList;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Utility to import .m3u playlist files.
 * Parses lines from an m3u file and matches them against the device's song
 * library.
 */
public class M3uImporter {

    /**
     * Reads an m3u file from the given URI and returns a list of matched songs.
     *
     * @param context  Application context
     * @param uri      URI of the .m3u file
     * @param allSongs Complete library to match against
     * @return ArrayList of matched SongsList objects
     */
    public static ArrayList<SongsList> importPlaylist(Context context, Uri uri, ArrayList<SongsList> allSongs) {
        ArrayList<SongsList> matched = new ArrayList<>();
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null)
                return matched;

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and extended info
                if (line.isEmpty() || line.startsWith("#"))
                    continue;

                // Try absolute path match
                for (SongsList song : allSongs) {
                    if (song.getPath().endsWith(line) || line.endsWith(new File(song.getPath()).getName())) {
                        matched.add(song);
                        break;
                    }
                }
            }

            reader.close();
            inputStream.close();

        } catch (Exception e) {
            Toast.makeText(context, "Error importing M3U: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return matched;
    }
}
