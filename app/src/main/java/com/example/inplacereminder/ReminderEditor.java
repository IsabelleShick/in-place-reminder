package com.example.inplacereminder;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderEditor extends AppCompatActivity {
    private long reminderId = -1;

    private Button btnSave;
    private Button btnDelete;
    private Spinner spPlaces;
    private Spinner spRepeat;
    private TextView tvTimeView;
    private TextView tvDateView;
    private EditText etDate, etTime;
    private EditText etTitle;
    private EditText etDescription;
    private ImageButton ib_back;
    private DB_OpenHelper dbHelper;
    private List<String> placeNames;
    private List<Long> placeIds;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("H:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reminder_editor);

        dbHelper = new DB_OpenHelper(this);

        btnSave = findViewById(R.id.btnSave);
        btnDelete = findViewById(R.id.btnDelete);
        tvTimeView = findViewById(R.id.tvTimeView);
        tvDateView = findViewById(R.id.tvDateView);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        ib_back = findViewById(R.id.ib_back);
        spPlaces = findViewById(R.id.spPlaces); // existing spinner for places
        spRepeat = findViewById(R.id.spRepeat); // new spinner - ensure layout contains this id

        // Populate repeat spinner: None, Sunday..Saturday, Every day
        String[] repeatOptions = new String[]{
                "None",
                "Sunday",
                "Monday",
                "Tuesday",
                "Wednesday",
                "Thursday",
                "Friday",
                "Saturday",
                "Every day"
        };
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, repeatOptions);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spRepeat != null) spRepeat.setAdapter(repeatAdapter);

        // Load and populate places spinner
        placeNames = new ArrayList<>();
        placeIds = new ArrayList<>();
        loadPlaces();
        ArrayAdapter<String> placesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, placeNames);
        placesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        if (spPlaces != null) spPlaces.setAdapter(placesAdapter);

        ib_back.setOnClickListener(v -> finish());

        tvDateView.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            int y = now.get(Calendar.YEAR);
            int m = now.get(Calendar.MONTH);
            int d = now.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePicker = new DatePickerDialog(
                    ReminderEditor.this,
                    (view, year, month, dayOfMonth) -> {
                        String dateStr = dayOfMonth + "/" + (month + 1) + "/" + year;
                        etDate.setText(dateStr);
                        tvDateView.setText(dateStr);
                    },
                    y, m, d
            );

            Calendar startOfToday = Calendar.getInstance();
            startOfToday.set(Calendar.HOUR_OF_DAY, 0);
            startOfToday.set(Calendar.MINUTE, 0);
            startOfToday.set(Calendar.SECOND, 0);
            startOfToday.set(Calendar.MILLISECOND, 0);
            datePicker.getDatePicker().setMinDate(startOfToday.getTimeInMillis());

            datePicker.show();
        });

        tvTimeView.setOnClickListener(v -> showTimePicker());

        if (getIntent() != null && getIntent().hasExtra("id")) {
            reminderId = getIntent().getLongExtra("id", -1);
            String title = getIntent().getStringExtra("title");
            String desc = getIntent().getStringExtra("desc");
            long time = getIntent().getLongExtra("time", 0L);
            long placeId = getIntent().getLongExtra("place_id", -1);

            if (title != null) etTitle.setText(title);
            if (desc != null) etDescription.setText(desc);

            if (time > 0L) {
                Date d = new Date(time);
                String dateStr = DATE_FMT.format(d);
                String timeStr = TIME_FMT.format(d);

                etDate.setText(dateStr);
                etTime.setText(timeStr);
                tvDateView.setText(dateStr);
                tvTimeView.setText(timeStr);
            }

            // Set the correct place in spinner if placeId is provided
            if (placeId >= 0) {
                // Find and select the matching place
                for (int i = 0; i < placeIds.size(); i++) {
                    if (placeIds.get(i) == placeId) {
                        spPlaces.setSelection(i);
                        break;
                    }
                }
            } else if (placeId == -1) {
                // Select "Any Location" (first option)
                spPlaces.setSelection(0);
            }

            // load repeat_weekday from DB if present
            if (reminderId != -1) {
                try (SQLiteDatabase read = dbHelper.getReadableDatabase();
                     Cursor c = read.rawQuery("SELECT repeat_weekday FROM " + DB_OpenHelper.TABLE_REMINDERS + " WHERE id = ?",
                             new String[]{String.valueOf(reminderId)})) {
                    if (c != null && c.moveToFirst()) {
                        int repeatWeekday = -1;
                        try {
                            repeatWeekday = c.getInt(0);
                        } catch (Exception ignored) {
                        }
                        // map stored value to spinner index:
                        int selIndex = 0;
                        if (repeatWeekday == -1) selIndex = 0;
                        else if (repeatWeekday >= 0 && repeatWeekday <= 6)
                            selIndex = repeatWeekday + 1;
                        else if (repeatWeekday == 7) selIndex = 8;
                        if (spRepeat != null) spRepeat.setSelection(selIndex);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        btnSave.setOnClickListener(v -> saveReminderToDatabase());
    }

    private void showTimePicker() {
        final Calendar now = Calendar.getInstance();
        int initHour = 12;
        int initMinute = 0;

        String existing = etTime.getText().toString().trim();
        if (!existing.isEmpty()) {
            try {
                Date parsed = TIME_FMT.parse(existing);
                if (parsed != null) {
                    Calendar c = Calendar.getInstance();
                    c.setTime(parsed);
                    initHour = c.get(Calendar.HOUR_OF_DAY);
                    initMinute = c.get(Calendar.MINUTE);
                }
            } catch (ParseException ignored) {
            }
        } else if (isDateToday()) {
            initHour = now.get(Calendar.HOUR_OF_DAY);
            initMinute = now.get(Calendar.MINUTE);
        }

        TimePickerDialog timePicker = new TimePickerDialog(
                ReminderEditor.this,
                (view, hourOfDay, minute) -> {
                    if (isDateToday()) {
                        Calendar chosen = Calendar.getInstance();
                        chosen.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        chosen.set(Calendar.MINUTE, minute);
                        chosen.set(Calendar.SECOND, 0);
                        chosen.set(Calendar.MILLISECOND, 0);

                        if (!chosen.after(now)) {
                            Toast.makeText(ReminderEditor.this,
                                    "Selected time is already past. Choose a future time.",
                                    Toast.LENGTH_SHORT).show();
                            showTimePicker();
                            return;
                        }
                    }
                    String timeStr = hourOfDay + ":" + String.format(Locale.getDefault(), "%02d", minute);
                    etTime.setText(timeStr);
                    tvTimeView.setText(timeStr);
                },
                initHour,
                initMinute,
                true
        );

        timePicker.show();
    }

    private boolean isDateToday() {
        String dateStr = etDate.getText().toString().trim();
        if (dateStr.isEmpty()) return false;

        try {
            Date selected = DATE_FMT.parse(dateStr);
            if (selected == null) return false;

            Calendar selCal = Calendar.getInstance();
            selCal.setTime(selected);
            Calendar today = Calendar.getInstance();

            return selCal.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                    && selCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isSelectedDateTimeInPast() {
        String dateStr = etDate.getText().toString().trim();
        String timeStr = etTime.getText().toString().trim();
        if (dateStr.isEmpty() || timeStr.isEmpty()) return false;

        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy H:mm", Locale.getDefault());
        sdf.setLenient(false);
        try {
            Date selected = sdf.parse(dateStr + " " + timeStr);
            if (selected == null) return false;
            Date now = new Date();
            return !selected.after(now);
        } catch (ParseException e) {
            return true;
        }
    }

    private void loadPlaces() {
        placeNames.clear();
        placeIds.clear();

        // Add "Any Location" as first option with id -1
        placeNames.add("Any Location");
        placeIds.add(-1L);

        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.query(
                     "places",
                     new String[]{"id", "place_name"},
                     null,
                     null,
                     null,
                     null,
                     "place_name ASC"
             )) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String name = cursor.getString(1);
                    placeIds.add(id);
                    placeNames.add(name);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading places", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveReminderToDatabase() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            title = "Untitled Reminder";
        }

        if (isSelectedDateTimeInPast()) {
            Toast.makeText(this, "Reminder date/time must be in the future.", Toast.LENGTH_SHORT).show();
            return;
        }

        long timeMs = 0L;
        String dateStr = etDate.getText().toString().trim();
        String timeStr = etTime.getText().toString().trim();
        if (!dateStr.isEmpty() && !timeStr.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy H:mm", Locale.getDefault());
                Date combined = sdf.parse(dateStr + " " + timeStr);
                if (combined != null) {
                    timeMs = combined.getTime();
                }
            } catch (ParseException ignored) {
            }
        }

        // determine place ID from spinner selection
        long placeIdForDb = 1; // fallback default
        String placeStr = null;
        int selectedPlacePosition = spPlaces != null ? spPlaces.getSelectedItemPosition() : -1;
        if (selectedPlacePosition >= 0 && selectedPlacePosition < placeIds.size()) {
            placeIdForDb = placeIds.get(selectedPlacePosition);
            placeStr = placeNames.get(selectedPlacePosition);
        }

        // determine repeat_weekday from spinner: -1 none, 0..6 Sun..Sat, 7 every day
        int repeatWeekday = -1;
        if (spRepeat != null) {
            int pos = spRepeat.getSelectedItemPosition();
            if (pos <= 0) repeatWeekday = -1;
            else if (pos >= 1 && pos <= 7) repeatWeekday = pos - 1;
            else repeatWeekday = 7;
        }

        try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(DB_OpenHelper.REMINDER_TITLE, title);
            values.put(DB_OpenHelper.REMINDER_DESCRIPTION, description);
            if (timeMs > 0L) values.put("time", timeMs);
            values.put(DB_OpenHelper.PLACE_ID, placeIdForDb);
            values.put("repeat_weekday", repeatWeekday);

            if (reminderId == -1) {
                try {
                    long newRowId = db.insertOrThrow(DB_OpenHelper.TABLE_REMINDERS, null, values);
                    if (newRowId != -1) {
                        if (timeMs > System.currentTimeMillis()) {
                            AlarmScheduler.scheduleReminder(this, newRowId, timeMs, title, description, placeStr, placeIdForDb);
                        }
                        Toast.makeText(this, "Reminder saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error saving reminder.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                try {
                    AlarmScheduler.cancelReminder(this, reminderId);

                    int rows = db.update(
                            DB_OpenHelper.TABLE_REMINDERS,
                            values,
                            "id = ?",
                            new String[]{String.valueOf(reminderId)}
                    );
                    if (rows > 0) {
                        if (timeMs > System.currentTimeMillis()) {
                            AlarmScheduler.scheduleReminder(this, reminderId, timeMs, title, description, placeStr, placeIdForDb);
                        }
                        Toast.makeText(this, "Reminder updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "No reminder updated.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "A database error occurred.", Toast.LENGTH_SHORT).show();
        }
    }
}