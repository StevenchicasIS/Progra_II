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
import java.util.ArrayList;
import java.util.List;

public class ChatsActivity extends AppCompatActivity implements ChatAdapter.OnChatDeletedListener {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout layoutNoChats;
    private ChatAdapter chatAdapter;
    private List<Chat> chatsList;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        layoutNoChats = findViewById(R.id.layoutNoChats);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatsList = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        chatAdapter = new ChatAdapter(this, chatsList, currentUserId);
        recyclerView.setAdapter(chatAdapter);

        loadChats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChats();
    }

    @Override
    public void onChatDeleted() {
        loadChats();
    }

    private void loadChats() {
        new LoadChatsTask().execute();
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class LoadChatsTask extends AsyncTask<Void, Void, List<Chat>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Chat> doInBackground(Void... voids) {
            List<Chat> lista = new ArrayList<>();
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_chats/_design/chats/_view/por_usuario?key=\"" + currentUserId + "\"";
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
                            Chat chat = Chat.fromJSON(doc);
                            lista.add(chat);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return lista;
        }

        @Override
        protected void onPostExecute(List<Chat> result) {
            progressBar.setVisibility(View.GONE);
            if (!result.isEmpty()) {
                chatsList.clear();
                chatsList.addAll(result);
                chatAdapter.notifyDataSetChanged();
                recyclerView.setVisibility(View.VISIBLE);
                layoutNoChats.setVisibility(View.GONE);
            } else {
                recyclerView.setVisibility(View.GONE);
                layoutNoChats.setVisibility(View.VISIBLE);
            }
        }
    }
}