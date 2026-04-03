# 🎵 SonicWave — Premium Offline Music Player for Android

<p align="center">
  <strong>A modern, feature-rich, offline music player built natively for Android using Java.</strong><br>
  Version 2.0.0 · Powered by <b>Media3 ExoPlayer</b> · Beautiful Dark UI · YouTube Music-style Seekbar · Full Equalizer · Home Screen Widget
</p>

---

## 📋 Table of Contents

- [Overview](#overview)
- [Screenshots](#screenshots)
- [Architecture](#architecture)
- [Features](#features)
  - [🎧 Playback Engine](#-playback-engine-media3-exoplayer)
  - [🏠 Home Dashboard](#-home-dashboard)
  - [📚 Library Intelligence](#-library-intelligence)
  - [🎨 Now Playing Screen](#-now-playing-screen-visual-polish)
  - [🎛️ Equalizer & Audio Effects](#️-equalizer--audio-effects)
  - [📝 Playlists & Favorites](#-playlists--favorites)
  - [🔀 Queue Management](#-queue-management)
  - [📲 System Integration](#-system-integration)
  - [🔍 Search](#-search)
  - [📊 Daily Usage Tracking](#-daily-usage-tracking)
  - [🎤 Offline Lyrics](#-offline-lyrics)
  - [🛡️ Duplicate Detection](#️-duplicate-detection)
  - [🧠 Mood-Based Music Suggestions](#-mood-based-music-suggestions)
  - [🚀 Performance Optimizations](#-performance-optimizations)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup & Installation](#setup--installation)
- [Permissions](#permissions)
- [Build Configuration](#build-configuration)
- [License](#license)

---

## Overview

**SonicWave** is a premium, fully offline music player application developed natively for Android using **Java**. It is designed to deliver a Spotify/Apple Music-level user experience without requiring any internet connection. The app scans your device's local storage for audio files and presents them in a beautifully designed dark-themed interface with advanced playback capabilities powered by Google's latest **Media3 ExoPlayer** library.

The application was built as part of a Mobile Application Development (MAD) project and features a comprehensive set of capabilities that rival commercial music players, including a waveform-styled seekbar, built-in equalizer with bass boost, drag-to-reorder queue, home screen widgets, offline lyrics rendering, mood-based music suggestions, and much more.

---

## Architecture

SonicWave follows a modular **Activity-Fragment-Service** architecture:

```
┌──────────────────────────────────────────────────┐
│                  SplashActivity                   │
│            (Animated Launch Screen)               │
└──────────────────┬───────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────┐
│                 MainActivity                      │
│  ┌──────────────────────────────────────────┐    │
│  │           ViewPager + BottomNav           │    │
│  │  ┌──────┬────────┬──────┬─────┬─────┬────┐│    │
│  │  │ Home │NowPlay │ All  │ Fav │Play │Mood││    │
│  │  │ Frag │ Frag   │ Song │Song │list │Quiz││    │
│  │  └──────┴────────┴──────┴─────┴─────┴────┘│    │
│  └──────────────────────────────────────────┘    │
│  ┌──────────────────────────────────────────┐    │
│  │          Bottom Mini Player Bar           │    │
│  └──────────────────────────────────────────┘    │
└──────────────────┬───────────────────────────────┘
                   ▼
┌──────────────────────────────────────────────────┐
│        PlaybackService (Foreground Service)       │
│   ┌────────────┐  ┌───────────────┐              │
│   │  ExoPlayer  │  │ MediaSession  │              │
│   │  (Media3)   │  │ (Lock/Notif)  │              │
│   └────────────┘  └───────────────┘              │
└──────────────────────────────────────────────────┘
```

---

## Features

### 🎧 Playback Engine (Media3 ExoPlayer)

The heart of SonicWave is a complete **Media3 ExoPlayer** integration running inside an Android `MediaSessionService`. This replaces the legacy `MediaPlayer` API with Google's newest, most powerful audio engine.

| Capability | Description |
|------------|-------------|
| **Background Playback** | Music continues playing when the app is minimized or the screen is off, via a foreground service |
| **Gapless Playback** | ExoPlayer natively eliminates silence between consecutive tracks, perfect for live albums and DJ mixes |
| **Lock Screen Controls** | Full media controls with album artwork displayed directly on the Android lock screen via `MediaSession` |
| **Notification Player** | A persistent pull-down notification with play/pause, next, previous controls and song metadata |
| **Resume After Kill** | The foreground service architecture ensures playback survives app process termination |
| **Audio Focus Management** | Properly requests and handles audio focus, auto-pausing when other apps need audio |
| **Headphone Disconnect** | Automatically pauses playback when headphones are unplugged (`setHandleAudioBecomingNoisy(true)`) |

**Key files:**
- `PlaybackService.java` — The `MediaSessionService` that hosts ExoPlayer and MediaSession
- `ExoPlayerManager.java` — Singleton wrapper that connects the UI to the background service via `MediaController`

---

### 🏠 Home Dashboard

A Netflix/Spotify-inspired landing page that greets users with personalized, horizontally scrolling content carousels.

| Section | Description |
|---------|-------------|
| **Continue Listening** | Shows the **last played song** with album art and a play button for instant resumption |
| **Recently Added** | Displays the 20 most recently added songs to the device, sorted by date, for instant access to new music |
| **Daily Usage Graph** | A weekly bar graph showing minutes spent each day, with "Tap for detailed analytics" |
| **Advanced Analytics** | Tapping the usage card opens a full-page dialog with month/year filtering, swipeable day-by-day bars, totals, and averages |
| **Discover by Mood** | Card with "Take the Quiz" button for mood-based song recommendations |

**Key files:**
- `HomeFragment.java` — Dashboard logic, usage graph, last-played song, analytics dialog
- `fragment_home.xml` — Dashboard layout with single-song card, bar graph, and mood card

---

### 📚 Library Intelligence

The "All Songs" tab is a full-featured **Library Browser** with a `TabLayout` providing four distinct browsing modes:

| Tab | Description |
|-----|-------------|
| **Songs** | A flat list of every audio file on the device, with title, artist, album art, and duration |
| **Albums** | Groups songs by album name, displayed in a grid with album artwork thumbnails |
| **Artists** | Groups songs by artist name, showing a count of tracks per artist |
| **Folders** | Mirrors the device's folder structure, perfect for users who organize music manually in directories |

Each grouping is powered by the `GroupAdapter` with a `LibraryGroupItem` model, and tapping any group filters the song list to show only those tracks.

**Full Metadata Parsing:**  
The scanner extracts rich metadata from each audio file via Android's `MediaStore`:
- Title, Artist, Album, Album Art
- Duration, File Path, File Size
- Year, Genre (via ID3 tag parsing at runtime)
- Bitrate, Sample Rate, Genre (extracted at runtime)

**Key files:**
- `AllSongFragment.java` — Library fragment with TabLayout + grouping logic
- `GroupAdapter.java` — RecyclerView adapter for album/artist/folder grid/list views
- `LibraryGroupItem.java` — Data model for grouped items
- `StorageScanner.java` — MediaStore query engine with metadata extraction

---

### 🎨 Now Playing Screen (Visual Polish)

The crown jewel of the app — a stunning, full-screen "Now Playing" experience with multiple layers of visual polish:

| Element | Description |
|---------|-------------|
| **Large Album Artwork** | The album art is displayed prominently at the center of the screen |
| **Blurred Background** | The album art is also rendered as a full-screen blurred background using `RenderEffect` (Android 12+) for an immersive look |
| **Dynamic Color Palette** | Using `androidx.palette`, the background tint dynamically adapts to the dominant color of the album art |
| **YouTube Music-style Seekbar** | A custom `WaveformSeekBar` with thin track, accent-filled progress, and circular thumb with glow-on-drag. Seek fires only on touch release for smooth scrubbing |
| **Genre Tag** | A styled chip badge showing the song's genre, displayed below the artist name |
| **Clean Controls** | Play/Pause, Next, Previous, Shuffle (mutually exclusive with Repeat), Repeat, Favorite, and Add to Playlist buttons |
| **Edge-to-Edge Layout** | The app uses `WindowCompat.setDecorFitsSystemWindows(false)` to draw behind the system status bar and navigation bar for a truly immersive feel |
| **State Restoration** | On cold start, the last played song is automatically restored in the Now Playing UI and mini player |

**Playback Controls:**
- Play / Pause (with dynamic icon toggle)
- Next / Previous track
- Shuffle toggle (with visual highlight via alpha + tint)
- Repeat toggle (single track loop)
- Favorite toggle (heart icon with filled/outlined states)

**Key files:**
- `CurrentSongFragment.java` — All Now Playing logic, genre display, palette, blur
- `fragment_current_song.xml` — Clean layout with seekbar, controls, genre tag, lyrics
- `WaveformSeekBar.java` — Custom YouTube Music-style seekbar view

---

### 🎛️ Equalizer & Audio Effects

A dedicated **Equalizer Activity** accessible from the navigation drawer, providing professional-grade audio tuning:

| Control | Description |
|---------|-------------|
| **Enable/Disable Toggle** | A master switch to turn the entire equalizer system on or off |
| **Preset Selection** | A dropdown spinner populated with the device's built-in EQ presets (e.g., Rock, Pop, Jazz, Classical, Bass Boost, etc.) |
| **Band Sliders** | Dynamically generated `SeekBar` sliders for each frequency band (typically 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz), allowing per-band gain adjustment |
| **Bass Boost** | A dedicated slider controlling the `BassBoost` audio effect engine (0–1000 strength) |
| **Virtualizer** | A surround-sound emulation slider using the `Virtualizer` effect (0–1000 strength) |
| **Volume Normalization** | Uses Android's `LoudnessEnhancer` to automatically balance volume levels across tracks with different recording levels |

All effects use Android's native `android.media.audiofx` APIs and are applied directly to the audio session.

**Key files:**
- `EqualizerActivity.java` — Full equalizer with dynamic band generation
- `activity_equalizer.xml` — ScrollView layout with all controls

---

### 📝 Playlists & Favorites

SonicWave includes a robust, database-backed system for organizing music:

**Playlists:**
- Create unlimited custom playlists with a dialog prompt
- Add songs to playlists from the Now Playing screen ("Add to Playlist" button)
- Clicking any playlist opens a **searchable song picker** with multi-select checkboxes to add songs
- After adding (or on Cancel), see all existing songs with checkboxes to **delete individual songs**
- "Add More" button to reopen the song picker without leaving the dialog
- Delete entire playlists (with cascade deletion)
- Playlists persist across app restarts via SQLite

**Liked Songs:**
- One-tap heart icon on the Now Playing screen to mark songs as favorites
- Dedicated **"Liked"** tab in the bottom navigation bar
- Liked songs stored in a separate SQLite database table
- Quick toggle with visual feedback (filled heart = favorited)

**Key files:**
- `PlaylistFragment.java` — Playlist listing, creation, and song-picker UI
- `FavSongFragment.java` — Liked songs listing fragment
- `PlaylistOperations.java` — CRUD operations for playlists
- `FavoritesOperations.java` — CRUD operations for favorites
- `PlaylistDBHandler.java` — SQLite schema for playlists
- `FavoritesDBHandler.java` — SQLite schema for favorites

---

### 🔀 Queue Management

A full **Queue Manager** accessible from the navigation drawer:

| Feature | Description |
|---------|-------------|
| **View Current Queue** | See all upcoming songs in playback order |
| **Drag-to-Reorder** | Long-press the drag handle (≡) on any song and physically drag it up or down to change the playback order. Powered by `ItemTouchHelper` |
| **Remove from Queue** | Tap the X button on any song to instantly remove it from the queue |
| **Tap to Play** | Tap any song in the queue to jump directly to it |
| **Swipe Gestures** | Swipe right on a song to add to queue, swipe left to remove (in the main library) |
| **Shuffle Intelligence** | When shuffle is activated, the app generates a randomized index list to avoid predictable patterns |

**Key files:**
- `QueueActivity.java` — Full-screen queue manager with `ItemTouchHelper` drag-and-drop
- `QueueAdapter.java` — RecyclerView adapter with drag handle touch events
- `activity_queue.xml` — Queue layout
- `item_queue.xml` — Queue item layout with drag handle and remove button

---

### 📲 System Integration

**Home Screen Widget:**  
A resizable Android App Widget that sits on the home screen, displaying:
- Current song title and artist
- Play/Pause, Next, and Previous buttons
- Album artwork thumbnail
- Tap-to-open-app functionality

The widget automatically updates whenever a new song starts playing.

**Lock Screen & Notification:**  
Powered by `MediaSession`, the system automatically generates:
- A full-screen lock screen media controller with album art
- A persistent notification with transport controls (play, pause, next, previous)
- Both update in real-time as songs change

**Key files:**
- `PlayerWidgetProvider.java` — `AppWidgetProvider` with remote views and broadcast receivers
- `widget_player.xml` — Widget layout
- `player_widget_info.xml` — Widget provider metadata

---

### 🔍 Search

Real-time search with results updating as the user types:

- Search across **song titles**, **artist names**, **album names**, and **folder paths**
- Integrated into the toolbar as a `SearchView`
- Case-insensitive matching
- Results filter the visible song list instantly
- Works within the library's currently active tab (Songs, Albums, Artists, Folders)

---

### 📊 Daily Usage Tracking

Track how much time you spend listening to music each day:

| Feature | Description |
|---------|-------------|
| **Weekly Bar Graph** | Visual bar graph on the Dashboard showing usage for the last 7 days |
| **Today Summary** | Accent-highlighted total for today's listening time |
| **Zoom View** | Tap the usage card to see a detailed breakdown with totals and daily averages |
| **Session Tracking** | Automatically records app open/close durations |
| **Persistent Storage** | Data stored in SharedPreferences, survives app restarts |

**Key files:**
- `UsageTracker.java` — SharedPreferences-based session tracker with weekly data API
- `HomeFragment.java` — Renders the bar graph and handles zoom dialog

---

### 🎤 Offline Lyrics

SonicWave can display lyrics without any internet connection using two strategies:

| Strategy | Description |
|----------|-------------|
| **LRC File Detection** | Automatically looks for a `.lrc` file with the same name as the audio file in the same directory. LRC timing tags (`[00:12.34]`) are stripped to display clean text |
| **TXT File Detection** | Falls back to a `.txt` file with the same name if no `.lrc` file exists |
| **Embedded Lyrics** | Attempts to extract lyrics embedded directly in the audio file's metadata tags |

When lyrics are found, they appear in a scrollable area below the playback controls on the Now Playing screen.

**Key files:**
- `LyricsLoader.java` — Multi-strategy lyrics resolver
- `CurrentSongFragment.java` — Renders lyrics in the UI

---

### 🛡️ Duplicate Detection

The storage scanner includes an intelligent **duplicate detection algorithm**:

- As songs are scanned from `MediaStore`, a composite signature key is generated for each track: `Title + Artist + Duration`
- The key is checked against a `HashSet` of previously seen signatures
- If a duplicate signature is found (e.g., the same MP3 exists in two different folders), the duplicate is silently skipped
- This keeps the library clean without requiring manual intervention

**Key files:**
- `StorageScanner.java` — HashSet-based deduplication in the scan loop

---

### 🧠 Mood-Based Music Suggestions

SonicWave includes an intelligent, **fully offline** mood-based recommendation system that suggests songs from the user's local library without any external APIs or internet connectivity.

#### How It Works

```
MediaStore Scanner
       ↓
Metadata Extraction (Genre, Title, Duration)
       ↓
MoodAlgorithm (Heuristic Classification + Scoring)
       ↓
SQLite Database (song_path → mood_tag)
       ↓
User Questionnaire → Detect Mood
       ↓
Query Database (Randomized Results)
       ↓
Dynamic Recommended Playlist
```

#### Multi-Factor Scoring Algorithm

Each song is classified using a weighted heuristic scoring system:

| Factor | Weight | Description |
|--------|--------|-------------|
| **Genre (ID3 Tag)** | +3 | Primary signal extracted via `MediaMetadataRetriever` |
| **Title Keywords** | +2 | Fallback signal using keyword matching (e.g., "sad", "party", "dream") |
| **Duration** | +1 | Supporting signal: short songs → energetic, long songs → calm |

#### Mood Categories & Genre Mapping

| Mood | Genres | Example Title Keywords |
|------|--------|------------------------|
| 😊 **HAPPY** | Pop, Dance, EDM, Disco, Funk, K-Pop, Bollywood | happy, love, party, dance, sunshine |
| 😢 **SAD** | Blues, Soul, Ballad, Emo, Country | sad, alone, cry, broken, goodbye |
| 😌 **CALM** | Classical, Jazz, Ambient, Lo-fi, Folk, Acoustic | peace, calm, dream, sleep, whisper |
| 🔥 **ENERGETIC** | Rock, Metal, Punk, Hip-Hop, Rap, Dubstep, Trap | fire, rage, power, fight, thunder |

#### Interactive Mood Questionnaire

A 5-question multiple-choice quiz presented with smooth animations and progress dots:

1. **How are you feeling right now?** — Cheerful / Down / Peaceful / Excited
2. **What kind of music would you like?** — Upbeat / Emotional / Mellow / Intense
3. **Energy level?** — Bright / Low / Quiet / Unstoppable
4. **Pick the vibe** — Celebration / Alone / Meditation / Workout
5. **What would improve your mood?** — Dancing / Soulful melody / Piano / Heavy riffs

The user's mood is determined via **majority voting** across all 5 answers. The app then queries the local SQLite database for up to 20 randomized matching songs and immediately starts playback.

#### Key Design Decisions

| Decision | Rationale |
|----------|----------|
| **SQLite storage** (not file modification) | Avoids Android scoped storage permission issues; no file writes required |
| **Heuristic scoring** (not ML/raw audio) | Instant classification with zero battery drain; no large model files needed |
| **Background `ExecutorService`** | Non-blocking UI; only processes untagged songs (skip-if-tagged optimization) |
| **`song_path` as key** (not MediaStore ID) | File paths are stable across rescans; MediaStore IDs can change |

**Key files:**
- `MoodAlgorithm.java` — Multi-factor scoring engine with background scanner
- `MoodDBHandler.java` — SQLite schema for the `mood_tags` table
- `MoodOperations.java` — CRUD operations (batch insert, query by mood)
- `MoodQuestionnaireFragment.java` — Interactive quiz UI with animations
- `fragment_mood_questionnaire.xml` — Quiz layout with Material Design cards
- `HomeFragment.java` — "Discover by Mood" card on the home dashboard

---

### 🚀 Performance Optimizations

| Optimization | Description |
|-------------|-------------|
| **RecyclerView + ViewHolder** | All lists use RecyclerView with the ViewHolder pattern for efficient scrolling |
| **Glide Image Loading** | Album artwork is loaded asynchronously via Glide with placeholder fallbacks, preventing UI lag |
| **Background Thread Metadata** | Heavy ID3 parsing (bitrate, sample rate, year, genre) runs on a background thread to prevent ANR (Application Not Responding) |
| **Handler-based Seekbar Updates** | The seekbar position updates every 100ms via a `Handler`/`Runnable` loop, not a blocking timer |
| **Singleton Player Manager** | The `ExoPlayerManager` uses a singleton pattern to prevent multiple player instances |
| **Lazy Fragment Loading** | Fragments are initialized lazily through `ViewPager` with an `offscreenPageLimit` |

---

## Tech Stack

| Technology | Version / Details |
|------------|------------------|
| **Language** | Java 17 |
| **Min SDK** | 21 (Android 5.0 Lollipop) |
| **Target SDK** | 34 (Android 14) |
| **Build System** | Gradle 8.x with Kotlin DSL |
| **Audio Engine** | [androidx.media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer) |
| **Media Session** | [androidx.media3.session](https://developer.android.com/media/media3/session) |
| **Image Loading** | [Glide](https://bumptech.github.io/glide/) |
| **Color Extraction** | [AndroidX Palette](https://developer.android.com/develop/ui/views/graphics/palette-colors) |
| **UI Framework** | Material Components, ConstraintLayout, RecyclerView, ViewPager, DrawerLayout |
| **Database** | SQLite (via `SQLiteOpenHelper`) |
| **Audio Effects** | `android.media.audiofx` (Equalizer, BassBoost, Virtualizer, LoudnessEnhancer) |

---

## Project Structure

```
app/src/main/java/com/example/madproject/
├── activities/
│   ├── SplashActivity.java          # Animated splash screen with fade-in
│   ├── MainActivity.java            # Main host activity with ViewPager + BottomNav
│   ├── QueueActivity.java           # Drag-to-reorder queue manager
│   └── EqualizerActivity.java       # Full equalizer with bands, bass, virtualizer
│
├── fragments/
│   ├── HomeFragment.java            # Netflix-style dashboard with carousels + mood card
│   ├── CurrentSongFragment.java     # Now Playing screen with waveform + lyrics
│   ├── AllSongFragment.java         # Library browser (Songs/Albums/Artists/Folders)
│   ├── FavSongFragment.java         # Favorites list
│   ├── PlaylistFragment.java        # Playlist management
│   └── MoodQuestionnaireFragment.java # Mood quiz UI with scoring + recommendations
│
├── adapters/
│   ├── SongAdapter.java             # RecyclerView adapter for song items
│   ├── GroupAdapter.java            # Adapter for album/artist/folder groups
│   ├── QueueAdapter.java            # Queue adapter with drag handle
│   ├── PlaylistAdapter.java         # Playlist list adapter
│   └── ViewPagerAdapter.java        # Fragment pager adapter
│
├── services/
│   └── PlaybackService.java         # Media3 MediaSessionService (ExoPlayer)
│
├── utils/
│   ├── ExoPlayerManager.java        # Singleton MediaController wrapper
│   ├── MediaPlayerManager.java      # Legacy MediaPlayer wrapper (deprecated)
│   ├── StorageScanner.java          # MediaStore scanner with deduplication
│   ├── MoodAlgorithm.java          # Multi-factor mood classification engine
│   ├── UsageTracker.java           # Daily usage tracking (SharedPreferences)
│   ├── M3uImporter.java            # M3U playlist file parser (legacy)
│   ├── LyricsLoader.java           # Offline lyrics loader (.lrc, .txt, embedded)
│   └── TimeFormatter.java          # Duration formatting utility
│
├── views/
│   └── WaveformSeekBar.java         # Custom waveform-style seekbar view
│
├── widgets/
│   └── PlayerWidgetProvider.java    # Home screen media control widget
│
├── models/
│   ├── SongsList.java               # Song data model with full metadata
│   ├── Playlist.java                # Playlist data model
│   └── LibraryGroupItem.java        # Group item model (album/artist/folder)
│
├── database/
│   ├── PlaylistDBHandler.java       # SQLite schema for playlists
│   ├── PlaylistOperations.java      # Playlist CRUD operations
│   ├── FavoritesDBHandler.java      # SQLite schema for favorites
│   ├── FavoritesOperations.java     # Favorites CRUD operations
│   ├── MoodDBHandler.java           # SQLite schema for mood tags
│   └── MoodOperations.java          # Mood tag CRUD + query by mood
│
└── interfaces/
    ├── SongSelectionListener.java   # Fragment-to-Activity communication
    └── PlaylistActionListener.java  # Playlist action callbacks

app/src/main/res/
├── layout/
│   ├── activity_main.xml            # Main activity with drawer + ViewPager
│   ├── activity_splash.xml          # Splash screen layout
│   ├── activity_queue.xml           # Queue manager layout
│   ├── activity_equalizer.xml       # Equalizer layout
│   ├── fragment_home.xml            # Home dashboard with mood discovery card
│   ├── fragment_current_song.xml    # Now Playing layout (waveform + lyrics)
│   ├── fragment_all_songs.xml       # Library layout with TabLayout
│   ├── fragment_favorites.xml       # Favorites layout
│   ├── fragment_playlist.xml        # Playlist layout
│   ├── fragment_mood_questionnaire.xml # Mood quiz layout with progress dots
│   ├── bottom_mini_player.xml       # Persistent mini player bar
│   ├── widget_player.xml            # Home screen widget layout
│   ├── item_song.xml                # Song list item
│   ├── item_album.xml               # Album grid item
│   ├── item_generic_list.xml        # Artist/Folder list item
│   ├── item_queue.xml               # Queue item with drag handle
│   └── item_playlist.xml            # Playlist item
├── xml/
│   └── player_widget_info.xml       # Widget provider metadata
├── drawable/                         # Vector icons (play, pause, next, etc.)
├── values/
│   ├── colors.xml                   # Dark theme color palette
│   ├── strings.xml                  # All string resources
│   └── themes.xml                   # Material dark theme definition
└── menu/
    ├── nav_menu.xml                 # Drawer menu (Queue, Equalizer, Import, About)
    └── toolbar_menu.xml             # Toolbar search menu
```

---

## Setup & Installation

### Prerequisites
- **Android Studio** Hedgehog (2023.1+) or newer
- **JDK 17** or higher
- An Android device or emulator running **API 21+** (Android 5.0 Lollipop or higher)

### Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/yourusername/SonicWave.git
   cd SonicWave
   ```

2. **Open in Android Studio:**
   - File → Open → Select the project root folder

3. **Sync Gradle:**
   - Android Studio will automatically download all dependencies
   - If not, click **"Sync Now"** in the Gradle notification bar

4. **Build the project:**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Run on a device:**
   - Connect an Android device via USB (with USB Debugging enabled)
   - Or launch an emulator from AVD Manager
   - Click the **Run ▶** button in Android Studio

6. **Grant permissions:**
   - On first launch, grant **Storage Permission** when prompted
   - On Android 13+, grant **Media Audio Permission**

---

## Permissions

| Permission | Purpose | API Level |
|------------|---------|-----------|
| `READ_EXTERNAL_STORAGE` | Access audio files on the device | API 21–32 |
| `READ_MEDIA_AUDIO` | Access audio files (scoped storage) | API 33+ |
| `FOREGROUND_SERVICE` | Keep playback alive in background | API 26+ |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Foreground service type declaration | API 34+ |

---

## Build Configuration

| Property | Value |
|----------|-------|
| Application ID | `com.example.madproject` |
| Compile SDK | 34 |
| Min SDK | 21 |
| Target SDK | 34 |
| Java Version | 17 |
| Version Code | 1 |
| Version Name | 1.0 |

### Dependencies

| Library | Purpose |
|---------|---------|
| `androidx.appcompat` | Backward-compatible Activity/Fragment APIs |
| `com.google.android.material` | Material Design components (BottomNav, TabLayout, FAB) |
| `androidx.constraintlayout` | Flexible constraint-based layouts |
| `androidx.drawerlayout` | Navigation drawer |
| `androidx.viewpager` | Swipeable fragment pages |
| `androidx.recyclerview` | Efficient scrolling lists |
| `androidx.palette` | Dynamic color extraction from album art |
| `com.github.bumptech.glide` | Async image loading with caching |
| `androidx.media3:exoplayer` | Modern audio playback engine |
| `androidx.media3:session` | Media session for system integration |
| `androidx.media3:ui` | Media3 UI components |

---

## License

This project was developed as part of a **Mobile Application Development (MAD)** academic project.

```
MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

<p align="center">
  <strong>SonicWave — Feel Every Frequency</strong><br>
  <br>
  Created by:<br>
  <strong>F030 - Mayur H. Doshi</strong><br>
  <strong>F030 - Keval N. Mehta</strong><br>
  <strong>F052 - Yash D. Mehta</strong><br>
  <br>
  Made with ❤️ for music lovers
</p>
