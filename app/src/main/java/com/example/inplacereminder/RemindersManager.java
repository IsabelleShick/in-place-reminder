package com.example.inplacereminder;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RemindersManager extends AppCompatActivity {

    private DB_OpenHelper dbHelper;
    private SQLiteDatabase db;
    private Cursor cursor;
    private SimpleCursorAdapter adapter;
    private ListView lvReminders;
    private ImageButton ib_back;
    private Button btnDelete, btnEdit, btnAddReminder;
    private long selectedReminderId = -1;
    private int selectedPosition = AdapterView.INVALID_POSITION;

    private static final String KEY_SELECTED_POS = "selected_pos";
    private static final String KEY_SELECTED_ID = "selected_id";

    private static final SimpleDateFormat TIME_DISPLAY_FMT =
            new SimpleDateFormat("d/M/yyyy H:mm", Locale.getDefault());
    private static final SimpleDateFormat TIME_ONLY_FMT =
            new SimpleDateFormat("H:mm", Locale.getDefault());

    private static final String[] WEEKDAY_NAMES = new String[]{
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reminder_manager);

        ib_back = findViewById(R.id.ib_back1);
        lvReminders = findViewById(R.id.lvReminders);
        btnDelete = findViewById(R.id.btnDelete);
        btnEdit = findViewById(R.id.btnEdit);
        btnAddReminder = findViewById(R.id.btnAddReminder);

        dbHelper = new DB_OpenHelper(this);
        db = dbHelper.getReadableDatabase();

        btnDelete.setEnabled(false);
        btnEdit.setEnabled(false);

        ib_back.setOnClickListener(v -> finish());

        lvReminders.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        loadRemindersIntoList();

        if (savedInstanceState != null) {
            selectedPosition = savedInstanceState.getInt(KEY_SELECTED_POS, AdapterView.INVALID_POSITION);
            selectedReminderId = savedInstanceState.getLong(KEY_SELECTED_ID, -1);
            if (selectedPosition != AdapterView.INVALID_POSITION && adapter != null && adapter.getCount() > selectedPosition) {
                lvReminders.setItemChecked(selectedPosition, true);
                btnDelete.setEnabled(true);
                btnEdit.setEnabled(true);
            }
        }

        lvReminders.setOnItemClickListener((parent, view, position, id) -> {
            Cursor c = (Cursor) adapter.getItem(position);
            if (c != null) {
                int idx = c.getColumnIndex("_id");
                if (idx != -1) {
                    selectedReminderId = c.getLong(idx);
                    selectedPosition = position;
                    lvReminders.setItemChecked(position, true);
                    btnDelete.setEnabled(true);
                    btnEdit.setEnabled(true);
                }
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (selectedReminderId != -1) {
                AlarmScheduler.cancelReminder(RemindersManager.this, selectedReminderId);

                SQLiteDatabase writable = dbHelper.getWritableDatabase();
                try {
                    writable.delete(DB_OpenHelper.TABLE_REMINDERS, "id = ?", new String[]{String.valueOf(selectedReminderId)});
                } finally {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                    }
                    loadRemindersIntoList();

                    selectedReminderId = -1;
                    selectedPosition = AdapterView.INVALID_POSITION;
                    lvReminders.clearChoices();
                    btnDelete.setEnabled(false);
                    btnEdit.setEnabled(false);
                }
            } else {
                btnDelete.setEnabled(false);
            }
        });

        btnEdit.setOnClickListener(v -> {
            if (selectedPosition == AdapterView.INVALID_POSITION) return;
            Cursor c = (Cursor) adapter.getItem(selectedPosition);
            if (c != null) {
                int idxId = c.getColumnIndex("_id");
                int idxTitle = c.getColumnIndex(DB_OpenHelper.REMINDER_TITLE);
                int idxDesc = c.getColumnIndex(DB_OpenHelper.REMINDER_DESCRIPTION);
                int idxTime = c.getColumnIndex("time");
                int idxPlaceId = c.getColumnIndex(DB_OpenHelper.PLACE_ID);

                long id = idxId != -1 ? c.getLong(idxId) : -1;
                String title = idxTitle != -1 ? c.getString(idxTitle) : "";
                String desc = idxDesc != -1 ? c.getString(idxDesc) : "";
                long time = idxTime != -1 ? c.getLong(idxTime) : 0L;
                long placeId = idxPlaceId != -1 ? c.getLong(idxPlaceId) : -1;

                Intent intent = new Intent(RemindersManager.this, ReminderEditor.class);
                intent.putExtra("id", id);
                intent.putExtra("title", title);
                intent.putExtra("desc", desc);
                intent.putExtra("time", time);
                intent.putExtra("place_id", placeId);
                startActivity(intent);
            }
        });

        btnAddReminder.setOnClickListener(v -> {
            Intent intent = new Intent(RemindersManager.this, ReminderEditor.class);
            intent.putExtra("id", -1); // Indicate new reminder
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null && (db == null || !db.isOpen())) {
            db = dbHelper.getReadableDatabase();
        }
        loadRemindersIntoList();
    }

    private void loadRemindersIntoList() {
        String[] columns = new String[]{
                "id as _id",
                DB_OpenHelper.REMINDER_TITLE,
                DB_OpenHelper.REMINDER_DESCRIPTION,
                "time",
                "repeat_weekday", // include repeat_weekday so we can inspect it in the binder
                DB_OpenHelper.PLACE_ID // include place_id to pass to editor
        };

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        cursor = db.query(DB_OpenHelper.TABLE_REMINDERS,
                columns,
                null, null, null, null,
                "id DESC");

        String[] from = new String[]{
                DB_OpenHelper.REMINDER_TITLE,
                DB_OpenHelper.REMINDER_DESCRIPTION,
                "time"
        };
        int[] to = new int[]{
                R.id.tvItemTitle,
                R.id.tvItemDesc,
                R.id.tvItemTime
        };

        if (adapter == null) {
            adapter = new SimpleCursorAdapter(
                    this,
                    R.layout.reminder_list_item,
                    cursor,
                    from,
                    to,
                    0
            );

            adapter.setViewBinder((view, cur, columnIndex) -> {
                if (view.getId() == R.id.tvItemTime) {
                    long millis = 0L;
                    try {
                        millis = cur.getLong(columnIndex);
                    } catch (Exception ignored) {
                    }

                    String repeatStr = "";
                    int repeatIdx = cur.getColumnIndex("repeat_weekday");
                    if (repeatIdx != -1) {
                        try {
                            repeatStr = cur.getString(repeatIdx);
                        } catch (Exception ignored) {
                        }
                    }

                    TextView tv = (TextView) view;
                    if (repeatStr != null && !repeatStr.isEmpty()) {
                        // repeat set: show all selected days, append time if available
                        String text;

                        // Parse comma-delimited day indices and build day names
                        String[] indices = repeatStr.split(",");
                        StringBuilder daysText = new StringBuilder();
                        for (String index : indices) {
                            try {
                                int dayIdx = Integer.parseInt(index.trim());
                                if (dayIdx >= 0 && dayIdx <= 6) {
                                    if (daysText.length() > 0) daysText.append(", ");
                                    daysText.append(WEEKDAY_NAMES[dayIdx].substring(0, 3)); // Show abbreviated names: Sun, Mon, etc.
                                }
                            } catch (NumberFormatException ignored) {
                            }
                        }
                        text = daysText.toString();

                        if (millis > 0L) {
                            try {
                                String t = TIME_ONLY_FMT.format(new Date(millis));
                                if (!text.isEmpty()) text = text + " " + t;
                                else text = t;
                            } catch (Exception ignored) {
                            }
                        }

                        tv.setText(text);
                    } else {
                        // no repeat: default behavior (full date/time)
                        if (millis <= 0L) {
                            tv.setText("");
                        } else {
                            tv.setText(TIME_DISPLAY_FMT.format(new Date(millis)));
                        }
                    }
                    return true;
                }
                return false;
            });

            lvReminders.setAdapter(adapter);
        } else {
            adapter.changeCursor(cursor);
        }

        if (selectedPosition != AdapterView.INVALID_POSITION && cursor != null && selectedPosition < cursor.getCount()) {
            lvReminders.setItemChecked(selectedPosition, true);
            btnDelete.setEnabled(true);
            btnEdit.setEnabled(true);
        } else {
            selectedPosition = AdapterView.INVALID_POSITION;
            selectedReminderId = -1;
            lvReminders.clearChoices();
            btnDelete.setEnabled(false);
            btnEdit.setEnabled(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_POS, selectedPosition);
        outState.putLong(KEY_SELECTED_ID, selectedReminderId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) {
            adapter.changeCursor(null);
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        if (db != null && db.isOpen()) db.close();
        if (dbHelper != null) dbHelper.close();
    }
}