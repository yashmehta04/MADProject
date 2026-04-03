package com.example.madproject.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.madproject.R;
import com.example.madproject.activities.MainActivity;

public class PlayerWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_PLAY_PAUSE = "com.example.madproject.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.madproject.ACTION_NEXT";
    public static final String ACTION_PREV = "com.example.madproject.ACTION_PREV";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_player);

        // Open app on click
        Intent openAppIntent = new Intent(context, MainActivity.class);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_album_art, openAppPendingIntent);
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent);

        // Play/Pause
        Intent playPauseIntent = new Intent(context, PlayerWidgetProvider.class);
        playPauseIntent.setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPausePending = PendingIntent.getBroadcast(context, 1, playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_play_pause, playPausePending);

        // Next
        Intent nextIntent = new Intent(context, PlayerWidgetProvider.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getBroadcast(context, 2, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_next, nextPending);

        // Previous
        Intent prevIntent = new Intent(context, PlayerWidgetProvider.class);
        prevIntent.setAction(ACTION_PREV);
        PendingIntent prevPending = PendingIntent.getBroadcast(context, 3, prevIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPending);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action == null)
            return;

        // Forward playback control to the PlaybackService via media session
        switch (action) {
            case ACTION_PLAY_PAUSE:
            case ACTION_NEXT:
            case ACTION_PREV:
                // Send a broadcast that MainActivity/PlaybackService can listen for
                Intent controlIntent = new Intent(action);
                controlIntent.setPackage(context.getPackageName());
                context.sendBroadcast(controlIntent);
                break;
        }
    }

    /**
     * Call this from anywhere to refresh the widget text/art.
     */
    public static void updateWidgetInfo(Context context, String title, String artist) {
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        ComponentName widgetComponent = new ComponentName(context, PlayerWidgetProvider.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);

        for (int id : widgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_player);
            views.setTextViewText(R.id.widget_title, title);
            views.setTextViewText(R.id.widget_artist, artist);
            widgetManager.updateAppWidget(id, views);
        }
    }
}
