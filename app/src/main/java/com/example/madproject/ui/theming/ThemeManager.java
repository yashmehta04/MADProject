package com.example.madproject.ui.theming;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.madproject.R;

/**
 * Theme manager for SonicWave Music Player
 * Handles dark/light mode theming and system theme integration
 * 
 * @since 2.2.0
 * @author SonicWave Team
 */
public class ThemeManager {

    private static final String TAG = "ThemeManager";
    private static volatile ThemeManager instance;
    
    // Theme modes
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;
    
    // Theme preferences
    private static final String PREF_THEME_MODE = "theme_mode";
    private static final String PREF_AMOLED_DARK = "amoled_dark";
    
    private final Context context;
    private int currentThemeMode;
    private boolean isAmoledDarkEnabled;
    
    private ThemeManager(Context context) {
        this.context = context.getApplicationContext();
        loadThemePreferences();
        applyTheme();
    }
    
    /**
     * Get singleton instance
     */
    public static ThemeManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Load theme preferences
     */
    private void loadThemePreferences() {
        android.content.SharedPreferences prefs = 
            context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE);
        
        currentThemeMode = prefs.getInt(PREF_THEME_MODE, THEME_SYSTEM);
        isAmoledDarkEnabled = prefs.getBoolean(PREF_AMOLED_DARK, false);
    }
    
    /**
     * Save theme preferences
     */
    public void saveThemePreferences(int themeMode, boolean amoledDark) {
        android.content.SharedPreferences prefs = 
            context.getSharedPreferences("theme_preferences", Context.MODE_PRIVATE);
        
        prefs.edit()
            .putInt(PREF_THEME_MODE, themeMode)
            .putBoolean(PREF_AMOLED_DARK, amoledDark)
            .apply();
        
        currentThemeMode = themeMode;
        isAmoledDarkEnabled = amoledDark;
        
        applyTheme();
    }
    
    /**
     * Apply current theme
     */
    private void applyTheme() {
        int nightMode;
        
        switch (currentThemeMode) {
            case THEME_LIGHT:
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case THEME_DARK:
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            case THEME_SYSTEM:
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode);
    }
    
    /**
     * Get current theme mode
     */
    public int getCurrentThemeMode() {
        return currentThemeMode;
    }
    
    /**
     * Check if dark mode is currently active
     */
    public boolean isDarkModeActive() {
        if (currentThemeMode == THEME_LIGHT) {
            return false;
        } else if (currentThemeMode == THEME_DARK) {
            return true;
        } else {
            // Follow system theme
            int nightModeFlags = context.getResources().getConfiguration().uiMode & 
                               android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
    }
    
    /**
     * Check if AMOLED dark mode is enabled
     */
    public boolean isAmoledDarkEnabled() {
        return isAmoledDarkEnabled && isDarkModeActive();
    }
    
    /**
     * Get theme color with proper contrast
     */
    public int getThemeColor(int colorResId) {
        try {
            return ContextCompat.getColor(context, colorResId);
        } catch (Resources.NotFoundException e) {
            return getDefaultThemeColor(colorResId);
        }
    }
    
    /**
     * Get primary color
     */
    public int getPrimaryColor() {
        if (isDarkModeActive()) {
            return isAmoledDarkEnabled() ? Color.BLACK : Color.parseColor("#121212");
        } else {
            return Color.parseColor("#2196F3");
        }
    }
    
    /**
     * Get primary dark color
     */
    public int getPrimaryDarkColor() {
        if (isDarkModeActive()) {
            return Color.parseColor("#000000");
        } else {
            return Color.parseColor("#1976D2");
        }
    }
    
    /**
     * Get accent color
     */
    public int getAccentColor() {
        return Color.parseColor("#FF4081");
    }
    
    /**
     * Get background color
     */
    public int getBackgroundColor() {
        if (isDarkModeActive()) {
            return isAmoledDarkEnabled() ? Color.BLACK : Color.parseColor("#121212");
        } else {
            return Color.parseColor("#FFFFFF");
        }
    }
    
    /**
     * Get surface color
     */
    public int getSurfaceColor() {
        if (isDarkModeActive()) {
            return isAmoledDarkEnabled() ? Color.parseColor("#1E1E1E") : Color.parseColor("#1E1E1E");
        } else {
            return Color.parseColor("#F5F5F5");
        }
    }
    
    /**
     * Get text primary color
     */
    public int getTextPrimaryColor() {
        if (isDarkModeActive()) {
            return Color.parseColor("#FFFFFF");
        } else {
            return Color.parseColor("#000000");
        }
    }
    
    /**
     * Get text secondary color
     */
    public int getTextSecondaryColor() {
        if (isDarkModeActive()) {
            return Color.parseColor("#B3FFFFFF");
        } else {
            return Color.parseColor("#8A000000");
        }
    }
    
    /**
     * Get glassmorphism background color
     */
    public int getGlassBackgroundColor() {
        if (isDarkModeActive()) {
            return Color.argb(51, 255, 255, 255); // 20% white
        } else {
            return Color.argb(51, 0, 0, 0); // 20% black
        }
    }
    
    /**
     * Get glassmorphism border color
     */
    public int getGlassBorderColor() {
        if (isDarkModeActive()) {
            return Color.argb(68, 255, 255, 255); // 27% white
        } else {
            return Color.argb(68, 0, 0, 0); // 27% black
        }
    }
    
    /**
     * Get mood color with theme adjustment
     */
    public int getMoodColor(String mood) {
        int baseColor = getBaseMoodColor(mood);
        
        if (isDarkModeActive()) {
            // Adjust colors for dark mode
            return adjustColorForDarkMode(baseColor);
        } else {
            return baseColor;
        }
    }
    
    /**
     * Get base mood color
     */
    private int getBaseMoodColor(String mood) {
        if (mood == null) return getAccentColor();
        
        switch (mood.toUpperCase()) {
            case "HAPPY":
                return Color.parseColor("#FFD700");
            case "SAD":
                return Color.parseColor("#4169E1");
            case "CALM":
                return Color.parseColor("#90EE90");
            case "ENERGETIC":
                return Color.parseColor("#FF6347");
            default:
                return getAccentColor();
        }
    }
    
    /**
     * Adjust color for dark mode
     */
    private int adjustColorForDarkMode(int color) {
        // Increase brightness for better visibility in dark mode
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        
        // Increase value (brightness) by 20%
        hsv[2] = Math.min(1.0f, hsv[2] * 1.2f);
        
        return Color.HSVToColor(hsv);
    }
    
    /**
     * Get default theme color
     */
    private int getDefaultThemeColor(int colorResId) {
        // Fallback colors for missing resources
        return getPrimaryColor();
    }
    
    /**
     * Get theme-aware status bar color
     */
    public int getStatusBarColor() {
        if (isDarkModeActive()) {
            return isAmoledDarkEnabled() ? Color.BLACK : Color.parseColor("#000000");
        } else {
            return getPrimaryDarkColor();
        }
    }
    
    /**
     * Get theme-aware navigation bar color
     */
    public int getNavigationBarColor() {
        if (isDarkModeActive()) {
            return isAmoledDarkEnabled() ? Color.BLACK : Color.parseColor("#121212");
        } else {
            return getBackgroundColor();
        }
    }
    
    /**
     * Get theme-aware ripple color
     */
    public int getRippleColor() {
        if (isDarkModeActive()) {
            return Color.argb(50, 255, 255, 255); // 20% white
        } else {
            return Color.argb(50, 0, 0, 0); // 20% black
        }
    }
    
    /**
     * Get theme-aware divider color
     */
    public int getDividerColor() {
        if (isDarkModeActive()) {
            return Color.parseColor("#33FFFFFF"); // 20% white
        } else {
            return Color.parseColor("#1F000000"); // 12% black
        }
    }
    
    /**
     * Check if color has sufficient contrast
     */
    public boolean hasSufficientContrast(int foreground, int background) {
        double contrastRatio = calculateContrastRatio(foreground, background);
        return contrastRatio >= 4.5; // WCAG AA standard
    }
    
    /**
     * Calculate contrast ratio between two colors
     */
    private double calculateContrastRatio(int foreground, int background) {
        double l1 = getLuminance(foreground);
        double l2 = getLuminance(background);
        
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        
        return (lighter + 0.05) / (darker + 0.05);
    }
    
    /**
     * Get luminance of a color
     */
    private double getLuminance(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        
        // Convert to sRGB
        double rsRGB = r / 255.0;
        double gsRGB = g / 255.0;
        double bsRGB = b / 255.0;
        
        // Convert to linear RGB
        double rLinear = rsRGB <= 0.03928 ? rsRGB / 12.92 : Math.pow((rsRGB + 0.055) / 1.055, 2.4);
        double gLinear = gsRGB <= 0.03928 ? gsRGB / 12.92 : Math.pow((gsRGB + 0.055) / 1.055, 2.4);
        double bLinear = bsRGB <= 0.03928 ? bsRGB / 12.92 : Math.pow((bsRGB + 0.055) / 1.055, 2.4);
        
        // Calculate luminance
        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear;
    }
    
    /**
     * Get contrasting text color for background
     */
    public int getContrastingTextColor(int backgroundColor) {
        double luminance = getLuminance(backgroundColor);
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
    
    /**
     * Apply theme to activity
     */
    public void applyThemeToActivity(android.app.Activity activity) {
        if (activity == null) return;
        
        // Set status bar color
        android.view.Window window = activity.getWindow();
        window.setStatusBarColor(getStatusBarColor());
        window.setNavigationBarColor(getNavigationBarColor());
        
        // Set system UI visibility
        int flags = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | 
                   android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                   android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        
        if (isDarkModeActive()) {
            flags |= android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        
        window.getDecorView().setSystemUiVisibility(flags);
    }
    
    /**
     * Get theme summary
     */
    public String getThemeSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("Theme Mode: ");
        
        switch (currentThemeMode) {
            case THEME_LIGHT:
                summary.append("Light");
                break;
            case THEME_DARK:
                summary.append("Dark");
                if (isAmoledDarkEnabled) {
                    summary.append(" (AMOLED)");
                }
                break;
            case THEME_SYSTEM:
                summary.append("System (");
                summary.append(isDarkModeActive() ? "Dark" : "Light");
                if (isDarkModeActive() && isAmoledDarkEnabled) {
                    summary.append(" (AMOLED)");
                }
                summary.append(")");
                break;
        }
        
        return summary.toString();
    }
    
    /**
     * Reset to default theme
     */
    public void resetToDefault() {
        saveThemePreferences(THEME_SYSTEM, false);
    }
}
