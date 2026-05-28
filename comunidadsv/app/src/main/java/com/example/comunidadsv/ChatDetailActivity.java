package com.example.comunidadsv;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ChatDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText edtMensaje;
    private ImageView btnEnviar;
    private ProgressBar progressBar;
    private TextView txtNombreUsuario;
    private ImageView imgFoto;

    private MensajeAdapter mensajeAdapter;
    private List<Mensaje> mensajesList;
    private String chatId;
    private String otroUsuarioId;
    private String otroUsuarioNombre;
    private String otroUsuarioFoto;
    private String currentUserId;
    private String currentUserNombre;

    private Handler handler = new Handler();
    private Runnable refreshRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        edtMensaje = findViewById(R.id.edtMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        progressBar = findViewById(R.id.progressBar);
        txtNombreUsuario = findViewById(R.id.txtNombreUsuario);
        imgFoto = findViewById(R.id.imgFoto);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mensajesList = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");
        currentUserNombre = prefs.getString("nombre", "Usuario");

        chatId = getIntent().getStringExtra("chatId");
        otroUsuarioId = getIntent().getStringExtra("otroUsuarioId");
        otroUsuarioNombre = getIntent().getStringExtra("otroUsuarioNombre");
        otroUsuarioFoto = getIntent().getStringExtra("otroUsuarioFoto");

        txtNombreUsuario.setText(otroUsuarioNombre);

        // Cargar foto del otro usuario
        if (!otroUsuarioFoto.isEmpty()) {
            android.graphics.Bitmap bitmap = ImageUtils.base64ToBitmap(otroUsuarioFoto);
            if (bitmap != null) {
                imgFoto.setImageBitmap(bitmap);
            } else {
                imgFoto.setImageResource(R.drawable.ic_profile);
            }
        } else {
            imgFoto.setImageResource(R.drawable.ic_profile);
        }

        mensajeAdapter = new MensajeAdapter(this, mensajesList, currentUserId);
        recyclerView.setAdapter(mensajeAdapter);

        loadMensajes();

        // Botón de enviar como ImageView
        btnEnviar.setOnClickListener(v -> {
            String texto = edtMensaje.getText().toString().trim();
            if (!texto.isEmpty()) {
                enviarMensaje(texto);
                edtMensaje.setText("");
            }
        });

        // Marcar chat como leído al abrir
        marcarChatComoLeido();

        // Auto-refresh cada 3 segundos
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadMensajes();
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(refreshRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
    }

    private void loadMensajes() {
        new LoadMensajesTask().execute();
    }

    private void enviarMensaje(String texto) {
        new EnviarMensajeTask().execute(texto);
    }

    private void marcarChatComoLeido() {
        new MarcarChatLeidoTask().execute();
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class MarcarChatLeidoTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String getUrl = Configuracion.SERVIDOR + "/db_chats/" + chatId;
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(15000);
                getConn.setReadTimeout(15000);

                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject doc = new JSONObject(sb.toString());

                    // Resetear contador del usuario actual
                    if (currentUserId.equals(doc.optString("usuario1Id"))) {
                        doc.put("noLeidosUsuario1", 0);
                    } else {
                        doc.put("noLeidosUsuario2", 0);
                    }

                    String rev = doc.getString("_rev");
                    String putUrl = Configuracion.SERVIDOR + "/db_chats/" + chatId + "?rev=" + rev;
                    URL putUrlObj = new URL(putUrl);
                    HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                    setBasicAuth(putConn);
                    putConn.setRequestMethod("PUT");
                    putConn.setRequestProperty("Content-Type", "application/json");
                    putConn.setDoOutput(true);
                    putConn.setConnectTimeout(15000);
                    putConn.setReadTimeout(15000);

                    OutputStream os = putConn.getOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                    writer.write(doc.toString());
                    writer.flush();
                    writer.close();
                    os.close();

                    return putConn.getResponseCode() == 201 || putConn.getResponseCode() == 202;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private class LoadMensajesTask extends AsyncTask<Void, Void, List<Mensaje>> {
        @Override
        protected List<Mensaje> doInBackground(Void... voids) {
            List<Mensaje> lista = new ArrayList<>();
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_mensajes/_design/mensajes/_view/por_chat?key=\"" + chatId + "\"&descending=false";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    JSONObject response = new JSONObject(sb.toString());
                    JSONArray rows = response.optJSONArray("rows");

                    if (rows != null) {
                        for (int i = 0; i < rows.length(); i++) {
                            JSONObject row = rows.getJSONObject(i);
                            JSONObject doc = row.getJSONObject("value");
                            Mensaje mensaje = Mensaje.fromJSON(doc);
                            lista.add(mensaje);

                            // Marcar como leído si es necesario
                            if (!mensaje.isLeido() && !mensaje.getEmisorId().equals(currentUserId)) {
                                marcarMensajeComoLeido(mensaje.getId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lista;
        }

        private void marcarMensajeComoLeido(String mensajeId) {
            try {
                String getUrl = Configuracion.SERVIDOR + "/db_mensajes/" + mensajeId;
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(15000);
                getConn.setReadTimeout(15000);

                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject doc = new JSONObject(sb.toString());

                    doc.put("leido", true);
                    String rev = doc.getString("_rev");

                    String putUrl = Configuracion.SERVIDOR + "/db_mensajes/" + mensajeId + "?rev=" + rev;
                    URL putUrlObj = new URL(putUrl);
                    HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                    setBasicAuth(putConn);
                    putConn.setRequestMethod("PUT");
                    putConn.setRequestProperty("Content-Type", "application/json");
                    putConn.setDoOutput(true);
                    putConn.setConnectTimeout(15000);
                    putConn.setReadTimeout(15000);

                    OutputStream os = putConn.getOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                    writer.write(doc.toString());
                    writer.flush();
                    writer.close();
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(List<Mensaje> result) {
            progressBar.setVisibility(View.GONE);
            if (result != null) {
                mensajesList.clear();
                mensajesList.addAll(result);
                mensajeAdapter.notifyDataSetChanged();
                if (mensajesList.size() > 0) {
                    recyclerView.scrollToPosition(mensajesList.size() - 1);
                }
            }
        }
    }

    private class EnviarMensajeTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";

        @Override
        protected Boolean doInBackground(String... params) {
            String texto = params[0];

            try {
                Mensaje mensaje = new Mensaje(chatId, currentUserId, currentUserNombre, texto);
                String urlStr = Configuracion.SERVIDOR + "/db_mensajes/" + mensaje.getId();
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(mensaje.toJSON().toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();
                boolean success = responseCode == 201 || responseCode == 202;

                if (success) {
                    // Actualizar último mensaje en el chat
                    actualizarUltimoMensaje(texto);
                    // Incrementar contador de no leídos para el otro usuario
                    incrementarNoLeidos();
                }

                return success;
            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        private void actualizarUltimoMensaje(String texto) {
            try {
                String getUrl = Configuracion.SERVIDOR + "/db_chats/" + chatId;
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(15000);
                getConn.setReadTimeout(15000);

                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject doc = new JSONObject(sb.toString());

                    doc.put("ultimoMensaje", texto);
                    doc.put("ultimoMensajeFecha", System.currentTimeMillis());
                    doc.put("ultimoMensajeRemitenteId", currentUserId);

                    String rev = doc.getString("_rev");
                    String putUrl = Configuracion.SERVIDOR + "/db_chats/" + chatId + "?rev=" + rev;
                    URL putUrlObj = new URL(putUrl);
                    HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                    setBasicAuth(putConn);
                    putConn.setRequestMethod("PUT");
                    putConn.setRequestProperty("Content-Type", "application/json");
                    putConn.setDoOutput(true);
                    putConn.setConnectTimeout(15000);
                    putConn.setReadTimeout(15000);

                    OutputStream os = putConn.getOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                    writer.write(doc.toString());
                    writer.flush();
                    writer.close();
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void incrementarNoLeidos() {
            try {
                String getUrl = Configuracion.SERVIDOR + "/db_chats/" + chatId;
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(15000);
                getConn.setReadTimeout(15000);

                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject doc = new JSONObject(sb.toString());

                    // Incrementar contador del otro usuario
                    if (otroUsuarioId.equals(doc.optString("usuario1Id"))) {
                        int current = doc.optInt("noLeidosUsuario1", 0);
                        doc.put("noLeidosUsuario1", current + 1);
                    } else {
                        int current = doc.optInt("noLeidosUsuario2", 0);
                        doc.put("noLeidosUsuario2", current + 1);
                    }

                    String rev = doc.getString("_rev");
                    String putUrl = Configuracion.SERVIDOR + "/db_chats/" + chatId + "?rev=" + rev;
                    URL putUrlObj = new URL(putUrl);
                    HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                    setBasicAuth(putConn);
                    putConn.setRequestMethod("PUT");
                    putConn.setRequestProperty("Content-Type", "application/json");
                    putConn.setDoOutput(true);
                    putConn.setConnectTimeout(15000);
                    putConn.setReadTimeout(15000);

                    OutputStream os = putConn.getOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                    writer.write(doc.toString());
                    writer.flush();
                    writer.close();
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                loadMensajes();
            } else {
                Toast.makeText(ChatDetailActivity.this, "Error al enviar mensaje: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}