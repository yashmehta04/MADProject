package com.example.madproject.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.adapters.PlaylistAdapter;
import com.example.madproject.database.PlaylistOperations;
import com.example.madproject.interfaces.PlaylistActionListener;
import com.example.madproject.models.Playlist;
import com.example.madproject.models.SongsList;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Fragment for managing playlists.
 * On click: shows song picker to add new songs, then shows existing songs with delete option.
 */
public class PlaylistFragment extends Fragment implements PlaylistAdapter.OnPlaylistClickListener {

    private RecyclerView recyclerView;
    private PlaylistAdapter playlistAdapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAddPlaylist;
    private ArrayList<Playlist> playlists;
    private PlaylistOperations playlistOps;
    private PlaylistActionListener playlistActionListener;

    public PlaylistFragment() {}

    public static PlaylistFragment newInstance() {
        return new PlaylistFragment();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof PlaylistActionListener) {
            playlistActionListener = (PlaylistActionListener) context;
        } else {
            throw new RuntimeException(context + " must implement PlaylistActionListener");
        }
        playlistOps = new PlaylistOperations(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);
        recyclerView = view.findViewById(R.id.rv_playlists);
        tvEmpty = view.findViewById(R.id.tv_empty_playlists);
        fabAddPlaylist = view.findViewById(R.id.fab_add_playlist);
        setupRecyclerView();
        setupFab();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
    }

    private void setupRecyclerView() {
        playlists = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        playlistAdapter = new PlaylistAdapter(getContext(), playlists, this);
        recyclerView.setAdapter(playlistAdapter);
    }

    private void setupFab() {
        fabAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
    }

    public void loadPlaylists() {
        if (playlistOps != null) {
            playlists = playlistOps.getAllPlaylists();
            if (playlistAdapter != null) playlistAdapter.updateData(playlists);
            updateEmptyState();
        }
    }

    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_create_playlist, null);
        EditText etName = dialogView.findViewById(R.id.et_playlist_name);

        new AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
                .setTitle("Create Playlist")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (!name.isEmpty()) {
                        long id = playlistOps.createPlaylist(name);
                        if (id != -1) {
                            loadPlaylists();
                            Toast.makeText(getContext(), "Playlist created!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Playlist already exists", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateEmptyState() {
        if (playlists == null || playlists.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        // ALWAYS show song picker first, then existing songs
        showPlaylistManager(playlist);
    }

    /**
     * Shows a two-part dialog:
     * 1st click -> Add songs dialog (with search + multi-select)
     * Cancel / after adding -> Show existing songs (with checkboxes to delete)
     */
    private void showPlaylistManager(Playlist playlist) {
        // Fetch all device songs
        ArrayList<SongsList> allSongs = null;
        if (playlistActionListener != null) {
            allSongs = playlistActionListener.getAllDeviceSongs();
        }
        if (allSongs == null || allSongs.isEmpty()) {
            Toast.makeText(getContext(), "No songs on device", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build existing song paths set for quick lookup
        ArrayList<SongsList> existingSongs = playlist.getSongs();
        if (existingSongs == null) existingSongs = new ArrayList<>();
        Set<String> existingPaths = new HashSet<>();
        for (SongsList s : existingSongs) existingPaths.add(s.getPath());

        // Create "Add Songs" dialog
        showAddSongsDialog(playlist, allSongs, existingPaths);
    }

    /**
     * Dialog with search bar + multi-select list for adding songs.
     */
    private void showAddSongsDialog(Playlist playlist, ArrayList<SongsList> allSongs, Set<String> existingPaths) {
        // Root layout
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(8), dp(16), dp(8));

        // Search field
        EditText etSearch = new EditText(requireContext());
        etSearch.setHint("Search songs...");
        etSearch.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        etSearch.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.text_hint));
        etSearch.setSingleLine(true);
        etSearch.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(etSearch);

        // Songs container in a scroll view
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(350)));
        LinearLayout songListLayout = new LinearLayout(requireContext());
        songListLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(songListLayout);
        root.addView(scrollView);

        // Build checkboxes for each song
        ArrayList<CheckBox> checkBoxes = new ArrayList<>();
        ArrayList<SongsList> songRefs = new ArrayList<>();

        for (SongsList song : allSongs) {
            if (existingPaths.contains(song.getPath())) continue; // Skip already in playlist

            CheckBox cb = new CheckBox(requireContext());
            String label = song.getTitle() + " — " + song.getArtist();
            cb.setText(label);
            cb.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            cb.setTag(label.toLowerCase()); // For search filtering
            cb.setPadding(dp(4), dp(6), dp(4), dp(6));
            songListLayout.addView(cb);
            checkBoxes.add(cb);
            songRefs.add(song);
        }

        if (checkBoxes.isEmpty()) {
            // No new songs to add — go directly to existing songs view
            showExistingSongsDialog(playlist);
            return;
        }

        // Search filter
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                for (CheckBox cb : checkBoxes) {
                    String tag = (String) cb.getTag();
                    cb.setVisibility(tag.contains(query) ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        new AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
                .setTitle("Add songs to \"" + playlist.getName() + "\"")
                .setView(root)
                .setPositiveButton("Add Selected", (dialog, which) -> {
                    Log.d("PlaylistFragment", "Starting to add songs to playlist: " + playlist.getName());
                    int added = 0;
                    int attempted = 0;
                    for (int i = 0; i < checkBoxes.size(); i++) {
                        if (checkBoxes.get(i).isChecked()) {
                            attempted++;
                            SongsList song = songRefs.get(i);
                            Log.d("PlaylistFragment", "Attempting to add song: " + song.getTitle() + " (ID: " + playlist.getId() + ")");
                            if (playlistOps.addSongToPlaylist(playlist.getId(), song)) {
                                added++;
                                Log.d("PlaylistFragment", "Successfully added song: " + song.getTitle());
                            } else {
                                Log.w("PlaylistFragment", "Failed to add song: " + song.getTitle());
                            }
                        }
                    }
                    Log.d("PlaylistFragment", "Total songs attempted to add: " + attempted + ", successfully added: " + added);
                    if (added > 0) {
                        Toast.makeText(getContext(), "Added " + added + " songs!", Toast.LENGTH_SHORT).show();
                        loadPlaylists();
                    } else {
                        Toast.makeText(getContext(), "No songs were added or they already exist", Toast.LENGTH_SHORT).show();
                    }
                    // Now show existing songs
                    showExistingSongsDialog(playlist);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Show existing songs with delete capability
                    showExistingSongsDialog(playlist);
                })
                .show();
    }

    /**
     * Shows existing songs in a playlist with checkboxes to delete + "Add More" button.
     */
    private void showExistingSongsDialog(Playlist playlist) {
        // Refresh playlist data
        ArrayList<SongsList> existingSongs = playlistOps.getPlaylistSongsById(playlist.getId());
        if (existingSongs == null || existingSongs.isEmpty()) {
            Toast.makeText(getContext(), "Playlist is empty. Add songs!", Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(8), dp(16), dp(8));

        // Header
        TextView header = new TextView(requireContext());
        header.setText(existingSongs.size() + " songs in \"" + playlist.getName() + "\"");
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
        header.setTextSize(14);
        header.setPadding(0, 0, 0, dp(8));
        root.addView(header);

        // Song list with checkboxes (checked = will be deleted)
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(350)));
        LinearLayout songListLayout = new LinearLayout(requireContext());
        songListLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(songListLayout);
        root.addView(scrollView);

        ArrayList<CheckBox> deleteCbs = new ArrayList<>();
        for (SongsList song : existingSongs) {
            CheckBox cb = new CheckBox(requireContext());
            cb.setText(song.getTitle());
            cb.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            cb.setPadding(dp(4), dp(6), dp(4), dp(6));
            songListLayout.addView(cb);
            deleteCbs.add(cb);
        }

        new AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
                .setTitle("Manage \"" + playlist.getName() + "\"")
                .setView(root)
                .setPositiveButton("Add More", (dialog, which) -> {
                    // Delete checked items first
                    deleteCheckedSongs(playlist, existingSongs, deleteCbs);
                    // Then reopen song picker
                    loadPlaylists();
                    Playlist refreshed = null;
                    for (Playlist p : playlists) {
                        if (p.getId() == playlist.getId()) { refreshed = p; break; }
                    }
                    if (refreshed != null) showPlaylistManager(refreshed);
                })
                .setNeutralButton("Delete Selected", (dialog, which) -> {
                    int deleted = deleteCheckedSongs(playlist, existingSongs, deleteCbs);
                    if (deleted > 0) {
                        Toast.makeText(getContext(), "Removed " + deleted + " songs", Toast.LENGTH_SHORT).show();
                        loadPlaylists();
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private int deleteCheckedSongs(Playlist playlist, ArrayList<SongsList> songs, ArrayList<CheckBox> cbs) {
        int deleted = 0;
        for (int i = 0; i < cbs.size(); i++) {
            if (cbs.get(i).isChecked()) {
                playlistOps.removeSongFromPlaylist(playlist.getId(), songs.get(i).getPath());
                deleted++;
            }
        }
        return deleted;
    }

    @Override
    public void onPlaylistDelete(Playlist playlist, int position) {
        new AlertDialog.Builder(requireContext(), R.style.DarkDialogTheme)
                .setTitle("Delete Playlist")
                .setMessage("Delete \"" + playlist.getName() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    playlistOps.deletePlaylist(playlist.getId());
                    loadPlaylists();
                    if (playlistActionListener != null) playlistActionListener.onPlaylistDeleted(playlist.getId());
                    Toast.makeText(getContext(), "Playlist deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        playlistActionListener = null;
    }
}
