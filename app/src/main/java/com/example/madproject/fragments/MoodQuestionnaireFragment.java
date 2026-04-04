package com.example.madproject.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.madproject.R;
import com.example.madproject.database.MoodOperations;
import com.example.madproject.interfaces.SongSelectionListener;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.MoodAlgorithm;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

/**
 * Interactive mood questionnaire fragment.
 * Presents 5 multiple-choice questions and determines user's current mood.
 * After completion, queries the mood database and plays matching songs.
 */
public class MoodQuestionnaireFragment extends Fragment {

    // Question data structure: [question, optionA, optionB, optionC, optionD]
    // Each option maps to a mood: A=HAPPY, B=SAD, C=CALM, D=ENERGETIC
    private static final String[][] QUESTIONS = {
            {
                    "How are you feeling right now?",
                    "😊 Great & cheerful",
                    "😢 A bit down",
                    "😌 Relaxed & peaceful",
                    "🔥 Pumped & excited"
            },
            {
                    "What kind of music would you like?",
                    "🎉 Something fun & upbeat",
                    "💭 Something emotional & deep",
                    "🌿 Something soothing & mellow",
                    "⚡ Something intense & powerful"
            },
            {
                    "How would you describe your energy level?",
                    "☀️ Bright and positive",
                    "🌧️ Low and reflective",
                    "🌙 Quiet and still",
                    "🌋 High and unstoppable"
            },
            {
                    "Pick the vibe that matches your day:",
                    "🎈 A celebration or hangout",
                    "📖 Sitting alone with thoughts",
                    "🧘 Meditation or a quiet walk",
                    "🏋️ A workout or adventure"
            },
            {
                    "What would improve your mood right now?",
                    "💃 Dancing to a catchy tune",
                    "🎻 Listening to a soulful melody",
                    "🎹 Gentle piano or lo-fi beats",
                    "🎸 Headbanging to heavy riffs"
            }
    };

    // Mood mapping per option: index 0=A, 1=B, 2=C, 3=D
    private static final String[] OPTION_MOODS = {
            MoodAlgorithm.MOOD_HAPPY,
            MoodAlgorithm.MOOD_SAD,
            MoodAlgorithm.MOOD_CALM,
            MoodAlgorithm.MOOD_ENERGETIC
    };

    private int currentQuestion = 0;
    private int happyCount = 0;
    private int sadCount = 0;
    private int calmCount = 0;
    private int energeticCount = 0;
    
    // Track selected answers for each question
    private Integer[] selectedAnswers = new Integer[QUESTIONS.length];

    // Views
    private TextView tvQuestionNumber;
    private TextView tvQuestionText;
    private MaterialButton btnOptionA, btnOptionB, btnOptionC, btnOptionD;
    private CardView cardQuestion, cardResult;
    private TextView tvResultEmoji, tvResultMood, tvResultCount;
    private MaterialButton btnPlayMood, btnRetake, btnGoDashboardQuestion, btnGoDashboardResult;
    private View[] progressDots;
    private View btnBackContainer;

    // Data
    private ArrayList<SongsList> allSongs;
    private SongSelectionListener songSelectionListener;
    private String detectedMood;

    public MoodQuestionnaireFragment() {
    }

    public static MoodQuestionnaireFragment newInstance(ArrayList<SongsList> songs) {
        MoodQuestionnaireFragment fragment = new MoodQuestionnaireFragment();
        fragment.allSongs = songs;
        return fragment;
    }

    public void setAllSongs(ArrayList<SongsList> songs) {
        this.allSongs = songs;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (allSongs == null) allSongs = new ArrayList<>();
        if (getActivity() instanceof SongSelectionListener) {
            songSelectionListener = (SongSelectionListener) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mood_questionnaire, container, false);
        initViews(view);
        setupListeners();
        displayQuestion(0);
        return view;
    }

    private void initViews(View view) {
        tvQuestionNumber = view.findViewById(R.id.tv_question_number);
        tvQuestionText = view.findViewById(R.id.tv_question_text);
        btnOptionA = view.findViewById(R.id.btn_option_a);
        btnOptionB = view.findViewById(R.id.btn_option_b);
        btnOptionC = view.findViewById(R.id.btn_option_c);
        btnOptionD = view.findViewById(R.id.btn_option_d);
        cardQuestion = view.findViewById(R.id.card_question);
        cardResult = view.findViewById(R.id.card_result);
        tvResultEmoji = view.findViewById(R.id.tv_result_emoji);
        tvResultMood = view.findViewById(R.id.tv_result_mood);
        tvResultCount = view.findViewById(R.id.tv_result_count);
        btnPlayMood = view.findViewById(R.id.btn_play_mood);
        btnRetake = view.findViewById(R.id.btn_retake);
        btnGoDashboardQuestion = view.findViewById(R.id.btn_go_dashboard_question);
        btnGoDashboardResult = view.findViewById(R.id.btn_go_dashboard_result);
        btnBackContainer = view.findViewById(R.id.btn_back_container);

        progressDots = new View[]{
                view.findViewById(R.id.progress_dot_1),
                view.findViewById(R.id.progress_dot_2),
                view.findViewById(R.id.progress_dot_3),
                view.findViewById(R.id.progress_dot_4),
                view.findViewById(R.id.progress_dot_5)
        };
    }

