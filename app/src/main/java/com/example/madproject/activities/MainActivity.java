package com.example.madproject.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.ViewPager;

import com.example.madproject.R;
import com.example.madproject.adapters.ViewPagerAdapter;
import com.example.madproject.database.PlaylistOperations;
import com.example.madproject.fragments.AllSongFragment;
import com.example.madproject.fragments.CurrentSongFragment;
import com.example.madproject.fragments.FavSongFragment;
import com.example.madproject.fragments.HomeFragment;
import com.example.madproject.fragments.MoodQuestionnaireFragment;
import com.example.madproject.fragments.PlaylistFragment;
import com.example.madproject.interfaces.PlaylistActionListener;
import com.example.madproject.interfaces.SongSelectionListener;
import com.example.madproject.models.Playlist;
import com.example.madproject.models.SongsList;
import com.example.madproject.services.NewSongDetectionService;
import com.example.madproject.utils.ExoPlayerManager;
import com.example.madproject.utils.MoodAlgorithm;
import com.example.madproject.utils.UsageTracker;
import com.example.madproject.utils.StorageScanner;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.example.madproject.utils.TimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main activity hosting fragments via ViewPager + TabLayout.
 * Handles MediaPlayer lifecycle, song switching, playback state,
 * and global controls. Implements interfaces for fragment communication.
 */
