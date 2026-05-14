package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;

public class LoginActivity extends AppCompatActivity {

    private EditText edtEmail, edtPassword;
    private Button btnLogin;
    private TextView txtRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtRegister = findViewById(R.id.txtRegister);

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                edtEmail.setError("Requerido");
                edtEmail.requestFocus();
                return;
            }
            if (TextUtils.isEmpty(password)) {
                edtPassword.setError("Requerido");
                edtPassword.requestFocus();
                return;
            }
            new LoginTask().execute(email, password);
        });

        txtRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
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

    private class LoginTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";
        private String nombreUsuario = "";

        @Override
        protected Boolean doInBackground(String... params) {
            String correo = params[0];
            String passwordHash = sha256(params[1]);
            try {
                String encodedCorreo = URLEncoder.encode("\"" + correo + "\"", "UTF-8");
                String urlStr = Configuracion.SERVIDOR + "/db_usuarios/_design/usuarios/_view/por_correo?key=" + encodedCorreo;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject respuesta = new JSONObject(sb.toString());
                    JSONArray rows = respuesta.optJSONArray("rows");
                    if (rows != null && rows.length() > 0) {
                        JSONObject usuario = rows.getJSONObject(0).getJSONObject("value");
                        String storedPass = usuario.getString("password");
                        if (storedPass.equals(passwordHash)) {
                            nombreUsuario = usuario.getString("nombre");
                            return true;
                        } else {
                            errorMsg = "Contraseña incorrecta";
                            return false;
                        }
                    } else {
                        errorMsg = "Correo no registrado";
                        return false;
                    }
                } else {
                    errorMsg = "Error HTTP: " + code;
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
                // Guardar sesión y nombre
                SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
                prefs.edit().putBoolean("logueado", true)
                        .putString("nombre", nombreUsuario)
                        .apply();

                // NOTIFICACIÓN DE BIENVENIDA (TOAST)
                Toast.makeText(LoginActivity.this, "¡Bienvenido " + nombreUsuario + "!", Toast.LENGTH_LONG).show();

                // Ir al FeedActivity
                startActivity(new Intent(LoginActivity.this, FeedActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        }
    }
}