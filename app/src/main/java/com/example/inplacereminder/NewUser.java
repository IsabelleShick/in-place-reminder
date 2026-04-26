package com.example.inplacereminder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class NewUser extends AppCompatActivity implements View.OnClickListener {

    private long currentUserId = -1; // holds the logged-in user's id saved at login (reads from SharedPreferences in onCreate)

    // NEW: Do not initialize helper with 'this' in field declaration (avoid using context before onCreate).
    DB_OpenHelper helper; // NEW

    Button btnRegister;
    EditText etName2, etPassword2;
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

        ib_back = findViewById(R.id.ib_back);

        ib_back.setOnClickListener(v -> finish());
        EdgeToEdge.enable(this);
        btnRegister.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
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

                // Insert user without picture
                helper.insertUser(name, hashPassword, null);

                Intent intent = new Intent(NewUser.this, WelcomePage.class);
                startActivity(intent);
            }
        }
    }

    // optional helper to expose the id
    public long getCurrentUserId() {
        return currentUserId; // NEW
    }


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