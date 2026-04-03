package com.example.madproject.activities;

import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import com.example.madproject.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;

public class EqualizerActivity extends AppCompatActivity {
    private static final String TAG = "EqualizerActivity";

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private LoudnessEnhancer loudnessEnhancer;

    private SwitchMaterial switchEnabled;
    private Spinner spinnerPresets;
    private LinearLayout bandsContainer;
    private SeekBar seekbarBass, seekbarVirtualizer, seekbarLoudness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        switchEnabled = findViewById(R.id.switch_eq_enabled);
        spinnerPresets = findViewById(R.id.spinner_presets);
        bandsContainer = findViewById(R.id.bands_container);
        seekbarBass = findViewById(R.id.seekbar_bass);
        seekbarVirtualizer = findViewById(R.id.seekbar_virtualizer);
        seekbarLoudness = findViewById(R.id.seekbar_loudness);

        int audioSessionId = getIntent().getIntExtra("audio_session_id", 0);

        try {
            equalizer = new Equalizer(0, audioSessionId);
            bassBoost = new BassBoost(0, audioSessionId);
            virtualizer = new Virtualizer(0, audioSessionId);
            loudnessEnhancer = new LoudnessEnhancer(audioSessionId);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing audio effects", e);
            return;
        }

        // Switch to enable/disable
        switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            equalizer.setEnabled(isChecked);
            bassBoost.setEnabled(isChecked);
            virtualizer.setEnabled(isChecked);
            loudnessEnhancer.setEnabled(isChecked);
        });

        // Setup Presets
        ArrayList<String> presetNames = new ArrayList<>();
        for (short i = 0; i < equalizer.getNumberOfPresets(); i++) {
            presetNames.add(equalizer.getPresetName(i));
        }

        ArrayAdapter<String> presetAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, presetNames);
        spinnerPresets.setAdapter(presetAdapter);

        spinnerPresets.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position,
                    long id) {
                equalizer.usePreset((short) position);
                updateBandSliders();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // Build EQ Band Sliders
        setupBandSliders();

        // Bass Boost
        seekbarBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    bassBoost.setStrength((short) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Virtualizer
        seekbarVirtualizer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    virtualizer.setStrength((short) progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Loudness Enhancer (Volume Normalization)
        seekbarLoudness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    loudnessEnhancer.setTargetGain(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void setupBandSliders() {
        short bands = equalizer.getNumberOfBands();
        short minLevel = equalizer.getBandLevelRange()[0];
        short maxLevel = equalizer.getBandLevelRange()[1];

        for (short i = 0; i < bands; i++) {
            TextView label = new TextView(this);
            label.setTextColor(getResources().getColor(R.color.text_secondary));
            label.setText(String.format("%d Hz", equalizer.getCenterFreq(i) / 1000));

            SeekBar seekBar = new SeekBar(this);
            seekBar.setMax(maxLevel - minLevel);
            seekBar.setProgress(equalizer.getBandLevel(i) - minLevel);

            final short band = i;
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        equalizer.setBandLevel(band, (short) (progress + minLevel));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            bandsContainer.addView(label);
            bandsContainer.addView(seekBar);
        }
    }

    private void updateBandSliders() {
        int childIndex = 0;
        short minLevel = equalizer.getBandLevelRange()[0];

        for (short i = 0; i < equalizer.getNumberOfBands(); i++) {
            // Skip label, get seekbar
            childIndex++; // label
            if (childIndex < bandsContainer.getChildCount()) {
                SeekBar seekBar = (SeekBar) bandsContainer.getChildAt(childIndex);
                seekBar.setProgress(equalizer.getBandLevel(i) - minLevel);
            }
            childIndex++; // seekbar
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (equalizer != null)
            equalizer.release();
        if (bassBoost != null)
            bassBoost.release();
        if (virtualizer != null)
            virtualizer.release();
        if (loudnessEnhancer != null)
            loudnessEnhancer.release();
    }
}
