package com.example.madproject.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.madproject.R;

/**
 * Simple test version of GlassmorphismMainActivity to identify crash issues
 */
public class GlassmorphismMainActivitySimple extends AppCompatActivity {

    private static final String TAG = "GlassmorphismMainActivitySimple";

    private TextView tvTest;
    private Button btnAbout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Use simple layout first
        setContentView(R.layout.activity_main_simple_test);
        
        try {
            tvTest = findViewById(R.id.tv_test);
            btnAbout = findViewById(R.id.btn_about);
            
            if (tvTest != null) {
                tvTest.setText("Glassmorphism UI Test - Working!");
            }
            
            if (btnAbout != null) {
                btnAbout.setOnClickListener(v -> showAboutDialog());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }

    private void showAboutDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("About SonicWave")
                    .setMessage("SonicWave Music Player\n\n"
                            + "Version: 2.1.0\n\n"
                            + "A modern, glassmorphic music player for Android.\n\n"
                            + "Created by:\n"
                            + "F030 - Mayur H. Doshi\n"
                            + "F030 - Keval N. Mehta\n"
                            + "F052 - Yash D. Mehta")
                    .setPositiveButton("OK", null)
                    .setIcon(R.drawable.ic_music_note)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
    }
}
