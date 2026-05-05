package com.example.inplacereminder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // AIzaSyAKR6a7-rct5GaA1fBdRxN73EDMRDS-470

    EditText etName, etPassword;
    Button btnLogin, btnNewAccount, btnAbout;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnLogin = findViewById(R.id.btnLogin);
        btnNewAccount = findViewById(R.id.btnNewAccount);
        btnAbout = findViewById(R.id.btnAbout);

        etName = findViewById(R.id.etName);
        etPassword = findViewById(R.id.etPassword);

        btnLogin.setOnClickListener(this);
        btnNewAccount.setOnClickListener(this);
        btnAbout.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == btnNewAccount) {
            Intent intent = new Intent(MainActivity.this, NewUser.class);
            startActivity(intent);
        }
        if (view == btnAbout) {
            Intent intent = new Intent(MainActivity.this, About.class);
            startActivity(intent);
        }

        if (view == btnLogin) {
            Toast.makeText(MainActivity.this, "Button clicked!", Toast.LENGTH_SHORT).show();

            String name = etName.getText().toString();
            String password = etPassword.getText().toString();
            String hashPassword = Integer.toString(password.hashCode());
            System.out.println("trying to connect with user name" + name + "and hash password " + hashPassword);
//                      DB_OpenHelper helper = new DB_OpenHelper(MainActivity.this);

            try {
                DB_OpenHelper helper = new DB_OpenHelper(MainActivity.this);
                SQLiteDatabase db = helper.getReadableDatabase();

                long count = DatabaseUtils.longForQuery(
                        db,
                        "SELECT COUNT(1) FROM users WHERE name = ? AND password_hash = ?",
                        new String[]{name, hashPassword}
                );

                if (count > 0) {
                    // query id and picture in one shot
                    android.database.Cursor c = null;
                    try {
                        c = db.rawQuery(
                                "SELECT id, picture FROM users WHERE name = ? AND password_hash = ?",
                                new String[]{name, hashPassword}
                        );
                        if (c != null && c.moveToFirst()) {
                            long userId = c.getLong(0);
                            byte[] pictureBytes = null;
                            if (!c.isNull(1)) {
                                pictureBytes = c.getBlob(1);
                            }

                            // save to SharedPreferences
                            prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            prefs.edit()
                                    .putLong("current_user_id", userId)
                                    .putString("current_user", name)
                                    .apply();

                            // store globally in Application
                            MyApplication app = (MyApplication) getApplication();
                            app.setCurrentUserId(userId);
                            app.setCurrentUserPictureBytes(pictureBytes);

                            Toast.makeText(MainActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, WelcomePage.class));
                        } else {
                            Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                        }
                    } finally {
                        if (c != null) c.close();
                        db.close();
                    }
                } else {
                    db.close();
                    Toast.makeText(MainActivity.this, "Invalid credentials", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                android.util.Log.e("DB_QUERY", "Query failed", e);
                Toast.makeText(MainActivity.this, "Database error", Toast.LENGTH_LONG).show();
            }
        }
    }
}