package com.example.inplacereminder;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reminder_channel_01";
    private static final String CHANNEL_NAME = "Reminders";
    private static final String CHANNEL_DESC = "Reminder notifications";
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Create notification channel if needed
        createChannelIfNeeded(context);

        // Read extras (caller that schedules the alarm must set these)
        long reminderId = intent != null ? intent.getLongExtra("id", -1L) : -1L;
        String title = intent != null ? intent.getStringExtra("title") : null;
        String desc = intent != null ? intent.getStringExtra("desc") : null;
        long time = intent != null ? intent.getLongExtra("time", 0L) : 0L;
        String place = intent != null ? intent.getStringExtra("place") : null; // optional

        // Build intent to open reminder details (reuse ReminderEditor which accepts extras)
        Intent activityIntent = new Intent(context, ReminderEditor.class);
        activityIntent.putExtra("id", reminderId);
        if (title != null) activityIntent.putExtra("title", title);
        if (desc != null) activityIntent.putExtra("desc", desc);
        if (time > 0L) activityIntent.putExtra("time", time);
        if (place != null) activityIntent.putExtra("place", place);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = (int) (reminderId == -1 ? System.currentTimeMillis() : (reminderId & 0xffffffff));
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingFlags |= PendingIntent.FLAG_MUTABLE;
        }

        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                activityIntent,
                pendingFlags
        );

        // Build the notification
        String notifTitle = (title == null || title.isEmpty()) ? "Reminder" : title;
        String notifText = (desc == null) ? "" : desc;

        NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(notifTitle)
                .setContentText(notifText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Post the notification (check POST_NOTIFICATIONS on Android 13+ and handle SecurityException)
        int notificationId = requestCode;
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);

        boolean canNotify = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            canNotify = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }

        if (canNotify) {
            try {
                nm.notify(notificationId, nb.build());
            } catch (SecurityException se) {
                Log.w(TAG, "Notification rejected by OS: missing permission", se);
                // as fallback, attempt to start the activity so user still sees reminder UI
                try {
                    context.startActivity(activityIntent);
                } catch (Exception ignored) {
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to post notification", e);
            }
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission denied or not requested; skipping notify()");
            try {
                context.startActivity(activityIntent);
            } catch (Exception ignored) {
            }
        }

        // Play default notification sound (fire-and-forget)
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (uri != null) {
                Ringtone r = RingtoneManager.getRingtone(context, uri);
                if (r != null) r.play();
            }
        } catch (Exception ignored) {
        }
    }

    private void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = nm.getNotificationChannel(CHANNEL_ID);
            if (channel == null) {
                channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription(CHANNEL_DESC);
                channel.setVibrationPattern(new long[]{0, 250, 100, 250});
                nm.createNotificationChannel(channel);
            }
        }
    }
}