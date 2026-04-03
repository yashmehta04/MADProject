package com.example.madproject.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.madproject.R;

/**
 * Base class for consistent UI styling across all activities
 * Provides centralized theme management and UI consistency utilities
 * 
 * <p>This class ensures consistent styling, colors, and behavior across
 * all UI components in the SonicWave application.</p>
 * 
 * @since 2.1.0
 * @author SonicWave Team
 */
public class UIStyleManager {

    private static final String TAG = "UIStyleManager";
    
    // Theme colors
    public static final int COLOR_PRIMARY = Color.parseColor("#2196F3");
    public static final int COLOR_PRIMARY_DARK = Color.parseColor("#1976D2");
    public static final int COLOR_ACCENT = Color.parseColor("#FF4081");
    public static final int COLOR_BACKGROUND = Color.parseColor("#121212");
    public static final int COLOR_SURFACE = Color.parseColor("#1E1E1E");
    public static final int COLOR_ON_PRIMARY = Color.parseColor("#FFFFFF");
    public static final int COLOR_ON_SURFACE = Color.parseColor("#FFFFFF");
    public static final int COLOR_TEXT_PRIMARY = Color.parseColor("#FFFFFF");
    public static final int COLOR_TEXT_SECONDARY = Color.parseColor("#B3FFFFFF");
    
    // Glassmorphism colors
    public static final int COLOR_GLASS_BACKGROUND = Color.parseColor("#33FFFFFF");
    public static final int COLOR_GLASS_BORDER = Color.parseColor("#44FFFFFF");
    public static final int COLOR_GLASS_HIGHLIGHT = Color.parseColor("#66FFFFFF");
    
    // Mood colors
    public static final int COLOR_MOOD_HAPPY = Color.parseColor("#FFD700");
    public static final int COLOR_MOOD_SAD = Color.parseColor("#4169E1");
    public static final int COLOR_MOOD_CALM = Color.parseColor("#90EE90");
    public static final int COLOR_MOOD_ENERGETIC = Color.parseColor("#FF6347");
    public static final int COLOR_MOOD_PARTY = Color.parseColor("#FF1493");
    
    /**
     * Apply consistent button styling
     * 
     * @param button The button to style
     * @param style The style type (PRIMARY, SECONDARY, ACCENT)
     */
    public static void styleButton(Button button, ButtonStyle style) {
        if (button == null) return;
        
        switch (style) {
            case PRIMARY:
                button.setBackgroundColor(COLOR_PRIMARY);
                button.setTextColor(COLOR_ON_PRIMARY);
                break;
                
            case SECONDARY:
                button.setBackgroundColor(COLOR_SURFACE);
                button.setTextColor(COLOR_TEXT_PRIMARY);
                break;
                
            case ACCENT:
                button.setBackgroundColor(COLOR_ACCENT);
                button.setTextColor(COLOR_ON_PRIMARY);
                break;
                
            case GLASS:
                button.setBackgroundColor(COLOR_GLASS_BACKGROUND);
                button.setTextColor(COLOR_TEXT_PRIMARY);
                button.setStrokeWidth(2);
                button.setStrokeColor(COLOR_GLASS_BORDER);
                break;
        }
        
        // Set consistent padding
        int padding = (int) (button.getContext().getResources().getDisplayMetrics().density * 16);
        button.setPadding(padding, padding, padding, padding);
    }
    
    /**
     * Apply consistent text styling
     * 
     * @param textView The text view to style
     * @param textStyle The text style (HEADER, BODY, CAPTION)
     */
    public static void styleText(TextView textView, TextStyle textStyle) {
        if (textView == null) return;
        
        switch (textStyle) {
            case HEADER:
                textView.setTextColor(COLOR_TEXT_PRIMARY);
                textView.setTextSize(20f);
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
                break;
                
            case BODY:
                textView.setTextColor(COLOR_TEXT_PRIMARY);
                textView.setTextSize(16f);
                textView.setTypeface(null, android.graphics.Typeface.NORMAL);
                break;
                
            case CAPTION:
                textView.setTextColor(COLOR_TEXT_SECONDARY);
                textView.setTextSize(14f);
                textView.setTypeface(null, android.graphics.Typeface.NORMAL);
                break;
                
            case GLASS:
                textView.setTextColor(COLOR_TEXT_PRIMARY);
                textView.setTextSize(16f);
                textView.setBackgroundColor(COLOR_GLASS_BACKGROUND);
                textView.setPadding(16, 12, 16, 12);
                break;
        }
    }
    
