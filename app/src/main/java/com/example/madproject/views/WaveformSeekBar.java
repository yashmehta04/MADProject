package com.example.madproject.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.madproject.R;

/**
 * YouTube Music-style seekbar with a thin progress track, filled progress,
 * and a circular thumb indicator. Replaces the old waveform-bar seekbar.
 */
public class WaveformSeekBar extends View {

    private Paint trackPaint;
    private Paint progressPaint;
    private Paint thumbPaint;
    private Paint thumbBorderPaint;
    private Paint bufferedPaint;

    private int progress = 0;
    private int max = 100;
    private boolean isDragging = false;
    private OnSeekBarChangeListener listener;

    // Dimensions
    private final int trackHeight = 6;  // dp converted in init
    private final int thumbRadius = 16; // dp converted in init
    private float trackHeightPx;
    private float thumbRadiusPx;

    public interface OnSeekBarChangeListener {
        void onProgressChanged(WaveformSeekBar seekBar, int progress, boolean fromUser);
        void onStartTrackingTouch(WaveformSeekBar seekBar);
        void onStopTrackingTouch(WaveformSeekBar seekBar);
    }

    public WaveformSeekBar(Context context) {
        super(context);
        init();
    }

    public WaveformSeekBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        float density = getContext().getResources().getDisplayMetrics().density;
        trackHeightPx = 4 * density;
        thumbRadiusPx = 7 * density;

        // Unfilled track (dark muted)
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_hint));
        trackPaint.setAlpha(80);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        // Played progress (accent cyan)
        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setColor(ContextCompat.getColor(getContext(), R.color.accent));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        // Thumb circle (solid accent)
        thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbPaint.setColor(ContextCompat.getColor(getContext(), R.color.accent));

        // Thumb border/glow
        thumbBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        thumbBorderPaint.setColor(ContextCompat.getColor(getContext(), R.color.accent));
        thumbBorderPaint.setAlpha(60);

        // Buffered paint (unused but reserved)
        bufferedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bufferedPaint.setColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
        bufferedPaint.setAlpha(40);
    }

    /**
     * Kept for backward compatibility — ignored in the new seekbar style.
     */
    public void setSampleFrom(String seedString) {
        // No-op: new seekbar doesn't use waveform data
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, Math.min(progress, max));
        invalidate();
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
    }

    public int getProgress() {
        return progress;
    }

    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float paddingLeft = getPaddingLeft() + thumbRadiusPx;
        float paddingRight = getPaddingRight() + thumbRadiusPx;
        float usableWidth = width - paddingLeft - paddingRight;
        float centerY = height / 2f;

        if (usableWidth <= 0) return;

        float progressRatio = (float) progress / max;

        // --- Draw unfilled track ---
        RectF trackRect = new RectF(
                paddingLeft,
                centerY - trackHeightPx / 2f,
                paddingLeft + usableWidth,
                centerY + trackHeightPx / 2f);
        canvas.drawRoundRect(trackRect, trackHeightPx / 2f, trackHeightPx / 2f, trackPaint);

        // --- Draw filled progress ---
        float progressX = paddingLeft + usableWidth * progressRatio;
        RectF progressRect = new RectF(
                paddingLeft,
                centerY - trackHeightPx / 2f,
                progressX,
                centerY + trackHeightPx / 2f);
        canvas.drawRoundRect(progressRect, trackHeightPx / 2f, trackHeightPx / 2f, progressPaint);

        // --- Draw thumb ---
        // Outer glow (larger, semi-transparent) — only when dragging
        if (isDragging) {
            canvas.drawCircle(progressX, centerY, thumbRadiusPx * 1.8f, thumbBorderPaint);
        }
        // Main thumb
        canvas.drawCircle(progressX, centerY, thumbRadiusPx, thumbPaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) return false;

        float paddingLeft = getPaddingLeft() + thumbRadiusPx;
        float paddingRight = getPaddingRight() + thumbRadiusPx;
        float usableWidth = getWidth() - paddingLeft - paddingRight;
        float x = event.getX() - paddingLeft;
        float ratio = Math.max(0, Math.min(x / usableWidth, 1.0f));
        int newProgress = (int) (ratio * max);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                if (listener != null) listener.onStartTrackingTouch(this);
                setProgress(newProgress);
                if (listener != null) listener.onProgressChanged(this, newProgress, true);
                return true;
            case MotionEvent.ACTION_MOVE:
                setProgress(newProgress);
                if (listener != null) listener.onProgressChanged(this, newProgress, true);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                setProgress(newProgress);
                if (listener != null) listener.onStopTrackingTouch(this);
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
