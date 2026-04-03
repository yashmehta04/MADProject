package com.example.madproject.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.adapters.GroupAdapter;
import com.example.madproject.adapters.SongAdapter;
import com.example.madproject.interfaces.SongSelectionListener;
import com.example.madproject.models.LibraryGroupItem;
import com.example.madproject.models.SongsList;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Fragment displaying all songs found on the device.
 * Uses RecyclerView with SongAdapter for the song list.
 */
public class AllSongFragment extends Fragment
        implements SongAdapter.OnSongClickListener, GroupAdapter.OnGroupClickListener {

    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private TextView tvEmpty;
    private ArrayList<SongsList> songsList;
    private SongSelectionListener songSelectionListener;

    private TabLayout tabLayout;
    private ConstraintLayout sublistHeader;
    private View btnBack;
    private TextView tvSublistTitle;

    // Group Data
    private ArrayList<LibraryGroupItem> albumsList;
    private ArrayList<LibraryGroupItem> artistsList;
    private ArrayList<LibraryGroupItem> foldersList;
    private GroupAdapter groupAdapter;

    private boolean isViewingSublist = false;
    private int currentTab = 0; // 0=Songs, 1=Albums, 2=Artists, 3=Folders

    public AllSongFragment() {
        // Required empty constructor
    }

    /**
     * Factory method to create a new instance with songs data.
     */
    public static AllSongFragment newInstance(ArrayList<SongsList> songs) {
        AllSongFragment fragment = new AllSongFragment();
        fragment.songsList = songs;
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Attach the song selection listener (implemented by MainActivity)
        if (context instanceof SongSelectionListener) {
            songSelectionListener = (SongSelectionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement SongSelectionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (songsList == null) {
            songsList = new ArrayList<>();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_songs, container, false);

        recyclerView = view.findViewById(R.id.rv_all_songs);
        tvEmpty = view.findViewById(R.id.tv_empty_songs);

        tabLayout = view.findViewById(R.id.tab_layout_library);
        sublistHeader = view.findViewById(R.id.sublist_header);
        btnBack = view.findViewById(R.id.btn_back_library);
        tvSublistTitle = view.findViewById(R.id.tv_sublist_title);

        setupRecyclerView();
        updateEmptyState();

        btnBack.setOnClickListener(v -> closeSublist());

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                closeSublist();
                loadTabContent();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        return view;
    }

    private void loadTabContent() {
        if (songsList == null || songsList.isEmpty())
            return;

        switch (currentTab) {
            case 0: // Songs
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                recyclerView.setAdapter(songAdapter);
                break;
            case 1: // Albums
                if (albumsList == null)
                    generateAlbums();
                recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                groupAdapter = new GroupAdapter(getContext(), albumsList, true, this);
                recyclerView.setAdapter(groupAdapter);
                break;
            case 2: // Artists
                if (artistsList == null)
                    generateArtists();
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                groupAdapter = new GroupAdapter(getContext(), artistsList, false, this);
                recyclerView.setAdapter(groupAdapter);
                break;
            case 3: // Folders
                if (foldersList == null)
                    generateFolders();
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                groupAdapter = new GroupAdapter(getContext(), foldersList, false, this);
                recyclerView.setAdapter(groupAdapter);
                break;
        }
    }

    private void generateAlbums() {
        albumsList = new ArrayList<>();
        Map<String, ArrayList<SongsList>> map = new HashMap<>();
        for (SongsList s : songsList) {
            String key = s.getAlbum();
            if (!map.containsKey(key))
                map.put(key, new ArrayList<>());
            map.get(key).add(s);
        }
        for (Map.Entry<String, ArrayList<SongsList>> entry : map.entrySet()) {
            ArrayList<SongsList> list = entry.getValue();
            albumsList
                    .add(new LibraryGroupItem(entry.getKey(), list.get(0).getArtist(), list.get(0).getAlbumId(), list));
        }
    }

    private void generateArtists() {
        artistsList = new ArrayList<>();
        Map<String, ArrayList<SongsList>> map = new HashMap<>();
        for (SongsList s : songsList) {
            String key = s.getArtist();
            if (!map.containsKey(key))
                map.put(key, new ArrayList<>());
            map.get(key).add(s);
        }
        for (Map.Entry<String, ArrayList<SongsList>> entry : map.entrySet()) {
            ArrayList<SongsList> list = entry.getValue();
            artistsList.add(new LibraryGroupItem(entry.getKey(), list.size() + " songs", -1, list));
        }
    }

    private void generateFolders() {
        foldersList = new ArrayList<>();
        Map<String, ArrayList<SongsList>> map = new HashMap<>();
        for (SongsList s : songsList) {
            File f = new File(s.getPath());
            String key = f.getParent();
            if (key == null)
                key = "Unknown Folder";
            if (!map.containsKey(key))
                map.put(key, new ArrayList<>());
            map.get(key).add(s);
        }
        for (Map.Entry<String, ArrayList<SongsList>> entry : map.entrySet()) {
            ArrayList<SongsList> list = entry.getValue();
            File f = new File(entry.getKey());
            foldersList.add(new LibraryGroupItem(f.getName(), list.size() + " songs", -1, list));
        }
    }

    @Override
    public void onGroupClick(LibraryGroupItem group) {
        // Open sublist
        isViewingSublist = true;
        tabLayout.setVisibility(View.GONE);
        sublistHeader.setVisibility(View.VISIBLE);
        tvSublistTitle.setText(group.getTitle());

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        SongAdapter subAdapter = new SongAdapter(getContext(), group.getSongs(), this);
        recyclerView.setAdapter(subAdapter);
    }

    private void closeSublist() {
        if (!isViewingSublist)
            return;
        isViewingSublist = false;
        tabLayout.setVisibility(View.VISIBLE);
        sublistHeader.setVisibility(View.GONE);
        loadTabContent(); // Restore current tab
    }

    /**
     * Sets up the RecyclerView with adapter and layout manager.
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        songAdapter = new SongAdapter(getContext(), songsList, this);
        recyclerView.setAdapter(songAdapter);

        // Add swipe to queue functionality
        androidx.recyclerview.widget.ItemTouchHelper itemTouchHelper = new androidx.recyclerview.widget.ItemTouchHelper(
                new androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(0,
                        androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        return false; // No drag and drop here
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        if (direction == androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                            int position = viewHolder.getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION && songSelectionListener != null
                                    && songsList != null) {
                                SongsList song = songsList.get(position);
                                songSelectionListener.onAddToQueue(song);
                            }
                            // Refresh to reset the swipe animation
                            songAdapter.notifyItemChanged(position);
                        }
                    }
                });
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    /**
     * Shows/hides empty state message.
     */
    private void updateEmptyState() {
        if (songsList == null || songsList.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Updates the songs list (called from MainActivity on refresh).
     */
    public void updateSongs(ArrayList<SongsList> newSongs) {
        this.songsList = newSongs;

        // Invalidate cached groups
        this.albumsList = null;
        this.artistsList = null;
        this.foldersList = null;

        if (songAdapter != null) {
            songAdapter.updateData(newSongs);
        }
        if (!isViewingSublist) {
            loadTabContent();
        }
        updateEmptyState();
    }

    /**
     * Returns the adapter for search filtering.
     */
    public SongAdapter getSongAdapter() {
        return songAdapter;
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
