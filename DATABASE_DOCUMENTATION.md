# 🗄️ SonicWave Database Documentation

**Comprehensive database architecture and schema documentation for SonicWave Music Player**

---

## 📋 Overview

SonicWave uses **SQLite databases** for persistent storage with a focus on mood classification and playlist management. All databases are stored in the app's private data directory and are fully offline.

### Database Architecture

```
┌─────────────────────────────────────────────────────────┐
│                 SonicWave Database Layer                │
├─────────────────────────────────────────────────────────┤
│                                                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │
│  │ mood_tags.db │  │ playlists.db │  │ favorites.db │ │
│  │             │  │             │  │             │ │
│  │ Mood Data   │  │ Playlists    │  │ Favorites    │ │
│  └─────────────┘  └─────────────┘  └─────────────┘ │
│                                                     │
└─────────────────────────────────────────────────────────┘
```

### Database Files Location

```
/data/data/com.example.madproject/databases/
├── mood_tags.db
├── mood_tags.db-journal
├── playlists.db
├── playlists.db-journal
├── favorites.db
└── favorites.db-journal
```

---

## 📊 Mood Tags Database (`mood_tags.db`)

**Purpose**: Store mood classification results for songs with confidence scores and metadata.

### Schema Overview

```sql
CREATE TABLE mood_tags (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    song_path TEXT NOT NULL UNIQUE,
    mood_tag TEXT NOT NULL,
    confidence_score REAL DEFAULT 0.0,
    cultural_context TEXT DEFAULT 'UNIVERSAL',
    audio_features TEXT,
    analysis_timestamp INTEGER DEFAULT 0
);
```

### Column Details

| Column | Type | Constraints | Description | Example |
|---------|--------|-------------|------------|----------|
| **id** | INTEGER PRIMARY KEY AUTOINCREMENT | Auto-increment unique identifier | 1, 2, 3... |
| **song_path** | TEXT NOT NULL UNIQUE | Absolute file path - UNIQUE (unique identifier), not PRIMARY KEY; id is the PRIMARY KEY | `/storage/emulated/0/Music/song.mp3` |
| **mood_tag** | TEXT NOT NULL | Mood category: HAPPY, SAD, CALM, ENERGETIC | `HAPPY`, `SAD`, `CALM`, `ENERGETIC` |
| **confidence_score** | REAL DEFAULT 0.0 | Algorithm confidence (0.0-1.0) | 0.85, 0.67, 0.92 |
| **cultural_context** | TEXT DEFAULT 'UNIVERSAL' | Music context: BOLLYWOOD, WESTERN, UNIVERSAL | `BOLLYWOOD`, `WESTERN`, `UNIVERSAL` |
| **audio_features** | TEXT | JSON/serialized audio analysis data | `{"tempo":120,"key":"C","mode":"major"}` |
| **analysis_timestamp** | INTEGER DEFAULT 0 | Unix timestamp (ms) when mood was analyzed | 1672531200000 |

### Database Indexes

```sql
CREATE INDEX idx_song_path ON mood_tags(song_path);
CREATE INDEX idx_mood_tag ON mood_tags(mood_tag);
CREATE INDEX idx_timestamp ON mood_tags(analysis_timestamp);
```

**Performance Impact**:
- `idx_song_path`: ~5ms lookup for single song
- `idx_mood_tag`: ~8ms for mood-based queries
- `idx_timestamp`: ~12ms for time-based sorting

### Version History

| Version | Changes | Migration Strategy |
|---------|----------|-------------------|
| **v1.0** | Basic mood storage (id, song_path, mood_tag) | N/A (initial) |
| **v2.0** | Added confidence_score, cultural_context, audio_features, analysis_timestamp | `ALTER TABLE` with defaults |

### Sample Data

```sql
INSERT INTO mood_tags (song_path, mood_tag, confidence_score, cultural_context, analysis_timestamp)
VALUES 
('/storage/emulated/0/Music/happy.mp3', 'HAPPY', 0.92, 'UNIVERSAL', 1672531200000),
('/storage/emulated/0/Music/sad.mp3', 'SAD', 0.78, 'WESTERN', 1672531200000),
('/storage/emulated/0/Music/energetic.mp3', 'ENERGETIC', 0.85, 'BOLLYWOOD', 1672531200000);
```

---

