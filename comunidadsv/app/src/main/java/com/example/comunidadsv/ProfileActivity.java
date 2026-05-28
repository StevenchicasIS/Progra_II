package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

public class ProfileActivity extends AppCompatActivity implements PostAdapter.OnPostActionListener {

    private TextView txtNombre, txtUbicacion;
    private TextView txtPublicacionesCount, txtSeguidoresCount, txtSeguidosCount;
    private Button btnEditarPerfil;
    private Button btnSeguir;
    private ImageView imgProfile;
    private ImageView btnLogout;
    private RecyclerView recyclerViewMisPublicaciones;
    private ProgressBar progressBar;
    private LinearLayout layoutNoPublicaciones;

    private LinearLayout navHome, navMap, navChat, navProfile;

    private PostAdapter postAdapter;
    private List<Post> misPublicaciones;
    private String userId;
    private String currentUserId;
    private boolean isOwnProfile;
    private String currentUserPhotoBase64 = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        txtNombre = findViewById(R.id.txtNombre);
        txtUbicacion = findViewById(R.id.txtUbicacion);
        txtPublicacionesCount = findViewById(R.id.txtPublicacionesCount);
        txtSeguidoresCount = findViewById(R.id.txtSeguidoresCount);
        txtSeguidosCount = findViewById(R.id.txtSeguidosCount);
        btnEditarPerfil = findViewById(R.id.btnEditarPerfil);
        btnSeguir = findViewById(R.id.btnSeguir);
        imgProfile = findViewById(R.id.imgProfile);
        btnLogout = findViewById(R.id.btnLogout);
        recyclerViewMisPublicaciones = findViewById(R.id.recyclerViewMisPublicaciones);
        progressBar = findViewById(R.id.progressBarMisPublicaciones);
        layoutNoPublicaciones = findViewById(R.id.layoutNoPublicaciones);

