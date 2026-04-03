# 🎵 Decible — Android Music Player

> A feature-rich, local music player for Android built as a Mobile Application Development (MAD) project by **Mayur, Keval, and Yash**.

---

## 📋 Table of Contents

1. [Project Overview](#project-overview)
2. [Tools & Environment](#tools--environment)
3. [Programming Language](#programming-language)
4. [Frameworks & Libraries](#frameworks--libraries)
5. [Project Architecture](#project-architecture)
6. [Package & File Structure](#package--file-structure)
7. [Application Flow](#application-flow)
8. [UI & Layout Design](#ui--layout-design)
9. [Database Design](#database-design)
10. [Key Features](#key-features)
11. [Fragments (Tabs)](#fragments-tabs)
12. [Adapters](#adapters)
13. [Data Models](#data-models)
14. [Permissions](#permissions)
15. [Color Scheme & Theming](#color-scheme--theming)
16. [Build Configuration](#build-configuration)
17. [How to Run the Project](#how-to-run-the-project)

---

## Project Overview

**Decible** is a native Android music player application that reads audio files stored on the device's external storage and provides a complete playback experience. The app includes playlist management, a favorites system, real-time search, shuffle, loop, and repeat modes — all wrapped in a dark-themed Material Design UI.

| Property | Value |
|---|---|
| App Name | Music Player (Decible) |
| Package | `com.example.soc_macmini_15.musicplayer` |
| Platform | Android |
| Min Android Version | API 21 — Android 5.0 (Lollipop) |
| Target Android Version | API 34 — Android 14 |
| Language | Java |
| IDE | Android Studio |
| Build System | Gradle |

---

## Tools & Environment

### IDE — Android Studio
The entire project is built and managed inside **Android Studio**, Google's official IDE for Android development. It provides:
- Layout editor for XML UI design (drag-and-drop + code view)
- Logcat for runtime debugging
- AVD Manager for running Android Virtual Devices (emulators)
- Gradle integration for dependency management
- Manifest editor and resource management

### Build System — Gradle
Gradle is the build automation tool used in this project. It manages:
- App compilation and packaging (`.apk` generation)
- Library/dependency downloads from Maven repositories
- Build variants (debug and release)
- ProGuard configuration for release builds

The project uses **Gradle Wrapper version 9.1.0**, meaning the correct Gradle version is automatically downloaded — no manual installation needed.

### Version Control — Git / GitHub
The project is hosted on GitHub as `Decible-MAD-Project--Music-Player-master`, indicating use of Git for source control. `.gitignore` files are present to exclude build artifacts and IDE-specific files from commits.

---

## Programming Language

### Java
The entire application is written in **Java**. No Kotlin is used. Java is the original language for Android development and is fully supported by Android Studio.

Key Java features used in this project:
- **Object-Oriented Programming (OOP)** — Classes, interfaces, inheritance (`AppCompatActivity`, `ListFragment`, `ArrayAdapter`)
- **Anonymous Inner Classes** — Used extensively for event listeners (`OnClickListener`, `OnItemClickListener`, `OnCompletionListener`)
- **Interfaces** — Custom Java interfaces are defined in Fragments to enable communication back to the Activity (the "callback pattern")
- **AsyncTask** — Used in `AllSongFragment` to load songs from storage on a background thread, keeping the UI thread responsive
- **Collections** — `ArrayList` and `Collections.shuffle()` are used for managing song lists and implementing shuffle mode
- **Handler & Runnable** — Used to update the SeekBar progress every 100 milliseconds in real time while a song plays

---

## Frameworks & Libraries

### AndroidX (Android Jetpack)
AndroidX is Google's modern set of Android support libraries. The following AndroidX libraries are used:

| Library | Version | Purpose |
|---|---|---|
| `androidx.appcompat:appcompat` | 1.6.1 | Backward-compatible versions of Activities, Fragments, AlertDialogs, and the Toolbar |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | Flexible, flat view layouts (used in splash and other screens) |
| `androidx.drawerlayout:drawerlayout` | 1.2.0 | The slide-out Navigation Drawer on the left side |
| `androidx.viewpager:viewpager` | 1.0.0 | Enables swiping between the four tab fragments |

### Material Components for Android
Google's Material Design component library (`com.google.android.material:material:1.11.0`) provides:

| Component | Usage |
|---|---|
| `FloatingActionButton` | The Refresh Songs button (bottom-right corner) and the Add Playlist button |
| `NavigationView` | The navigation drawer's menu and header |
| `TabLayout` | The horizontal tab bar (All Songs, Current Song, Favorites, Playlists) |
| `Snackbar` | Imported but available for brief dismissible notifications |

### Android SDK (Built-in APIs)
Several Android SDK APIs are used without additional dependencies:

| API | Purpose |
|---|---|
| `android.media.MediaPlayer` | Core audio playback — loads, plays, pauses, seeks, and loops audio files |
| `android.database.sqlite.SQLiteOpenHelper` | Base class for managing SQLite database creation and version upgrades |
| `android.database.sqlite.SQLiteDatabase` | Executes SQL queries (INSERT, SELECT, DELETE, UPDATE) |
| `android.provider.MediaStore` | Queries the device's media content provider to get all audio files |
| `android.content.ContentResolver` | Interface to query `MediaStore` using a URI-based content system |
| `android.os.Handler` | Posts delayed tasks on the main (UI) thread — used for the SeekBar update loop |
| `android.widget.SearchView` | The collapsible search bar in the action bar |

### Testing Libraries

| Library | Version | Purpose |
|---|---|
| `junit:junit` | 4.13.2 | Unit testing framework |
| `androidx.test.ext:junit` | 1.1.5 | AndroidX JUnit extensions for instrumented tests |
| `androidx.test.espresso:espresso-core` | 3.5.1 | UI instrumented testing framework |

---

## Project Architecture

The project follows a **Fragment-based MVC (Model-View-Controller)** pattern:

```
MainActivity (Controller + Host)
│
├── Fragments (Views)
│   ├── AllSongFragment      — Displays all device songs
│   ├── FavSongFragment      — Displays favorited songs
│   ├── CurrentSongFragment  — Displays the currently playing song info
│   └── PlaylistFragment     — Displays user-created playlists
│
├── Models (Data)
│   ├── SongsList            — Represents a single audio track
│   └── Playlist             — Represents a user playlist
│
├── Adapters (View Binding)
│   ├── SongAdapter          — Binds SongsList data to ListView rows
│   ├── PlaylistAdapter      — Binds Playlist data to ListView rows
│   └── ViewPagerAdapter     — Creates and manages the four tab fragments
│
└── DB (Persistence)
    ├── FavoritesDBHandler   — SQLiteOpenHelper for favorites.db
    ├── FavoritesOperations  — CRUD operations on the Favorites table
    ├── PlaylistDBHandler    — SQLiteOpenHelper for playlists.db
    └── PlaylistOperations   — CRUD operations on playlist tables
```

### Fragment-to-Activity Communication Pattern
Fragments cannot directly call methods on their parent Activity in a safe, decoupled way. This project solves this using **Java Interfaces as callbacks**, a standard Android pattern:

1. Each Fragment defines a `public interface` inside itself (e.g., `createDataParse` in `AllSongFragment`).
2. `MainActivity` `implements` all these interfaces.
3. When a Fragment attaches to the Activity (`onAttach`), it casts the `Context` to the interface type and stores it.
4. The Fragment calls interface methods (e.g., `createDataParse.onDataPass(name, path)`) which execute inside `MainActivity`.

This keeps Fragments reusable and decoupled from the specific Activity that hosts them.

---

## Package & File Structure

```
app/src/main/
│
├── AndroidManifest.xml                  ← App permissions, activities, launch config
│
├── java/com/example/soc_macmini_15/musicplayer/
│   │
│   ├── Activity/
│   │   ├── MainActivity.java            ← Main screen: hosts all fragments + playback engine
│   │   └── SplashActivity.java          ← Branded loading screen (2 seconds)
│   │
│   ├── Fragments/
│   │   ├── AllSongFragment.java         ← Tab 1: full device song library
│   │   ├── CurrentSongFragment.java     ← Tab 2: currently playing song details
│   │   ├── FavSongFragment.java         ← Tab 3: favorited songs list
│   │   └── PlaylistFragment.java        ← Tab 4: user-created playlists
│   │
│   ├── Adapter/
│   │   ├── SongAdapter.java             ← ListView adapter for song rows
│   │   ├── PlaylistAdapter.java         ← ListView adapter for playlist rows
│   │   └── ViewPagerAdapter.java        ← Manages 4 tab fragments in ViewPager
│   │
│   ├── DB/
│   │   ├── FavoritesDBHandler.java      ← Creates/upgrades favorites.db schema
│   │   ├── FavoritesOperations.java     ← Add, remove, get all favorite songs
│   │   ├── PlaylistDBHandler.java       ← Creates/upgrades playlists.db schema
│   │   └── PlaylistOperations.java      ← Create, rename, delete playlists; add/remove songs
│   │
│   └── Model/
│       ├── SongsList.java               ← Data class: title, artist (subtitle), file path
│       └── Playlist.java                ← Data class: playlist ID, playlist name
│
└── res/
    ├── layout/
    │   ├── activity_main.xml            ← Root layout: DrawerLayout + Toolbar + Player + Tabs + ViewPager
    │   ├── activity_splash.xml          ← Full-screen splash with app name and team name
    │   ├── player_layout.xml            ← Playback controls bar (buttons + seekbar + timestamps)
    │   ├── fragment_tab.xml             ← Shared layout for All Songs, Favorites (ListView + ProgressBar)
    │   ├── fragment_playlist.xml        ← Playlist tab layout (ListView + FAB)
    │   ├── playlist_items.xml           ← Single song row (title TextView + subtitle TextView)
    │   ├── playlist_item.xml            ← Single playlist row
    │   └── drawer_header_layout.xml     ← Navigation drawer header with background image
    │
    ├── drawable/                        ← All vector icons (play, pause, shuffle, loop, etc.)
    ├── menu/
    │   ├── action_bar_menu.xml          ← Toolbar menu: Search + Favorites star
    │   └── navigation_menu.xml          ← Drawer menu: About item
    ├── values/
    │   ├── colors.xml                   ← App color palette
    │   ├── strings.xml                  ← All string constants
    │   └── styles.xml                   ← App themes and styles
    └── xml/
        └── searchable.xml               ← SearchManager configuration
```

---

## Application Flow

The following describes the complete user journey through the app from launch to playback:

```
App Launch
    │
    ▼
SplashActivity
  • Full-screen branded screen
  • Displays app name and team credit
  • Automatically redirects to MainActivity after 2 seconds using Handler.postDelayed()
    │
    ▼
MainActivity.onCreate()
  • Inflates activity_main.xml (DrawerLayout root)
  • Initialises all UI views (buttons, seekbar, toolbar, tabs, viewpager)
  • Checks READ_EXTERNAL_STORAGE permission
    │
    ├── Permission DENIED → App exits with Toast message
    │
    └── Permission GRANTED
          │
          ▼
      ViewPagerAdapter created → 4 Tabs initialised
          │
          ├── Tab 1: AllSongFragment
          │     • Runs LoadSongsTask (AsyncTask)
          │     • Shows ProgressBar while loading
          │     • Queries MediaStore via ContentResolver
          │     • Populates ListView with all device audio files
          │     • Tap song → onDataPass() callback → attachMusic()
          │     • Long press → Options dialog (Play Next / Add to Playlist)
          │
          ├── Tab 2: CurrentSongFragment
          │     • Shows the currently playing song's name and artist
          │     • Updates when a new song is selected
          │
          ├── Tab 3: FavSongFragment
          │     • Reads from favorites.db via FavoritesOperations
          │     • Shows favorited songs list
          │     • Tap → plays the song
          │     • Long press → confirmation dialog to remove from favorites
          │
          └── Tab 4: PlaylistFragment
                • Reads from playlists.db via PlaylistOperations
                • Shows list of user-created playlists
                • FAB (+ button) → Create New Playlist dialog
                • Tap playlist → shows songs inside it in a dialog
                • Tap song in dialog → plays it
                • Long press playlist → Delete or Rename options
                • Long press song in playlist dialog → Remove from playlist option

Song Selected → attachMusic(name, path)
  • Resets MediaPlayer
  • Sets new data source (file path)
  • Calls mediaPlayer.prepare() then mediaPlayer.start()
  • Sets SeekBar max to song duration
  • Starts playCycle() → Handler loop updates SeekBar + time every 100ms
  • Sets OnCompletionListener → auto-advance if loop mode is on

Playback Controls (player_layout.xml bar):
  • Play/Pause → toggle mediaPlayer.isPlaying()
  • Previous   → go to currentPosition - 1 (or restart if < 10ms played)
  • Next        → go to currentPosition + 1 (or next shuffle index)
  • Repeat      → mediaPlayer.setLooping(true/false)
  • Loop        → playContinueFlag toggles auto-advance on song completion
  • Shuffle     → createShuffleIndices() shuffles index list; next uses shuffled order

Favorites Toggle (Action Bar Star Icon):
  • Tap star → add current song to favorites.db
  • Tap filled star → remove current song from favorites.db
  • Icon updates to reflect current state

Search (Action Bar Magnifier):
  • SearchView expands in toolbar
  • onQueryTextChange() fires on every character typed
  • Filters song list in real time (case-insensitive contains match)
  • Works on both All Songs and Favorites tabs

Navigation Drawer (Hamburger Icon):
  • Slides in from left
  • Header shows background image
  • "About" menu item → AlertDialog with app description and team names
```

---

## UI & Layout Design

### Overall Layout Structure (`activity_main.xml`)
The root view is a `DrawerLayout` — a special Android layout that allows a panel (the navigation drawer) to slide in from the side. Inside it are two direct children:

**1. Main Content (`RelativeLayout`):**
Positioned using `android:layout_below` references to stack elements vertically:
- **Toolbar** — Custom action bar at the very top (dark blue background). Contains the hamburger icon, title, search, and favorites menu items.
- **Player Controls Bar** (`player_layout.xml` included via `<include>`) — Fixed 100dp high bar directly below the toolbar showing playback buttons and seekbar.
- **TabLayout** — Row of four tabs below the player bar. Uses `tabMode="fixed"` and `tabGravity="fill"` so all four tabs equally share the screen width. Active tab is indicated by a black underline (`tabIndicatorColor`).
- **ViewPager** — Fills the remaining screen below the tabs. Displays the content of whichever tab is selected. Swiping left/right also navigates between tabs.
- **FloatingActionButton** — Anchored to the bottom-right corner of the screen. Refreshes the song library.

**2. NavigationView (Drawer):**
Positioned with `android:layout_gravity="start"` so it slides in from the left edge. Contains:
- A header layout (`drawer_header_layout.xml`) with a background image
- A menu (`navigation_menu.xml`) with the "About" item

### Player Controls Bar (`player_layout.xml`)
A `RelativeLayout` of fixed 100dp height with two horizontal rows:

**Row 1 — Button Controls:**
```
[Shuffle]  [Repeat]  [Previous]  [▶ Play/Pause]  [Next]  [Loop]
```
All are `ImageButton` with `selectableItemBackgroundBorderless` (ripple effect on tap, no rectangle background). Play/Pause button is slightly larger (50dp vs 40dp) to serve as the focal point.

**Row 2 — Seek Controls:**
```
[0:00]  ══════════════════════  [3:45]
          (SeekBar)
```
`TextView` for current time, `SeekBar` (250dp wide), `TextView` for total duration. SeekBar thumb and progress are both white (`text_color`) for visibility against the dark background.

### Song List Row (`playlist_items.xml`)
Each song in a ListView is rendered as a small card with:
- **Title** — Song name in a larger font
- **Subtitle** — Artist name in a slightly smaller/lighter font

### Splash Screen (`activity_splash.xml`)
Full-screen layout (no action bar) with the app name and the team credit line ("MAD Project by Mayur, Keval, and Yash").

### Navigation Drawer Header (`drawer_header_layout.xml`)
A header section with a background image (`header_background.jpg`) displayed at the top of the drawer panel.

---

## Database Design

The project uses **two separate SQLite databases** stored locally on the device.

### Database 1 — `favorites.db`

Managed by `FavoritesDBHandler` (extends `SQLiteOpenHelper`).

**Table: `favorites`**

| Column | Type | Constraint | Description |
|---|---|---|---|
| `songID` | INTEGER | — | Song identifier |
| `title` | TEXT | — | Song title |
| `subtitle` | TEXT | — | Artist name |
| `songpath` | TEXT | PRIMARY KEY | Full file path to audio file |

`songpath` is the primary key because it uniquely identifies a file on the device, and prevents the same song from being added to favorites twice.

**Operations (`FavoritesOperations.java`):**
- `addSongFav(SongsList song)` — Inserts a new row into the favorites table
- `removeSong(String path)` — Deletes the row where `songpath` matches
- `getAllFavorites()` — Returns all rows as an `ArrayList<SongsList>`

---

### Database 2 — `playlists.db`

Managed by `PlaylistDBHandler` (extends `SQLiteOpenHelper`). Uses two tables with a foreign key relationship.

**Table: `playlists`**

| Column | Type | Constraint | Description |
|---|---|---|---|
| `playlistId` | INTEGER | PRIMARY KEY AUTOINCREMENT | Auto-generated unique ID |
| `playlistName` | TEXT | UNIQUE | The user-given name for the playlist |

**Table: `playlist_songs`**

| Column | Type | Constraint | Description |
|---|---|---|---|
| `playlistId` | INTEGER | FK → `playlists.playlistId` ON DELETE CASCADE | Links song to its playlist |
| `songId` | INTEGER | — | Song identifier |
| `title` | TEXT | — | Song title |
| `subtitle` | TEXT | — | Artist name |
| `songpath` | TEXT | — | Full file path |
| *(composite)* | — | PRIMARY KEY (`playlistId`, `songpath`) | Prevents duplicate songs in the same playlist |

The `ON DELETE CASCADE` constraint means that when a playlist is deleted, all its associated songs in `playlist_songs` are automatically deleted as well — no orphan rows.

**Operations (`PlaylistOperations.java`):**
- `createPlaylist(String name)` — Inserts a new playlist by name
- `getPlaylists()` — Returns all playlists as `ArrayList<Playlist>`
- `deletePlaylist(int id)` — Deletes a playlist (cascades to its songs)
- `updatePlaylistName(int id, String name)` — Renames a playlist
- `addSongToPlaylist(int playlistId, SongsList song)` — Adds a song to a playlist
- `removeSongFromPlaylist(int playlistId, String path)` — Removes a song from a playlist
- `getPlaylistSongs(int playlistId)` — Returns all songs in a specific playlist

---

## Key Features

### 1. Splash Screen
On launch, `SplashActivity` is the entry point (marked with `MAIN` + `LAUNCHER` in the Manifest). It displays the branded screen for exactly 2 seconds using `Handler.postDelayed()`, then uses an `Intent` to navigate to `MainActivity` and calls `finish()` so the back button doesn't return to the splash.

### 2. Runtime Permission Handling
Android 6.0+ requires apps to request "dangerous" permissions at runtime. The app checks for `READ_EXTERNAL_STORAGE` permission on startup:
- If already granted → immediately loads songs via `ViewPagerAdapter`
- If not granted → shows the system permission dialog
- If denied → shows a Toast and calls `finish()` (app cannot function without storage access)

### 3. Asynchronous Song Loading
`AllSongFragment` uses an `AsyncTask` (`LoadSongsTask`) to query `MediaStore` on a **background thread**. This prevents the UI from freezing ("ANR — App Not Responding") while reading potentially hundreds of audio files. A `ProgressBar` is shown during loading and hidden once the list is ready.

### 4. MediaStore Song Scanning
The app uses Android's `ContentResolver` with `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` to discover all audio files on the device. It reads three columns:
- `MediaStore.Audio.Media.TITLE` → song title
- `MediaStore.Audio.Media.ARTIST` → artist name
- `MediaStore.Audio.Media.DATA` → absolute file path

This approach relies on Android's own media indexer, so the app automatically reflects songs that have been scanned by the system.

### 5. MediaPlayer Playback Engine
The core playback is handled by Android's built-in `MediaPlayer` class:
- `mediaPlayer.reset()` → clears the previous source before loading a new song
- `mediaPlayer.setDataSource(path)` → points to the audio file
- `mediaPlayer.prepare()` → synchronously prepares the file for playback
- `mediaPlayer.start()` / `mediaPlayer.pause()` → play and pause
- `mediaPlayer.seekTo(int ms)` → jump to a specific position (used by SeekBar)
- `mediaPlayer.setLooping(boolean)` → enables single-song repeat
- `mediaPlayer.setOnCompletionListener()` → fires when a song ends naturally

### 6. Real-Time SeekBar with Handler Loop
The SeekBar updates 10 times per second to show playback progress. This is done using a `Handler` + `Runnable` pattern:
```java
private void playCycle() {
    seekbarController.setProgress(mediaPlayer.getCurrentPosition());
    tvCurrentTime.setText(getTimeFormatted(mediaPlayer.getCurrentPosition()));
    if (mediaPlayer.isPlaying()) {
        runnable = () -> playCycle();
        handler.postDelayed(runnable, 100); // re-schedule itself every 100ms
    }
}
```
The user can also drag the SeekBar to seek — this is handled by `SeekBar.OnSeekBarChangeListener`.

### 7. Favorites System
A heart/star icon in the action bar toggles the current song's favorite status:
- **Tap (not favorited)** → inserts into `favorites.db`, icon changes to filled heart
- **Tap (already favorited)** → removes from `favorites.db`, icon reverts to outline
- `favFlag` boolean tracks the current state per song session
- The Favorites tab reflects changes immediately via `setPagerLayout()` refresh

### 8. Playlist Management
Full CRUD for playlists:
- **Create** — Tap the FAB (+) in the Playlists tab → enter name → saved to `playlists.db`
- **Rename** — Long press playlist → "Rename Playlist" → enter new name
- **Delete** — Long press playlist → "Delete Playlist" → removes playlist and all its songs
- **Add songs** — Long press any song in All Songs tab → "Add to Playlist" → select playlist (or create new)
- **View songs** — Tap a playlist → dialog shows all songs inside
- **Remove songs** — Long press song inside playlist dialog → "Remove from playlist"
- **Play playlist** — Tap a song inside a playlist dialog → starts playing from that song

### 9. Repeat Mode
Tapping the Replay button toggles `mediaPlayer.setLooping()`:
- **On** → same song restarts automatically when it ends
- **Off** → song ends and stays paused

### 10. Loop / Auto-Advance Mode
Tapping the Loop button toggles `playContinueFlag`:
- **On** → when a song completes (`OnCompletionListener`), the next song in the list is automatically loaded and played
- **Off** → playback stops after the current song ends

### 11. Shuffle Mode
Tapping the Shuffle button toggles `shuffleMode`:
- **On** → `createShuffleIndices()` creates a shuffled copy of index positions using `Collections.shuffle()`. Next button navigates through this shuffled order.
- **Off** → next button increments `currentPosition` linearly
- Button alpha changes (0.5 = inactive, 1.0 = active) for visual feedback

### 12. Real-Time Search
The SearchView in the toolbar calls `onQueryTextChange()` on every keystroke:
- The search string is stored in `MainActivity.searchText`
- `setPagerLayout()` re-creates the ViewPager adapter, which causes the fragment to re-query with the new filter
- Song filtering uses case-insensitive `String.contains()` matching on song titles
- Search works across both the All Songs and Favorites tabs

### 13. Long-Press Context Menu
Long-pressing a song in the All Songs list shows an options dialog:
- **Play Next** → sets the selected song as the current song reference
- **Add to Playlist** → shows a list of existing playlists to choose from (or option to create a new one inline)

### 14. Navigation Drawer
A hamburger icon in the toolbar opens a side drawer:
- Slides in from the left using `DrawerLayout`
- Header displays an image
- "About" menu item shows an `AlertDialog` with the app description and the team names (Mayur, Keval, and Yash)

### 15. Song List Refresh
A `FloatingActionButton` anchored to the bottom-right of the screen calls `setPagerLayout()` when tapped. This recreates the `ViewPagerAdapter`, which triggers fresh `MediaStore` queries in all fragments — picking up any newly added audio files.

---

## Fragments (Tabs)

### Tab 1 — AllSongFragment
- Extends `ListFragment`
- Uses `AsyncTask` to query `MediaStore` on a background thread
- Shows `ProgressBar` while loading
- Populates a `ListView` with `SongAdapter`
- Handles search filtering via `onQueryTextChange()`
- Single tap → passes song name + path to `MainActivity` via `createDataParse` interface
- Long press → triggers `showOptionsDialog()` in `MainActivity`

### Tab 2 — CurrentSongFragment
- Shows details (title, artist) of the song currently loaded in `MediaPlayer`
- Data is passed to it through the `ViewPagerAdapter` and `MainActivity`

### Tab 3 — FavSongFragment
- Extends `ListFragment`
- Loads songs synchronously from `FavoritesOperations.getAllFavorites()`
- Same search filtering capability as `AllSongFragment`
- Single tap → plays the song
- Long press → shows a delete confirmation `AlertDialog`. On confirm, calls `favoritesOperations.removeSong()` and refreshes the list

### Tab 4 — PlaylistFragment
- Extends `Fragment` (not `ListFragment` — it has its own layout with a FAB)
- Loads playlists from `PlaylistOperations.getPlaylists()`
- FAB launches "Create New Playlist" dialog
- Single tap on a playlist → opens a modal `AlertDialog` containing a `ListView` of that playlist's songs
- Long press on playlist → options dialog (Delete / Rename)
- Long press on song inside playlist dialog → "Remove from playlist" option

---

## Adapters

### SongAdapter
Extends `ArrayAdapter<SongsList>` and implements `Filterable`. For each item in the `ListView`, it inflates `playlist_items.xml` and binds:
- `tv_music_name` → `SongsList.getTitle()`
- `tv_music_subtitle` → `SongsList.getSubTitle()`

Uses the ViewHolder-lite pattern via `convertView` recycling to avoid inflating new views unnecessarily.

### PlaylistAdapter
Similar to `SongAdapter` but binds `Playlist` objects. Inflates `playlist_item.xml` and displays the playlist name.

### ViewPagerAdapter
Extends `FragmentPagerAdapter`. Overrides:
- `getItem(int position)` → returns the appropriate Fragment instance for each tab position (0=All Songs, 1=Current Song, 2=Favorites, 3=Playlists)
- `getCount()` → returns 4
- `getPageTitle(int position)` → returns the tab label (used by `TabLayout`)

Takes `ContentResolver` as a constructor parameter and passes it to `AllSongFragment.getInstance()` so it can query `MediaStore`.

---

## Data Models

### SongsList
```java
public class SongsList {
    private String title;    // Song name from MediaStore
    private String subTitle; // Artist name from MediaStore
    private String path;     // Absolute file path — used as MediaPlayer data source and DB key
}
```
Provides getters and setters. `path` is used as the primary key in the Favorites database.

### Playlist
```java
public class Playlist {
    private int id;       // Auto-incremented DB primary key
    private String name;  // User-given playlist name
}
```
Provides getters and setters including `setName()` for rename support.

---

## Permissions

Declared in `AndroidManifest.xml`:

| Permission | Reason |
|---|---|
| `READ_EXTERNAL_STORAGE` | Required to query `MediaStore` and access audio files stored on the device. Requested at runtime. |
| `WAKE_LOCK` | Prevents the CPU from sleeping while music is playing in the background, ensuring uninterrupted playback. |

`MainActivity` has `android:launchMode="singleTop"` — if the activity is already at the top of the back stack, it reuses the existing instance instead of creating a new one (prevents duplicate MainActivity instances from search intents).

`MainActivity` also has `android:screenOrientation="portrait"` — the app is locked to portrait mode only.

---

## Color Scheme & Theming

The app uses a consistent dark blue + white color scheme defined in `colors.xml`:

| Name | Hex | Usage |
|---|---|---|
| `colorPrimary` | `#03A9F4` | Light blue — TabLayout background, FAB background, tab bar |
| `colorPrimaryDark` | `#03A9F4` | Status bar color |
| `colorAccent` | `#000000` | Tab indicator underline |
| `drawer_color` | `#03A9F4` | Toolbar, player controls bar, navigation drawer background |
| `text_color` | `#ffffff` | All text, icons, SeekBar thumb and progress |
| `color_black` | `#000000` | ViewPager/list area background |
| `off_color` | `#dfdada` | Unselected tab text |
| `light_color` | `#fafafa` | Light surfaces |
| `gray_color` | `#9c9b9b` | Secondary/muted text |

The base theme is `Theme.AppCompat.Light.NoActionBar` — a light base theme with the system action bar disabled (replaced by the custom `Toolbar`). The splash screen uses `AppTheme.NoActionBar` with `android:windowFullscreen="true"` for an immersive, edge-to-edge splash effect.

---

## Build Configuration

From `app/build.gradle`:

```groovy
android {
    namespace "com.example.soc_macmini_15.musicplayer"
    compileSdkVersion 34
    defaultConfig {
        applicationId "com.example.soc_macmini_15.musicplayer"
        minSdkVersion 21         // Android 5.0+
        targetSdkVersion 34      // Android 14
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false  // ProGuard disabled (can be enabled for production)
        }
    }
}
```

---

## How to Run the Project

### Prerequisites
- **Android Studio** (latest stable recommended — Hedgehog / Iguana or newer)
- **Java JDK 11 or 17** (bundled with modern Android Studio)
- **Android device or emulator** running API 21+

### Steps

1. **Clone or download** the project and open the root folder in Android Studio.

2. **Let Gradle sync** — Android Studio will automatically download all dependencies listed in `build.gradle`. This requires an internet connection on the first run.

3. **Connect a device or start an emulator:**
   - Physical device: enable Developer Options + USB Debugging, connect via USB
   - Emulator: use AVD Manager to create a device image (API 21+)

4. **Run the app** — click the green ▶ Run button or press `Shift + F10`.

5. **Grant permission** — on first launch, the app will ask for `READ_EXTERNAL_STORAGE`. Tap "Allow". If using an emulator, add audio files to the emulator storage first (use Device File Explorer or `adb push`).

6. **Explore:**
   - Browse songs in the All Songs tab
   - Tap a song to start playing
   - Use the player controls bar to pause, skip, shuffle, and loop
   - Star songs to favorite them
   - Create playlists and add songs via long press

---

## Team

| Name | Role |
|---|---|
| Mayur | Developer |
| Keval | Developer |
| Yash | Developer |

> *Decible — MAD Project | Mobile Application Development*
