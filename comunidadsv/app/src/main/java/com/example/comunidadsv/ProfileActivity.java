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

        postAdapter = new PostAdapter(this, misPublicaciones, currentUserId, this);
        recyclerViewMisPublicaciones.setAdapter(postAdapter);

        setupBottomNavigation();

        loadUserData();
        loadUserPosts();

        txtPublicacionesCount.setText("0");
        txtSeguidoresCount.setText("0");
        txtSeguidosCount.setText("0");

        btnEditarPerfil.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivityForResult(intent, 200);
        });

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK) {
            // Actualizar todos los datos al volver de editar perfil
            loadUserData();
            loadUserPosts();
        }
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FeedActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        navMap.setOnClickListener(v -> {
            Toast.makeText(this, "Próximamente: Mapa", Toast.LENGTH_SHORT).show();
        });

        navChat.setOnClickListener(v -> {
            Toast.makeText(this, "Próximamente: Chat", Toast.LENGTH_SHORT).show();
        });

        navProfile.setOnClickListener(v -> {
            loadUserData();
            loadUserPosts();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        loadUserPosts();
    }

    private void loadUserData() {
        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        String nombre = prefs.getString("nombre", "Usuario");
        String ubicacion = prefs.getString("ubicacion", "Sin ubicación");
        String fotoBase64 = prefs.getString("fotoPerfil", "");
        userId = prefs.getString("userId", "");

        txtNombre.setText(nombre);
        txtUbicacion.setText(ubicacion);

        // Cargar foto desde Base64
        if (!fotoBase64.isEmpty()) {
            Bitmap bitmap = base64ToBitmap(fotoBase64);
            if (bitmap != null) {
                imgProfile.setImageBitmap(bitmap);
            } else {
                imgProfile.setImageResource(R.drawable.ic_profile);
            }
        } else {
            imgProfile.setImageResource(R.drawable.ic_profile);
        }
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
                String encodedUserId = java.net.URLEncoder.encode("\"" + currentUserId + "\"", "UTF-8");
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

                int getCode = getConn.getResponseCode();
                if (getCode != 200) {
                    errorMsg = "Error al obtener publicación: " + getCode;
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
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                postAdapter.updatePost(position, post);
                Toast.makeText(ProfileActivity.this, "Like actualizado", Toast.LENGTH_SHORT).show();
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

                int getCode = getConn.getResponseCode();
                if (getCode != 200) {
                    errorMsg = "Error al obtener publicación: " + getCode;
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
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                postAdapter.updatePost(position, post);
                Toast.makeText(ProfileActivity.this, "Comentario agregado", Toast.LENGTH_SHORT).show();
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