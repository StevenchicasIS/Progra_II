package com.example.comunidadsv;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.google.android.material.imageview.ShapeableImageView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    try {
                        selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                        imgProfile.setImageBitmap(selectedBitmap);
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

        // Cargar foto guardada
        File photoFile = new File(getFilesDir(), "profile_" + userId + ".jpg");
        if (photoFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            if (bitmap != null) imgProfile.setImageBitmap(bitmap);
        }

        btnChangePhoto.setOnClickListener(v -> openGallery());
        btnGuardar.setOnClickListener(v -> saveChanges());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
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

        if (selectedBitmap != null) {
            try {
                File photoFile = new File(getFilesDir(), "profile_" + userId + ".jpg");
                FileOutputStream fos = new FileOutputStream(photoFile);
                selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al guardar la foto", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new UpdateProfileTask().execute(newNombre, newUbicacion);
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class UpdateProfileTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnGuardar.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String nombre = params[0];
            String ubicacion = params[1];

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

                userDoc.put("nombre", nombre);
                userDoc.put("ubicacion", ubicacion);

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
                if (updateCode == 201 || updateCode == 202) {
                    return true;
                } else {
                    errorMsg = "Error al actualizar: " + updateCode;
                    return false;
                }
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
                SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("nombre", edtNombre.getText().toString().trim());
                editor.putString("ubicacion", edtUbicacion.getText().toString().trim());
                editor.apply();

                Toast.makeText(EditProfileActivity.this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(EditProfileActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}