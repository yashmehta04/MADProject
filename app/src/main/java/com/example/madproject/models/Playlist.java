package com.example.madproject.models;

import java.util.ArrayList;

/**
 * Model class representing a playlist.
 * Contains a name and a list of songs.
 */
public class Playlist {

    private int id;
    private String name;
    private ArrayList<SongsList> songs;

    public Playlist() {
        this.songs = new ArrayList<>();
    }

    public Playlist(int id, String name) {
        this.id = id;
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<SongsList> getSongs() {
        return songs;
    }

    public void setSongs(ArrayList<SongsList> songs) {
        this.songs = songs;
    }

    public int getSongCount() {
        return songs != null ? songs.size() : 0;
    }
}
