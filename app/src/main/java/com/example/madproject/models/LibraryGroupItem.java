package com.example.madproject.models;

import java.util.ArrayList;

public class LibraryGroupItem {
    private String title;
    private String subtitle;
    private long albumId; // For album art
    private ArrayList<SongsList> songs;

    public LibraryGroupItem(String title, String subtitle, long albumId, ArrayList<SongsList> songs) {
        this.title = title;
        this.subtitle = subtitle;
        this.albumId = albumId;
        this.songs = songs;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public long getAlbumId() {
        return albumId;
    }

    public ArrayList<SongsList> getSongs() {
        return songs;
    }
}