## 📝 Playlists Database (`playlists.db`)

**Purpose**: Manage user-created playlists and song-to-playlist relationships.

### Schema Overview

```sql
CREATE TABLE playlists (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    created_at INTEGER DEFAULT 0
);

CREATE TABLE playlist_songs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    playlist_id INTEGER NOT NULL,
    song_path TEXT NOT NULL,
    song_title TEXT,
    song_artist TEXT,
    song_duration INTEGER,
    song_album TEXT,
    song_album_id INTEGER,
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE
);
```

### Column Details

| Table | Column | Type | Constraints | Description | Example |
|-------|---------|--------|-------------|------------|----------|
| **playlists** | id | INTEGER PRIMARY KEY AUTOINCREMENT | Auto-increment playlist identifier | 1, 2, 3... |
| | name | TEXT NOT NULL UNIQUE | Playlist name (must be unique) | `My Favorites`, `Workout Mix` |
| | created_at | INTEGER DEFAULT 0 | Creation timestamp (Unix ms) | 1672531200000 |
| **playlist_songs** | id | INTEGER PRIMARY KEY AUTOINCREMENT | Auto-increment relationship ID | 1, 2, 3... |
| | playlist_id | INTEGER NOT NULL | Foreign key to playlists table | 1, 2, 3... |
| | song_path | TEXT NOT NULL | Absolute file path to song | `/storage/emulated/0/Music/song.mp3` |
| | song_title | TEXT | Song title (cached for display) | `My Song Title` |
| | song_artist | TEXT | Song artist (cached for display) | `Artist Name` |
| | song_duration | INTEGER | Song duration in milliseconds | 240000 |
| | song_album | TEXT | Song album name | `Album Name` |
| | song_album_id | INTEGER | Album art ID for display | 1234567890123456 |

### Database Relationships

```
playlists (1) ←→ (many) playlist_songs
    ↓
playlist_id (FK)    song_path (cached metadata)
```

### Foreign Key Constraints

- **ON DELETE CASCADE**: When a playlist is deleted, all associated songs are automatically removed
- **NOT NULL**: playlist_id cannot be null (must belong to a playlist)
- **UNIQUE**: playlist names must be unique across all playlists

### Sample Data

```sql
-- Create playlists
INSERT INTO playlists (name, created_at) VALUES 
('My Favorites', 1672531200000),
('Workout Mix', 1672531300000),
('Relaxing Jazz', 1672531400000);

-- Add songs to playlists
INSERT INTO playlist_songs (playlist_id, song_path, song_title, song_artist, song_duration, song_album)
VALUES 
(1, '/storage/emulated/0/Music/song1.mp3', 'Happy Song', 'Artist 1', 180000, 'Album 1'),
(1, '/storage/emulated/0/Music/song2.mp3', 'Another Song', 'Artist 2', 210000, 'Album 2'),
(2, '/storage/emulated/0/Music/workout.mp3', 'Workout Track', 'DJ Fitness', 240000, 'Gym Hits');
```

---

## ❤️ Favorites Database (`favorites.db`)

**Purpose**: Store user's favorite songs for quick access.

### Schema Overview

```sql
CREATE TABLE favorites (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    song_path TEXT NOT NULL UNIQUE,
    song_title TEXT,
    song_artist TEXT,
    song_album TEXT,
    song_album_id INTEGER,
    song_duration INTEGER,
    added_at INTEGER DEFAULT 0
);
```

### Column Details

| Column | Type | Constraints | Description | Example |
|---------|--------|-------------|------------|----------|
| **id** | INTEGER PRIMARY KEY AUTOINCREMENT | Auto-increment favorite identifier | 1, 2, 3... |
| **song_path** | TEXT NOT NULL UNIQUE | Absolute file path - UNIQUE, not PRIMARY KEY; id is the PRIMARY KEY | `/storage/emulated/0/Music/song.mp3` |
| **song_title** | TEXT | Song title (cached for display) | `My Song Title` |
| **song_artist** | TEXT | Song artist (cached for display) | `Artist Name` |
| **song_album** | TEXT | Song album name | `Album Name` |
| **song_album_id** | INTEGER | Album art ID for display | 1234567890123456 |
| **song_duration** | INTEGER | Song duration in milliseconds | 240000 |
| **added_at** | INTEGER DEFAULT 0 | When favorited (Unix timestamp ms) | 1672531200000 |

