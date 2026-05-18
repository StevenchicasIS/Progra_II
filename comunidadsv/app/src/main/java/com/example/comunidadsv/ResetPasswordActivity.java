package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText edtNewPassword, edtConfirmPassword;
    private Button btnReset;
    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private ProgressBar progressBar;
    private boolean isPasswordVisible = false;
    private boolean isConfirmVisible = false;
    private String resetToken;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnReset = findViewById(R.id.btnReset);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        progressBar = findViewById(R.id.progressBar);

        // Recuperar token
        resetToken = getIntent().getStringExtra("token");
        userEmail = getIntent().getStringExtra("email");

        if (resetToken == null || userEmail == null) {
            SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
            resetToken = prefs.getString("reset_token", null);
            userEmail = prefs.getString("reset_email", null);
        }

        if (resetToken == null || userEmail == null) {
            Toast.makeText(this, "Enlace inválido o expirado", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Toggle para nueva contraseña
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                edtNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                edtNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivTogglePassword.setImageResource(R.drawable.ic_eye_open);
            }
            isPasswordVisible = !isPasswordVisible;
            edtNewPassword.setSelection(edtNewPassword.getText().length());
        });

        // Toggle para confirmar contraseña
        ivToggleConfirmPassword.setOnClickListener(v -> {
            if (isConfirmVisible) {
                edtConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_closed);
            } else {
                edtConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_open);
            }
            isConfirmVisible = !isConfirmVisible;
            edtConfirmPassword.setSelection(edtConfirmPassword.getText().length());
        });

        btnReset.setOnClickListener(v -> {
            String newPass = edtNewPassword.getText().toString().trim();
            String confirmPass = edtConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(newPass)) {
                edtNewPassword.setError("Nueva contraseña requerida");
                return;
            }
            if (newPass.length() < 6) {
                edtNewPassword.setError("Mínimo 6 caracteres");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                edtConfirmPassword.setError("Las contraseñas no coinciden");
                return;
            }

            new ResetPasswordTask().execute(newPass);
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

    private class ResetPasswordTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(android.view.View.VISIBLE);
            btnReset.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String newPasswordHash = sha256(params[0]);

            try {
                // Buscar usuario por correo
                String searchUrl = Configuracion.SERVIDOR + "/db_usuarios/_design/usuarios/_view/por_correo?key=\"" + userEmail + "\"";
                URL url = new URL(searchUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    JSONObject respuesta = new JSONObject(sb.toString());
                    org.json.JSONArray rows = respuesta.optJSONArray("rows");

                    if (rows != null && rows.length() > 0) {
                        JSONObject usuario = rows.getJSONObject(0).getJSONObject("value");
                        String userId = usuario.getString("_id");

                        // Obtener documento completo
                        String getUrl = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
                        URL getUrlObj = new URL(getUrl);
                        HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                        setBasicAuth(getConn);
                        getConn.setRequestMethod("GET");

                        InputStream getIn = getConn.getInputStream();
                        BufferedReader getReader = new BufferedReader(new InputStreamReader(getIn));
                        StringBuilder getSb = new StringBuilder();
                        while ((line = getReader.readLine()) != null) getSb.append(line);
                        JSONObject fullUser = new JSONObject(getSb.toString());

                        // Verificar token
                        String storedToken = fullUser.optString("reset_token");
                        long expires = fullUser.optLong("reset_expires", 0);

                        if (storedToken == null || storedToken.equals("null") || !storedToken.equals(resetToken)) {
                            errorMsg = "Token inválido";
                            return false;
                        }

                        if (System.currentTimeMillis() > expires) {
                            errorMsg = "Token expirado. Solicita un nuevo enlace";
                            return false;
                        }

                        // Actualizar contraseña y limpiar token
                        fullUser.put("password", newPasswordHash);
                        fullUser.put("reset_token", JSONObject.NULL);
                        fullUser.put("reset_expires", JSONObject.NULL);

                        String updateUrl = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
                        URL updateUrlObj = new URL(updateUrl);
                        HttpURLConnection updateConn = (HttpURLConnection) updateUrlObj.openConnection();
                        setBasicAuth(updateConn);
                        updateConn.setRequestMethod("PUT");
                        updateConn.setRequestProperty("Content-Type", "application/json");
                        updateConn.setDoOutput(true);

                        OutputStream os = updateConn.getOutputStream();
                        OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                        writer.write(fullUser.toString());
                        writer.flush();
                        writer.close();
                        os.close();

                        int updateCode = updateConn.getResponseCode();
                        return updateCode == 201 || updateCode == 202;
                    } else {
                        errorMsg = "Usuario no encontrado";
                        return false;
                    }
                } else {
                    errorMsg = "Error de conexión: " + code;
                    return false;
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(android.view.View.GONE);
            btnReset.setEnabled(true);

            if (success) {
                // Limpiar SharedPreferences
                getSharedPreferences("ComunidadSV", MODE_PRIVATE)
                        .edit()
                        .remove("reset_token")
                        .remove("reset_email")
                        .apply();

                Toast.makeText(ResetPasswordActivity.this,
                        "¡Contraseña actualizada exitosamente!",
                        Toast.LENGTH_LONG).show();

                startActivity(new Intent(ResetPasswordActivity.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(ResetPasswordActivity.this,
                        "Error: " + errorMsg,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}