    private void setupListeners() {
        btnOptionA.setOnClickListener(v -> onOptionSelected(0));
        btnOptionB.setOnClickListener(v -> onOptionSelected(1));
        btnOptionC.setOnClickListener(v -> onOptionSelected(2));
        btnOptionD.setOnClickListener(v -> onOptionSelected(3));

        btnPlayMood.setOnClickListener(v -> playMoodPlaylist());
        btnRetake.setOnClickListener(v -> resetQuiz());

        View.OnClickListener dashboardListener = v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        };

        if (btnGoDashboardQuestion != null) {
            btnGoDashboardQuestion.setOnClickListener(dashboardListener);
        }
        if (btnGoDashboardResult != null) {
            btnGoDashboardResult.setOnClickListener(dashboardListener);
        }
        
        // Back button listener - go to dashboard for question 1, previous for others
        if (btnBackContainer != null) {
            btnBackContainer.setOnClickListener(v -> {
                if (currentQuestion > 0) {
                    // Remove current question's mood vote before going back
                    Integer previousAnswer = selectedAnswers[currentQuestion];
                    if (previousAnswer != null) {
                        // Decrement the corresponding mood count
                        switch (previousAnswer) {
                            case 0: // HAPPY
                                happyCount--;
                                break;
                            case 1: // SAD
                                sadCount--;
                                break;
                            case 2: // CALM
                                calmCount--;
                                break;
                            case 3: // ENERGETIC
                                energeticCount--;
                                break;
                        }
                        // Clear the stored answer
                        selectedAnswers[currentQuestion] = null;
                    }
                    
                    // Go to previous question
                    currentQuestion--;
                    displayQuestion(currentQuestion);
                } else {
                    // On first question, go to dashboard
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            });
        }
    }

    /**
     * Displays the question at the given index.
     */
    private void displayQuestion(int index) {
        if (index >= QUESTIONS.length) return;

        tvQuestionNumber.setText("Question " + (index + 1) + " of " + QUESTIONS.length);
        tvQuestionText.setText(QUESTIONS[index][0]);
        btnOptionA.setText(QUESTIONS[index][1]);
        btnOptionB.setText(QUESTIONS[index][2]);
        btnOptionC.setText(QUESTIONS[index][3]);
        btnOptionD.setText(QUESTIONS[index][4]);

        // Reset button styles
        resetButtonStyles();

        // Update progress dots
        updateProgressDots(index);

        // Always show back button (Requirement 2)
        if (btnBackContainer != null) {
            btnBackContainer.setVisibility(View.VISIBLE);
        }

        // Animate card entrance
        animateCardIn(cardQuestion);
    }

    /**
     * Handles option selection. Records the mood vote and advances to the next question.
     */
    private void onOptionSelected(int optionIndex) {
        // Visual feedback — highlight the selected button
        MaterialButton selectedBtn = getButtonByIndex(optionIndex);
        if (selectedBtn != null) {
            selectedBtn.setStrokeColorResource(R.color.accent);
            selectedBtn.setStrokeWidth(3);
        }

        // If user is re-selecting on the same question, remove previous vote
        Integer previousAnswer = selectedAnswers[currentQuestion];
        if (previousAnswer != null) {
            // Decrement the previous mood count
            switch (previousAnswer) {
                case 0: // HAPPY
                    happyCount--;
                    break;
                case 1: // SAD
                    sadCount--;
                    break;
                case 2: // CALM
                    calmCount--;
                    break;
                case 3: // ENERGETIC
                    energeticCount--;
                    break;
            }
        }

        // Store the new answer
        selectedAnswers[currentQuestion] = optionIndex;

        // Record the mood vote
        String mood = OPTION_MOODS[optionIndex];
        if (MoodAlgorithm.MOOD_HAPPY.equals(mood)) {
            happyCount++;
        } else if (MoodAlgorithm.MOOD_SAD.equals(mood)) {
            sadCount++;
        } else if (MoodAlgorithm.MOOD_CALM.equals(mood)) {
            calmCount++;
        } else if (MoodAlgorithm.MOOD_ENERGETIC.equals(mood)) {
            energeticCount++;
        }

        currentQuestion++;

        // Small delay for visual feedback before moving on
        cardQuestion.postDelayed(() -> {
            if (currentQuestion < QUESTIONS.length) {
                displayQuestion(currentQuestion);
            } else {
                showResult();
            }
        }, 400);
    }

