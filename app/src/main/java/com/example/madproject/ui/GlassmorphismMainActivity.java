package com.example.madproject.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.OnBackPressedCallback;

import com.example.madproject.R;
import com.example.madproject.models.SongsList;
import com.example.madproject.ui.adapters.MoodCategoryAdapter;
import com.example.madproject.ui.adapters.BackgroundParticleAdapter;
import com.example.madproject.utils.MoodAlgorithm;
// import com.example.madproject.utils.MoodOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Modern Glassmorphism Main Activity with immersive scroll-based UI,
 * real motion, depth, and interactions.
 */
public class GlassmorphismMainActivity extends AppCompatActivity {

    private static final String TAG = "GlassmorphismMainActivity";

    // UI Components
    private RecyclerView rvMoodCategories;
    private RecyclerView rvRecentSongs;
    private RecyclerView rvBackgroundParticles;
    private TextView tvTotalSongs;
    private TextView tvMoodAccuracy;
    private ImageView ivBackgroundGradient;
    private CardView cardHero;
    private Button btnAbout;

    // Adapters
    private MoodCategoryAdapter moodCategoryAdapter;
    private RecyclerView.Adapter recentSongsAdapter;
    private BackgroundParticleAdapter particleAdapter;

    // Data
    private List<MoodCategory> moodCategories;
    private List<SongsList> recentSongs;
    private List<BackgroundParticle> particles;
    private Random random = new Random();

    // Animation
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator backgroundAnimator;
    private boolean isAnimating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set glassmorphism theme
        setTheme(R.style.GlassmorphismTheme);
        setContentView(R.layout.activity_main_glassmorphism);

