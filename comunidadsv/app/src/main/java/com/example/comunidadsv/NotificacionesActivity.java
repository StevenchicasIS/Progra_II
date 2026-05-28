package com.example.comunidadsv;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class NotificacionesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutNoNotificaciones;
    private NotificacionAdapter adapter;
    private List<Notificacion> notificacionesList;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle("Notificaciones");

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        layoutNoNotificaciones = findViewById(R.id.layoutNoNotificaciones);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificacionesList = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        adapter = new NotificacionAdapter(this, notificacionesList, currentUserId);
        recyclerView.setAdapter(adapter);

        loadNotificaciones();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotificaciones();
    }

    private void loadNotificaciones() {
        new LoadNotificacionesTask().execute();
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class LoadNotificacionesTask extends AsyncTask<Void, Void, List<Notificacion>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Notificacion> doInBackground(Void... voids) {
            List<Notificacion> lista = new ArrayList<>();
            try {
                String encodedUserId = URLEncoder.encode("\"" + currentUserId + "\"", "UTF-8");
                String urlStr = Configuracion.SERVIDOR + "/db_notificaciones/_design/notificaciones/_view/por_usuario?key=" + encodedUserId + "&descending=true";
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
                            Notificacion notificacion = Notificacion.fromJSON(doc);
                            lista.add(notificacion);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lista;
        }

        @Override
        protected void onPostExecute(List<Notificacion> result) {
            progressBar.setVisibility(View.GONE);
            if (!result.isEmpty()) {
                notificacionesList.clear();
                notificacionesList.addAll(result);
                adapter.notifyDataSetChanged();
                recyclerView.setVisibility(View.VISIBLE);
                layoutNoNotificaciones.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                layoutNoNotificaciones.setVisibility(View.VISIBLE);
            }
        }
    }

    private abstract class AsyncTask<Params, Progress, Result> extends android.os.AsyncTask<Params, Progress, Result> {}
}