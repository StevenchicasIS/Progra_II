package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtNombre, txtUbicacion;
    private TextView txtPublicacionesCount, txtSeguidoresCount, txtSeguidosCount;
    private Button btnEditarPerfil;
    private ImageView imgProfile;
    private ImageView btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        txtNombre = findViewById(R.id.txtNombre);
        txtUbicacion = findViewById(R.id.txtUbicacion);
        txtPublicacionesCount = findViewById(R.id.txtPublicacionesCount);
        txtSeguidoresCount = findViewById(R.id.txtSeguidoresCount);
        txtSeguidosCount = findViewById(R.id.txtSeguidosCount);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        imgProfile = findViewById(R.id.imgProfile);
        btnLogout = findViewById(R.id.btnLogout);

        loadUserData();
        txtPublicacionesCount.setText("0");
        txtSeguidoresCount.setText("0");
        txtSeguidosCount.setText("0");

        btnEditarPerfil.setOnClickListener(v ->
                startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class))
        );

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");
        String ubicacion = prefs.getString("ubicacion", "Sin ubicación");
        String userId = prefs.getString("userId", "");

        txtNombre.setText(nombre);
        txtUbicacion.setText(ubicacion);

        if (!userId.isEmpty()) {
            File photoFile = new File(getFilesDir(), "profile_" + userId + ".jpg");
            if (photoFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                } else {
                    imgProfile.setImageResource(R.drawable.ic_profile);
                }
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile);
            }
        } else {
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cerrar sesión");
        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?");
        builder.setPositiveButton("Sí, cerrar", (dialog, which) -> logout());
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}