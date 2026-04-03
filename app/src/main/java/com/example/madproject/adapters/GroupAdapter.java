package com.example.madproject.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.madproject.R;
import com.example.madproject.models.LibraryGroupItem;
import java.util.ArrayList;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private final Context context;
    private ArrayList<LibraryGroupItem> groupsList;
    private final boolean isGrid;
    private final OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(LibraryGroupItem group);
    }

    public GroupAdapter(Context context, ArrayList<LibraryGroupItem> groupsList, boolean isGrid,
            OnGroupClickListener listener) {
        this.context = context;
        this.groupsList = groupsList;
        this.isGrid = isGrid;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isGrid ? R.layout.item_album : R.layout.item_generic_list;
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        return new GroupViewHolder(view, isGrid);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        LibraryGroupItem group = groupsList.get(position);

        if (isGrid) {
            holder.tvTitle.setText(group.getTitle());
            holder.tvSubtitle.setText(group.getSubtitle());
            try {
                Uri albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"), group.getAlbumId());
                holder.ivIcon.setImageURI(albumArtUri);
                if (holder.ivIcon.getDrawable() == null) {
                    holder.ivIcon.setImageResource(R.drawable.ic_music_note);
                }
            } catch (Exception e) {
                holder.ivIcon.setImageResource(R.drawable.ic_music_note);
            }
        } else {
            holder.tvTitle.setText(group.getTitle());
            holder.tvSubtitle.setText(group.getSubtitle());
            holder.ivIcon.setImageResource(R.drawable.ic_music_note);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupsList != null ? groupsList.size() : 0;
    }

    public void updateData(ArrayList<LibraryGroupItem> newGroups) {
        this.groupsList = newGroups;
        notifyDataSetChanged();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvSubtitle;

        GroupViewHolder(@NonNull View itemView, boolean isGrid) {
            super(itemView);
            if (isGrid) {
                ivIcon = itemView.findViewById(R.id.iv_album_art);
                tvTitle = itemView.findViewById(R.id.tv_album_name);
                tvSubtitle = itemView.findViewById(R.id.tv_album_artist);
            } else {
                ivIcon = itemView.findViewById(R.id.iv_item_icon);
                tvTitle = itemView.findViewById(R.id.tv_item_name);
                tvSubtitle = itemView.findViewById(R.id.tv_item_info);
            }
        }
    }
}