public class MainActivity extends AppCompatActivity
        implements SongSelectionListener, PlaylistActionListener,
        NavigationView.OnNavigationItemSelectedListener,
        HomeFragment.MoodQuizLauncher {

    private static final int PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private ViewPager viewPager;
    private FloatingActionButton fabRefresh;

    // Mini Player UI
    private View miniPlayerContainer;
    private ImageView miniPlayerAlbumArt;
    private TextView miniPlayerTitle;
    private TextView miniPlayerArtist;
    private ImageButton miniPlayerBtnPlayPause;
    private ImageButton miniPlayerBtnNext;
    private ProgressBar miniPlayerProgress;

    // Fragments
    private HomeFragment homeFragment;
    private AllSongFragment allSongFragment;
    private CurrentSongFragment currentSongFragment;
    private FavSongFragment favSongFragment;
    private PlaylistFragment playlistFragment;
    private MoodQuestionnaireFragment moodQuestionnaireFragment;

    // Search
    private SearchView toolbarSearchView;
    private MenuItem searchMenuItem;

    // Background thread management
    private ExecutorService backgroundExecutor;
    
    // New songs detection
    private BroadcastReceiver newSongsReceiver;

    // Data
    private ArrayList<SongsList> allSongs;
    private ArrayList<SongsList> currentQueue;
    private int currentSongIndex = -1;
    private SongsList currentSong;

    // Playback state
    private ExoPlayerManager playerManager;
    private boolean shuffleOn = false;
    private boolean repeatOn = false;
    private boolean playContinueFlag = true;
    private ArrayList<Integer> shuffledIndices;

    // Sleep Timer
    private Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (playerManager != null && playerManager.isPlaying()) {
                playerManager.pause();
                if (currentSongFragment != null) {
                    currentSongFragment.updatePlayPauseButton();
                }
            }
            Toast.makeText(MainActivity.this, "Sleep timer finished. Playback paused.", Toast.LENGTH_SHORT).show();
        }
    };

    // Audio Noisy Receiver for Headphone disconnect
    private final BroadcastReceiver audioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (playerManager != null && playerManager.isPlaying()) {
                    onPlayPauseToggle(); // Pause playback
                    Toast.makeText(context, "Headphones disconnected. Paused.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };
    private boolean receiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        playerManager = ExoPlayerManager.getInstance();
        playerManager.initialize(this, null);
        allSongs = new ArrayList<>();
        
        // Initialize background thread pool
        backgroundExecutor = Executors.newSingleThreadExecutor();

        initViews();
        setupToolbar();
        setupDrawer();
        checkPermissionsAndLoad();

        // Usage tracking
        UsageTracker.startSession(this);

        // Register Audio Noisy Receiver
        registerReceiver(audioNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        receiverRegistered = true;
        
        // Temporarily disable new songs detection to fix crash
        // setupNewSongsReceiver();
        // startService(new Intent(this, NewSongDetectionService.class));
    }
    
    /**
     * Setup broadcast receiver for new songs detection
     */
    private void setupNewSongsReceiver() {
        newSongsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.madproject.NEW_SONGS_DETECTED".equals(intent.getAction())) {
                    int newSongsCount = intent.getIntExtra("new_songs_count", 0);
                    if (newSongsCount > 0) {
                        // Refresh the library to show new songs
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, 
                                "Found " + newSongsCount + " new songs! Refreshing library...", 
                                Toast.LENGTH_LONG).show();
                            refreshSongLibrary();
                        });
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.madproject.NEW_SONGS_DETECTED");
        registerReceiver(newSongsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }
    
    /**
     * Initialize all views.
     */
    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        viewPager = findViewById(R.id.view_pager);
        fabRefresh = findViewById(R.id.fab_refresh);

        // Mini player views
        miniPlayerContainer = findViewById(R.id.bottom_mini_player);
        miniPlayerAlbumArt = findViewById(R.id.mini_player_album_art);
        miniPlayerTitle = findViewById(R.id.mini_player_title);
        miniPlayerArtist = findViewById(R.id.mini_player_artist);
        miniPlayerBtnPlayPause = findViewById(R.id.mini_player_btn_play_pause);
        miniPlayerBtnNext = findViewById(R.id.mini_player_btn_next);
        miniPlayerProgress = findViewById(R.id.mini_player_progress);

        fabRefresh.setOnClickListener(v -> refreshSongLibrary());

        // Mini player listeners
        miniPlayerBtnPlayPause.setOnClickListener(v -> onPlayPauseToggle());
        miniPlayerBtnNext.setOnClickListener(v -> onNextSong());
        miniPlayerContainer.setOnClickListener(v -> viewPager.setCurrentItem(1, true));
    }

    /**
     * Sets up the toolbar as the action bar.
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("SonicWave");
        }
    }

    /**
     * Sets up the navigation drawer.
     */
    private void setupDrawer() {
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
    }

    /**
     * Sets up ViewPager with bottom navigation and all fragments.
     */
    private void setupViewPager() {
        homeFragment = HomeFragment.newInstance(allSongs);
        allSongFragment = AllSongFragment.newInstance(allSongs);
        currentSongFragment = CurrentSongFragment.newInstance();
        favSongFragment = FavSongFragment.newInstance();
        playlistFragment = PlaylistFragment.newInstance();
        moodQuestionnaireFragment = MoodQuestionnaireFragment.newInstance(allSongs);

        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(homeFragment, "Home"); // Index 0
        adapter.addFragment(currentSongFragment, "Now Playing"); // Index 1
        adapter.addFragment(allSongFragment, "Library"); // Index 2
        adapter.addFragment(playlistFragment, "Playlists"); // Index 3
        adapter.addFragment(favSongFragment, "Liked Songs"); // Index 4

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(4);

        // Connect BottomNavigationView
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (id == R.id.nav_library) {
                viewPager.setCurrentItem(2, true);
                return true;
            } else if (id == R.id.nav_playlists) {
                viewPager.setCurrentItem(3, true);
                return true;
            } else if (id == R.id.nav_liked) {
                viewPager.setCurrentItem(4, true);
                return true;
            } else if (id == R.id.nav_search) {
                // Expand the toolbar SearchView and focus it
                viewPager.setCurrentItem(2, true);
                if (searchMenuItem != null) {
                    searchMenuItem.expandActionView();
                }
                return true;
            }
            return false;
        });

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // Hide mini player if we are viewing the Current Song fragment
                if (position == 1 && currentSong != null) {
                    miniPlayerContainer.setVisibility(View.GONE);
                } else if (currentSong != null) {
                    miniPlayerContainer.setVisibility(View.VISIBLE);
                }

                // Sync bottom nav
                if (position == 0)
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                else if (position == 2)
                    bottomNavigationView.setSelectedItemId(R.id.nav_library);
                else if (position == 3)
                    bottomNavigationView.setSelectedItemId(R.id.nav_playlists);
                else if (position == 4)
                    bottomNavigationView.setSelectedItemId(R.id.nav_liked);
            }
        });
    }

    // ======================== Permissions ========================

    /**
     * Checks for storage permission and loads songs if granted.
     */
    private void checkPermissionsAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_AUDIO
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.READ_MEDIA_AUDIO },
                        PERMISSION_REQUEST_CODE);
            } else {
                loadSongsAndSetup();
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                        PERMISSION_REQUEST_CODE);
            } else {
                loadSongsAndSetup();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadSongsAndSetup();
            } else {
                Toast.makeText(this, "Storage permission is required to scan music files",
                        Toast.LENGTH_LONG).show();
                setupViewPager(); // Setup with empty list
            }
        }
    }

    /**
     * Scans songs and sets up the ViewPager asynchronously.
     */
    private void loadSongsAndSetup() {
        Toast.makeText(this, "Scanning music library...", Toast.LENGTH_SHORT).show();
        backgroundExecutor.execute(() -> {
            ArrayList<SongsList> scannedSongs = StorageScanner.scanSongs(MainActivity.this);
            runOnUiThread(() -> {
                allSongs = scannedSongs;
                setupViewPager();

                // Restore last played song from SharedPreferences
                restoreLastPlayedSong();

                // Trigger background mood tagging for new songs
                MoodAlgorithm.tagSongsInBackground(MainActivity.this, allSongs, () -> {
                    runOnUiThread(() -> {
                        android.util.Log.d("MainActivity", "Mood tagging complete");
                    });
                });
            });
        });
    }

    // ======================== Refresh ========================

    /**
     * Re-scans storage and refreshes all fragments asynchronously.
     */
    private void refreshSongLibrary() {
        Toast.makeText(this, "Refreshing library...", Toast.LENGTH_SHORT).show();
        backgroundExecutor.execute(() -> {
            ArrayList<SongsList> scannedSongs = StorageScanner.scanSongs(MainActivity.this);
            runOnUiThread(() -> {
                allSongs = scannedSongs;
                if (homeFragment != null) {
                    homeFragment.updateDashboards(allSongs);
                    Toast.makeText(MainActivity.this, "Library refreshed! Found " + allSongs.size() + " songs.",
                            Toast.LENGTH_SHORT).show();
                }
                
                // Trigger background mood tagging for new/updated songs
                MoodAlgorithm.tagSongsInBackground(MainActivity.this, allSongs, () -> {
                    runOnUiThread(() -> {
                        android.util.Log.d("MainActivity", "Mood tagging complete for refreshed library");
                        // Refresh fragments that might depend on mood data
                        if (homeFragment != null) {
                            homeFragment.updateDashboards(allSongs);
                        }
                    });
                });
            });
        });
    }

    /**
     * Restores last played song UI on cold start.
     * Does NOT auto-play, just shows it in Now Playing + mini player.
     */
    private void restoreLastPlayedSong() {
        String lastPath = UsageTracker.getLastSongPath(this);
        if (lastPath == null || allSongs == null) return;

        // Find matching song in device library
        for (SongsList song : allSongs) {
            if (song.getPath().equals(lastPath)) {
                this.currentSong = song;
                if (currentQueue == null || currentQueue.isEmpty()) {
                    currentQueue = new ArrayList<>(allSongs);
                    currentSongIndex = allSongs.indexOf(song);
                }
                // Update UI without playing
                if (currentSongFragment != null) {
                    currentSongFragment.updateCurrentSong(song);
                }
                updateMiniPlayer(song);
                return;
            }
        }
    }

    // ======================== SongSelectionListener ========================

    @Override
    public void onSongSelected(ArrayList<SongsList> songsList, int position) {
        if (songsList == null || position < 0 || position >= songsList.size())
            return;

        this.currentQueue = new ArrayList<>(songsList);
        this.currentSongIndex = position;
        this.currentSong = currentQueue.get(position);

        // Generate shuffled indices
        generateShuffledIndices();

        // Play the song
        playSong(currentSong);

        // Switch to Now Playing tab
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
    }

    @Override
    public void onPlayPauseToggle() {
        playerManager.togglePlayPause();
        if (currentSongFragment != null) {
            currentSongFragment.updatePlayPauseButton();
        }
        updateMiniPlayerPlayPause();
    }

    @Override
    public void onNextSong() {
        if (currentQueue == null || currentQueue.isEmpty())
            return;

        if (shuffleOn && shuffledIndices != null && !shuffledIndices.isEmpty()) {
            // Find current position in shuffled list and move to next
            int shufflePos = shuffledIndices.indexOf(currentSongIndex);
            shufflePos = (shufflePos + 1) % shuffledIndices.size();
            currentSongIndex = shuffledIndices.get(shufflePos);
        } else {
            currentSongIndex = (currentSongIndex + 1) % currentQueue.size();
        }

        currentSong = currentQueue.get(currentSongIndex);
        playSong(currentSong);
    }

    @Override
    public void onPreviousSong() {
        if (currentQueue == null || currentQueue.isEmpty())
            return;

        // If more than 3 seconds in, restart the song
        if (playerManager.getCurrentPosition() > 3000) {
            playerManager.seekTo(0);
            if (currentSongFragment != null) {
                currentSongFragment.updateCurrentSong(currentSong);
            }
            return;
        }

        if (shuffleOn && shuffledIndices != null && !shuffledIndices.isEmpty()) {
            int shufflePos = shuffledIndices.indexOf(currentSongIndex);
            shufflePos = (shufflePos - 1 + shuffledIndices.size()) % shuffledIndices.size();
            currentSongIndex = shuffledIndices.get(shufflePos);
        } else {
            currentSongIndex = (currentSongIndex - 1 + currentQueue.size()) % currentQueue.size();
        }

        currentSong = currentQueue.get(currentSongIndex);
        playSong(currentSong);
    }

    @Override
    public void onSeekTo(int position) {
        playerManager.seekTo(position);
    }

    @Override
    public void onShuffleToggle() {
        shuffleOn = !shuffleOn;
        if (shuffleOn) {
            generateShuffledIndices();
        }
    }

    @Override
    public void onRepeatToggle() {
        repeatOn = !repeatOn;
        playerManager.setLooping(repeatOn);
    }

    @Override
    public boolean isPlaying() {
        return playerManager.isPlaying();
    }

    @Override
    public int getCurrentPosition() {
        return playerManager.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        return playerManager.getDuration();
    }

    @Override
    public boolean isShuffleOn() {
        return shuffleOn;
    }

    @Override
    public boolean isRepeatOn() {
        return repeatOn;
    }

    @Override
    public SongsList getCurrentSong() {
        return currentSong;
    }

    @Override
    public void onAddToQueue(SongsList song) {
        if (currentQueue == null) {
            currentQueue = new ArrayList<>();
        }
        currentQueue.add(song);

        // Add to shuffle indices if active
        if (shuffleOn && shuffledIndices != null) {
            shuffledIndices.add(currentQueue.size() - 1);
        }
        Toast.makeText(this, song.getTitle() + " added to queue", Toast.LENGTH_SHORT).show();
    }

    // ======================== PlaylistActionListener ========================

    @Override
    public void onPlaylistSelected(Playlist playlist) {
        if (playlist.getSongs() != null && !playlist.getSongs().isEmpty()) {
            // Match playlist songs with full song data from allSongs
            ArrayList<SongsList> playlistSongs = new ArrayList<>();
            for (SongsList pSong : playlist.getSongs()) {
                for (SongsList fullSong : allSongs) {
                    if (fullSong.getPath().equals(pSong.getPath())) {
                        playlistSongs.add(fullSong);
                        break;
                    }
                }
                // If not found in allSongs, still add with limited data
                if (playlistSongs.isEmpty()
                        || !playlistSongs.get(playlistSongs.size() - 1).getPath().equals(pSong.getPath())) {
                    playlistSongs.add(pSong);
                }
            }

            if (!playlistSongs.isEmpty()) {
                onSongSelected(playlistSongs, 0);
            }
        } else {
            Toast.makeText(this, "Playlist is empty. Add songs first!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPlaylistDeleted(int playlistId) {
        // Nothing extra needed; fragment handles UI update
    }

    @Override
    public ArrayList<SongsList> getAllDeviceSongs() {
        return allSongs;
    }

    // ======================== Playback Helpers ========================

    /**
     * Plays a specific song and updates the Now Playing fragment.
     */
    private void playSong(SongsList song) {
        if (song == null)
            return;

        String albumArt = "content://media/external/audio/albumart/" + song.getAlbumId();
        playerManager.playSong(this, song.getPath(), song.getTitle(), song.getArtist(), song.getAlbum(), albumArt);
        playerManager.setLooping(repeatOn);

        // Save last played for state restoration
        UsageTracker.saveLastPlayedSong(this, song.getTitle(), song.getArtist(),
                song.getPath(), song.getAlbumId(), song.getDuration());

        // Set completion listener for auto-continue via Media3 Player Listener
        playerManager.setPlayerListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                    if (!repeatOn && playContinueFlag) {
                        onNextSong();
                    }
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                // Sync mini player and now playing play/pause icons
                updateMiniPlayerPlayPause();
                if (currentSongFragment != null) {
                    currentSongFragment.updatePlayPauseButton();
                }
            }
        });

        // Update Now Playing fragment
        if (currentSongFragment != null) {
            currentSongFragment.updateCurrentSong(song);
        }

        updateMiniPlayer(song);

        // Update home screen widget
        com.example.madproject.widgets.PlayerWidgetProvider.updateWidgetInfo(
                this, song.getTitle(), song.getArtist());
    }

    /**
     * Updates the mini player UI.
     */
    private void updateMiniPlayer(SongsList song) {
        if (song == null)
            return;

        // Show only if not on the now playing screen
        if (viewPager != null && viewPager.getCurrentItem() != 1) {
            miniPlayerContainer.setVisibility(View.VISIBLE);
        }

        miniPlayerTitle.setText(song.getTitle());
        miniPlayerArtist.setText(song.getArtist());
        miniPlayerProgress.setMax(song.getDuration() > 0 ? (int) song.getDuration() : 100);

        try {
            android.net.Uri albumArtUri = android.content.ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"), song.getAlbumId());
            Glide.with(this).load(albumArtUri).placeholder(R.drawable.ic_music_note).into(miniPlayerAlbumArt);
        } catch (Exception e) {
            miniPlayerAlbumArt.setImageResource(R.drawable.ic_music_note);
        }
        updateMiniPlayerPlayPause();

        // Start mini player progress updates
        miniPlayerHandler.removeCallbacks(miniPlayerUpdater);
        miniPlayerHandler.post(miniPlayerUpdater);
    }

    private void updateMiniPlayerPlayPause() {
        if (playerManager.isPlaying()) {
            miniPlayerBtnPlayPause.setImageResource(R.drawable.ic_pause);
        } else {
            miniPlayerBtnPlayPause.setImageResource(R.drawable.ic_play);
        }
    }

    // Mini player progress update handler
    private final Handler miniPlayerHandler = new Handler(Looper.getMainLooper());
    private final Runnable miniPlayerUpdater = new Runnable() {
        @Override
        public void run() {
            if (playerManager.isPlaying()) {
                miniPlayerProgress.setProgress(playerManager.getCurrentPosition());
            }
            miniPlayerHandler.postDelayed(this, 500);
        }
    };

    /**
     * Generates shuffled indices for the current queue using intelligent shuffling.
     * Prevents the same artist from playing back-to-back when possible.
     */
    private void generateShuffledIndices() {
        if (currentQueue == null || currentQueue.isEmpty())
            return;

        shuffledIndices = new ArrayList<>();
        for (int i = 0; i < currentQueue.size(); i++) {
            if (i != currentSongIndex) {
                shuffledIndices.add(i);
            }
        }

        // Initial random shuffle
        Collections.shuffle(shuffledIndices);

        // Intelligent spacing: avoid back-to-back same artist
        for (int i = 1; i < shuffledIndices.size(); i++) {
            SongsList prevSong = currentQueue.get(shuffledIndices.get(i - 1));
            SongsList currSong = currentQueue.get(shuffledIndices.get(i));

            if (prevSong.getArtist().equals(currSong.getArtist())) {
                // Find a swap candidate
                for (int j = i + 1; j < shuffledIndices.size(); j++) {
                    SongsList candidate = currentQueue.get(shuffledIndices.get(j));
                    if (!candidate.getArtist().equals(prevSong.getArtist())) {
                        Collections.swap(shuffledIndices, i, j);
                        break;
                    }
                }
            }
        }

        // Move current song index to front
        if (currentSongIndex >= 0 && currentSongIndex < currentQueue.size()) {
            shuffledIndices.add(0, currentSongIndex);
        }
    }

    // ======================== Options Menu (Search) ========================

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // Setup SearchView and store references for bottom nav search
        searchMenuItem = menu.findItem(R.id.action_search);
        toolbarSearchView = (SearchView) searchMenuItem.getActionView();
        toolbarSearchView.setQueryHint("Search songs...");

        toolbarSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Filter in AllSongFragment
                if (allSongFragment != null && allSongFragment.getSongAdapter() != null) {
                    allSongFragment.getSongAdapter().getFilter().filter(newText);
                }
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sleep_off) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            Toast.makeText(this, "Sleep timer disabled", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.sleep_15) {
            setSleepTimer(15);
            return true;
        } else if (id == R.id.sleep_30) {
            setSleepTimer(30);
            return true;
        } else if (id == R.id.sleep_60) {
            setSleepTimer(60);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setSleepTimer(int minutes) {
        sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
        sleepTimerHandler.postDelayed(sleepTimerRunnable, minutes * 60 * 1000L);
        Toast.makeText(this, "Sleep timer set for " + minutes + " minutes", Toast.LENGTH_SHORT).show();
    }

    // ======================== Navigation Drawer ========================

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_about) {
            showAboutDialog();
        } else if (id == R.id.nav_queue) {
            // Open Queue Manager
            QueueActivity.QueueHolder.setQueue(currentQueue);
            Intent queueIntent = new Intent(this, QueueActivity.class);
            startActivity(queueIntent);
        } else if (id == R.id.nav_equalizer) {
            // Open Equalizer with correct audio session ID
            Intent eqIntent = new Intent(this, EqualizerActivity.class);
            eqIntent.putExtra("audio_session_id", com.example.madproject.services.PlaybackService.getAudioSessionId());
            startActivity(eqIntent);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Shows the About dialog.
     */
    private void showAboutDialog() {
        new AlertDialog.Builder(this, R.style.DarkDialogTheme)
                .setTitle("About SonicWave")
                .setMessage("SonicWave Music Player\n\n"
                        + "Version: 2.1.0\n\n"
                        + "A modern, glassmorphic music player for Android.\n\n"
                        + "Created by:\n"
                        + "F030 - Mayur H. Doshi\n"
                        + "F030 - Keval N. Mehta\n"
                        + "F052 - Yash D. Mehta\n\n"
                        + "Features:\n"
                        + "• Modern glassmorphism UI\n"
                        + "• Immersive scroll-based design\n"
                        + "• Real motion & depth effects\n"
                        + "• Local music playback\n"
                        + "• Mood-based suggestions\n"
                        + "• User feedback & mood correction\n"
                        + "• Favorites management\n"
                        + "• Custom playlists\n"
                        + "• Equalizer & audio effects\n"
                        + "• Shuffle & repeat modes\n"
                        + "• Daily usage tracking\n"
                        + "• Real-time search")
                .setPositiveButton("OK", null)
                .setIcon(R.drawable.ic_music_note)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    // ======================== MoodQuizLauncher ========================

    @Override
    public void onLaunchMoodQuiz() {
        // Show mood quiz as a dialog/fullscreen fragment instead of ViewPager page
        if (moodQuestionnaireFragment != null && getSupportFragmentManager() != null) {
            getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                            android.R.anim.fade_in, android.R.anim.fade_out)
                    .add(android.R.id.content, moodQuestionnaireFragment, "mood_quiz")
                    .addToBackStack("mood_quiz")
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Remove pending handler callbacks
        if (miniPlayerHandler != null) {
            miniPlayerHandler.removeCallbacks(miniPlayerUpdater);
        }
        
        // Clear sleep timer
        if (sleepTimerHandler != null) {
            sleepTimerHandler.removeCallbacksAndMessages(null);
        }
        
        // Release player resources
        if (playerManager != null) {
            playerManager.releasePlayer();
        }
        
        // Temporarily disable service stop
        // stopService(new Intent(this, NewSongDetectionService.class));
        
        // Unregister receivers
        if (receiverRegistered && audioNoisyReceiver != null) {
            unregisterReceiver(audioNoisyReceiver);
            receiverRegistered = false;
        }
        
        // Temporarily disable newSongsReceiver cleanup
        // if (newSongsReceiver != null) {
        //     unregisterReceiver(newSongsReceiver);
        //     newSongsReceiver = null;
        // }
        
        // Cleanup background executor
        if (backgroundExecutor != null && !backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdown();
        }
        
        // Stop usage tracking
        UsageTracker.endSession(this);
    }
}
