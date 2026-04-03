package com.example.madproject.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.DialogFragment;

import com.example.madproject.R;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.MoodAlgorithm;
import com.example.madproject.utils.UserFeedbackManager;

/**
 * Dialog for mood correction feedback.
 * Allows users to correct mood predictions and provide feedback.
 */
public class MoodCorrectionDialog extends DialogFragment {
    
    public interface MoodCorrectionListener {
        void onMoodCorrected(String songPath, String originalMood, String correctedMood);
    }
    
    private static final String ARG_SONG = "song";
    private static final String ARG_PREDICTED_MOOD = "predicted_mood";
    private static final String ARG_CONFIDENCE = "confidence";
    private static final String ARG_REASONING = "reasoning";
    
    private SongsList song;
    private String predictedMood;
    private double confidence;
    private String reasoning;
    private MoodCorrectionListener listener;
    private Spinner moodSpinner;
    private UserFeedbackManager feedbackManager;
    
    public static MoodCorrectionDialog newInstance(SongsList song, String predictedMood, 
                                           double confidence, String reasoning) {
        MoodCorrectionDialog dialog = new MoodCorrectionDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SONG, song);
        args.putString(ARG_PREDICTED_MOOD, predictedMood);
        args.putDouble(ARG_CONFIDENCE, confidence);
        args.putString(ARG_REASONING, reasoning);
        dialog.setArguments(args);
        return dialog;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle args = getArguments();
        if (args != null) {
            song = (SongsList) args.getSerializable(ARG_SONG);
            predictedMood = args.getString(ARG_PREDICTED_MOOD);
            confidence = args.getDouble(ARG_CONFIDENCE);
            reasoning = args.getString(ARG_REASONING);
        }
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_mood_correction, null);
        
        // Initialize feedback manager
        feedbackManager = new UserFeedbackManager(requireContext());
        
        // Setup UI components
        setupUI(view);
        
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setView(view)
               .setTitle("Correct Mood Tag")
               .setPositiveButton("Submit", (dialog, which) -> submitCorrection())
               .setNegativeButton("Cancel", (dialog, which) -> dismiss())
               .setNeutralButton("Keep Original", (dialog, which) -> dismiss());
        
        return builder.create();
    }
    
    private void setupUI(View view) {
        // Song info
        TextView tvSongTitle = view.findViewById(R.id.tv_song_title);
        TextView tvSongArtist = view.findViewById(R.id.tv_song_artist);
        TextView tvPredictedMood = view.findViewById(R.id.tv_predicted_mood);
        TextView tvConfidence = view.findViewById(R.id.tv_confidence);
        TextView tvReasoning = view.findViewById(R.id.tv_reasoning);
        
        tvSongTitle.setText(song.getTitle());
        tvSongArtist.setText(song.getArtist());
        tvPredictedMood.setText("Predicted: " + MoodAlgorithm.getMoodDisplayName(predictedMood));
        tvConfidence.setText("Confidence: " + String.format("%.1f%%", confidence * 100));
        tvReasoning.setText("Reasoning: " + reasoning);
        
        // Mood spinner
        moodSpinner = view.findViewById(R.id.spinner_mood_correction);
        String[] moods = {
            MoodAlgorithm.MOOD_HAPPY,
            MoodAlgorithm.MOOD_SAD,
            MoodAlgorithm.MOOD_CALM,
            MoodAlgorithm.MOOD_ENERGETIC,
            MoodAlgorithm.MOOD_PARTY
        };
        
        String[] moodDisplayNames = new String[moods.length];
        for (int i = 0; i < moods.length; i++) {
            moodDisplayNames[i] = MoodAlgorithm.getMoodDisplayName(moods[i]);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_dropdown_item, moodDisplayNames);
        moodSpinner.setAdapter(adapter);
        
        // Set default to predicted mood
        for (int i = 0; i < moods.length; i++) {
            if (moods[i].equals(predictedMood)) {
                moodSpinner.setSelection(i);
                break;
            }
        }
        
        // Show suggestion if available
        String suggestedCorrection = feedbackManager.getSuggestedCorrection(predictedMood, song);
        if (suggestedCorrection != null && !suggestedCorrection.equals(predictedMood)) {
            TextView tvSuggestion = view.findViewById(R.id.tv_suggestion);
            tvSuggestion.setVisibility(View.VISIBLE);
            tvSuggestion.setText("💡 Suggestion: " + MoodAlgorithm.getMoodDisplayName(suggestedCorrection));
        }
    }
    
    private void submitCorrection() {
        String correctedMood = getSelectedMood();
        
        if (correctedMood == null) {
            Toast.makeText(getContext(), "Please select a mood", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (correctedMood.equals(predictedMood)) {
            Toast.makeText(getContext(), "No change in mood selected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Record the correction
        boolean success = feedbackManager.recordMoodCorrection(
                song.getPath(), predictedMood, correctedMood, confidence, reasoning);
        
        if (success) {
            Toast.makeText(getContext(), "Mood corrected successfully!", Toast.LENGTH_SHORT).show();
            
            // Notify listener
            if (listener != null) {
                listener.onMoodCorrected(song.getPath(), predictedMood, correctedMood);
            }
            
            dismiss();
        } else {
            Toast.makeText(getContext(), "Failed to save correction", Toast.LENGTH_LONG).show();
        }
    }
    
    private String getSelectedMood() {
        int position = moodSpinner.getSelectedItemPosition();
        String[] moods = {
            MoodAlgorithm.MOOD_HAPPY,
            MoodAlgorithm.MOOD_SAD,
            MoodAlgorithm.MOOD_CALM,
            MoodAlgorithm.MOOD_ENERGETIC,
            MoodAlgorithm.MOOD_PARTY
        };
        
        if (position >= 0 && position < moods.length) {
            return moods[position];
        }
        
        return null;
    }
    
    public void setMoodCorrectionListener(MoodCorrectionListener listener) {
        this.listener = listener;
    }
    
    /**
     * Show mood statistics
     */
    private void showMoodStatistics() {
        UserFeedbackManager.FeedbackStats stats = feedbackManager.getFeedbackStats();
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Mood Correction Statistics")
               .setMessage(stats.getSummary())
               .setPositiveButton("OK", null)
               .show();
    }
}
