package com.example.madproject.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.example.madproject.R;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.MoodAlgorithm;
import com.example.madproject.utils.UserFeedbackManager;

/**
 * Quick mood correction dialog with radio buttons for easy mood selection.
 * Simple interface for users to correct mood predictions in the Now Playing screen.
 */
public class QuickMoodCorrectionDialog extends DialogFragment {
    
    public interface QuickMoodCorrectionListener {
        void onMoodCorrected(String songPath, String correctedMood);
    }
    
    private static final String ARG_SONG = "song";
    private static final String ARG_CURRENT_MOOD = "current_mood";
    
    private SongsList song;
    private String currentMood;
    private QuickMoodCorrectionListener listener;
    private UserFeedbackManager feedbackManager;
    
    public static QuickMoodCorrectionDialog newInstance(SongsList song, String currentMood) {
        QuickMoodCorrectionDialog dialog = new QuickMoodCorrectionDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SONG, song);
        args.putString(ARG_CURRENT_MOOD, currentMood);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle args = getArguments();
        if (args != null) {
            song = (SongsList) args.getSerializable(ARG_SONG);
            currentMood = args.getString(ARG_CURRENT_MOOD);
        }
        
        feedbackManager = new UserFeedbackManager(requireContext());
    }
    
    @Override
    public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_quick_mood_correction, null);
        
        setupUI(view);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
               .setTitle("Correct Mood Tag")
               .setNegativeButton("Cancel", (dialog, which) -> dismiss());
        
        return builder.create();
    }
    
    private void setupUI(View view) {
        // Song info
        TextView tvSongTitle = view.findViewById(R.id.tv_song_title);
        TextView tvSongArtist = view.findViewById(R.id.tv_song_artist);
        TextView tvCurrentMood = view.findViewById(R.id.tv_current_mood);
        
        tvSongTitle.setText(song.getTitle());
        tvSongArtist.setText(song.getArtist());
        tvCurrentMood.setText("Current: " + MoodAlgorithm.getMoodDisplayName(currentMood));
        
        // Radio group for mood selection
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_moods);
        
        // Set current mood as default selection
        if (currentMood.equals(MoodAlgorithm.MOOD_HAPPY)) {
            radioGroup.check(R.id.radio_happy);
        } else if (currentMood.equals(MoodAlgorithm.MOOD_SAD)) {
            radioGroup.check(R.id.radio_sad);
        } else if (currentMood.equals(MoodAlgorithm.MOOD_CALM)) {
            radioGroup.check(R.id.radio_calm);
        } else if (currentMood.equals(MoodAlgorithm.MOOD_ENERGETIC)) {
            radioGroup.check(R.id.radio_energetic);
        }
        
        // Confirm button
        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> confirmCorrection(radioGroup));
    }
    
    private void confirmCorrection(RadioGroup radioGroup) {
        int selectedId = radioGroup.getCheckedRadioButtonId();
        String correctedMood = null;
        
        if (selectedId == R.id.radio_happy) {
            correctedMood = MoodAlgorithm.MOOD_HAPPY;
        } else if (selectedId == R.id.radio_sad) {
            correctedMood = MoodAlgorithm.MOOD_SAD;
        } else if (selectedId == R.id.radio_calm) {
            correctedMood = MoodAlgorithm.MOOD_CALM;
        } else if (selectedId == R.id.radio_energetic) {
            correctedMood = MoodAlgorithm.MOOD_ENERGETIC;
        }
        
        if (correctedMood == null) {
            Toast.makeText(getContext(), "Please select a mood", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (correctedMood.equals(currentMood)) {
            Toast.makeText(getContext(), "Mood unchanged", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        
        // Record the correction
        boolean success = feedbackManager.recordMoodCorrection(
                song.getPath(), currentMood, correctedMood, 1.0, "User corrected via quick dialog");
        
        if (success) {
            Toast.makeText(getContext(), "Mood corrected successfully!", Toast.LENGTH_SHORT).show();
            
            // Notify listener
            if (listener != null) {
                listener.onMoodCorrected(song.getPath(), correctedMood);
            }
            
            dismiss();
        } else {
            Toast.makeText(getContext(), "Failed to save correction", Toast.LENGTH_LONG).show();
        }
    }
    
    public void setQuickMoodCorrectionListener(QuickMoodCorrectionListener listener) {
        this.listener = listener;
    }
}
