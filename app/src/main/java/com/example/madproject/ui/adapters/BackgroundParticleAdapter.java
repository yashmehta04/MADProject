package com.example.madproject.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madproject.R;
import com.example.madproject.ui.GlassmorphismMainActivity.BackgroundParticle;

import java.util.List;

/**
 * Glassmorphism adapter for background particles with smooth animations
 */
public class BackgroundParticleAdapter extends RecyclerView.Adapter<BackgroundParticleAdapter.ParticleViewHolder> {

    private List<BackgroundParticle> particles;

    public BackgroundParticleAdapter(List<BackgroundParticle> particles) {
        this.particles = particles;
    }

    @NonNull
    @Override
    public ParticleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_background_particle, parent, false);
        return new ParticleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParticleViewHolder holder, int position) {
        BackgroundParticle particle = particles.get(position);
        
        // Apply glassmorphism effect
        holder.ivParticle.setAlpha((int) (particle.currentScale * 255));
        
        // Apply rotation animation
        holder.ivParticle.setRotation(particle.currentRotation);
        
        // Apply scale animation
        holder.ivParticle.setScaleX(particle.currentScale);
        holder.ivParticle.setScaleY(particle.currentScale);
    }

    @Override
    public int getItemCount() {
        return particles.size();
    }

    static class ParticleViewHolder extends RecyclerView.ViewHolder {
        ImageView ivParticle;

        ParticleViewHolder(@NonNull View itemView) {
            super(itemView);
            ivParticle = itemView.findViewById(R.id.iv_particle);
        }
    }
}