        navHome = findViewById(R.id.navHome);
        navMap = findViewById(R.id.navMap);
        navChat = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);

        recyclerViewMisPublicaciones.setLayoutManager(new LinearLayoutManager(this));
        misPublicaciones = new ArrayList<>();

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.isEmpty()) {
            userId = currentUserId;
        }

        isOwnProfile = userId.equals(currentUserId);

        if (isOwnProfile) {
            btnEditarPerfil.setVisibility(View.VISIBLE);
            btnSeguir.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            btnEditarPerfil.setVisibility(View.GONE);
            btnSeguir.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
            checkIfFollowing();
        }

        postAdapter = new PostAdapter(this, misPublicaciones, currentUserId, this);
        recyclerViewMisPublicaciones.setAdapter(postAdapter);

        setupBottomNavigation();

        loadUserData();
        loadUserPosts();
        loadFollowersCount();
        loadFollowingCount();

        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, 200);
        });

        btnSeguir.setOnClickListener(v -> {
            if (btnSeguir.getText().toString().equals("Seguir")) {
                enviarSolicitudSeguimiento();
            } else {
                dejarDeSeguir();
            }
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FeedActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        navMap.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MapsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        navChat.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, ChatsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        navProfile.setOnClickListener(v -> {
            loadUserData();
            loadUserPosts();
            loadFollowersCount();
            loadFollowingCount();
            if (!isOwnProfile) {
                checkIfFollowing();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        loadUserPosts();
        loadFollowersCount();
        loadFollowingCount();
        if (!isOwnProfile) {
            checkIfFollowing();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) {
            loadUserData();
            loadUserPosts();
        }
    }

    private void loadUserData() {
        new LoadUserDataTask().execute();
    }

    private Bitmap base64ToBitmap(String base64String) {
        try {
            byte[] decodedString = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadUserPosts() {
        new LoadUserPostsTask().execute();
    }

    private void loadFollowersCount() {
        new LoadFollowersCountTask().execute();
    }

    private void loadFollowingCount() {
        new LoadFollowingCountTask().execute();
    }

    private void checkIfFollowing() {
        new CheckFollowingTask().execute();
    }

    private void enviarSolicitudSeguimiento() {
        new EnviarSolicitudTask().execute();
    }

    private void dejarDeSeguir() {
        new UnfollowTask().execute();
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

    private class LoadUserDataTask extends AsyncTask<Void, Void, String[]> {
        @Override
        protected String[] doInBackground(Void... voids) {
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
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
                    JSONObject user = new JSONObject(sb.toString());

                    return new String[]{
                            user.optString("nombre", "Usuario"),
                            user.optString("ubicacion", "Sin ubicación"),
                            user.optString("fotoPerfil", "")
                    };
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new String[]{"Usuario", "Sin ubicación", ""};
        }

        @Override
        protected void onPostExecute(String[] data) {
            txtNombre.setText(data[0]);
            txtUbicacion.setText(data[1]);

            if (!data[2].isEmpty()) {
                Bitmap bitmap = base64ToBitmap(data[2]);
                if (bitmap != null) {
                    imgProfile.setImageBitmap(bitmap);
                    currentUserPhotoBase64 = data[2];
                } else {
                    imgProfile.setImageResource(R.drawable.ic_profile);
                }
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile);
            }

            if (isOwnProfile) {
                SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("nombre", data[0]);
                editor.putString("ubicacion", data[1]);
                editor.apply();
            }
        }
    }

    private class LoadFollowersCountTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            int count = 0;
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_seguidores/_design/seguidores/_view/por_seguido?key=\"" + userId + "\"";
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
            txtSeguidoresCount.setText(String.valueOf(count));
        }
    }

    private class LoadFollowingCountTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... voids) {
            int count = 0;
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_seguidores/_design/seguidores/_view/por_seguidor?key=\"" + userId + "\"";
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
            txtSeguidosCount.setText(String.valueOf(count));
        }
    }

    private class CheckFollowingTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String urlStr = Configuracion.SERVIDOR + "/db_seguidores/_design/seguidores/_view/por_seguidor?key=\"" + currentUserId + "\"";
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
                            String followingId = row.optString("value");
                            if (followingId.equals(userId)) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean isFollowing) {
            if (isFollowing) {
                btnSeguir.setText("Dejar de seguir");
                btnSeguir.setBackgroundTintList(getColorStateList(R.color.gray));
            } else {
                btnSeguir.setText("Seguir");
                btnSeguir.setBackgroundTintList(getColorStateList(R.color.green_primary));
            }
        }
    }

    private class EnviarSolicitudTask extends AsyncTask<Void, Void, Boolean> {
        private String errorMsg = "";

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
                String emisorNombre = prefs.getString("nombre", "Usuario");

                String emisorFoto = "";
                String userUrl = Configuracion.SERVIDOR + "/db_usuarios/" + currentUserId;
                URL userUrlObj = new URL(userUrl);
                HttpURLConnection userConn = (HttpURLConnection) userUrlObj.openConnection();
                setBasicAuth(userConn);
                userConn.setRequestMethod("GET");

                if (userConn.getResponseCode() == 200) {
                    InputStream in = userConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject user = new JSONObject(sb.toString());
                    emisorFoto = user.optString("fotoPerfil", "");
                }

                Solicitud solicitud = new Solicitud(currentUserId, emisorNombre, emisorFoto, userId);
                String putUrl = Configuracion.SERVIDOR + "/db_solicitudes/" + solicitud.getId();
                URL putUrlObj = new URL(putUrl);
                HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                setBasicAuth(putConn);
                putConn.setRequestMethod("PUT");
                putConn.setRequestProperty("Content-Type", "application/json");
                putConn.setDoOutput(true);

                OutputStream os = putConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(solicitud.toJSON().toString());
                writer.flush();
                writer.close();
                os.close();

                return putConn.getResponseCode() == 201 || putConn.getResponseCode() == 202;
            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(ProfileActivity.this, "Solicitud enviada", Toast.LENGTH_SHORT).show();
                btnSeguir.setText("Solicitud enviada");
                btnSeguir.setEnabled(false);
            } else {
                Toast.makeText(ProfileActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class UnfollowTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String searchUrl = Configuracion.SERVIDOR + "/db_seguidores/_design/seguidores/_view/por_seguidor?key=\"" + currentUserId + "\"";
                URL searchUrlObj = new URL(searchUrl);
                HttpURLConnection searchConn = (HttpURLConnection) searchUrlObj.openConnection();
                setBasicAuth(searchConn);
                searchConn.setRequestMethod("GET");

                if (searchConn.getResponseCode() == 200) {
                    InputStream in = searchConn.getInputStream();
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
                            if (followingId.equals(userId)) {
                                String docId = row.optString("id");
                                String deleteUrl = Configuracion.SERVIDOR + "/db_seguidores/" + docId;
                                URL deleteUrlObj = new URL(deleteUrl);
                                HttpURLConnection deleteConn = (HttpURLConnection) deleteUrlObj.openConnection();
                                setBasicAuth(deleteConn);
                                deleteConn.setRequestMethod("DELETE");
                                return deleteConn.getResponseCode() == 200;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(ProfileActivity.this, "Dejaste de seguir", Toast.LENGTH_SHORT).show();
                btnSeguir.setText("Seguir");
                btnSeguir.setBackgroundTintList(getColorStateList(R.color.green_primary));
                btnSeguir.setEnabled(true);
                loadFollowersCount();
                loadFollowingCount();
            } else {
                Toast.makeText(ProfileActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class LoadUserPostsTask extends AsyncTask<Void, Void, List<Post>> {
        private String errorMsg = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            layoutNoPublicaciones.setVisibility(View.GONE);
            recyclerViewMisPublicaciones.setVisibility(View.GONE);
        }

        @Override
        protected List<Post> doInBackground(Void... voids) {
            List<Post> userPosts = new ArrayList<>();
            try {
                String encodedUserId = java.net.URLEncoder.encode("\"" + userId + "\"", "UTF-8");
                String urlStr = Configuracion.SERVIDOR + "/db_publicaciones/_design/publicaciones/_view/por_usuario?key=" + encodedUserId + "&descending=true";
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);

                    JSONObject response = new JSONObject(sb.toString());
                    JSONArray rows = response.optJSONArray("rows");

                    if (rows != null && rows.length() > 0) {
                        for (int i = 0; i < rows.length(); i++) {
                            JSONObject row = rows.getJSONObject(i);
                            JSONObject doc = row.getJSONObject("value");
                            String docId = doc.getString("_id");
                            Post post = Post.fromJSON(doc, docId);
                            userPosts.add(post);
                        }
                    }
                } else {
                    errorMsg = "Error HTTP: " + responseCode;
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
                e.printStackTrace();
            }
            return userPosts;
        }

        @Override
        protected void onPostExecute(List<Post> result) {
            progressBar.setVisibility(View.GONE);

            if (!result.isEmpty()) {
                misPublicaciones.clear();
                misPublicaciones.addAll(result);
                postAdapter.notifyDataSetChanged();
                recyclerViewMisPublicaciones.setVisibility(View.VISIBLE);
                layoutNoPublicaciones.setVisibility(View.GONE);
                txtPublicacionesCount.setText(String.valueOf(result.size()));
            } else {
                recyclerViewMisPublicaciones.setVisibility(View.GONE);
                layoutNoPublicaciones.setVisibility(View.VISIBLE);
                txtPublicacionesCount.setText("0");
                if (!errorMsg.isEmpty()) {
                    Toast.makeText(ProfileActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
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

                OutputStream os = putConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(doc.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = putConn.getResponseCode();
                return responseCode == 201 || responseCode == 202;

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
                Toast.makeText(ProfileActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
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

                OutputStream os = putConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(doc.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = putConn.getResponseCode();
                return responseCode == 201 || responseCode == 202;

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
                Toast.makeText(ProfileActivity.this, "Error: " + errorMsg, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cerrar sesión");
        builder.setMessage("¿Estás seguro de que deseas cerrar sesión?");
        builder.setPositiveButton("Sí, cerrar", (dialog, which) -> logout());
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}