package com.example.madproject.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.madproject.R;

/**
 * Splash screen activity that displays for 2 seconds with a fade-in animation
 * before navigating to the main activity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.iv_splash_logo);
        TextView appName = findViewById(R.id.tv_splash_name);
        TextView tagline = findViewById(R.id.tv_splash_tagline);

        // Fade-in animation for logo
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1500);
        fadeIn.setFillAfter(true);

        // Staggered fade-in for text
        AlphaAnimation textFadeIn = new AlphaAnimation(0.0f, 1.0f);
        textFadeIn.setDuration(1200);
        textFadeIn.setStartOffset(300);
        textFadeIn.setFillAfter(true);

        AlphaAnimation taglineFadeIn = new AlphaAnimation(0.0f, 1.0f);
        taglineFadeIn.setDuration(1000);
        taglineFadeIn.setStartOffset(600);
        taglineFadeIn.setFillAfter(true);

        logo.startAnimation(fadeIn);
        appName.startAnimation(textFadeIn);
        tagline.startAnimation(taglineFadeIn);

        // Navigate to MainActivity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, SPLASH_DURATION);
    }
}