    /**
     * Determines the final mood using majority logic and shows the result card.
     */
    private void showResult() {
        // Determine winning mood via majority
        int maxVotes = Math.max(Math.max(happyCount, sadCount),
                Math.max(calmCount, energeticCount));

        if (happyCount == maxVotes) {
            detectedMood = MoodAlgorithm.MOOD_HAPPY;
        } else if (energeticCount == maxVotes) {
            detectedMood = MoodAlgorithm.MOOD_ENERGETIC;
        } else if (sadCount == maxVotes) {
            detectedMood = MoodAlgorithm.MOOD_SAD;
        } else {
            detectedMood = MoodAlgorithm.MOOD_CALM;
        }

        // Query matching songs
        MoodOperations moodOps = new MoodOperations(requireContext());
        ArrayList<SongsList> moodSongs = moodOps.getSongsByMood(allSongs, detectedMood, 20);

        // Set result UI
        String displayName = MoodAlgorithm.getMoodDisplayName(detectedMood);
        String emoji;
        switch (detectedMood) {
            case MoodAlgorithm.MOOD_HAPPY:
                emoji = "😊";
                break;
            case MoodAlgorithm.MOOD_SAD:
                emoji = "😢";
                break;
            case MoodAlgorithm.MOOD_CALM:
                emoji = "😌";
                break;
            case MoodAlgorithm.MOOD_ENERGETIC:
                emoji = "🔥";
                break;
            default:
                emoji = "🎵";
        }

        tvResultEmoji.setText(emoji);
        tvResultMood.setText("Your Mood: " + displayName); 
        tvResultMood.setTextColor(MoodAlgorithm.getMoodColor(detectedMood));

        if (moodSongs.isEmpty()) {
            tvResultCount.setText("No songs matched, but we'll play your full library shuffled!");
            btnPlayMood.setText("🎵  Shuffle All Songs");
        } else {
            tvResultCount.setText("We found " + moodSongs.size() + " songs for you!");
            btnPlayMood.setText("🎵  Play My Mood Playlist");
        }

        // Transition: hide question, show result
        cardQuestion.setVisibility(View.GONE);
        cardResult.setVisibility(View.VISIBLE);
        animateCardIn(cardResult);

        // Hide header progress dots
        View progressContainer = getView() != null ? getView().findViewById(R.id.progress_container) : null;
        if (progressContainer != null) {
            progressContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Plays the mood playlist by querying SQLite and passing results to the player.
     */
    private void playMoodPlaylist() {
        if (detectedMood == null || allSongs == null || allSongs.isEmpty()) {
            Toast.makeText(getContext(), "No songs available", Toast.LENGTH_SHORT).show();
            return;
        }

        MoodOperations moodOps = new MoodOperations(requireContext());
        ArrayList<SongsList> moodSongs = moodOps.getSongsByMood(allSongs, detectedMood, 20);

        if (moodSongs.isEmpty()) {
            // Fallback: shuffle all songs
            moodSongs = new ArrayList<>(allSongs);
            java.util.Collections.shuffle(moodSongs);
            if (moodSongs.size() > 20) {
                moodSongs = new ArrayList<>(moodSongs.subList(0, 20));
            }
        }

        if (songSelectionListener != null && !moodSongs.isEmpty()) {
            songSelectionListener.onSongSelected(moodSongs, 0);
            Toast.makeText(getContext(), "Playing " + MoodAlgorithm.getMoodDisplayName(detectedMood)
                    + " playlist!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Resets the quiz to start over.
     */
    private void resetQuiz() {
        currentQuestion = 0;
        happyCount = 0;
        sadCount = 0;
        calmCount = 0;
        energeticCount = 0;
        detectedMood = null;

        cardQuestion.setVisibility(View.VISIBLE);
        cardResult.setVisibility(View.GONE);

        View progressContainer = getView() != null ? getView().findViewById(R.id.progress_container) : null;
        if (progressContainer != null) {
            progressContainer.setVisibility(View.VISIBLE);
        }

        displayQuestion(0);
    }

    // ======================== UI Helpers ========================

    private MaterialButton getButtonByIndex(int index) {
        switch (index) {
            case 0:
                return btnOptionA;
            case 1:
                return btnOptionB;
            case 2:
                return btnOptionC;
            case 3:
                return btnOptionD;
            default:
                return null;
        }
    }

    private void resetButtonStyles() {
        MaterialButton[] buttons = {btnOptionA, btnOptionB, btnOptionC, btnOptionD};
        for (MaterialButton btn : buttons) {
            btn.setStrokeColorResource(R.color.divider);
            btn.setStrokeWidth(1);
        }
    }

    private void updateProgressDots(int activeIndex) {
        for (int i = 0; i < progressDots.length; i++) {
            if (i <= activeIndex) {
                progressDots[i].setBackgroundResource(R.drawable.bg_mood_dot_active);
            } else {
                progressDots[i].setBackgroundResource(R.drawable.bg_mood_dot_inactive);
            }
        }
    }

    private void animateCardIn(View card) {
        card.setAlpha(0f);
        card.setTranslationY(40f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(card, "alpha", 0f, 1f);
        ObjectAnimator slideUp = ObjectAnimator.ofFloat(card, "translationY", 40f, 0f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(fadeIn, slideUp);
        set.setDuration(350);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.start();
    }
}
