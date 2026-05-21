package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

public class SolicitudesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutNoSolicitudes;
    private List<Solicitud> solicitudes;
    private SolicitudAdapter adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solicitudes);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        layoutNoSolicitudes = findViewById(R.id.layoutNoSolicitudes);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        solicitudes = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        adapter = new SolicitudAdapter(this, solicitudes, currentUserId);
        recyclerView.setAdapter(adapter);

        loadSolicitudes();
    }

    private void loadSolicitudes() {
        new LoadSolicitudesTask().execute();
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class LoadSolicitudesTask extends AsyncTask<Void, Void, List<Solicitud>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Solicitud> doInBackground(Void... voids) {
            List<Solicitud> lista = new ArrayList<>();
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_solicitudes/_design/solicitudes/_view/pendientes?key=\"" + currentUserId + "\"";
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

                    JSONObject response = new JSONObject(sb.toString());
                    JSONArray rows = response.optJSONArray("rows");

                    if (rows != null) {
                        for (int i = 0; i < rows.length(); i++) {
                            JSONObject row = rows.getJSONObject(i);
                            JSONObject doc = row.getJSONObject("value");
                            Solicitud solicitud = Solicitud.fromJSON(doc);
                            lista.add(solicitud);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lista;
        }

        @Override
        protected void onPostExecute(List<Solicitud> result) {
            progressBar.setVisibility(View.GONE);
            if (!result.isEmpty()) {
                solicitudes.clear();
                solicitudes.addAll(result);
                adapter.notifyDataSetChanged();
                recyclerView.setVisibility(View.VISIBLE);
                layoutNoSolicitudes.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                layoutNoSolicitudes.setVisibility(View.VISIBLE);
            }
        }
    }
}