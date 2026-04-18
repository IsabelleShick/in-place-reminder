package com.example.inplacereminder;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class WelcomePage extends AppCompatActivity {
    DB_OpenHelper helper;
    Button btnMyReminders, btnMyPlaces, btnSettings, btnAbout;
    ImageButton ibProfile;
    ImageButton ib_back;
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_PROFILE_URI = "profile_uri";

    // NEW: holds the id of the currently logged user (read from SharedPreferences or DB)
    private long currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        btnMyReminders = findViewById(R.id.btnMyReminders);
        btnMyPlaces = findViewById(R.id.btnMyPlaces);
        btnSettings = findViewById(R.id.btnSettings);
        btnAbout = findViewById(R.id.btnAbout);
        ibProfile = findViewById(R.id.ibProfile);
        ib_back = findViewById(R.id.ib_back);

        helper = new DB_OpenHelper(this);

        // NEW: fetch current user id from SharedPreferences, fallback to DB by username
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getLong("current_user_id", -1);
        if (currentUserId == -1) {
            String currentUserName = prefs.getString("current_user", null);
            if (currentUserName != null) {
                SQLiteDatabase db = helper.getReadableDatabase();
                try {
                    currentUserId = DatabaseUtils.longForQuery(
                            db,
                            "SELECT id FROM users WHERE name = ?",
                            new String[]{currentUserName}
                    );
                    // save back to prefs for future fast access
                    prefs.edit().putLong("current_user_id", currentUserId).apply();
                } catch (Exception e) {
                    // leave currentUserId as -1 if not found or on error
                    currentUserId = -1;
                } finally {
                    db.close();
                }
            }
        }

        // existing: load saved profile image URI from SharedPreferences
//        String profileUriStr = prefs.getString(KEY_PROFILE_URI, null);
//        if (profileUriStr != null && !profileUriStr.isEmpty()) {
//            Uri profileUri = Uri.parse(profileUriStr);
//            ibProfile.setImageURI(profileUri);
        if (currentUserId != -1) {
            byte[] pictureData = helper.getUserPicture(currentUserId);
            if (pictureData != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length);
                ibProfile.setImageBitmap(helper.getCircularBitmap(bitmap));
            } else {
                ibProfile.setImageResource(R.drawable.no_profile);
            }

        } else {
            ibProfile.setImageResource(R.drawable.no_profile);
        }

        ib_back.setOnClickListener(v -> finish());

        ibProfile.setOnClickListener(v -> startActivity(new android.content.Intent(WelcomePage.this, Settings.class)));

        btnMyReminders.setOnClickListener(v -> startActivity(new android.content.Intent(WelcomePage.this, RemindersManager.class)));
        btnMyPlaces.setOnClickListener(v -> startActivity(new android.content.Intent(WelcomePage.this, PlaceManager.class)));
        btnSettings.setOnClickListener(v -> startActivity(new android.content.Intent(WelcomePage.this, Settings.class)));
        btnAbout.setOnClickListener(v -> startActivity(new android.content.Intent(WelcomePage.this, About.class)));

        // Request POST_NOTIFICATIONS permission if needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    // optional getter if other classes in this activity need the id
    public long getCurrentUserId() {
        return currentUserId;
    }
}