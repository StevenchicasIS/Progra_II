package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FeedActivity extends AppCompatActivity {

    private TextView txtUserName, txtWelcomeMessage;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        txtUserName = findViewById(R.id.txtUserName);
        txtWelcomeMessage = findViewById(R.id.txtWelcomeMessage);
        btnLogout = findViewById(R.id.btnLogout);

        // Recuperar el nombre del usuario
        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");

        // Mostrar en el feed
        txtUserName.setText(nombre);
        txtWelcomeMessage.setText("Bienvenido a ComunidadSV, " + nombre);

        // Botón de cerrar sesión (provisional)
        btnLogout.setOnClickListener(v -> {
            // Limpiar preferencias de sesión
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            // Mostrar mensaje
            Toast.makeText(FeedActivity.this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

            // Redirigir al LoginActivity
            Intent intent = new Intent(FeedActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}