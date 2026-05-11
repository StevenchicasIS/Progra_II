package com.example.comunidadsv;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    Button btnRegister;
    TextView txtLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);

        btnRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    RegisterActivity.this,
                    FeedActivity.class
            );

            startActivity(intent);

        });

        txtLogin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    RegisterActivity.this,
                    LoginActivity.class
            );

            startActivity(intent);

        });
    }
}