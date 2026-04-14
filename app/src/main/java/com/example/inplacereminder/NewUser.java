package com.example.inplacereminder;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.icu.util.Calendar;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;

public class NewUser extends AppCompatActivity implements View.OnClickListener {

    private static final String IMAGE_DIRECTORY = "/demonuts";
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_GALLERY = 2;
    private static final String AUTHORITY = "com.example.firestorepicapplication.fileprovider";
    private int GALLERY = 1, CAMERA = 2;

    // SharedPreferences keys
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_PROFILE_URI = "profile_uri";

    // NEW: pendingPicture holds the image bytes (JPEG) selected by the user.
    // These bytes are stored and later passed to insertUser(...) so the picture
    // is saved in the DB together with the other user info.
    private byte[] pendingPicture = null; // NEW

    private long currentUserId = -1; // holds the logged-in user's id saved at login (reads from SharedPreferences in onCreate)

    // NEW: Do not initialize helper with 'this' in field declaration (avoid using context before onCreate).
    DB_OpenHelper helper; // NEW

    Button btnImage2, btnRegister;
    Button btnGallery, btnCamera;
    EditText etName2, etPassword2;
    Dialog dialog;
    ImageView imageView;
    ImageButton ibPicture;
    ImageButton ib_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_user);

        // NEW: initialize DB helper here using valid context
        helper = new DB_OpenHelper(this); // NEW

        btnRegister = findViewById(R.id.btnRegister);
        etName2 = findViewById(R.id.etName2);
        etPassword2 = findViewById(R.id.etPassword2);

        ibPicture = findViewById(R.id.ibProfile);
        // initialize imageView so it's not null when setting bitmaps
        imageView = findViewById(R.id.ibProfile); // ImageButton is a subclass of ImageView

        ib_back = findViewById(R.id.ib_back);
        dialog = new Dialog(NewUser.this);
        dialog.setContentView(R.layout.picture_dialog);
        btnGallery = dialog.findViewById(R.id.btnGallery);
        btnCamera = dialog.findViewById(R.id.btnCamera);

        ib_back.setOnClickListener(v -> finish());
        EdgeToEdge.enable(this);
        btnRegister.setOnClickListener(this);
        ibPicture.setOnClickListener(this);
        btnGallery.setOnClickListener(this);
        btnCamera.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {

        if (view == ibPicture) {
            Toast.makeText(NewUser.this, "Image Button clicked!", Toast.LENGTH_SHORT).show();
            dialog.setCancelable(true);
            dialog.show();
        }
        if (view == btnGallery) {
            choosePhotoFromGallery();
            dialog.dismiss();
        }
        if (view == btnCamera) {
            takePhotoFromCamera();
            dialog.dismiss();
        }

        if (view == btnRegister) {
            String name = etName2.getText().toString();
            String password = etPassword2.getText().toString();

            if (!isNameValid(name)) {
                Toast.makeText(NewUser.this, "Name must be between 2 and 15 characters", Toast.LENGTH_SHORT).show();
            } else if (!isPasswordStrong(password)) {
                Toast.makeText(NewUser.this, "Password must be 8-20 characters, no spaces, at least one lower, upper, digit and special", Toast.LENGTH_LONG).show();
            } else if (helper.userExists(etName2.getText().toString())) {
                Toast.makeText(NewUser.this, "User name already exists, please choose another", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(NewUser.this, "Registered Successfully!", Toast.LENGTH_SHORT).show();
                name = etName2.getText().toString();
                password = etPassword2.getText().toString();
                String hashPassword = Integer.toString(password.hashCode());

                // NEW: pass pendingPicture (may be null) so the user's picture is stored in DB during registration
                helper.insertUser(name, hashPassword, pendingPicture); // NEW

                Intent intent = new Intent(NewUser.this, WelcomePage.class);
                startActivity(intent);
            }
        }
    }

    private void requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void setRequestStoragePermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        }
    }

    private void choosePhotoFromGallery() {
        setRequestStoragePermission(); // request storage permission for gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY);
    }

    private void takePhotoFromCamera() {
        requestCameraPermission(); // request camera permission for camera
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                if (contentURI != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                        String path = saveImage(bitmap);
                        Toast.makeText(this, "Image Saved!", Toast.LENGTH_SHORT).show();
                        if (imageView != null) imageView.setImageBitmap(bitmap);

                        // NEW: store JPEG bytes in pendingPicture so it can be saved to DB on registration
                        pendingPicture = bitmapToBytes(bitmap); // NEW

                        // save profile URI to SharedPreferences as file: URI
                        if (path != null && !path.isEmpty()) {
                            Uri fileUri = Uri.fromFile(new File(path));
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                    .edit()
                                    .putString(KEY_PROFILE_URI, fileUri.toString())
                                    .apply();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
        if (requestCode == CAMERA) {
            if (data != null && data.getExtras() != null) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                if (thumbnail != null) {
                    if (imageView != null)
                        imageView.setImageBitmap(helper.getCircularBitmap(thumbnail));
                    String savedPath = saveImage(thumbnail);
                    Toast.makeText(this, "Image Saved!", Toast.LENGTH_SHORT).show();

                    // NEW: store JPEG bytes in pendingPicture so it can be saved to DB on registration
                    pendingPicture = bitmapToBytes(thumbnail); // NEW

                    // save profile URI to SharedPreferences as file: URI
                    if (savedPath != null && !savedPath.isEmpty()) {
                        Uri fileUri = Uri.fromFile(new File(savedPath));
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_PROFILE_URI, fileUri.toString())
                                .apply();
                    }
                }
            }
        }
    }

    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File wallpaperDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs();
        }

        try {
            File f = new File(wallpaperDirectory, MessageFormat.format("{0}.jpg", Calendar.getInstance().getTimeInMillis()));
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());
            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    // optional helper to expose the id
    public long getCurrentUserId() {
        return currentUserId; // NEW
    }

    // NEW: convert a Bitmap into JPEG byte[] so it can be stored in DB as BLOB
    private byte[] bitmapToBytes(Bitmap bmp) { // NEW
        if (bmp == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        return baos.toByteArray();
    } // NEW

    private boolean isPasswordStrong(String pwd) {
        if (pwd == null) return false;
        if (pwd.length() < 8 || pwd.length() > 20) {

            return false;
        }
        if (pwd.contains(" ")) return false;

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : pwd.toCharArray()) {
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasLower && hasUpper && hasDigit && hasSpecial;
    }

    private boolean isNameValid(String name) {
        return name != null && name.length() >= 2 && name.length() <= 15;
    }
}