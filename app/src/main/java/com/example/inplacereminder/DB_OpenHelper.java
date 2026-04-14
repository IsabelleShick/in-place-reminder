package com.example.inplacereminder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

import androidx.annotation.Nullable;

/**
 * DB_OpenHelper
 * <p>
 * Manages local SQLite DB: creation, upgrades and common helper methods.
 */
public class DB_OpenHelper extends SQLiteOpenHelper {

    /* DATABASE CONFIG */
    private static final String DATABASE_NAME = "in_place_reminder.db";
    private static final int DATABASE_VERSION = 5; // bumped to add repeat_weekday

    /* TABLE / COLUMN CONSTANTS */
    public static final String TABLE_REMINDERS = "reminders";
    public static final String REMINDER_TITLE = "title";
    public static final String REMINDER_DESCRIPTION = "message";
    public static final String PLACE_ID = "place_id";

    public DB_OpenHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // PLACES table
        String CREATE_PLACES_TABLE =
                "CREATE TABLE places (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "place_name TEXT NOT NULL, " +
                        "lat REAL NOT NULL, " +
                        "lon REAL NOT NULL" +
                        ")";
        db.execSQL(CREATE_PLACES_TABLE);

        // REMINDERS table
        // note: repeat_weekday column added here (backwards-safe default -1)
        String CREATE_REMINDERS_TABLE =
                "CREATE TABLE " + TABLE_REMINDERS + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        REMINDER_TITLE + " TEXT, " +
                        "place_id INTEGER NOT NULL, " +
                        "year INTEGER, " +
                        "month INTEGER, " +
                        "date INTEGER, " +
                        "time TEXT, " +
                        "every_month BOOLEAN DEFAULT 0, " +
                        "every_date BOOLEAN DEFAULT 0, " +
                        "every_time BOOLEAN DEFAULT 0, " +
                        REMINDER_DESCRIPTION + " TEXT, " +
                        "alarm_sound TEXT, " +
                        // -1 = none, 0..6 = Sunday..Saturday, 7 = Every day
                        "repeat_weekday INTEGER DEFAULT -1, " +
                        "FOREIGN KEY (place_id) REFERENCES places(id)" +
                        ")";
        db.execSQL(CREATE_REMINDERS_TABLE);

        // USERS table
        String CREATE_USERS_TABLE =
                "CREATE TABLE users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "name TEXT NOT NULL UNIQUE, " +
                        "password_hash TEXT NOT NULL, " +
                        "picture BLOB" +
                        ")";
        db.execSQL(CREATE_USERS_TABLE);

        // Insert default admin user (Admin / 1234 hashed as 1509442)
        db.execSQL("INSERT INTO users (name, password_hash) VALUES ('Admin', '1509442')");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Version 2: add profile picture column
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE users ADD COLUMN picture BLOB");
        }

        // Version 3: add title column to reminders
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS +
                    " ADD COLUMN " + REMINDER_TITLE + " TEXT");
        }

        // Version 4: insert default place
        if (oldVersion < 4) {
            db.execSQL("INSERT INTO places (place_name, lat, lon) " +
                    "VALUES ('Anywhere', 0.0, 0.0)");
        }

        // Version 5: add repeat_weekday column (backwards-safe)
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE " + TABLE_REMINDERS + " ADD COLUMN repeat_weekday INTEGER DEFAULT -1");
        }
    }

    /* ------------------ USER TABLE OPERATIONS ------------------ */

    /**
     * Insert new user into database.
     * Returns row id or -1 on failure.
     */
    public long insertUser(String name, String passwordHash, byte[] picture) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("password_hash", passwordHash);

        if (picture != null) {
            cv.put("picture", picture);
        }

        long id = db.insert("users", null, cv);
        db.close();

        return id;
    }

    /**
     * Update profile picture for existing user
     *
     * @return number of rows updated
     */
    public int updateUserPicture(String name, byte[] picture) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("picture", picture);

        int rows = db.update("users", cv, "name = ?", new String[]{name});
        db.close();

        return rows;
    }

    /**
     * Check if a username already exists
     */
    public boolean userExists(String name) {
        if (name == null || name.isEmpty()) return false;

        SQLiteDatabase db = getReadableDatabase();
        long count = DatabaseUtils.longForQuery(
                db,
                "SELECT COUNT(1) FROM users WHERE name = ?",
                new String[]{name}
        );
        db.close();
        return count > 0;
    }

    /**
     * Get profile picture of a specific user by id
     *
     * @return byte[] image data or null
     */
    public byte[] getUserPicture(Long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT picture FROM users WHERE id = ?",
                new String[]{String.valueOf(id)}
        );

        byte[] picture = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                picture = cursor.isNull(0) ? null : cursor.getBlob(0);
            }
            cursor.close();
        }
        db.close();
        return picture;
    }

    /**
     * Return first user name (useful as primary user in single-user apps).
     */
    @Nullable
    public String getPrimaryUserName() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        String name = null;
        try {
            c = db.rawQuery("SELECT name FROM users LIMIT 1", null);
            if (c != null && c.moveToFirst()) {
                name = c.getString(0);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return name;
    }

    /**
     * Verify credentials for a given user name.
     * Note: This compares the provided candidatePassword to the stored value.
     * If your app stores hashed passwords, pass the hashed candidate or adapt hashing here.
     */
    public boolean verifyUserCredentials(String name, String candidatePassword) {
        if (name == null || name.isEmpty()) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT password_hash FROM users WHERE name = ?", new String[]{name});
            if (c != null && c.moveToFirst()) {
                String stored = c.getString(0);
                if (stored == null) return false;
                return stored.equals(candidatePassword);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
            db.close();
        }
        return false;
    }

    /**
     * Return cursor with user row for the provided name.
     * Caller must close the returned cursor when done.
     */
    @Nullable
    public Cursor getUserCursorByName(String name) {
        if (name == null) return null;
        SQLiteDatabase db = getReadableDatabase();
        try {
            return db.rawQuery("SELECT id, name, password_hash, picture FROM users WHERE name = ?", new String[]{name});
        } catch (Exception e) {
            // If an error occurs, ensure DB is closed and return null
            try {
                db.close();
            } catch (Exception ignored) {
            }
            return null;
        }
    }

    /* ------------------ UTIL ------------------ */

    public Bitmap getCircularBitmap(Bitmap src) {
        if (src == null) return null;
        int size = Math.min(src.getWidth(), src.getHeight());

        int x = (src.getWidth() - size) / 2;
        int y = (src.getHeight() - size) / 2;

        Bitmap squared = Bitmap.createBitmap(src, x, y, size, size);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);

        final android.graphics.Paint paint = new android.graphics.Paint();
        paint.setAntiAlias(true);
        paint.setShader(new android.graphics.BitmapShader(squared, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP));

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);
        return output;
    }
}