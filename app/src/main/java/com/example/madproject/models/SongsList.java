package com.example.madproject.models;

/**
 * Model class representing a single song/audio file.
 * Stores metadata retrieved from MediaStore.
 */
public class SongsList implements java.io.Serializable {

    private long id;
    private String title;
    private String artist;
    private String path;
    private long duration;
    private String album;
    private long albumId;

    // New metadata fields
    private String year;
    private String genre;
    private int bitrate;
    private int sampleRate;
    private long size;
    private long dateAdded;
    
    // Temporary field for recommendation scoring
    public transient double tempMoodScore;

    // Default constructor
    public SongsList() {
    }

    // Parameterized constructor
    public SongsList(long id, String title, String artist, String path, long duration, String album, long albumId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.album = album;
        this.albumId = albumId;
    }

    // Expanded constructor
    public SongsList(long id, String title, String artist, String path, long duration, String album, long albumId,
            String year, String genre, int bitrate, int sampleRate, long size, long dateAdded) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.duration = duration;
        this.album = album;
        this.albumId = albumId;
        this.year = year;
        this.genre = genre;
        this.bitrate = bitrate;
        this.sampleRate = sampleRate;
        this.size = size;
        this.dateAdded = dateAdded;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getBitrate() {
        return bitrate;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SongsList song = (SongsList) o;
        return path != null && path.equals(song.path);
    }

    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
}
