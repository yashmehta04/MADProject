package com.example.madproject.fragments;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;
import android.os.Build;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.madproject.R;
import com.example.madproject.database.FavoritesOperations;
import com.example.madproject.database.MoodOperations;
import com.example.madproject.database.PlaylistOperations;
import com.example.madproject.interfaces.SongSelectionListener;
import com.example.madproject.models.Playlist;
import com.example.madproject.models.SongsList;
import com.example.madproject.utils.TimeFormatter;
import com.example.madproject.utils.LyricsLoader;
import com.example.madproject.utils.MoodAlgorithm;
import com.example.madproject.utils.HybridMoodAnalyzer;
import com.example.madproject.utils.GenreMetadataExtractor;
import com.example.madproject.views.WaveformSeekBar;
import com.example.madproject.dialogs.QuickMoodCorrectionDialog;

import java.util.ArrayList;

/**
 * Fragment for the "Now Playing" screen.
 * Displays current song info, album art, and playback controls.
 * Updates seekbar in real-time using Handler + Runnable.
 */
public class CurrentSongFragment extends Fragment {

    // UI components
    private ImageView ivAlbumArt;
    private TextView tvSongTitle;
    private TextView tvArtist;
    private TextView tvCurrentTime;
    private TextView tvTotalTime;
    private WaveformSeekBar seekBar;
    private ImageButton btnPlayPause;
    private ImageButton btnNext;
    private ImageButton btnPrevious;
    private ImageButton btnShuffle;
    private ImageButton btnRepeat;
    private ImageButton btnFavorite;
    private ImageButton btnAddToPlaylist;
    private ImageButton btnMoodCorrection;
    private View containerView;
    private ImageView ivBlurredBackground;
    private TextView tvGenreTag;
    private TextView tvLyrics;
    private View lyricsContainer;
    private View metadataTagsContainer;
    private TextView tvYearTag;
    private TextView tvBitrateTag;
    private TextView tvFormatTag;
    private TextView tvMoodTag;
    private TextView tvGenreInfoTag;

    // State
    private SongSelectionListener songSelectionListener;
    private FavoritesOperations favoritesOps;
    private PlaylistOperations playlistOps;
    private SongsList currentSong;
    private boolean isFavorite = false;
    private boolean isUserSeeking = false;

