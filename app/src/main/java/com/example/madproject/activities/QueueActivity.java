package com.example.madproject.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.adapters.QueueAdapter;
import com.example.madproject.models.SongsList;

import java.util.ArrayList;

import androidx.appcompat.widget.SearchView;

public class QueueActivity extends AppCompatActivity implements QueueAdapter.OnQueueActionListener {

    private RecyclerView rvQueue;
    private QueueAdapter adapter;
    private ArrayList<SongsList> queue;
    private ArrayList<SongsList> filteredQueue;
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        rvQueue = findViewById(R.id.rv_queue);
        searchView = findViewById(R.id.search_view_queue);
        rvQueue.setLayoutManager(new LinearLayoutManager(this));

        // Get queue from static holder
        queue = QueueHolder.getQueue();
        if (queue == null)
            queue = new ArrayList<>();
        
        // Initialize filtered queue
        filteredQueue = new ArrayList<>(queue);

        adapter = new QueueAdapter(this, filteredQueue, this);
        rvQueue.setAdapter(adapter);

        // Setup search functionality
        setupSearchView();

        // Setup drag-to-reorder via ItemTouchHelper
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                
                // Update filtered queue
                adapter.onItemMove(fromPosition, toPosition);
                
                // Update original queue to maintain consistency
                SongsList item = filteredQueue.remove(fromPosition);
                filteredQueue.add(toPosition, item);
                
                // Find and update in original queue
                int originalFromPos = queue.indexOf(item);
                if (originalFromPos != -1) {
                    SongsList targetItem = filteredQueue.get(toPosition);
                    int originalToPos = queue.indexOf(targetItem);
                    if (originalToPos != -1) {
                        SongsList originalItem = queue.remove(originalFromPos);
                        queue.add(originalToPos, originalItem);
                    }
                }
                
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // Not used — we have a remove button instead
            }
        };

        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rvQueue);
        adapter.setTouchHelper(touchHelper);
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterQueue(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterQueue(newText);
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            // Reset to full queue when search is closed
            filteredQueue.clear();
            filteredQueue.addAll(queue);
            adapter.notifyDataSetChanged();
            return false;
        });
    }

    private void filterQueue(String query) {
        filteredQueue.clear();
        
        if (query == null || query.trim().isEmpty()) {
            filteredQueue.addAll(queue);
        } else {
            String searchQuery = query.toLowerCase().trim();
            for (SongsList song : queue) {
                if (song.getTitle().toLowerCase().contains(searchQuery) ||
                    song.getArtist().toLowerCase().contains(searchQuery) ||
                    (song.getAlbum() != null && song.getAlbum().toLowerCase().contains(searchQuery))) {
                    filteredQueue.add(song);
                }
            }
        }
        
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onQueueItemRemoved(int position) {
        Toast.makeText(this, "Removed from queue", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onQueueReordered(ArrayList<SongsList> newQueue) {
        QueueHolder.setQueue(newQueue);
    }

    @Override
    public void onQueueItemClick(int position) {
        // Get the song from filtered queue
        SongsList selectedSong = filteredQueue.get(position);
        
        // Find the position in the original queue
        int originalPosition = queue.indexOf(selectedSong);
        
        // Navigate back and tell main activity which song to play
        setResult(RESULT_OK, getIntent().putExtra("queue_position", originalPosition));
        finish();
    }

    /**
     * Static holder so the queue can be shared between activities without
     * Parcelable overhead.
     */
    public static class QueueHolder {
        private static ArrayList<SongsList> queue;

        public static void setQueue(ArrayList<SongsList> q) {
            queue = q;
        }

        public static ArrayList<SongsList> getQueue() {
            return queue;
        }
    }
}
