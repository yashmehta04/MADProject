package com.example.madproject.fragments;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.madproject.R;
import com.example.madproject.adapters.SongAdapter;
import com.example.madproject.interfaces.SongSelectionListener;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.UsageTracker;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class HomeFragment extends Fragment implements SongAdapter.OnSongClickListener {

    private RecyclerView rvRecentlyAdded;
    private MaterialButton btnStartMoodQuiz;
    private LinearLayout usageGraphContainer;
    private LinearLayout usageLabelsContainer;
    private TextView tvTodayUsage;
    private CardView cardDailyUsage;
    private CardView cardContinueListening;
    private ImageView ivContinueAlbumArt;
    private TextView tvContinueSongTitle;
    private TextView tvContinueSongArtist;
    private ImageButton btnContinuePlay;
    private TextView tvNoLastSong;
    private ArrayList<SongsList> allSongs;
    private SongSelectionListener songSelectionListener;

    public interface MoodQuizLauncher {
        void onLaunchMoodQuiz();
    }

    private MoodQuizLauncher moodQuizLauncher;

    public HomeFragment() {
    }

    public static HomeFragment newInstance(ArrayList<SongsList> songs) {
        HomeFragment fragment = new HomeFragment();
        fragment.allSongs = songs;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (allSongs == null) allSongs = new ArrayList<>();
        if (getActivity() instanceof SongSelectionListener) {
            songSelectionListener = (SongSelectionListener) getActivity();
        }
        if (getActivity() instanceof MoodQuizLauncher) {
            moodQuizLauncher = (MoodQuizLauncher) getActivity();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        rvRecentlyAdded = view.findViewById(R.id.rv_recently_added_list);
        btnStartMoodQuiz = view.findViewById(R.id.btn_start_mood_quiz);
        usageGraphContainer = view.findViewById(R.id.usage_graph_container);
        usageLabelsContainer = view.findViewById(R.id.usage_labels_container);
        tvTodayUsage = view.findViewById(R.id.tv_today_usage);
        cardDailyUsage = view.findViewById(R.id.card_daily_usage);
        cardContinueListening = view.findViewById(R.id.card_continue_listening);
        ivContinueAlbumArt = view.findViewById(R.id.iv_continue_album_art);
        tvContinueSongTitle = view.findViewById(R.id.tv_continue_song_title);
        tvContinueSongArtist = view.findViewById(R.id.tv_continue_song_artist);
        btnContinuePlay = view.findViewById(R.id.btn_continue_play);
        tvNoLastSong = view.findViewById(R.id.tv_no_last_song);

        if (btnStartMoodQuiz != null) {
            btnStartMoodQuiz.setOnClickListener(v -> {
                if (moodQuizLauncher != null) moodQuizLauncher.onLaunchMoodQuiz();
            });
        }

        if (cardDailyUsage != null) {
            cardDailyUsage.setOnClickListener(v -> showAdvancedUsageDialog());
        }

        updateDashboards(allSongs);
        updateContinueListening();
        renderUsageGraph();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateContinueListening();
        renderUsageGraph();
    }

    /**
     * Shows the last played song in "Continue Listening" section.
     */
    private void updateContinueListening() {
        if (getContext() == null || cardContinueListening == null) return;

        String lastTitle = UsageTracker.getLastSongTitle(requireContext());
        String lastArtist = UsageTracker.getLastSongArtist(requireContext());
        String lastPath = UsageTracker.getLastSongPath(requireContext());
        long lastAlbumId = UsageTracker.getLastSongAlbumId(requireContext());

        if (lastTitle != null && lastPath != null) {
            cardContinueListening.setVisibility(View.VISIBLE);
            tvNoLastSong.setVisibility(View.GONE);
            tvContinueSongTitle.setText(lastTitle);
            tvContinueSongArtist.setText(lastArtist != null ? lastArtist : "Unknown artist");

            // Load album art
            if (lastAlbumId >= 0) {
                try {
                    Uri albumArtUri = ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"), lastAlbumId);
                    Glide.with(this).load(albumArtUri)
                            .placeholder(R.drawable.ic_music_note)
                            .error(R.drawable.ic_music_note)
                            .into(ivContinueAlbumArt);
                } catch (Exception e) {
                    ivContinueAlbumArt.setImageResource(R.drawable.ic_music_note);
                }
            }

            // Play button - find song in allSongs list and play it
            btnContinuePlay.setOnClickListener(v -> {
                if (songSelectionListener != null && allSongs != null) {
                    for (int i = 0; i < allSongs.size(); i++) {
                        if (allSongs.get(i).getPath().equals(lastPath)) {
                            songSelectionListener.onSongSelected(allSongs, i);
                            return;
                        }
                    }
                }
            });

            // Card click also triggers playback
            cardContinueListening.setOnClickListener(v ->
                    btnContinuePlay.performClick());
        } else {
            cardContinueListening.setVisibility(View.GONE);
            tvNoLastSong.setVisibility(View.VISIBLE);
        }
    }

    public void updateDashboards(ArrayList<SongsList> songs) {
        this.allSongs = songs;
        if (rvRecentlyAdded == null || songs == null || songs.isEmpty()) return;

        // Recently Added - sort by dateAdded in descending order (newest first)
        ArrayList<SongsList> recentAdded = new ArrayList<>(songs);
        recentAdded.sort((s1, s2) -> Long.compare(s2.getDateAdded(), s1.getDateAdded()));
        
        // Take top 20 most recently added songs
        if (recentAdded.size() > 20) recentAdded = new ArrayList<>(recentAdded.subList(0, 20));

        SongAdapter adapterRecent = new SongAdapter(getContext(), recentAdded, this);
        rvRecentlyAdded.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvRecentlyAdded.setAdapter(adapterRecent);
    }

    private void renderUsageGraph() {
        if (getContext() == null || usageGraphContainer == null) return;

        int[] usage = UsageTracker.getWeeklyUsage(requireContext());
        String[] labels = UsageTracker.getWeeklyLabels();
        int todayMin = UsageTracker.getTodayUsage(requireContext());

        if (tvTodayUsage != null) {
            tvTodayUsage.setText(todayMin >= 60
                    ? "Today: " + (todayMin / 60) + "h " + (todayMin % 60) + "m"
                    : "Today: " + todayMin + " min");
        }

        int maxUsage = 1;
        for (int u : usage) if (u > maxUsage) maxUsage = u;

        usageGraphContainer.removeAllViews();
        usageLabelsContainer.removeAllViews();

        float density = getResources().getDisplayMetrics().density;
        int maxBarHeight = (int) (120 * density);
        int accentColor = ContextCompat.getColor(requireContext(), R.color.accent);
        int dimColor = ContextCompat.getColor(requireContext(), R.color.text_hint);

        for (int i = 0; i < 7; i++) {
            LinearLayout column = new LinearLayout(requireContext());
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            colParams.setMargins((int)(2 * density), 0, (int)(2 * density), 0);
            column.setLayoutParams(colParams);

            TextView minuteLabel = new TextView(requireContext());
            minuteLabel.setText(usage[i] + "m");
            minuteLabel.setTextSize(9);
            minuteLabel.setTextColor(dimColor);
            minuteLabel.setGravity(Gravity.CENTER);
            column.addView(minuteLabel);

            View bar = new View(requireContext());
            int barHeight = maxUsage > 0 ? (int) ((float) usage[i] / maxUsage * maxBarHeight) : (int)(4 * density);
            barHeight = Math.max((int)(4 * density), barHeight);
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, barHeight);
            barParams.topMargin = (int) (4 * density);
            bar.setLayoutParams(barParams);

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(i == 6 ? accentColor : Color.argb(120, 0, 229, 255));
            gd.setCornerRadii(new float[]{6*density,6*density,6*density,6*density,0,0,0,0});
            bar.setBackground(gd);

            column.addView(bar);
            usageGraphContainer.addView(column);

            TextView dayLabel = new TextView(requireContext());
            dayLabel.setText(labels[i]);
            dayLabel.setTextSize(11);
            dayLabel.setTextColor(i == 6 ? accentColor : dimColor);
            dayLabel.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            dayLabel.setLayoutParams(labelParams);
            usageLabelsContainer.addView(dayLabel);
        }
    }

    /**
     * Shows an advanced usage analytics dialog with swipeable pages, month/year filter.
     */
    private void showAdvancedUsageDialog() {
        if (getContext() == null) return;

        // Create a scrollable dialog with month navigation
        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dp(20), dp(16), dp(20), dp(16));

        // --- Filter row: Year + Month spinners ---
        LinearLayout filterRow = new LinearLayout(requireContext());
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setGravity(Gravity.CENTER_VERTICAL);
        filterRow.setPadding(0, 0, 0, dp(12));

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH);

        // Year spinner
        ArrayList<String> years = new ArrayList<>();
        for (int y = currentYear - 2; y <= currentYear; y++) years.add(String.valueOf(y));
        Spinner yearSpinner = new Spinner(requireContext());
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, years);
        yearSpinner.setAdapter(yearAdapter);
        yearSpinner.setSelection(years.size() - 1); // current year

        // Month spinner
        String[] monthNames = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        Spinner monthSpinner = new Spinner(requireContext());
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, monthNames);
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setSelection(currentMonth);

        TextView yearLabel = new TextView(requireContext());
        yearLabel.setText("Year: ");
        yearLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

        TextView monthLabel = new TextView(requireContext());
        monthLabel.setText("  Month: ");
        monthLabel.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));

        filterRow.addView(yearLabel);
        filterRow.addView(yearSpinner);
        filterRow.addView(monthLabel);
        filterRow.addView(monthSpinner);
        rootLayout.addView(filterRow);

        // --- Chart container (swipeable) ---
        HorizontalScrollView hScroll = new HorizontalScrollView(requireContext());
        hScroll.setHorizontalScrollBarEnabled(true);

        LinearLayout chartContainer = new LinearLayout(requireContext());
        chartContainer.setOrientation(LinearLayout.HORIZONTAL);
        chartContainer.setGravity(Gravity.BOTTOM);
        chartContainer.setMinimumHeight(dp(220));
        hScroll.addView(chartContainer);
        rootLayout.addView(hScroll);

        // Labels container
        HorizontalScrollView labelsScroll = new HorizontalScrollView(requireContext());
        labelsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout labelsContainer = new LinearLayout(requireContext());
        labelsContainer.setOrientation(LinearLayout.HORIZONTAL);
        labelsScroll.addView(labelsContainer);
        rootLayout.addView(labelsScroll);

        // Summary text
        TextView tvSummary = new TextView(requireContext());
        tvSummary.setPadding(0, dp(12), 0, 0);
        tvSummary.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        tvSummary.setTextSize(13);
        rootLayout.addView(tvSummary);

        // Scroll sync
        hScroll.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            labelsScroll.scrollTo(scrollX, 0);
        });

        // Populate chart based on selected month/year
        Runnable updateChart = () -> {
            int selYear = Integer.parseInt(years.get(yearSpinner.getSelectedItemPosition()));
            int selMonth = monthSpinner.getSelectedItemPosition();

            int[] monthUsage = UsageTracker.getMonthlyUsage(requireContext(), selYear, selMonth);
            chartContainer.removeAllViews();
            labelsContainer.removeAllViews();

            int maxVal = 1;
            int totalMin = 0;
            for (int u : monthUsage) {
                if (u > maxVal) maxVal = u;
                totalMin += u;
            }

            float density = getResources().getDisplayMetrics().density;
            int maxH = dp(180);
            int barW = dp(36);
            int accentC = ContextCompat.getColor(requireContext(), R.color.accent);
            int dimC = ContextCompat.getColor(requireContext(), R.color.text_hint);

            for (int d = 0; d < monthUsage.length; d++) {
                LinearLayout col = new LinearLayout(requireContext());
                col.setOrientation(LinearLayout.VERTICAL);
                col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(barW, LinearLayout.LayoutParams.MATCH_PARENT);
                cp.setMargins(dp(2), 0, dp(2), 0);
                col.setLayoutParams(cp);

                // Minute label
                TextView mLabel = new TextView(requireContext());
                mLabel.setText(monthUsage[d] > 0 ? monthUsage[d] + "m" : "");
                mLabel.setTextSize(8);
                mLabel.setTextColor(dimC);
                mLabel.setGravity(Gravity.CENTER);
                col.addView(mLabel);

                // Bar
                View bar = new View(requireContext());
                int bH = maxVal > 0 ? (int)((float)monthUsage[d] / maxVal * maxH) : dp(3);
                bH = Math.max(dp(3), bH);
                LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, bH);
                bp.topMargin = dp(2);
                bar.setLayoutParams(bp);

                boolean isToday = (selYear == currentYear && selMonth == currentMonth
                        && d == now.get(Calendar.DAY_OF_MONTH) - 1);
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setColor(isToday ? accentC : Color.argb(120, 0, 229, 255));
                gd.setCornerRadii(new float[]{4*density,4*density,4*density,4*density,0,0,0,0});
                bar.setBackground(gd);

                col.addView(bar);
                chartContainer.addView(col);

                // Day label
                TextView dLabel = new TextView(requireContext());
                dLabel.setText(String.valueOf(d + 1));
                dLabel.setTextSize(10);
                dLabel.setTextColor(isToday ? accentC : dimC);
                dLabel.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(barW, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(dp(2), dp(4), dp(2), 0);
                dLabel.setLayoutParams(lp);
                labelsContainer.addView(dLabel);
            }

            // Scroll to end (today)
            hScroll.post(() -> hScroll.fullScroll(View.FOCUS_RIGHT));

            // Summary
            String avgStr = monthUsage.length > 0 ? formatTime(totalMin / monthUsage.length) : "0 min";
            tvSummary.setText("Total: " + formatTime(totalMin)
                    + "\nDaily Avg: " + avgStr
                    + "\n" + monthNames[selMonth] + " " + selYear);
        };

        // Wire spinners
        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { updateChart.run(); }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        yearSpinner.setOnItemSelectedListener(spinnerListener);
        monthSpinner.setOnItemSelectedListener(spinnerListener);

        // Initial render
        updateChart.run();

        new AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
                .setTitle("Usage Analytics")
                .setView(rootLayout)
                .setPositiveButton("Close", null)
                .show();
    }

    private String formatTime(int minutes) {
        if (minutes >= 60) return (minutes / 60) + "h " + (minutes % 60) + "m";
        return minutes + " min";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onSongClick(ArrayList<SongsList> songs, int position) {
        if (songSelectionListener != null) {
            songSelectionListener.onSongSelected(songs, position);
        }
    }
}
