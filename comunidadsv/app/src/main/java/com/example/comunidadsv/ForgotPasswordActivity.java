package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtEmail;
    private Button btnSend;
    private ProgressBar progressBar;
    private TextView txtBackToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtEmail = findViewById(R.id.edtEmail);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        txtBackToLogin = findViewById(R.id.txtBackToLogin);

        btnSend.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                edtEmail.setError("Correo requerido");
                edtEmail.requestFocus();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edtEmail.setError("Correo inválido");
                edtEmail.requestFocus();
                return;
            }
            new SendResetLinkTask().execute(email);
        });

        txtBackToLogin.setOnClickListener(v -> finish());
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class SendResetLinkTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";
        private String resetToken = "";
        private String userId = "";
        private String userEmail = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(android.view.View.VISIBLE);
            btnSend.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            userEmail = params[0];

            try {
                // 1. Buscar usuario por correo usando la vista
                String encodedCorreo = URLEncoder.encode("\"" + userEmail + "\"", "UTF-8");
                String searchUrl = Configuracion.SERVIDOR + "/db_usuarios/_design/usuarios/_view/por_correo?key=" + encodedCorreo;

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
                    JSONArray rows = respuesta.optJSONArray("rows");

                    if (rows != null && rows.length() > 0) {
                        JSONObject usuario = rows.getJSONObject(0).getJSONObject("value");
                        userId = usuario.getString("_id");

                        // 2. Obtener el documento completo
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

                        // 3. Generar token único
                        resetToken = UUID.randomUUID().toString();
                        long expires = System.currentTimeMillis() + 3600000; // 1 hora

                        // 4. Actualizar usuario con token
                        fullUser.put("reset_token", resetToken);
                        fullUser.put("reset_expires", expires);

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
                        errorMsg = "Correo no registrado";
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
            btnSend.setEnabled(true);

            if (success) {
                // Guardar token temporalmente
                SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
                prefs.edit()
                        .putString("reset_token", resetToken)
                        .putString("reset_email", userEmail)
                        .apply();

                // Abrir ResetPasswordActivity
                Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
                intent.putExtra("token", resetToken);
                intent.putExtra("email", userEmail);
                startActivity(intent);

                Toast.makeText(ForgotPasswordActivity.this,
                        "Token generado. Ahora puedes cambiar tu contraseña",
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(ForgotPasswordActivity.this,
                        "Error: " + errorMsg,
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}