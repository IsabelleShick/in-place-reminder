package com.example.inplacereminder;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;

public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";

    /**
     * Schedule a single reminder alarm. If the time is in the past or equal to now it is
     * bumped to a short time in the future so AlarmManager will still fire immediately.
     */
    public static void scheduleReminder(Context context, long reminderId, long timeMs, String title, String desc, String place) {
        scheduleReminder(context, reminderId, timeMs, title, desc, place, -1L);
    }

    /**
     * Schedule a single reminder alarm with placeId. If the time is in the past or equal to now it is
     * bumped to a short time in the future so AlarmManager will still fire immediately.
     */
    public static void scheduleReminder(Context context, long reminderId, long timeMs, String title, String desc, String place, long placeId) {
        String repeatDays = getRepeatDaysForReminder(context, reminderId);

        // If repeat days are set, calculate next occurrence; otherwise use provided timeMs
        if (repeatDays != null && !repeatDays.isEmpty()) {
            timeMs = getNextOccurrenceForRepeat(repeatDays, timeMs);
        }
        
        long now = System.currentTimeMillis();
        // If requested time is in the past or exactly now, bump to now + 1s so AM will fire immediately
        if (timeMs <= now) {
            Log.i(TAG, "scheduleReminder: requested time <= now, bumping to now+1s (id=" + reminderId + ")");
            timeMs = now + 1000L;
        }

        // On Android 12+ optionally request the exact alarm permission UI if not allowed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager amCheck = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (amCheck != null && !amCheck.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission missing for package; prompting settings (id=" + reminderId + ")");
                if (context instanceof Activity) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                    ((Activity) context).startActivity(intent);
                } else {
                    Toast.makeText(context, "Exact alarm permission required. Open app settings to allow exact alarms.", Toast.LENGTH_LONG).show();
                }
                // Do not abort entirely — still attempt to schedule below (device may allow it).
            }
        }

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("id", reminderId);
        if (title != null) intent.putExtra("title", title);
        if (desc != null) intent.putExtra("description", desc);
        intent.putExtra("time", timeMs);
        if (place != null) intent.putExtra("place", place);
        intent.putExtra("place_id", placeId); // NEW
        intent.putExtra("repeat_days", repeatDays); // NEW: pass repeat days for rescheduling

        int requestCode = (int) reminderId;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, flags);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            Log.w(TAG, "AlarmManager is null; cannot schedule (id=" + reminderId + ")");
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMs, pi);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                am.setExact(AlarmManager.RTC_WAKEUP, timeMs, pi);
            } else {
                am.set(AlarmManager.RTC_WAKEUP, timeMs, pi);
            }
            Log.i(TAG, "Alarm scheduled id=" + reminderId + " at " + timeMs);
        } catch (SecurityException se) {
            Log.w(TAG, "SecurityException scheduling alarm id=" + reminderId, se);
        } catch (Exception e) {
            Log.w(TAG, "Exception scheduling alarm id=" + reminderId, e);
        }
    }

    /**
     * Get the repeat days string for a reminder from the database
     */
    private static String getRepeatDaysForReminder(Context context, long reminderId) {
        DB_OpenHelper dbHelper = new DB_OpenHelper(context);
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor c = db.query(
                     DB_OpenHelper.TABLE_REMINDERS,
                     new String[]{"repeat_weekday"},
                     "id = ?",
                     new String[]{String.valueOf(reminderId)},
                     null, null, null
             )) {
            if (c != null && c.moveToFirst()) {
                try {
                    return c.getString(0);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching repeat days for reminder " + reminderId, e);
        } finally {
            dbHelper.close();
        }
        return null;
    }

    /**
     * Calculate the next occurrence time for a repeating reminder
     *
     * @param repeatDays   Comma-delimited string of day indices (0=Sunday, 6=Saturday)
     * @param originalTime The original reminder time (used to extract time of day)
     * @return The timestamp of the next occurrence
     */
    private static long getNextOccurrenceForRepeat(String repeatDays, long originalTime) {
        if (repeatDays == null || repeatDays.isEmpty()) {
            return originalTime;
        }

        // Parse selected day indices
        int[] selectedDays = new int[7];
        for (int i = 0; i < 7; i++) {
            selectedDays[i] = 0;
        }

        String[] indices = repeatDays.split(",");
        for (String index : indices) {
            try {
                int dayIdx = Integer.parseInt(index.trim());
                if (dayIdx >= 0 && dayIdx <= 6) {
                    selectedDays[dayIdx] = 1;
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Extract time of day from original time
        Calendar originalCal = Calendar.getInstance();
        originalCal.setTimeInMillis(originalTime);
        int hourOfDay = originalCal.get(Calendar.HOUR_OF_DAY);
        int minute = originalCal.get(Calendar.MINUTE);
        int second = originalCal.get(Calendar.SECOND);

        // Find next occurrence
        Calendar nextOccurrence = Calendar.getInstance();
        nextOccurrence.setTimeInMillis(System.currentTimeMillis());
        nextOccurrence.set(Calendar.HOUR_OF_DAY, hourOfDay);
        nextOccurrence.set(Calendar.MINUTE, minute);
        nextOccurrence.set(Calendar.SECOND, second);
        nextOccurrence.set(Calendar.MILLISECOND, 0);

        // If we've already passed this time today, start checking from tomorrow
        if (nextOccurrence.getTimeInMillis() <= System.currentTimeMillis()) {
            nextOccurrence.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Find the next day that matches one of the selected days
        int daysChecked = 0;
        while (daysChecked < 7) {
            int dayOfWeek = nextOccurrence.get(Calendar.DAY_OF_WEEK) - 1; // Convert to 0=Sunday, 6=Saturday
            if (selectedDays[dayOfWeek] == 1) {
                return nextOccurrence.getTimeInMillis();
            }
            nextOccurrence.add(Calendar.DAY_OF_MONTH, 1);
            daysChecked++;
        }

        // Fallback: if no days selected, return original time
        return originalTime;
    }

    public static void cancelReminder(Context context, long reminderId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        int requestCode = (int) reminderId;
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flags |= PendingIntent.FLAG_IMMUTABLE;

        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, flags);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null && pi != null) {
            try {
                am.cancel(pi);
                pi.cancel();
                Log.i(TAG, "Alarm cancelled id=" + reminderId);
            } catch (SecurityException se) {
                Log.w(TAG, "SecurityException cancelling alarm id=" + reminderId, se);
            } catch (Exception e) {
                Log.w(TAG, "Exception cancelling alarm id=" + reminderId, e);
            }
        }
    }

    public static void rescheduleAll(Context context) {
        DB_OpenHelper dbHelper = new DB_OpenHelper(context);
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = dbHelper.getReadableDatabase();
            String[] cols = new String[]{"id", DB_OpenHelper.REMINDER_TITLE, DB_OpenHelper.REMINDER_DESCRIPTION, "time"};
            String selection = "time > ?";
            String[] selectionArgs = new String[]{String.valueOf(System.currentTimeMillis())};
            c = db.query(DB_OpenHelper.TABLE_REMINDERS, cols, selection, selectionArgs, null, null, "time ASC");
            if (c != null) {
                while (c.moveToNext()) {
                    long id = c.getLong(0);
                    String title = c.isNull(1) ? null : c.getString(1);
                    String desc = c.isNull(2) ? null : c.getString(2);
                    long time = 0L;
                    try {
                        time = c.isNull(3) ? 0L : c.getLong(3);
                    } catch (Exception ignored) {
                        try {
                            String s = c.isNull(3) ? null : c.getString(3);
                            if (s != null) time = Long.parseLong(s);
                        } catch (Exception ignored2) {
                        }
                    }
                    if (time > System.currentTimeMillis()) {
                        scheduleReminder(context, id, time, title, desc, null);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null && !c.isClosed()) c.close();
            if (db != null && db.isOpen()) db.close();
            dbHelper.close();
        }
    }
}