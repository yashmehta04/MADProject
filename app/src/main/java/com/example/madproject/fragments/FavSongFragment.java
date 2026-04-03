package com.example.madproject.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.adapters.SongAdapter;
import com.example.madproject.database.FavoritesOperations;
import com.example.madproject.interfaces.SongSelectionListener;
import com.example.madproject.models.SongsList;

import java.util.ArrayList;

/**
 * Fragment displaying the user's favorite songs.
 * Loads favorites from the SQLite database.
 */
public class FavSongFragment extends Fragment implements SongAdapter.OnSongClickListener {

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private TextView tvEmpty;
    private ArrayList<SongsList> favoritesList;
    private FavoritesOperations favoritesOps;
    private SongSelectionListener songSelectionListener;

    public FavSongFragment() {
        // Required empty constructor
    }

    public static FavSongFragment newInstance() {
        return new FavSongFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        recyclerView = view.findViewById(R.id.rv_favorites);
        tvEmpty = view.findViewById(R.id.tv_empty_favorites);

        setupRecyclerView();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh favorites every time the fragment is visible
        loadFavorites();
    }

    /**
     * Sets up the RecyclerView.
     */
    private void setupRecyclerView() {
        favoritesList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        songAdapter = new SongAdapter(getContext(), favoritesList, this);
        recyclerView.setAdapter(songAdapter);
    }

    /**
     * Loads favorites from the database and updates the UI.
     */
    public void loadFavorites() {
        if (favoritesOps != null) {
            favoritesList = favoritesOps.getAllFavorites();
            if (songAdapter != null) {
                songAdapter.updateData(favoritesList);
            }
            updateEmptyState();
        }
    }

    /**
     * Shows/hides the empty state message.
     */
    private void updateEmptyState() {
        if (favoritesList == null || favoritesList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSongClick(ArrayList<SongsList> songs, int position) {
        if (songSelectionListener != null) {
            songSelectionListener.onSongSelected(songs, position);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        songSelectionListener = null;
    }
}
