package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
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

public class FollowListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutNoUsuarios;
    private FollowAdapter adapter;
    private List<User> userList;
    private String userId;
    private String type; // "followers" o "following"
    private String toolbarTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        layoutNoUsuarios = findViewById(R.id.layoutNoUsuarios);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userList = new ArrayList<>();

        userId = getIntent().getStringExtra("userId");
        type = getIntent().getStringExtra("type");

        if (type.equals("followers")) {
            toolbarTitle = "Seguidores";
        } else {
            toolbarTitle = "Seguidos";
        }
        toolbar.setTitle(toolbarTitle);

        adapter = new FollowAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        new LoadUsersTask().execute();
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class LoadUsersTask extends AsyncTask<Void, Void, List<User>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<User> doInBackground(Void... voids) {
            List<User> lista = new ArrayList<>();
            try {
                String viewName;
                if (type.equals("followers")) {
                    viewName = "por_seguido";
                } else {
                    viewName = "por_seguidor";
                }

                String encodedUserId = URLEncoder.encode("\"" + userId + "\"", "UTF-8");
                String urlStr = Configuracion.SERVIDOR + "/db_seguidores/_design/seguidores/_view/" + viewName + "?key=" + encodedUserId;
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
                            String relatedUserId;
                            if (type.equals("followers")) {
                                // En por_seguido, el value es el followerId
                                relatedUserId = row.optString("value");
                            } else {
                                // En por_seguidor, el value es el followingId
                                relatedUserId = row.optString("value");
                            }

                            // Obtener datos del usuario relacionado
                            String userUrl = Configuracion.SERVIDOR + "/db_usuarios/" + relatedUserId;
                            URL userUrlObj = new URL(userUrl);
                            HttpURLConnection userConn = (HttpURLConnection) userUrlObj.openConnection();
                            setBasicAuth(userConn);
                            userConn.setRequestMethod("GET");
                            userConn.setConnectTimeout(10000);
                            userConn.setReadTimeout(10000);

                            if (userConn.getResponseCode() == 200) {
                                InputStream in2 = userConn.getInputStream();
                                BufferedReader reader2 = new BufferedReader(new InputStreamReader(in2));
                                StringBuilder sb2 = new StringBuilder();
                                String line2;
                                while ((line2 = reader2.readLine()) != null) sb2.append(line2);
                                JSONObject userDoc = new JSONObject(sb2.toString());

                                User user = new User();
                                user.setId(relatedUserId);
                                user.setNombre(userDoc.optString("nombre", "Usuario"));
                                user.setUbicacion(userDoc.optString("ubicacion", "Sin ubicación"));
                                user.setFotoPerfil(userDoc.optString("fotoPerfil", ""));
                                lista.add(user);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lista;
        }

        @Override
        protected void onPostExecute(List<User> result) {
            progressBar.setVisibility(View.GONE);
            if (result != null && !result.isEmpty()) {
                userList.clear();
                userList.addAll(result);
                adapter.notifyDataSetChanged();
                recyclerView.setVisibility(View.VISIBLE);
                layoutNoUsuarios.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                layoutNoUsuarios.setVisibility(View.VISIBLE);
            }
        }
    }
}