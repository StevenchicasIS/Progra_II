package com.example.comunidadsv;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtName, edtEmail, edtPassword, edtUbicacion;
    private Button btnRegister;
    private TextView txtLogin;
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtUbicacion = findViewById(R.id.edtUbicacion);
        btnRegister = findViewById(R.id.btnRegister);
        txtLogin = findViewById(R.id.txtLogin);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        // Toggle para mostrar/ocultar contraseña
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Ocultar contraseña
                edtPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_eye_closed);
                isPasswordVisible = false;
            } else {
                // Mostrar contraseña
                edtPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_eye_open);
                isPasswordVisible = true;
            }
            // Mantener el cursor al final del texto
            edtPassword.setSelection(edtPassword.getText().length());
        });

        btnRegister.setOnClickListener(v -> {
            if (validarCampos()) {
                String nombre = edtName.getText().toString().trim();
                String correo = edtEmail.getText().toString().trim();
                String password = edtPassword.getText().toString().trim();
                String ubicacion = edtUbicacion.getText().toString().trim();
                new RegistrarUsuarioTask().execute(nombre, correo, password, ubicacion);
            }
        });

        txtLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
        });
    }

    private boolean validarCampos() {
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();
        String ubicacion = edtUbicacion.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            edtName.setError("Nombre obligatorio");
            edtName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Correo obligatorio");
            edtEmail.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.setError("Correo inválido");
            edtEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Contraseña obligatoria");
            edtPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            edtPassword.setError("Mínimo 6 caracteres");
            edtPassword.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(ubicacion)) {
            edtUbicacion.setError("¿Dónde vives?");
            edtUbicacion.requestFocus();
            return false;
        }
        return true;
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return input;
        }
    }

    private class RegistrarUsuarioTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";

        @Override
        protected Boolean doInBackground(String... params) {
            String nombre = params[0];
            String correo = params[1];
            String passwordHash = sha256(params[2]);
            String ubicacion = params[3];

            try {
                JSONObject usuario = new JSONObject();
                usuario.put("tipo", "usuario");
                usuario.put("nombre", nombre);
                usuario.put("correo", correo);
                usuario.put("password", passwordHash);
                usuario.put("ubicacion", ubicacion);
                usuario.put("reset_token", JSONObject.NULL);
                usuario.put("reset_expires", JSONObject.NULL);

                URL url = new URL(Configuracion.SERVIDOR + "/db_usuarios");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(usuario.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == 201) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) result.append(line);
                    JSONObject resp = new JSONObject(result.toString());
                    return resp.optBoolean("ok", false);
                } else {
                    InputStream errorIn = conn.getErrorStream();
                    if (errorIn != null) {
                        BufferedReader errorReader = new BufferedReader(new InputStreamReader(errorIn));
                        StringBuilder errorSb = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorReader.readLine()) != null) errorSb.append(errorLine);
                        errorMsg = errorSb.toString();
                    } else {
                        errorMsg = "Error código: " + responseCode;
                    }
                    return false;
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(RegisterActivity.this, "Registro exitoso. Inicia sesión", Toast.LENGTH_LONG).show();
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(RegisterActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        }
    }
}