        // Setup back navigation callback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Navigate to MainActivity
                Intent intent = new Intent(GlassmorphismMainActivity.this, com.example.madproject.activities.MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });

        // Initialize UI safely
        try {
            initViews();
            initData();
            setupAdapters();
            startAnimations();
            setupListeners();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing glassmorphic UI", e);
            // Fallback to basic initialization
        }
    }

    /**
     * Setup edge-to-edge display for immersive experience
     */
    private void setupEdgeToEdge() {
        try {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
                if (insets != null) {
                    int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                    int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                    
                    // Apply padding to avoid system bars
                    v.setPadding(0, statusBarHeight, 0, navigationBarHeight);
                }
                return insets;
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up edge-to-edge display", e);
            // Continue without edge-to-edge if it fails
        }
    }

    /**
     * Initialize all views
     */
    private void initViews() {
        try {
            rvMoodCategories = findViewById(R.id.rv_mood_categories);
            rvRecentSongs = findViewById(R.id.rv_recent_songs);
            rvBackgroundParticles = findViewById(R.id.rv_background_particles);
            tvTotalSongs = findViewById(R.id.tv_total_songs);
            tvMoodAccuracy = findViewById(R.id.tv_mood_accuracy);
            ivBackgroundGradient = findViewById(R.id.iv_background_gradient);
            cardHero = findViewById(R.id.card_hero);
            btnAbout = findViewById(R.id.btn_about);
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    /**
     * Initialize data
     */
    private void initData() {
        // Initialize mood categories
        moodCategories = createMoodCategories();
        
        // Initialize recent songs
        recentSongs = new ArrayList<>();
        loadRecentSongs();
        
        // Initialize background particles
        particles = createBackgroundParticles();
    }

    /**
     * Load recent songs from storage
     */
    private void loadRecentSongs() {
        try {
            // For now, create placeholder data
            // In a full implementation, this would load from SharedPreferences or database
            for (int i = 0; i < 10; i++) {
                SongsList song = new SongsList();
                song.setTitle("Recent Song " + (i + 1));
                song.setArtist("Artist " + (i + 1));
                song.setPath("/path/to/recent/song" + i + ".mp3");
                recentSongs.add(song);
            }
        } catch (Exception e) {
            Log.e("GlassmorphismMainActivity", "Error loading recent songs", e);
        }
    }

    /**
     * Create mood categories with glassmorphism styling
     */
    private List<MoodCategory> createMoodCategories() {
        List<MoodCategory> categories = new ArrayList<>();
        
        categories.add(new MoodCategory("Happy", MoodAlgorithm.MOOD_HAPPY, 
                R.drawable.ic_happy, R.color.mood_happy_glass, 45));
        categories.add(new MoodCategory("Sad", MoodAlgorithm.MOOD_SAD, 
                R.drawable.ic_sad, R.color.mood_sad_glass, 23));
        categories.add(new MoodCategory("Calm", MoodAlgorithm.MOOD_CALM, 
                R.drawable.ic_calm, R.color.mood_calm_glass, 67));
        categories.add(new MoodCategory("Energetic", MoodAlgorithm.MOOD_ENERGETIC, 
                R.drawable.ic_energetic, R.color.mood_energetic_glass, 89));
        
        return categories;
    }

    /**
     * Create background particles for animation
     */
    private List<BackgroundParticle> createBackgroundParticles() {
        List<BackgroundParticle> particles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            particles.add(new BackgroundParticle(
                    random.nextInt(360),
                    random.nextFloat() * 100,
                    random.nextFloat() * 0.5f + 0.1f,
                    (long) (random.nextFloat() * 2000 + 1000)
            ));
        }
        return particles;
    }

    /**
     * Setup adapters with glassmorphism styling
     */
    private void setupAdapters() {
        // Mood categories adapter
        moodCategoryAdapter = new MoodCategoryAdapter(moodCategories, this::onMoodCategoryClicked);
        rvMoodCategories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMoodCategories.setAdapter(moodCategoryAdapter);

        // Recent songs adapter
        recentSongsAdapter = new RecyclerView.Adapter() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                return null;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            }

            @Override
            public int getItemCount() {
                return recentSongs.size();
            }
        };
        rvRecentSongs.setLayoutManager(new LinearLayoutManager(this));
        rvRecentSongs.setAdapter(recentSongsAdapter);

        // Background particles adapter
        particleAdapter = new BackgroundParticleAdapter(particles);
        rvBackgroundParticles.setLayoutManager(new LinearLayoutManager(this));
        rvBackgroundParticles.setAdapter(particleAdapter);
    }

    /**
     * Start immersive animations
     */
    private void startAnimations() {
        // Animate hero card entrance
        animateHeroCard();
        
        // Animate mood categories
        animateMoodCategories();
        
        // Animate background particles
        animateBackgroundParticles();
        
        // Animate stats
        animateStats();
    }

    /**
     * Animate hero card with smooth entrance
     */
    private void animateHeroCard() {
        cardHero.setTranslationY(200f);
        cardHero.setAlpha(0f);
        
        cardHero.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    /**
     * Animate mood categories with staggered entrance
     */
    private void animateMoodCategories() {
        rvMoodCategories.setTranslationX(100f);
        rvMoodCategories.setAlpha(0f);
        
        rvMoodCategories.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }

    /**
     * Animate background particles continuously
     */
    private void animateBackgroundParticles() {
        backgroundAnimator = ValueAnimator.ofFloat(0f, 360f);
        backgroundAnimator.setDuration(30000);
        backgroundAnimator.setRepeatCount(ValueAnimator.INFINITE);
        backgroundAnimator.addUpdateListener(animation -> {
            float rotation = (float) animation.getAnimatedValue();
            updateParticles(rotation);
        });
        backgroundAnimator.start();
    }

    /**
     * Update particle positions
     */
    private void updateParticles(float rotation) {
        for (int i = 0; i < particles.size(); i++) {
            BackgroundParticle particle = particles.get(i);
            particle.currentRotation = rotation;
            particle.currentScale = 0.5f + (float) Math.sin(Math.toRadians(rotation + particle.initialAngle)) * 0.3f;
        }
        particleAdapter.notifyDataSetChanged();
    }

    /**
     * Animate stats with counting effect
     */
    private void animateStats() {
        animateNumber(tvTotalSongs, 0, 1234, 1500);
        animateNumber(tvMoodAccuracy, 0, 90, 2000);
    }

    /**
     * Animate number counting
     */
    private void animateNumber(TextView textView, int start, int end, int duration) {
        ValueAnimator animator = ValueAnimator.ofInt(start, end);
        animator.setDuration(duration);
        animator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            textView.setText(String.valueOf(value));
        });
        animator.start();
    }

    /**
     * Handle mood category click with glassmorphism feedback
     */
    private void onMoodCategoryClicked(MoodCategory category) {
        // Create ripple effect
        createRippleEffect(category.moodColor);
        
        // Navigate to mood songs
        Intent intent = new Intent(this, MoodSongsActivity.class);
        intent.putExtra("mood", category.mood);
        intent.putExtra("mood_name", category.name);
        startActivity(intent);
        
        // Add transition animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Handle song click with glassmorphism feedback
     */
    private void onSongClicked(SongsList song) {
        // Create ripple effect
        createRippleEffect(Color.parseColor("#6C63FF"));
        
        // Navigate to now playing
        Intent intent = new Intent(this, GlassmorphismNowPlayingActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
        
        // Add transition animation
        overridePendingTransition(R.anim.slide_in_bottom, R.anim.slide_out_top);
    }

    /**
     * Create ripple effect at touch point
     */
    private void createRippleEffect(int color) {
        if (color == 0) {
            Log.w("GlassmorphismMainActivity", "Invalid color for ripple effect");
            return;
        }
        
        // Basic ripple effect implementation
        // In a full implementation, this would create a circular ripple animation
        // at the touch point with proper animation and fade out
        Log.d("GlassmorphismMainActivity", "Creating ripple effect with color: " + color);
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

    /**
     * Mood category data class
     */
    public static class MoodCategory {
        public String name;
        public String mood;
        public int iconRes;
        public int colorRes;
        public int moodColor;
        public int songCount;

        public MoodCategory(String name, String mood, int iconRes, int colorRes, int songCount) {
            this.name = name;
            this.mood = mood;
            this.iconRes = iconRes;
            this.colorRes = colorRes;
            this.songCount = songCount;
            this.moodColor = MoodAlgorithm.getMoodColor(mood);
        }
    }

    /**
     * Setup click listeners
     */
    private void setupListeners() {
        try {
            // About button click listener
            if (btnAbout != null) {
                btnAbout.setOnClickListener(v -> showAboutDialog());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    /**
     * Shows the About dialog with glassmorphism styling.
     */
    private void showAboutDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("About SonicWave")
                    .setMessage("SonicWave Music Player\n\n"
                        + "Version: 2.2.0\n\n"
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
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    /**
     * Background particle data class
     */
    public static class BackgroundParticle {
        public float initialAngle;
        public float initialScale;
        public float currentRotation;
        public float currentScale;
        public long animationDuration;

        public BackgroundParticle(float initialAngle, float initialScale, float currentScale, long animationDuration) {
            this.initialAngle = initialAngle;
            this.initialScale = initialScale;
            this.currentScale = currentScale;
            this.animationDuration = animationDuration;
        }
    }
}
