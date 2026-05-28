package com.example.comunidadsv;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.imageview.ShapeableImageView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class EditProfileActivity extends AppCompatActivity {

    private ShapeableImageView imgProfile;
    private EditText edtNombre, edtUbicacion;
    private Button btnGuardar;
    private ProgressBar progressBar;
    private String userId;
    private Uri selectedImageUri = null;
    private Bitmap selectedBitmap = null;
    private String imagenBase64 = "";
    private String currentFotoPerfil = "";
    private static final int REQUEST_PERMISSION_CODE = 100;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        imgProfile.setImageBitmap(selectedBitmap);
                        imagenBase64 = bitmapToBase64(selectedBitmap);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        imgProfile = findViewById(R.id.imgProfile);
        edtNombre = findViewById(R.id.edtNombre);
        edtUbicacion = findViewById(R.id.edtUbicacion);
        btnGuardar = findViewById(R.id.btnGuardar);
        progressBar = findViewById(R.id.progressBar);
        ImageView btnChangePhoto = findViewById(R.id.btnChangePhoto);

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        if (userId == null) {
            Toast.makeText(this, "Error: usuario no identificado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String currentName = prefs.getString("nombre", "");
        String currentLocation = prefs.getString("ubicacion", "");
        edtNombre.setText(currentName);
        edtUbicacion.setText(currentLocation);

        // CORREGIDO: Cargar foto desde CouchDB, no desde SharedPreferences
        cargarFotoDesdeCouchDB();

        btnChangePhoto.setOnClickListener(v -> checkPermissionAndOpenGallery());
        btnGuardar.setOnClickListener(v -> saveChanges());
    }

    // CORREGIDO: Nuevo método para cargar foto desde CouchDB
    private void cargarFotoDesdeCouchDB() {
        new CargarFotoTask().execute();
    }

    private class CargarFotoTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject user = new JSONObject(sb.toString());
                    return user.optString("fotoPerfil", "");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String fotoBase64) {
            currentFotoPerfil = fotoBase64;
            if (!fotoBase64.isEmpty()) {
                Bitmap bitmap = base64ToBitmap(fotoBase64);
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                    imagenBase64 = fotoBase64;
                } else {
                    imgProfile.setImageResource(R.drawable.ic_profile);
                }
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private void checkPermissionAndOpenGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_PERMISSION_CODE);
            } else {
                openGallery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSION_CODE);
            } else {
                openGallery();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Permiso denegado. No puedes cambiar la foto.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        int maxWidth = 500;
        int maxHeight = 500;
        if (bitmap.getWidth() > maxWidth || bitmap.getHeight() > maxHeight) {
            float ratio = Math.min((float) maxWidth / bitmap.getWidth(),
                    (float) maxHeight / bitmap.getHeight());
            int newWidth = Math.round(bitmap.getWidth() * ratio);
            int newHeight = Math.round(bitmap.getHeight() * ratio);
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] imageBytes = baos.toByteArray();
        return android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedString = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveChanges() {
        String newNombre = edtNombre.getText().toString().trim();
        String newUbicacion = edtUbicacion.getText().toString().trim();

        if (TextUtils.isEmpty(newNombre)) {
            edtNombre.setError("El nombre es obligatorio");
            edtNombre.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(newUbicacion)) {
            edtUbicacion.setError("La ubicación es obligatoria");
            edtUbicacion.requestFocus();
            return;
        }

        new UpdateProfileTask().execute(newNombre, newUbicacion, imagenBase64);
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class UpdateProfileTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";
        private String newNombre;
        private String newUbicacion;
        private String newFotoBase64;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnGuardar.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            newNombre = params[0];
            newUbicacion = params[1];
            newFotoBase64 = params[2];

            try {
                String getUrl = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
                URL url = new URL(getUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");

                int code = conn.getResponseCode();
                if (code != 200) {
                    errorMsg = "Error al obtener usuario: " + code;
                    return false;
                }

                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject userDoc = new JSONObject(sb.toString());

                userDoc.put("nombre", newNombre);
                userDoc.put("ubicacion", newUbicacion);
                if (!newFotoBase64.isEmpty()) {
                    userDoc.put("fotoPerfil", newFotoBase64);
                }

                String putUrl = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
                HttpURLConnection putConn = (HttpURLConnection) new URL(putUrl).openConnection();
                setBasicAuth(putConn);
                putConn.setRequestMethod("PUT");
                putConn.setRequestProperty("Content-Type", "application/json");
                putConn.setDoOutput(true);

                OutputStream os = putConn.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                writer.write(userDoc.toString());
                writer.flush();
                writer.close();
                os.close();

                int updateCode = putConn.getResponseCode();
                return updateCode == 201 || updateCode == 202;

            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            btnGuardar.setEnabled(true);

            if (success) {
                // CORREGIDO: Actualizar SharedPreferences solo con nombre y ubicación, NO con la foto
                SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("nombre", newNombre);
                editor.putString("ubicacion", newUbicacion);
                // IMPORTANTE: NO guardar la foto en SharedPreferences
                editor.apply();

                Toast.makeText(EditProfileActivity.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                resultIntent.putExtra("perfil_actualizado", true);
                setResult(RESULT_OK, resultIntent);

                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}