### Sample Data

```sql
INSERT INTO favorites (song_path, song_title, song_artist, song_duration, song_album, added_at)
VALUES 
('/storage/emulated/0/Music/fav1.mp3', 'Favorite Song 1', 'Artist 1', 180000, 'Album 1', 1672531200000),
('/storage/emulated/0/Music/fav2.mp3', 'Favorite Song 2', 'Artist 2', 210000, 'Album 2', 1672531300000);
```

---

## 🔧 Database Operations

### Connection Management

All database operations use `SQLiteOpenHelper` subclasses:

```java
// Example: MoodDBHandler
public class MoodDBHandler extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mood_tags.db";
    private static final int DATABASE_VERSION = 2;
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_MOOD_TAGS_TABLE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle schema migrations
    }
}
```

### Transaction Management

Critical operations use transactions for data integrity:

```java
SQLiteDatabase db = dbHandler.getWritableDatabase();
db.beginTransaction();
try {
    // Multiple INSERT/UPDATE operations
    db.insert(TABLE_NAME, null, values);
    db.insert(TABLE_NAME, null, values2);
    db.setTransactionSuccessful();
} finally {
    db.endTransaction();
}
```

### Performance Optimizations

1. **Batch Operations**: Multiple INSERTs in single transaction
2. **Indexes**: Optimized for common query patterns
3. **Connection Pooling**: Reuse database connections
4. **Lazy Loading**: Load data only when needed

---

## 📈 Performance Metrics

### Database Size Impact

| Library Size | Mood DB Size | Playlist DB Size | Favorites DB Size |
|-------------|---------------|-----------------|------------------|
| **1,000 songs** | ~150KB | ~80KB | ~60KB |
| **5,000 songs** | ~750KB | ~400KB | ~300KB |
| **10,000 songs** | ~1.5MB | ~800KB | ~600KB |
| **50,000 songs** | ~7.5MB | ~4MB | ~3MB |

### Query Performance

| Operation | Database | Time | Notes |
|-----------|-----------|------|-------|
| **Single song lookup** | mood_tags | ~5ms | Indexed by song_path |
| **Mood-based query** | mood_tags | ~8ms | Indexed by mood_tag |
| **Playlist songs** | playlists | ~12ms | JOIN with playlist_songs |
| **Favorites list** | favorites | ~6ms | Simple SELECT all |

---

## 🔍 Database Debugging

### Common Issues

1. **Database Locked**: Ensure proper transaction management
2. **Corruption**: Check journal files and disk space
3. **Performance**: Verify indexes are created
4. **Migration Failures**: Test onUpgrade() thoroughly

### Debug Commands

```sql
-- Check table schema
.schema mood_tags

-- Analyze query performance
EXPLAIN QUERY PLAN SELECT * FROM mood_tags WHERE mood_tag = 'HAPPY';

-- Check indexes
.indexlist mood_tags

-- Database statistics
PRAGMA table_info(mood_tags);
PRAGMA index_list(mood_tags);
```

---

## 🛠️ Development Guidelines

### Best Practices

1. **Always close cursors** to prevent memory leaks
2. **Use transactions** for multiple operations
3. **Validate inputs** before database operations
4. **Handle migrations** properly in onUpgrade()
5. **Use parameterized queries** to prevent SQL injection

### Error Handling

```java
SQLiteDatabase db = null;
try {
    db = dbHandler.getWritableDatabase();
    // Database operations
} catch (SQLiteException e) {
    Log.e(TAG, "Database error", e);
    // Handle error gracefully
} finally {
    if (db != null) {
        db.close();
    }
}
```

---

## 📚 Related Files

### Database Handlers
- `MoodDBHandler.java` - Mood tags database schema
- `PlaylistDBHandler.java` - Playlists database schema
- `FavoritesDBHandler.java` - Favorites database schema

### Operations Classes
- `MoodOperations.java` - Mood tag CRUD operations
- `PlaylistOperations.java` - Playlist CRUD operations
- `FavoritesOperations.java` - Favorites CRUD operations

### Models
- `SongsList.java` - Song data model
- `Playlist.java` - Playlist data model

---

**Last Updated**: April 2026  
**Version**: 2.1.0  
**Database Version**: mood_tags v2.0, playlists v1.0, favorites v1.0
