package com.example.comunidadsv;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class WelcomeActivity extends AppCompatActivity {

    Button btnComenzar;
    TextView txtLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnComenzar = findViewById(R.id.btnComenzar);
        txtLogin = findViewById(R.id.txtLogin);

        btnComenzar.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            WelcomeActivity.this,
                            RegisterActivity.class
                    )
            );

        });

        txtLogin.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            WelcomeActivity.this,
                            LoginActivity.class
                    )
            );

        });
    }
}