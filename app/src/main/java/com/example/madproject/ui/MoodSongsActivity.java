package com.example.madproject.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.models.SongsList;
import com.example.madproject.ui.adapters.MoodSongsAdapter;
import com.example.madproject.utils.MoodAlgorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mood Songs Activity with glassmorphism design
 * Shows songs filtered by selected mood
 */
public class MoodSongsActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView rvMoodSongs;
    private TextView tvMoodTitle;
    private TextView tvSongCount;
    private ImageView ivMoodIcon;
    private CardView cardHeader;

    // Data
    private String selectedMood;
    private String moodName;
    private List<SongsList> moodSongs;
    private MoodSongsAdapter moodSongsAdapter;

    // Animation
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator backgroundAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set glassmorphism theme
        setTheme(R.style.GlassmorphismTheme);
        setContentView(R.layout.activity_mood_songs_glassmorphism);

        // Enable edge-to-edge display
        setupEdgeToEdge();

        // Get mood data from intent
        getIntentData();

        // Initialize UI
        initViews();
        initData();
        setupAdapter();
        startAnimations();
    }

    /**
     * Setup edge-to-edge display
     */
    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            
            v.setPadding(0, statusBarHeight, 0, navigationBarHeight);
            
            return insets;
        });
    }

    /**
     * Get mood data from intent
     */
    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            selectedMood = intent.getStringExtra("mood");
            moodName = intent.getStringExtra("mood_name");
        }
        
        if (selectedMood == null) {
            selectedMood = MoodAlgorithm.MOOD_HAPPY;
        }
        if (moodName == null) {
            moodName = "Happy";
        }
    }

    /**
     * Initialize all views
     */
    private void initViews() {
        rvMoodSongs = findViewById(R.id.rv_mood_songs);
        tvMoodTitle = findViewById(R.id.tv_mood_title);
        tvSongCount = findViewById(R.id.tv_song_count);
        ivMoodIcon = findViewById(R.id.iv_mood_icon);
        cardHeader = findViewById(R.id.card_header);
    }

    /**
     * Initialize data
     */
    private void initData() {
        // Update UI with mood information
        updateMoodDisplay();
        
        // Load mood songs
        moodSongs = loadMoodSongs();
    }

    /**
     * Update mood display
     */
    private void updateMoodDisplay() {
        tvMoodTitle.setText(moodName + " Songs");
        
        // Set mood icon
        int iconRes = getMoodIcon(selectedMood);
        ivMoodIcon.setImageResource(iconRes);
        
        // Update song count
        tvSongCount.setText(String.valueOf(moodSongs.size()));
    }

    /**
     * Get mood icon resource
     */
    private int getMoodIcon(String mood) {
        if (mood == null) return R.drawable.ic_happy;
        
        switch (mood) {
            case MoodAlgorithm.MOOD_HAPPY:
                return R.drawable.ic_happy;
            case MoodAlgorithm.MOOD_SAD:
                return R.drawable.ic_sad;
            case MoodAlgorithm.MOOD_CALM:
                return R.drawable.ic_calm;
            case MoodAlgorithm.MOOD_ENERGETIC:
                return R.drawable.ic_energetic;
            case MoodAlgorithm.MOOD_PARTY:
                return R.drawable.ic_party;
            default:
                return R.drawable.ic_happy;
        }
    }

    /**
     * Load mood songs (placeholder implementation)
     */
    private List<SongsList> loadMoodSongs() {
        List<SongsList> songs = new ArrayList<>();
        
        // TODO: Load actual songs from database filtered by mood
        // For now, create placeholder data
        for (int i = 0; i < 20; i++) {
            SongsList song = new SongsList();
            song.setTitle(moodName + " Song " + (i + 1));
            song.setArtist("Artist " + (i + 1));
            song.setPath("/path/to/song" + i + ".mp3");
            songs.add(song);
        }
        
        return songs;
    }

    /**
     * Setup adapter
     */
    private void setupAdapter() {
        moodSongsAdapter = new MoodSongsAdapter(moodSongs, this::onSongClicked);
        rvMoodSongs.setLayoutManager(new LinearLayoutManager(this));
        rvMoodSongs.setAdapter(moodSongsAdapter);
    }

    /**
     * Start animations
     */
    private void startAnimations() {
        // Animate header entrance
        animateHeader();
        
        // Animate songs list
        animateSongsList();
        
        // Start background animation
        startBackgroundAnimation();
    }

    /**
     * Animate header entrance
     */
    private void animateHeader() {
        cardHeader.setTranslationY(-100f);
        cardHeader.setAlpha(0f);
        
        cardHeader.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    /**
     * Animate songs list entrance
     */
    private void animateSongsList() {
        rvMoodSongs.setTranslationY(100f);
        rvMoodSongs.setAlpha(0f);
        
        rvMoodSongs.postDelayed(() -> {
            rvMoodSongs.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(1000)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }, 300);
    }

    /**
     * Start background animation
     */
    private void startBackgroundAnimation() {
        backgroundAnimator = ValueAnimator.ofFloat(0f, 1f);
        backgroundAnimator.setDuration(5000);
        backgroundAnimator.setRepeatCount(ValueAnimator.INFINITE);
        backgroundAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            // Apply subtle background animation
        });
        backgroundAnimator.start();
    }

    /**
     * Handle song click
     */
    private void onSongClicked(SongsList song) {
        // Navigate to now playing
        Intent intent = new Intent(this, GlassmorphismNowPlayingActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
        
        // Add transition animation
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Resume animations
        if (backgroundAnimator != null && !backgroundAnimator.isRunning()) {
            backgroundAnimator.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Pause animations
        if (backgroundAnimator != null && backgroundAnimator.isRunning()) {
            backgroundAnimator.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up animations
        if (backgroundAnimator != null) {
            backgroundAnimator.cancel();
        }
        
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
    }
}