    // Seekbar update handler
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable seekBarUpdater = new Runnable() {
        @Override
        public void run() {
            if (songSelectionListener != null && !isUserSeeking) {
                int currentPosition = songSelectionListener.getCurrentPosition();
                int duration = songSelectionListener.getDuration();
                if (duration > 0) {
                    seekBar.setMax(duration);
                }
                seekBar.setProgress(currentPosition);
                tvCurrentTime.setText(TimeFormatter.formatTime(currentPosition));
                // Also keep play/pause in sync
                updatePlayPauseButton();
            }
            handler.postDelayed(this, 200);
        }
    };

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(seekBarUpdater);
    }

    public CurrentSongFragment() {
    }

    public static CurrentSongFragment newInstance() {
        return new CurrentSongFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof SongSelectionListener) {
            songSelectionListener = (SongSelectionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement SongSelectionListener");
        }
        favoritesOps = new FavoritesOperations(context);
        playlistOps = new PlaylistOperations(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_current_song, container, false);
        initViews(view);
        setupListeners();
        return view;
    }

    private void initViews(View view) {
        ivAlbumArt = view.findViewById(R.id.iv_current_album_art);
        tvSongTitle = view.findViewById(R.id.tv_current_title);
        tvArtist = view.findViewById(R.id.tv_current_artist);
        tvCurrentTime = view.findViewById(R.id.tv_current_time);
        tvTotalTime = view.findViewById(R.id.tv_total_time);
        seekBar = view.findViewById(R.id.seekbar);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnNext = view.findViewById(R.id.btn_next);
        btnPrevious = view.findViewById(R.id.btn_previous);
        btnShuffle = view.findViewById(R.id.btn_shuffle);
        btnRepeat = view.findViewById(R.id.btn_repeat);
        btnFavorite = view.findViewById(R.id.btn_favorite);
        btnAddToPlaylist = view.findViewById(R.id.btn_add_to_playlist);
        btnMoodCorrection = view.findViewById(R.id.btn_mood_correction);
        containerView = view.findViewById(R.id.now_playing_container);
        ivBlurredBackground = view.findViewById(R.id.iv_blurred_background);
        tvGenreTag = view.findViewById(R.id.tv_genre_tag);
        tvLyrics = view.findViewById(R.id.tv_lyrics);
        lyricsContainer = view.findViewById(R.id.lyrics_container);
        metadataTagsContainer = view.findViewById(R.id.metadata_tags_container);
        tvYearTag = view.findViewById(R.id.tv_year_tag);
        tvBitrateTag = view.findViewById(R.id.tv_bitrate_tag);
        tvFormatTag = view.findViewById(R.id.tv_format_tag);
        tvMoodTag = view.findViewById(R.id.tv_mood_tag);
        tvGenreInfoTag = view.findViewById(R.id.tv_song_genre_tag);
    }

    private void setupListeners() {
        btnPlayPause.setOnClickListener(v -> {
            if (songSelectionListener != null) {
                songSelectionListener.onPlayPauseToggle();
                updatePlayPauseButton();
            }
        });

        btnNext.setOnClickListener(v -> {
            if (songSelectionListener != null) {
                songSelectionListener.onNextSong();
            }
        });

        btnPrevious.setOnClickListener(v -> {
            if (songSelectionListener != null) {
                songSelectionListener.onPreviousSong();
            }
        });

        // Shuffle — mutually exclusive with Repeat
        btnShuffle.setOnClickListener(v -> {
            if (songSelectionListener != null) {
                if (songSelectionListener.isRepeatOn()) {
                    songSelectionListener.onRepeatToggle();
                    updateRepeatButton();
                }
                songSelectionListener.onShuffleToggle();
                updateShuffleButton();
            }
        });

        // Repeat — mutually exclusive with Shuffle
        btnRepeat.setOnClickListener(v -> {
            if (songSelectionListener != null) {
                if (songSelectionListener.isShuffleOn()) {
                    songSelectionListener.onShuffleToggle();
                    updateShuffleButton();
                }
                songSelectionListener.onRepeatToggle();
                updateRepeatButton();
            }
        });

        btnFavorite.setOnClickListener(v -> toggleFavorite());
        btnAddToPlaylist.setOnClickListener(v -> showAddToPlaylistDialog());
        btnMoodCorrection.setOnClickListener(v -> showMoodCorrectionDialog());

        seekBar.setOnSeekBarChangeListener(new WaveformSeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(WaveformSeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvCurrentTime.setText(TimeFormatter.formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(WaveformSeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(WaveformSeekBar seekBar) {
                isUserSeeking = false;
                if (songSelectionListener != null) {
                    songSelectionListener.onSeekTo(seekBar.getProgress());
                }
            }
        });
    }

    /**
     * Updates the UI with the currently playing song.
     */
    public void updateCurrentSong(SongsList song) {
        this.currentSong = song;
        if (song == null || getView() == null) return;

        tvSongTitle.setText(song.getTitle());
        tvArtist.setText(song.getArtist());
        tvTotalTime.setText(TimeFormatter.formatTime(song.getDuration()));

        // Load album art with Glide and apply Palette
        try {
            Uri albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), song.getAlbumId());

            Glide.with(this)
                    .asBitmap()
                    .load(albumArtUri)
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource,
                                @Nullable Transition<? super Bitmap> transition) {
                            ivAlbumArt.setImageBitmap(resource);

                            if (ivBlurredBackground != null) {
                                ivBlurredBackground.setImageBitmap(resource);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    ivBlurredBackground.setRenderEffect(
                                            RenderEffect.createBlurEffect(100f, 100f, Shader.TileMode.MIRROR));
                                }
                            }

                            Palette.from(resource).generate(palette -> {
                                if (palette != null && getActivity() != null) {
                                    int defaultColor = ContextCompat.getColor(requireContext(), R.color.background);
                                    int mutedColor = palette.getDarkMutedColor(defaultColor);
                                    if (containerView != null) {
                                        int semiTransparentColor = (mutedColor & 0x00FFFFFF) | 0x88000000;
                                        containerView.setBackgroundColor(semiTransparentColor);
                                    }
                                }
                            });
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }
                    });
        } catch (Exception e) {
            ivAlbumArt.setImageResource(R.drawable.ic_music_note);
        }

        // Fetch genre + metadata + Lyrics on a background thread
        new Thread(() -> {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(requireContext(), Uri.parse(song.getPath()));

                String genreStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
                String yearStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
                String bitrateStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
                String formatStr = song.getPath().substring(song.getPath().lastIndexOf('.') + 1).toUpperCase();

                // Load lyrics
                String lyrics = LyricsLoader.loadLyrics(song.getPath());

                requireActivity().runOnUiThread(() -> {
                    // Genre tag display
                    if (tvGenreTag != null) {
                        if (genreStr != null && !genreStr.isEmpty()) {
                            tvGenreTag.setText(genreStr);
                            tvGenreTag.setVisibility(View.VISIBLE);
                        } else {
                            tvGenreTag.setVisibility(View.GONE);
                        }
                    }
                    
                    // Metadata tags display
                    if (metadataTagsContainer != null) {
                        boolean hasMetadata = false;
                        
                        // Year tag
                        if (tvYearTag != null && yearStr != null && !yearStr.isEmpty()) {
                            tvYearTag.setText(yearStr);
                            tvYearTag.setVisibility(View.VISIBLE);
                            hasMetadata = true;
                        } else if (tvYearTag != null) {
                            tvYearTag.setVisibility(View.GONE);
                        }
                        
                        // Bitrate tag
                        if (tvBitrateTag != null && bitrateStr != null && !bitrateStr.isEmpty()) {
                            String bitrateDisplay = bitrateStr.equals("0") ? "Unknown" : (bitrateStr + "kbps");
                            tvBitrateTag.setText(bitrateDisplay);
                            tvBitrateTag.setVisibility(View.VISIBLE);
                            hasMetadata = true;
                        } else if (tvBitrateTag != null) {
                            tvBitrateTag.setVisibility(View.GONE);
                        }
                        
                        // Format tag
                        if (tvFormatTag != null && formatStr != null && !formatStr.isEmpty()) {
                            tvFormatTag.setText(formatStr);
                            tvFormatTag.setVisibility(View.VISIBLE);
                            hasMetadata = true;
                        } else if (tvFormatTag != null) {
                            tvFormatTag.setVisibility(View.GONE);
                        }
                        
                        // Show container if we have any metadata
                        metadataTagsContainer.setVisibility(hasMetadata ? View.VISIBLE : View.GONE);
                    }
                    
                    // Mood tag display
                    if (tvMoodTag != null) {
                        // Check if already analyzed with hybrid system
                        HybridMoodAnalyzer hybridAnalyzer = HybridMoodAnalyzer.getInstance(requireContext());
                        
                        if (hybridAnalyzer.isAnalyzed(song.getPath())) {
                            // Get cached detailed mood info
                            MoodOperations moodOps = new MoodOperations(requireContext());
                            MoodOperations.DetailedMoodInfo moodInfo = moodOps.getDetailedMoodInfo(song.getPath());
                            
                            if (moodInfo != null) {
                                String moodDisplay = MoodAlgorithm.getMoodDisplayName(moodInfo.mood);
                                tvMoodTag.setText(moodDisplay);
                                tvMoodTag.setVisibility(View.VISIBLE);
                                
                                Log.d("CurrentSong", "Using cached mood: " + moodInfo.mood + 
                                      " (confidence: " + moodInfo.confidence + ")");
                            } else {
                                tvMoodTag.setVisibility(View.GONE);
                            }
                        } else {
                            // Analyze with hybrid system in background
                            tvMoodTag.setText("Analyzing...");
                            tvMoodTag.setVisibility(View.VISIBLE);
                            
                            hybridAnalyzer.analyzeSongAsync(song.getPath(), 
                                new HybridMoodAnalyzer.MoodAnalysisCallback() {
                                    @Override
                                    public void onAnalysisComplete(HybridMoodAnalyzer.MoodResult result) {
                                        requireActivity().runOnUiThread(() -> {
                                            String moodDisplay = MoodAlgorithm.getMoodDisplayName(result.mood);
                                            tvMoodTag.setText(moodDisplay);
                                            tvMoodTag.setVisibility(View.VISIBLE);
                                            
                                            Log.d("CurrentSong", "Hybrid analysis complete: " + result);
                                        });
                                    }
                                    
                                    @Override
                                    public void onAnalysisError(Exception error) {
                                        requireActivity().runOnUiThread(() -> {
                                            // Fallback to legacy system
                                            MoodOperations moodOps = new MoodOperations(requireContext());
                                            String mood = moodOps.getMoodTag(song.getPath());
                                            
                                            if (mood != null && !mood.isEmpty()) {
                                                String moodDisplay = MoodAlgorithm.getMoodDisplayName(mood);
                                                tvMoodTag.setText(moodDisplay);
                                                tvMoodTag.setVisibility(View.VISIBLE);
                                            } else {
                                                tvMoodTag.setVisibility(View.GONE);
                                            }
                                            
                                            Log.e("CurrentSong", "Hybrid analysis failed, using fallback", error);
                                        });
                                    }
                                });
                        }
                    }
                    
                    // Genre tag display
                    if (tvGenreInfoTag != null) {
                        String genre = GenreMetadataExtractor.extractGenre(song);
                        if (genre != null && !genre.equals("Unknown")) {
                            tvGenreInfoTag.setText("🎵 " + genre);
                            tvGenreInfoTag.setVisibility(View.VISIBLE);
                        } else {
                            tvGenreInfoTag.setVisibility(View.GONE);
                        }
                    }
                    
                    // Lyrics display
                    if (lyricsContainer != null) {
                        if (lyrics != null && !lyrics.isEmpty()) {
                            tvLyrics.setText(lyrics);
                            lyricsContainer.setVisibility(View.VISIBLE);
                        } else {
                            lyricsContainer.setVisibility(View.GONE);
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("CurrentSong", "Error extracting metadata", e);
            } finally {
                try {
                    mmr.release();
                } catch (Exception ignored) {
                }
            }
        }).start();

        // Update seekbar
        if (songSelectionListener != null) {
            int duration = songSelectionListener.getDuration();
            if (duration > 0) {
                seekBar.setMax(duration);
            } else {
                // Fallback to song's stored duration
                seekBar.setMax((int) song.getDuration());
            }
        }

        // Update states
        isFavorite = favoritesOps.isFavorite(song.getPath());
        updateFavoriteButton();
        updatePlayPauseButton();
        updateShuffleButton();
        updateRepeatButton();

        handler.removeCallbacks(seekBarUpdater);
        handler.post(seekBarUpdater);
    }

    /**
     * Updates the play/pause icon to reflect current playback state.
     * PLAYING → show PAUSE icon. PAUSED → show PLAY icon.
     */
    public void updatePlayPauseButton() {
        if (songSelectionListener != null && btnPlayPause != null) {
            if (songSelectionListener.isPlaying()) {
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            } else {
                btnPlayPause.setImageResource(R.drawable.ic_play);
            }
        }
    }

    private void updateShuffleButton() {
        if (songSelectionListener != null && btnShuffle != null) {
            if (songSelectionListener.isShuffleOn()) {
                btnShuffle.setAlpha(1.0f);
                btnShuffle.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent));
            } else {
                btnShuffle.setAlpha(0.5f);
                btnShuffle.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
        }
    }

    private void updateRepeatButton() {
        if (songSelectionListener != null && btnRepeat != null) {
            if (songSelectionListener.isRepeatOn()) {
                btnRepeat.setAlpha(1.0f);
                btnRepeat.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent));
            } else {
                btnRepeat.setAlpha(0.5f);
                btnRepeat.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
        }
    }

    private void toggleFavorite() {
        if (currentSong == null) return;

        if (isFavorite) {
            favoritesOps.removeFavorite(currentSong.getPath());
            isFavorite = false;
        } else {
            favoritesOps.addFavorite(currentSong);
            isFavorite = true;
        }
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        if (btnFavorite != null) {
            if (isFavorite) {
                btnFavorite.setImageResource(R.drawable.ic_favorite_filled);
                btnFavorite.setColorFilter(ContextCompat.getColor(requireContext(), R.color.accent));
            } else {
                btnFavorite.setImageResource(R.drawable.ic_favorite_border);
                btnFavorite.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
            }
        }
    }

    private void showAddToPlaylistDialog() {
        if (currentSong == null) {
            Toast.makeText(getContext(), "No song playing", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Playlist> playlists = playlistOps.getAllPlaylists();
        if (playlists.isEmpty()) {
            Toast.makeText(getContext(), "No playlists. Create one first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] playlistNames = new String[playlists.size()];
        for (int i = 0; i < playlists.size(); i++) {
            playlistNames[i] = playlists.get(i).getName();
        }

        new AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
                .setTitle("Add to Playlist")
                .setItems(playlistNames, (dialog, which) -> {
                    Playlist selected = playlists.get(which);
                    boolean added = playlistOps.addSongToPlaylist(selected.getId(), currentSong);
                    if (added) {
                        Toast.makeText(getContext(), "Added to \"" + selected.getName() + "\"",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Already in \"" + selected.getName() + "\"",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showMoodCorrectionDialog() {
        if (currentSong == null) {
            Toast.makeText(getContext(), "No song playing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get current mood from database
        MoodOperations moodOps = new MoodOperations(requireContext());
        String currentMood = moodOps.getMoodTag(currentSong.getPath());
        
        if (currentMood == null) {
            Toast.makeText(getContext(), "No mood tag available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show quick mood correction dialog
        QuickMoodCorrectionDialog dialog = QuickMoodCorrectionDialog.newInstance(currentSong, currentMood);
        dialog.setQuickMoodCorrectionListener((songPath, correctedMood) -> {
            // Update the mood tag display immediately
            updateMoodTagDisplay(correctedMood);
            Toast.makeText(getContext(), "Mood updated successfully!", Toast.LENGTH_SHORT).show();
        });
        dialog.show(getParentFragmentManager(), "quick_mood_correction");
    }
    
    private void updateMoodTagDisplay(String mood) {
        if (tvMoodTag != null) {
            String moodDisplay = MoodAlgorithm.getMoodDisplayName(mood);
            tvMoodTag.setText(moodDisplay);
            tvMoodTag.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (currentSong != null) {
            handler.post(seekBarUpdater);
            updatePlayPauseButton();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(seekBarUpdater);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up Handler callbacks to prevent memory leaks
        if (handler != null) {
            handler.removeCallbacks(seekBarUpdater);
        }
        songSelectionListener = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (handler != null) {
            handler.removeCallbacks(seekBarUpdater);
        }
        songSelectionListener = null;
    }
}