    /**
     * Get mood color for the specified mood
     * 
     * @param mood The mood string
     * @return The corresponding color
     */
    public static int getMoodColor(String mood) {
        if (mood == null) return COLOR_PRIMARY;
        
        switch (mood.toUpperCase()) {
            case "HAPPY":
                return COLOR_MOOD_HAPPY;
            case "SAD":
                return COLOR_MOOD_SAD;
            case "CALM":
                return COLOR_MOOD_CALM;
            case "ENERGETIC":
                return COLOR_MOOD_ENERGETIC;
            case "PARTY":
                return COLOR_MOOD_PARTY;
            default:
                return COLOR_PRIMARY;
        }
    }
    
    /**
     * Apply glassmorphism effect to a view
     * 
     * @param view The view to apply the effect to
     * @param alpha The alpha value (0-255)
     */
    public static void applyGlassmorphism(View view, int alpha) {
        if (view == null) return;
        
        // Set background with glass effect
        view.setBackgroundColor(Color.argb(alpha, 255, 255, 255));
        
        // Set elevation for depth effect
        view.setElevation(8f);
        
        // Add subtle border
        view.setBackground(createGlassBorder(view.getContext()));
    }
    
    /**
     * Create glass border drawable
     */
    private static android.graphics.drawable.Drawable createGlassBorder(Context context) {
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setStroke(2, COLOR_GLASS_BORDER);
        drawable.setCornerRadius(16f);
        return drawable;
    }
    
    /**
     * Apply consistent card styling
     * 
     * @param cardView The card view to style
     * @param elevation The elevation value
     */
    public static void styleCard(View cardView, float elevation) {
        if (cardView == null) return;
        
        cardView.setBackgroundColor(COLOR_SURFACE);
        cardView.setElevation(elevation);
        
        // Apply rounded corners
        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(16f);
        drawable.setColor(COLOR_SURFACE);
        cardView.setBackground(drawable);
    }
    
