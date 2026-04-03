package com.example.madproject.ui.accessibility;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * Accessibility manager for SonicWave Music Player
 * Ensures compliance with Android accessibility guidelines
 * 
 * @since 2.2.0
 * @author SonicWave Team
 */
public class AccessibilityManager {

    /**
     * Setup accessibility for image views
     */
    public static void setupImageAccessibility(ImageView imageView, String contentDescription) {
        if (imageView == null) return;
        
        imageView.setContentDescription(contentDescription);
        ViewCompat.setAccessibilityDelegate(imageView, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.ImageView.class.getName());
                info.setContentDescription(contentDescription);
            }
        });
    }

    /**
     * Setup accessibility for text views
     */
    public static void setupTextAccessibility(TextView textView, String contentDescription) {
        if (textView == null) return;
        
        if (contentDescription != null) {
            textView.setContentDescription(contentDescription);
        }
        
        // Ensure text is readable by screen readers
        textView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    /**
     * Setup accessibility for buttons
     */
    public static void setupButtonAccessibility(View button, String contentDescription) {
        if (button == null) return;
        
        button.setContentDescription(contentDescription);
        ViewCompat.setAccessibilityDelegate(button, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.Button.class.getName());
                info.setContentDescription(contentDescription);
                info.setClickable(true);
            }
        });
    }

    /**
     * Announce changes to screen readers
     */
    public static void announceForAccessibility(Context context, String message) {
        if (context == null) return;
        
        android.view.View announcementView = new android.view.View(context);
        announcementView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        announcementView.announceForAccessibility(message);
    }

    /**
     * Setup focus management for lists
     */
    public static void setupListFocusManagement(androidx.recyclerview.widget.RecyclerView recyclerView) {
        if (recyclerView == null) return;
        
        recyclerView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        recyclerView.setAccessibilityDelegate(new androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate(recyclerView));
    }

    /**
     * Check if accessibility is enabled
     */
    public static boolean isAccessibilityEnabled(Context context) {
        if (context == null) return false;
        
        android.accessibilityservice.AccessibilityManager am = 
            (android.accessibilityservice.AccessibilityManager) 
            context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        
        return am != null && am.isEnabled();
    }

    /**
     * Setup high contrast mode support
     */
    public static void setupHighContrastSupport(View view) {
        if (view == null) return;
        
        // Check if high contrast mode is enabled
        if (isHighContrastModeEnabled(view.getContext())) {
            // Apply high contrast colors
            applyHighContrastColors(view);
        }
    }

    private static boolean isHighContrastModeEnabled(Context context) {
        if (context == null) return false;
        
        android.accessibilityservice.AccessibilityManager am = 
            (android.accessibilityservice.AccessibilityManager) 
            context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        
        return am != null && am.isHighTextContrastEnabled();
    }

    private static void applyHighContrastColors(View view) {
        // Apply high contrast color scheme
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            textView.setTextColor(android.graphics.Color.BLACK);
            if (textView.getBackground() != null) {
                textView.getBackground().setColorFilter(android.graphics.Color.WHITE);
            }
        }
    }

    /**
     * Setup font scaling support
     */
    public static void setupFontScalingSupport(TextView textView) {
        if (textView == null) return;
        
        // Ensure text scales properly with system font size
        textView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 
            textView.getTextSize() / textView.getResources().getDisplayMetrics().scaledDensity);
    }

    /**
     * Setup navigation accessibility
     */
    public static void setupNavigationAccessibility(View view, String navigationHint) {
        if (view == null) return;
        
        view.setContentDescription(navigationHint);
        ViewCompat.setAccessibilityDelegate(view, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.view.View.class.getName());
                info.setContentDescription(navigationHint);
            }
        });
    }

    /**
     * Setup progress bar accessibility
     */
    public static void setupProgressBarAccessibility(android.widget.ProgressBar progressBar, String contentDescription) {
        if (progressBar == null) return;
        
        progressBar.setContentDescription(contentDescription);
        ViewCompat.setAccessibilityDelegate(progressBar, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.ProgressBar.class.getName());
                info.setContentDescription(contentDescription);
                
                // Add progress information
                if (progressBar.isIndeterminate()) {
                    info.setContentDescription(contentDescription + " loading");
                } else {
                    int progress = progressBar.getProgress();
                    int max = progressBar.getMax();
                    info.setContentDescription(contentDescription + " " + progress + " of " + max);
                }
            }
        });
    }

    /**
     * Setup seek bar accessibility
     */
    public static void setupSeekBarAccessibility(android.widget.SeekBar seekBar, String contentDescription) {
        if (seekBar == null) return;
        
        seekBar.setContentDescription(contentDescription);
        ViewCompat.setAccessibilityDelegate(seekBar, new androidx.core.view.AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(android.widget.SeekBar.class.getName());
                info.setContentDescription(contentDescription);
                
                // Add seek bar specific actions
                AccessibilityNodeInfoCompat.AccessibilityActionCompat 
                    actionIncrease = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD, "Increase");
                info.addAction(actionIncrease);
                
                AccessibilityNodeInfoCompat.AccessibilityActionCompat 
                    actionDecrease = new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD, "Decrease");
                info.addAction(actionDecrease);
            }
        });
    }

    /**
     * Validate accessibility compliance
     */
    public static boolean validateAccessibilityCompliance(View rootView) {
        if (rootView == null) return false;
        
        boolean isCompliant = true;
        
        // Check all views for accessibility
        checkViewAccessibility(rootView, isCompliant);
        
        return isCompliant;
    }

    private static void checkViewAccessibility(View view, boolean isCompliant) {
        if (view == null) return;
        
        // Check if view has content description when needed
        if (shouldHaveContentDescription(view) && 
            (view.getContentDescription() == null || view.getContentDescription().toString().isEmpty())) {
            isCompliant = false;
        }
        
        // Check if view is important for accessibility
        if (view.getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_NO) {
            // Ensure it's not an interactive element
            if (view.isClickable() || view.isFocusable()) {
                isCompliant = false;
            }
        }
        
        // Recursively check child views
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup viewGroup = (android.view.ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                checkViewAccessibility(viewGroup.getChildAt(i), isCompliant);
            }
        }
    }

    private static boolean shouldHaveContentDescription(View view) {
        return view instanceof ImageView || 
               view instanceof android.widget.Button ||
               view instanceof android.widget.ImageButton ||
               view instanceof android.widget.SeekBar ||
               view instanceof android.widget.ProgressBar;
    }
}
