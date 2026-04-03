package com.example.madproject.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import com.example.madproject.R;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.MoodAlgorithm;
// import com.example.madproject.utils.MoodOperations;
import com.example.madproject.dialogs.QuickMoodCorrectionDialog;

/**
 * Modern Glassmorphism Now Playing Activity with immersive effects,
 * real motion, depth, and smooth interactions.
 */
public class GlassmorphismNowPlayingActivity extends AppCompatActivity {

    // UI Components
    private ImageView ivAlbumArt;
    private ImageView ivBlurredBackground;
    private TextView tvSongTitle;
    private TextView tvSongArtist;
    private TextView tvMoodTag;
    private TextView tvGenreTag;
    private CardView cardAlbumArt;
    private CardView cardSongInfo;
    private CardView cardControls;

    // Data
    private SongsList currentSong;
    private String currentMood;
    private String currentGenre;
    private boolean isPlaying = false;

    // Animation
    private Handler animationHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator pulseAnimator;
    private ValueAnimator slideAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set glassmorphism theme
        setTheme(R.style.GlassmorphismTheme);
        setContentView(R.layout.fragment_current_song_glassmorphism);

        // Enable edge-to-edge display
        setupEdgeToEdge();

        // Initialize UI
        initViews();
        loadData();
        setupAnimations();
        startImmersiveEffects();
    }

    /**
     * Setup edge-to-edge display
     */
    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            
            // Apply padding to avoid system bars
            v.setPadding(0, statusBarHeight, 0, navigationBarHeight);
            
            return insets;
        });
    }

    /**
     * Initialize all views
     */
    private void initViews() {
        ivAlbumArt = findViewById(R.id.iv_album_art);
        ivBlurredBackground = findViewById(R.id.iv_blurred_background);
        tvSongTitle = findViewById(R.id.tv_song_title);
        tvSongArtist = findViewById(R.id.tv_song_artist);
        tvMoodTag = findViewById(R.id.tv_mood_tag);
        tvGenreTag = findViewById(R.id.tv_genre_tag);
        cardAlbumArt = findViewById(R.id.card_album_art);
        cardSongInfo = findViewById(R.id.card_song_info);
        cardControls = findViewById(R.id.card_controls);
    }

    /**
     * Load song data
     */
    private void loadData() {
        // Get song from intent
        Intent intent = getIntent();
        if (intent != null) {
            currentSong = (SongsList) intent.getSerializableExtra("song");
        }
        
        if (currentSong != null) {
            // Load mood and genre from database
            loadMoodAndGenre();
            
            // Update UI
            updateSongInfo();
        }
    }

    /**
     * Load mood and genre from database
     */
    private void loadMoodAndGenre() {
        try {
            // MoodOperations moodOps = new MoodOperations(this);
            // currentMood = moodOps.getMoodTag(currentSong.getPath());
            
            // TODO: Load genre from database
            currentGenre = "Pop"; // Placeholder
            
        } catch (Exception e) {
            currentMood = null;
            currentGenre = null;
        }
    }

    /**
     * Update song information display
     */
    private void updateSongInfo() {
        tvSongTitle.setText(currentSong.getTitle());
        tvSongArtist.setText(currentSong.getArtist());
        
        // Update mood tag
        if (currentMood != null) {
            String moodDisplay = MoodAlgorithm.getMoodDisplayName(currentMood);
            tvMoodTag.setText(moodDisplay);
            tvMoodTag.setVisibility(View.VISIBLE);
        }
        
        // Update genre tag
        if (currentGenre != null) {
            tvGenreTag.setText(currentGenre);
            tvGenreTag.setVisibility(View.VISIBLE);
        }
        
        // Load album art
        loadAlbumArt();
    }

    /**
     * Load album art with glassmorphism effect
     */
    private void loadAlbumArt() {
        if (currentSong == null || ivAlbumArt == null || ivBlurredBackground == null) {
            Log.w("GlassmorphismNowPlaying", "Cannot load album art - missing views or song data");
            return;
        }
        
        try {
            // Load actual album art using Glide
            Glide.with(this)
                .load(currentSong.getPath())
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        ivAlbumArt.setImageDrawable(resource);
                        ivBlurredBackground.setImageDrawable(resource);
                    }
                    
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // Fallback to placeholder
                        ivAlbumArt.setImageResource(R.drawable.ic_music_note);
                        ivBlurredBackground.setImageResource(R.drawable.ic_music_note);
                    }
                });
        } catch (Exception e) {
            Log.e("GlassmorphismNowPlaying", "Error loading album art", e);
            // Fallback to placeholder
            ivAlbumArt.setImageResource(R.drawable.ic_music_note);
            ivBlurredBackground.setImageResource(R.drawable.ic_music_note);
        }
    }

    /**
     * Setup animations
     */
    private void setupAnimations() {
        // Animate album art entrance
        animateAlbumArt();
        
        // Animate song info entrance
        animateSongInfo();
        
        // Animate controls entrance
        animateControls();
        
        // Start pulse animation for playing state
        startPulseAnimation();
    }

    /**
     * Animate album art with smooth entrance
     */
    private void animateAlbumArt() {
        cardAlbumArt.setTranslationY(200f);
        cardAlbumArt.setAlpha(0f);
        
        cardAlbumArt.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(1000)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
    }

    /**
     * Animate song info with slide effect
     */
    private void animateSongInfo() {
        cardSongInfo.setTranslationX(100f);
        cardSongInfo.setAlpha(0f);
        
        cardSongInfo.postDelayed(() -> {
            cardSongInfo.animate()
                    .translationX(0f)
                    .alpha(1f)
                    .setDuration(800)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }, 300);
    }

    /**
     * Animate controls with slide effect
     */
    private void animateControls() {
        cardControls.setTranslationY(100f);
        cardControls.setAlpha(0f);
        
        cardControls.postDelayed(() -> {
            cardControls.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(800)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }, 500);
    }

    /**
     * Start pulse animation for playing state
     */
    private void startPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            cardAlbumArt.setScaleX(scale);
            cardAlbumArt.setScaleY(scale);
        });
        pulseAnimator.start();
    }

    /**
     * Start immersive effects
     */
    private void startImmersiveEffects() {
        // Start background particle animation
        startBackgroundAnimation();
        
        // Start color transitions
        startColorTransitions();
        
        // Start parallax effects
        startParallaxEffects();
    }

    /**
     * Start background animation
     */
    private void startBackgroundAnimation() {
        slideAnimator = ValueAnimator.ofFloat(0f, 1f);
        slideAnimator.setDuration(10000);
        slideAnimator.setRepeatCount(ValueAnimator.INFINITE);
        slideAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            float alpha = 0.3f + (float) Math.sin(value * Math.PI * 2) * 0.1f;
            ivBlurredBackground.setAlpha(alpha);
        });
        slideAnimator.start();
    }

    /**
     * Start color transitions
     */
    private void startColorTransitions() {
        if (currentMood == null) {
            Log.w("GlassmorphismNowPlaying", "Cannot start color transitions - no mood data");
            return;
        }
        
        try {
            // Implement color transitions based on mood
            // This would change the gradient colors based on the current mood
            int moodColor = getMoodColor(currentMood);
            
            // Create subtle color animation
            ValueAnimator colorAnimator = ValueAnimator.ofArgb(0x40000000, moodColor | 0x40000000);
            colorAnimator.setDuration(3000);
            colorAnimator.setRepeatCount(ValueAnimator.INFINITE);
            colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
            colorAnimator.addUpdateListener(animation -> {
                int color = (int) animation.getAnimatedValue();
                if (ivBlurredBackground != null) {
                    ivBlurredBackground.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP);
                }
            });
            colorAnimator.start();
            
            Log.d("GlassmorphismNowPlaying", "Color transitions started for mood: " + currentMood);
        } catch (Exception e) {
            Log.e("GlassmorphismNowPlaying", "Error starting color transitions", e);
        }
    }

    /**
     * Start parallax effects
     */
    private void startParallaxEffects() {
        if (cardAlbumArt == null) {
            Log.w("GlassmorphismNowPlaying", "Cannot start parallax effects - album art view not found");
            return;
        }
        
        try {
            // Implement parallax scrolling effects
            // This would create depth perception during scrolling
            ValueAnimator parallaxAnimator = ValueAnimator.ofFloat(0f, 1f);
            parallaxAnimator.setDuration(8000);
            parallaxAnimator.setRepeatCount(ValueAnimator.INFINITE);
            parallaxAnimator.setRepeatMode(ValueAnimator.REVERSE);
            parallaxAnimator.addUpdateListener(animation -> {
                float value = (float) animation.getAnimatedValue();
                float translationX = (float) Math.sin(value * Math.PI * 2) * 20f;
                float translationY = (float) Math.cos(value * Math.PI * 2) * 10f;
                
                cardAlbumArt.setTranslationX(translationX);
                cardAlbumArt.setTranslationY(translationY);
            });
            parallaxAnimator.start();
            
            Log.d("GlassmorphismNowPlaying", "Parallax effects started");
        } catch (Exception e) {
            Log.e("GlassmorphismNowPlaying", "Error starting parallax effects", e);
        }
    }

    /**
     * Handle mood correction with glassmorphism feedback
     */
    private void showMoodCorrectionDialog() {
        if (currentSong == null || currentMood == null) {
            return;
        }
        
        // Create glassmorphic correction dialog
        QuickMoodCorrectionDialog dialog = QuickMoodCorrectionDialog.newInstance(currentSong, currentMood);
        dialog.setQuickMoodCorrectionListener((songPath, correctedMood) -> {
            // Update mood with animation
            updateMoodWithAnimation(correctedMood);
        });
        dialog.show(getSupportFragmentManager(), "glass_mood_correction");
    }

    /**
     * Update mood with smooth animation
     */
    private void updateMoodWithAnimation(String newMood) {
        // Create color transition
        int fromColor = getMoodColor(currentMood);
        int toColor = getMoodColor(newMood);
        
        // Animate color change
        ValueAnimator colorAnimator = ValueAnimator.ofArgb(fromColor, toColor);
        colorAnimator.setDuration(500);
        colorAnimator.addUpdateListener(animation -> {
            int color = (int) animation.getAnimatedValue();
            tvMoodTag.getBackground().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        });
        colorAnimator.start();
        
        // Update text
        currentMood = newMood;
        String moodDisplay = MoodAlgorithm.getMoodDisplayName(newMood);
        tvMoodTag.setText(moodDisplay);
    }

    /**
     * Get mood color
     */
    private int getMoodColor(String mood) {
        if (mood == null) return Color.parseColor("#6C63FF");
        
        switch (mood) {
            case MoodAlgorithm.MOOD_HAPPY:
                return Color.parseColor("#FFD93D");
            case MoodAlgorithm.MOOD_SAD:
                return Color.parseColor("#6C63FF");
            case MoodAlgorithm.MOOD_CALM:
                return Color.parseColor("#6BCF7F");
            case MoodAlgorithm.MOOD_ENERGETIC:
                return Color.parseColor("#FF6B6B");
            case MoodAlgorithm.MOOD_PARTY:
                return Color.parseColor("#FF61D8");
            default:
                return Color.parseColor("#6C63FF");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Resume animations
        if (pulseAnimator != null && !pulseAnimator.isRunning()) {
            pulseAnimator.start();
        }
        
        if (slideAnimator != null && !slideAnimator.isRunning()) {
            slideAnimator.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        // Pause animations
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            pulseAnimator.pause();
        }
        
        if (slideAnimator != null && slideAnimator.isRunning()) {
            slideAnimator.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up animations
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
        }
        
        if (slideAnimator != null) {
            slideAnimator.cancel();
        }
        
        if (animationHandler != null) {
            animationHandler.removeCallbacksAndMessages(null);
        }
    }
}
