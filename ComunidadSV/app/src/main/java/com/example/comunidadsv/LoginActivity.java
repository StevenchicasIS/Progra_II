package com.example.comunidadsv;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    Button btnLogin;
    TextView txtRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);

        btnLogin.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    FeedActivity.class
            );

            startActivity(intent);

        });

        txtRegister.setOnClickListener(v -> {

            Intent intent = new Intent(
                    LoginActivity.this,
                    RegisterActivity.class
            );

            startActivity(intent);

        });
    }
}