package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FeedActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> posts;
    private ProgressBar progressBar;
    private String currentUserId;
    private String currentUserNombre;
    private TextView tabParaTi, tabSiguiendo;
    private LinearLayout navHome, navMap, navChat, navProfile;
    private FloatingActionButton fabAddPost;
    private RelativeLayout iconSolicitudes, iconNotificaciones;
    private TextView badgeSolicitudes, badgeNotificaciones;

    private String currentFilter = "parati";
    private Set<String> followingIds = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tabParaTi = findViewById(R.id.tabParaTi);
        tabSiguiendo = findViewById(R.id.tabSiguiendo);
        navHome = findViewById(R.id.navHome);
        navMap = findViewById(R.id.navMap);
        navChat = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);
        fabAddPost = findViewById(R.id.fabAddPost);
        iconSolicitudes = findViewById(R.id.iconSolicitudes);
        iconNotificaciones = findViewById(R.id.iconNotificaciones);
        badgeSolicitudes = findViewById(R.id.badgeSolicitudes);
        badgeNotificaciones = findViewById(R.id.badgeNotificaciones);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        posts = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");
        currentUserNombre = prefs.getString("nombre", "Usuario");

        Toast.makeText(this, "Bienvenido " + currentUserNombre, Toast.LENGTH_SHORT).show();

        postAdapter = new PostAdapter(this, posts, currentUserId, this);
        recyclerView.setAdapter(postAdapter);

        iconSolicitudes.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, SolicitudesActivity.class);
            startActivity(intent);
        });

        iconNotificaciones.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, NotificacionesActivity.class);
            startActivity(intent);
        });

        loadFollowingList();
        loadSolicitudesCount();
        loadNotificacionesCount();

        tabParaTi.setOnClickListener(v -> {
            setTabSelected(tabParaTi);
            currentFilter = "parati";
            loadPosts();
        });

        tabSiguiendo.setOnClickListener(v -> {
            setTabSelected(tabSiguiendo);
            currentFilter = "siguiendo";
            loadPosts();
        });

        navHome.setOnClickListener(v -> {
            recyclerView.smoothScrollToPosition(0);
        });

        navMap.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, MapsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        navChat.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, ChatsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(FeedActivity.this, CreatePostActivity.class);
            startActivityForResult(intent, 100);
        });

        loadPosts();
    }

    private void loadSolicitudesCount() {
        new LoadSolicitudesCountTask().execute();
    }

    private void loadNotificacionesCount() {
        new LoadNotificacionesCountTask().execute();
    }

    private void updateBadge(TextView badge, int count) {
        if (count > 0) {
            badge.setText(String.valueOf(count));
            badge.setVisibility(View.VISIBLE);
        } else {
            badge.setVisibility(View.GONE);
        }
    }

    private void setTabSelected(TextView selectedTab) {
        tabParaTi.setTextColor(getColor(R.color.gray_text));
        tabSiguiendo.setTextColor(getColor(R.color.gray_text));

        tabParaTi.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabSiguiendo.setTypeface(null, android.graphics.Typeface.NORMAL);

        selectedTab.setTextColor(getColor(R.color.green_primary));
        selectedTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFollowingList();
        loadSolicitudesCount();
        loadNotificacionesCount();
        loadPosts();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadFollowingList();
            loadPosts();
        }
    }

    private void loadFollowingList() {
        new LoadFollowingTask().execute();
    }

    private void loadPosts() {
        new LoadPostsTask().execute();
    }

    private String obtenerNombreUsuario(String userId) {
        try {
            String urlStr = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            setBasicAuth(conn);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject user = new JSONObject(sb.toString());
                return user.optString("nombre", "Usuario");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Usuario";
    }

    private String obtenerFotoUsuario(String userId) {
        try {
            String urlStr = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            setBasicAuth(conn);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject user = new JSONObject(sb.toString());
                return user.optString("fotoPerfil", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void onLikeClicked(Post post, int position) {
        new LikePostTask().execute(post, position);
    }

    @Override
    public void onCommentAdded(Post post, int position, String commentText) {
        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        String userId = prefs.getString("userId", "");
        String userName = prefs.getString("nombre", "Usuario");

        Comment newComment = new Comment(userId, userName, commentText, System.currentTimeMillis());
        post.getComments().add(newComment);

        new UpdatePostTask().execute(post, position);
    }

    @Override
    public void onShareClicked(Post post) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, post.getTitulo());
        shareIntent.putExtra(Intent.EXTRA_TEXT, post.getTitulo() + "\n\n" + post.getContenido() + "\n\n- Compartido desde ComunidadSV");
        startActivity(Intent.createChooser(shareIntent, "Compartir vía"));
    }

    @Override
    public void onCommentDeleted(Post post, int position, int commentIndex) {
        post.getComments().remove(commentIndex);
        new UpdatePostTask().execute(post, position);
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class LoadNotificacionesCountTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            int count = 0;
            try {
                String encodedUserId = URLEncoder.encode("\"" + currentUserId + "\"", "UTF-8");
                String urlStr = Configuracion.SERVIDOR + "/db_notificaciones/_design/notificaciones/_view/no_leidas?key=" + encodedUserId;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject response = new JSONObject(sb.toString());
                    JSONArray rows = response.optJSONArray("rows");
                    if (rows != null) {
                        count = rows.length();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return count;
        }

        @Override
        protected void onPostExecute(Integer count) {
            updateBadge(badgeNotificaciones, count);
        }
    }

    private class LoadSolicitudesCountTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            int count = 0;
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_solicitudes/_design/solicitudes/_view/pendientes?key=\"" + currentUserId + "\"";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject response = new JSONObject(sb.toString());
                    JSONArray rows = response.optJSONArray("rows");
                    if (rows != null) {
                        count = rows.length();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return count;
        }

        @Override
        protected void onPostExecute(Integer count) {
            updateBadge(badgeSolicitudes, count);
        }
    }

    private class LoadFollowingTask extends AsyncTask<Void, Void, Set<String>> {
        @Override
        protected Set<String> doInBackground(Void... voids) {
            Set<String> following = new HashSet<>();
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_seguidores/_design/seguidores/_view/por_seguidor?key=\"" + currentUserId + "\"";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

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
                            String followingId = row.optString("value");
                            following.add(followingId);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return following;
        }

        @Override
        protected void onPostExecute(Set<String> result) {
            if (result != null) {
                followingIds = result;
            }
        }
    }

    private class LoadPostsTask extends AsyncTask<Void, Void, List<Post>> {
        private String errorMsg = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Post> doInBackground(Void... voids) {
            List<Post> postList = new ArrayList<>();
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_publicaciones/_design/publicaciones/_view/por_fecha?descending=true";
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
                            String docId = doc.getString("_id");
                            Post post = Post.fromJSON(doc, docId);

                            if (currentFilter.equals("siguiendo")) {
                                if (followingIds.contains(post.getUserId())) {
                                    postList.add(post);
                                }
                            } else {
                                postList.add(post);
                            }
                        }
                    }
                } else {
                    errorMsg = "Error HTTP: " + conn.getResponseCode();
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
                e.printStackTrace();
            }
            return postList;
        }

        @Override
        protected void onPostExecute(List<Post> result) {
            progressBar.setVisibility(View.GONE);
            if (!result.isEmpty()) {
                posts.clear();
                posts.addAll(result);
                postAdapter.notifyDataSetChanged();
            } else {
                posts.clear();
                postAdapter.notifyDataSetChanged();
                if (currentFilter.equals("siguiendo") && followingIds.isEmpty()) {
                    Toast.makeText(FeedActivity.this, "No sigues a nadie. Sigue usuarios para ver sus publicaciones.", Toast.LENGTH_LONG).show();
                }
            }
            if (!errorMsg.isEmpty()) {
                Toast.makeText(FeedActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class LikePostTask extends AsyncTask<Object, Void, Boolean> {
        private Post post;
        private int position;
        private String errorMsg = "";

        @Override
        protected Boolean doInBackground(Object... params) {
            post = (Post) params[0];
            position = (int) params[1];

            try {
                String getUrl = Configuracion.SERVIDOR + "/db_publicaciones/" + post.getId();
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(10000);
                getConn.setReadTimeout(10000);

                if (getConn.getResponseCode() != 200) {
                    errorMsg = "Error al obtener publicación";
                    return false;
                }

                InputStream in = getConn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                String docJson = sb.toString();

                JSONObject doc = new JSONObject(docJson);
                JSONArray likedByArray = doc.optJSONArray("likedBy");
                if (likedByArray == null) likedByArray = new JSONArray();

                int currentLikes = doc.optInt("likes", 0);
                boolean yaDioLike = false;

                for (int i = 0; i < likedByArray.length(); i++) {
                    if (likedByArray.getString(i).equals(currentUserId)) {
                        yaDioLike = true;
                        break;
                    }
                }

                if (yaDioLike) {
                    JSONArray newLikedBy = new JSONArray();
                    for (int i = 0; i < likedByArray.length(); i++) {
                        if (!likedByArray.getString(i).equals(currentUserId)) {
                            newLikedBy.put(likedByArray.getString(i));
                        }
                    }
                    doc.put("likedBy", newLikedBy);
                    doc.put("likes", currentLikes - 1);
                    post.setLikes(currentLikes - 1);
                    post.getLikedBy().remove(currentUserId);
                } else {
                    likedByArray.put(currentUserId);
                    doc.put("likedBy", likedByArray);
                    doc.put("likes", currentLikes + 1);
                    post.setLikes(currentLikes + 1);
                    post.getLikedBy().add(currentUserId);
                }

                String rev = doc.getString("_rev");
                String putUrl = Configuracion.SERVIDOR + "/db_publicaciones/" + post.getId() + "?rev=" + rev;
                URL putUrlObj = new URL(putUrl);
                HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                setBasicAuth(putConn);
                putConn.setRequestMethod("PUT");
                putConn.setRequestProperty("Content-Type", "application/json");
                putConn.setDoOutput(true);
                putConn.setConnectTimeout(10000);
                putConn.setReadTimeout(10000);

                OutputStream os = putConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(doc.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = putConn.getResponseCode();
                boolean success = responseCode == 201 || responseCode == 202;

                // CREAR NOTIFICACIÓN DE LIKE
                if (success && !post.getUserId().equals(currentUserId)) {
                    String nombreEmisor = obtenerNombreUsuario(currentUserId);
                    String fotoEmisor = obtenerFotoUsuario(currentUserId);
                    String mensaje = nombreEmisor + " le dio like a tu publicación \"" + post.getTitulo() + "\"";

                    Notificacion notificacion = new Notificacion(
                            post.getUserId(),
                            "like",
                            currentUserId,
                            nombreEmisor,
                            fotoEmisor,
                            post.getId(),
                            post.getTitulo(),
                            mensaje
                    );
                    crearNotificacion(notificacion);
                }

                return success;

            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                postAdapter.updatePost(position, post);
            } else {
                Toast.makeText(FeedActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class UpdatePostTask extends AsyncTask<Object, Void, Boolean> {
        private Post post;
        private int position;
        private String errorMsg = "";

        @Override
        protected Boolean doInBackground(Object... params) {
            post = (Post) params[0];
            position = (int) params[1];

            try {
                String getUrl = Configuracion.SERVIDOR + "/db_publicaciones/" + post.getId();
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(10000);
                getConn.setReadTimeout(10000);

                if (getConn.getResponseCode() != 200) {
                    errorMsg = "Error al obtener publicación";
                    return false;
                }

                InputStream in = getConn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                String docJson = sb.toString();

                JSONObject doc = new JSONObject(docJson);
                JSONArray commentsArray = new JSONArray();

                for (Comment c : post.getComments()) {
                    commentsArray.put(c.toJSON());
                }
                doc.put("comments", commentsArray);

                String rev = doc.getString("_rev");
                String putUrl = Configuracion.SERVIDOR + "/db_publicaciones/" + post.getId() + "?rev=" + rev;
                URL putUrlObj = new URL(putUrl);
                HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                setBasicAuth(putConn);
                putConn.setRequestMethod("PUT");
                putConn.setRequestProperty("Content-Type", "application/json");
                putConn.setDoOutput(true);
                putConn.setConnectTimeout(10000);
                putConn.setReadTimeout(10000);

                OutputStream os = putConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(doc.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = putConn.getResponseCode();
                boolean success = responseCode == 201 || responseCode == 202;

                // CREAR NOTIFICACIÓN DE COMENTARIO
                if (success && !post.getUserId().equals(currentUserId)) {
                    String nombreEmisor = obtenerNombreUsuario(currentUserId);
                    String fotoEmisor = obtenerFotoUsuario(currentUserId);
                    String mensaje = nombreEmisor + " comentó en tu publicación \"" + post.getTitulo() + "\"";

                    Notificacion notificacion = new Notificacion(
                            post.getUserId(),
                            "comentario",
                            currentUserId,
                            nombreEmisor,
                            fotoEmisor,
                            post.getId(),
                            post.getTitulo(),
                            mensaje
                    );
                    crearNotificacion(notificacion);
                }

                return success;

            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                postAdapter.updatePost(position, post);
            } else {
                Toast.makeText(FeedActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void crearNotificacion(Notificacion notificacion) {
        new CrearNotificacionTask().execute(notificacion);
    }

    private class CrearNotificacionTask extends AsyncTask<Notificacion, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Notificacion... params) {
            Notificacion n = params[0];
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_notificaciones/" + n.getId();
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(n.toJSON().toString());
                writer.flush();
                writer.close();
                os.close();

                return conn.getResponseCode() == 201 || conn.getResponseCode() == 202;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}