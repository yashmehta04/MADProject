package com.example.madproject.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.ui.GlassmorphismMainActivity.MoodCategory;

import java.util.List;

/**
 * Glassmorphism adapter for mood categories with smooth animations
 */
public class MoodCategoryAdapter extends RecyclerView.Adapter<MoodCategoryAdapter.MoodCategoryViewHolder> {

    private List<MoodCategory> moodCategories;
    private OnMoodCategoryClickListener listener;

    public interface OnMoodCategoryClickListener {
        void onMoodCategoryClicked(MoodCategory category);
    }

    public MoodCategoryAdapter(List<MoodCategory> moodCategories, OnMoodCategoryClickListener listener) {
        this.moodCategories = moodCategories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MoodCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mood_category_glass, parent, false);
        return new MoodCategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodCategoryViewHolder holder, int position) {
        MoodCategory category = moodCategories.get(position);
        
        holder.tvMoodName.setText(category.name);
        holder.tvMoodCount.setText(String.valueOf(category.songCount));
        holder.ivMoodIcon.setImageResource(category.iconRes);
        
        // Set glassmorphism background color
        holder.cardView.setCardBackgroundColor(
                holder.itemView.getContext().getResources().getColor(category.colorRes));
        
        // Set click listener with glassmorphism feedback
        holder.itemView.setOnClickListener(v -> {
            // Add scale animation
            holder.itemView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        holder.itemView.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start();
                    })
                    .start();
            
            if (listener != null) {
                listener.onMoodCategoryClicked(category);
            }
        });
        
        // Animate entrance
        animateItemEntrance(holder.itemView, position);
    }

    @Override
    public int getItemCount() {
        return moodCategories.size();
    }

    /**
     * Animate item entrance with staggered effect
     */
    private void animateItemEntrance(View itemView, int position) {
        itemView.setTranslationY(50f);
        itemView.setAlpha(0f);
        
        itemView.postDelayed(() -> {
            itemView.animate()
                    .translationY(0f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }, position * 100);
    }

    static class MoodCategoryViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView ivMoodIcon;
        TextView tvMoodName;
        TextView tvMoodCount;

        MoodCategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_mood_category);
            ivMoodIcon = itemView.findViewById(R.id.iv_mood_icon);
            tvMoodName = itemView.findViewById(R.id.tv_mood_name);
            tvMoodCount = itemView.findViewById(R.id.tv_mood_count);
        }
    }
}
