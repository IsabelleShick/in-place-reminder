package com.example.inplacereminder;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Settings extends AppCompatActivity {
    ImageButton ib_back, ibProfile;
    private ListView listActions;

    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_PROFILE_URI = "profile_uri";
    // optional key that your app might use to store signed-in user id
    private static final String KEY_CURRENT_USER_ID = "current_user_id";

    private static final int REQ_GALLERY = 1001;
    private static final int REQ_CAMERA = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        ib_back = findViewById(R.id.ib_back4);
        ibProfile = findViewById(R.id.ibProfile);
        listActions = findViewById(R.id.listActions);

        ib_back.setOnClickListener(v -> finish());

        // show existing image (from Application or prefs) as before
        MyApplication app = null;
        try {
            app = (MyApplication) getApplicationContext();
        } catch (ClassCastException ignored) {
            app = null;
        }

        Bitmap bmp = null;
        if (app != null) bmp = app.getCurrentUserBitmap();

        if (bmp != null) {
            ibProfile.setImageBitmap(getCircularBitmap(bmp));
        } else {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String profileUriStr = prefs.getString(KEY_PROFILE_URI, null);
            if (profileUriStr != null && !profileUriStr.isEmpty()) {
                try {
                    ibProfile.setImageURI(Uri.parse(profileUriStr));
                } catch (Exception e) {
                    ibProfile.setImageResource(R.drawable.no_profile);
                }
            } else {
                ibProfile.setImageResource(R.drawable.no_profile);
            }
        }

        // when profile image is clicked, show dialog to pick Gallery or Camera
        ibProfile.setOnClickListener(v -> showImagePickerDialog());

        // Populate actions list
        String[] actions = new String[]{"Log out" /* add more items later */};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, actions);
        listActions.setAdapter(adapter);
        listActions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) { // Log out
                    performLogout();
                }
                // handle other positions later
            }
        });
    }

    private void performLogout() {
        // Clear stored user info from SharedPreferences (profile URI and optional user id)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor e = prefs.edit();
        e.remove(KEY_PROFILE_URI);
        e.remove(KEY_CURRENT_USER_ID);
        e.apply();

        // Optionally clear any in-memory state on your Application subclass if available
        try {
            MyApplication app = (MyApplication) getApplicationContext();
            // if your MyApplication has a method to clear current user, call it here:
            // app.clearCurrentUser();
        } catch (ClassCastException ignored) {
        }

        // Navigate to MainActivity and clear back stack
        Intent intent = new Intent(Settings.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showImagePickerDialog() {
        String[] items = new String[]{"Gallery", "Camera"};
        new AlertDialog.Builder(this)
                .setTitle("Select image")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        Intent pick = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        pick.setType("image/*");
                        startActivityForResult(pick, REQ_GALLERY);
                    } else {
                        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        try {
                            startActivityForResult(cam, REQ_CAMERA);
                        } catch (android.content.ActivityNotFoundException e) {
                            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    // existing onActivityResult and getCircularBitmap remain the same...
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        try {
            if (requestCode == REQ_GALLERY && data != null) {
                Uri selectedUri = data.getData();
                if (selectedUri != null) {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedUri);
                    ibProfile.setImageBitmap(getCircularBitmap(bitmap));
                    prefs.edit().putString(KEY_PROFILE_URI, selectedUri.toString()).apply();
                }
            } else if (requestCode == REQ_CAMERA && data != null) {
                Object extra = data.getExtras() != null ? data.getExtras().get("data") : null;
                if (extra instanceof Bitmap) {
                    Bitmap thumbnail = (Bitmap) extra;
                    ibProfile.setImageBitmap(getCircularBitmap(thumbnail));
                    File f = new File(getFilesDir(), "profile_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(f);
                        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.flush();
                        Uri saved = Uri.fromFile(f);
                        prefs.edit().putString(KEY_PROFILE_URI, saved.toString()).apply();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getCircularBitmap(Bitmap src) {
        if (src == null) return null;
        int size = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - size) / 2;
        int y = (src.getHeight() - size) / 2;
        Bitmap squared = Bitmap.createBitmap(src, x, y, size, size);

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setShader(new BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));

        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);
        return output;
    }
}