    /**
     * Apply consistent theme to an activity
     * 
     * @param context The activity context
     */
    public static void applyTheme(Context context) {
        if (context == null) return;
        
        // Set status bar color
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            android.view.Window window = activity.getWindow();
            window.setStatusBarColor(COLOR_PRIMARY_DARK);
            window.setNavigationBarColor(COLOR_BACKGROUND);
        }
    }
    
    /**
     * Get theme color from attributes
     * 
     * @param context The context
     * @param attr The attribute resource
     * @return The color value
     */
    public static int getThemeColor(Context context, int attr) {
        if (context == null) return COLOR_PRIMARY;
        
        TypedArray typedArray = context.obtainStyledAttributes(new int[]{attr});
        int color = typedArray.getColor(0, COLOR_PRIMARY);
        typedArray.recycle();
        return color;
    }
    
    /**
     * Apply ripple effect to a view
     * 
     * @param view The view to apply ripple to
     * @param rippleColor The ripple color
     */
    public static void applyRippleEffect(View view, int rippleColor) {
        if (view == null) return;
        
        android.content.res.ColorStateList rippleColorStateList = 
            android.content.res.ColorStateList.valueOf(rippleColor);
        
        android.graphics.drawable.RippleDrawable rippleDrawable = 
            new android.graphics.drawable.RippleDrawable(rippleColorStateList, null, null);
        
        view.setBackground(rippleDrawable);
    }
    
    /**
     * Button style enumeration
     */
    public enum ButtonStyle {
        PRIMARY,    // Primary action button
        SECONDARY,  // Secondary action button
        ACCENT,     // Accent button
        GLASS       // Glassmorphism button
    }
    
    /**
     * Text style enumeration
     */
    public enum TextStyle {
        HEADER,     // Large header text
        BODY,       // Regular body text
        CAPTION,    // Small caption text
        GLASS       // Glassmorphism text
    }
    
    /**
     * Validate and sanitize color values
     * 
     * @param color The color to validate
     * @return True if color is valid
     */
    public static boolean isValidColor(int color) {
        return color >= 0 && color <= 0xFFFFFFFF;
    }
    
    /**
     * Get contrasting text color for background
     * 
     * @param backgroundColor The background color
     * @return The contrasting text color (black or white)
     */
    public static int getContrastingTextColor(int backgroundColor) {
        // Calculate luminance
        double luminance = (0.299 * Color.red(backgroundColor) + 
                           0.587 * Color.green(backgroundColor) + 
                           0.114 * Color.blue(backgroundColor)) / 255;
        
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
    
    /**
     * Apply consistent animation duration
     * 
     * @param view The view to animate
     * @param animationType The type of animation
     */
    public static void applyAnimation(View view, AnimationType animationType) {
        if (view == null) return;
        
        long duration = getAnimationDuration(animationType);
        
        switch (animationType) {
            case FADE_IN:
                view.setAlpha(0f);
                view.animate().alpha(1f).setDuration(duration).start();
                break;
                
            case FADE_OUT:
                view.setAlpha(1f);
                view.animate().alpha(0f).setDuration(duration).start();
                break;
                
            case SLIDE_UP:
                view.setTranslationY(view.getHeight());
                view.animate().translationY(0f).setDuration(duration).start();
                break;
                
            case SLIDE_DOWN:
                view.setTranslationY(-view.getHeight());
                view.animate().translationY(0f).setDuration(duration).start();
                break;
                
            case SCALE_IN:
                view.setScaleX(0f);
                view.setScaleY(0f);
                view.animate().scaleX(1f).scaleY(1f).setDuration(duration).start();
                break;
        }
    }
    
    /**
     * Get animation duration for type
     */
    private static long getAnimationDuration(AnimationType type) {
        switch (type) {
            case FADE_IN:
            case FADE_OUT:
                return 300L;
            case SLIDE_UP:
            case SLIDE_DOWN:
                return 400L;
            case SCALE_IN:
                return 250L;
            default:
                return 300L;
        }
    }
    
    /**
     * Animation type enumeration
     */
    public enum AnimationType {
        FADE_IN,
        FADE_OUT,
        SLIDE_UP,
        SLIDE_DOWN,
        SCALE_IN
    }
    
    /**
     * Apply consistent spacing
     * 
     * @param view The view to apply spacing to
     * @param spacingType The type of spacing
     * @param value The spacing value in dp
     */
    public static void applySpacing(View view, SpacingType spacingType, int value) {
        if (view == null) return;
        
        float density = view.getContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (value * density);
        
        switch (spacingType) {
            case MARGIN:
                setMargins(view, pixels, pixels, pixels, pixels);
                break;
            case PADDING:
                view.setPadding(pixels, pixels, pixels, pixels);
                break;
            case MARGIN_HORIZONTAL:
                setMargins(view, pixels, 0, pixels, 0);
                break;
            case PADDING_HORIZONTAL:
                view.setPadding(pixels, 0, pixels, 0);
                break;
            case MARGIN_VERTICAL:
                setMargins(view, 0, pixels, 0, pixels);
                break;
            case PADDING_VERTICAL:
                view.setPadding(0, pixels, 0, pixels);
                break;
        }
    }
    
    /**
     * Set margins for a view
     */
    private static void setMargins(View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof android.view.ViewGroup.MarginLayoutParams) {
            android.view.ViewGroup.MarginLayoutParams params = 
                (android.view.ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.setMargins(left, top, right, bottom);
            view.setLayoutParams(params);
        }
    }
    
    /**
     * Spacing type enumeration
     */
    public enum SpacingType {
        MARGIN,
        PADDING,
        MARGIN_HORIZONTAL,
        PADDING_HORIZONTAL,
        MARGIN_VERTICAL,
        PADDING_VERTICAL
    }
}
