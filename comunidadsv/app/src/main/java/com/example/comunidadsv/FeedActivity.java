package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FeedActivity extends AppCompatActivity {

    private TextView tabParaTi, tabCercanos, tabSiguiendo;
    private LinearLayout navHome, navMap, navChat, navProfile;
    private FloatingActionButton fabAddPost;
    private TextView txtUserName; // Para mostrar el nombre del usuario

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        // Inicializar vistas
        tabParaTi = findViewById(R.id.tabParaTi);
        tabCercanos = findViewById(R.id.tabCercanos);
        tabSiguiendo = findViewById(R.id.tabSiguiendo);
        navHome = findViewById(R.id.navHome);
        navMap = findViewById(R.id.navMap);
        navChat = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);
        fabAddPost = findViewById(R.id.fabAddPost);

        // Opcional: si quieres mostrar el nombre en algún lugar, puedes agregar un TextView en el header
        // Por ahora lo usaremos para un Toast de bienvenida

        // Recuperar el nombre del usuario
        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");

        // Mostrar mensaje de bienvenida
        Toast.makeText(this, "Bienvenido " + nombre, Toast.LENGTH_SHORT).show();

        // Configurar TABS
        tabParaTi.setOnClickListener(v -> {
            setTabSelected(tabParaTi);
            Toast.makeText(this, "Mostrando publicaciones para ti", Toast.LENGTH_SHORT).show();
        });

        tabCercanos.setOnClickListener(v -> {
            setTabSelected(tabCercanos);
            Toast.makeText(this, "Mostrando publicaciones cercanas", Toast.LENGTH_SHORT).show();
        });

        tabSiguiendo.setOnClickListener(v -> {
            setTabSelected(tabSiguiendo);
            Toast.makeText(this, "Mostrando publicaciones que sigues", Toast.LENGTH_SHORT).show();
        });

        // Configurar Bottom Navigation
        navHome.setOnClickListener(v -> {
            // Ya estamos en Home
            Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show();
        });

        navMap.setOnClickListener(v -> {
            Toast.makeText(this, "Próximamente: Mapa", Toast.LENGTH_SHORT).show();
        });

        navChat.setOnClickListener(v -> {
            Toast.makeText(this, "Próximamente: Chat", Toast.LENGTH_SHORT).show();
        });

        navProfile.setOnClickListener(v -> {
            Toast.makeText(this, "Próximamente: Perfil", Toast.LENGTH_SHORT).show();
        });

        // Botón flotante para crear publicación
        fabAddPost.setOnClickListener(v -> {
            Toast.makeText(this, "Próximamente: Crear publicación", Toast.LENGTH_SHORT).show();
        });
    }

    private void setTabSelected(TextView selectedTab) {
        // Resetear todos los tabs
        tabParaTi.setTextColor(getColor(R.color.gray_text));
        tabCercanos.setTextColor(getColor(R.color.gray_text));
        tabSiguiendo.setTextColor(getColor(R.color.gray_text));

        // Poner normal a todos
        tabParaTi.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabCercanos.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabSiguiendo.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Seleccionar el tab clickeado
        selectedTab.setTextColor(getColor(R.color.green_primary));
        selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }
}