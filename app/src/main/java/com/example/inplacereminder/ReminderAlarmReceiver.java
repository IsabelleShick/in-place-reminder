package com.example.inplacereminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderAlarmReceiver";
    private static final String CHANNEL_ID = "reminders_channel";
    private static final String CHANNEL_NAME = "Reminders";
    private static final int CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;
    private static final SimpleDateFormat DISPLAY_FMT = new SimpleDateFormat("d/M/yyyy H:mm", Locale.getDefault());

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("id", -1);
        String title = intent.getStringExtra("title");
        long timeMs = intent.getLongExtra("time", 0L);
        String description = intent.getStringExtra("description");

        if (title == null || title.isEmpty()) title = "Reminder";

        createChannelIfNeeded(context);

        String timeText = timeMs > 0L ? DISPLAY_FMT.format(new Date(timeMs)) : "";
        String contentText = timeText.isEmpty() ? (description == null ? "" : description) : timeText;

        Intent openApp = new Intent(context, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent contentIntent = PendingIntent.getActivity(context, (int) id, openApp, flags);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 250, 250, 250});

        NotificationManagerCompat nmCompat = NotificationManagerCompat.from(context);

        // Check that notifications are enabled by the user and that we have permission on Android 13+.
        boolean canPost = nmCompat.areNotificationsEnabled();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            canPost &= ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }

        if (!canPost) {
            Log.w(TAG, "Notifications disabled or POST_NOTIFICATIONS permission missing; starting ReminderEditor directly for id=" + id);
            try {
                context.startActivity(openApp);
            } catch (Exception e) {
                Log.w(TAG, "Failed to start ReminderEditor as fallback", e);
            }
            return;
        }

        try {
            nmCompat.notify((int) id, nb.build());
        } catch (SecurityException se) {
            Log.w(TAG, "SecurityException while posting notification id=" + id, se);
        } catch (Exception e) {
            Log.w(TAG, "Unexpected exception while posting notification id=" + id, e);
        }
    }

    private void createChannelIfNeeded(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel ch = nm.getNotificationChannel(CHANNEL_ID);
            if (ch == null) {
                ch = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, CHANNEL_IMPORTANCE);
                ch.setDescription("Notifications for scheduled reminders");
                nm.createNotificationChannel(ch);
            }
        }
    }
}