package com.example.comunidadsv;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends AppCompatActivity {

    private MapView mapView;
    private ProgressBar progressBar;
    private LinearLayout navHome, navMap, navChat, navProfile;
    private Toolbar toolbar;
    private List<Post> postsConUbicacion = new ArrayList<>();
    private MyLocationNewOverlay myLocationOverlay;
    private String currentUserId;
    private Set<String> followingIds = new HashSet<>();

    private ItemizedIconOverlay<OverlayItem> itemizedOverlay;
    private ArrayList<OverlayItem> overlayItems = new ArrayList<>();
    private Map<String, Post> markerToPost = new HashMap<>();

    // Para contar cuántas publicaciones hay en cada coordenada
    private Map<String, Integer> coordenadaCount = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue("ComunidadSV/1.0 (Android)");

        setContentView(R.layout.activity_maps_osm);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mapView = findViewById(R.id.mapView);
        progressBar = findViewById(R.id.progressBar);
        navHome = findViewById(R.id.navHome);
        navMap = findViewById(R.id.navMap);
        navChat = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        setupBottomNavigation();
        setupMap();
        setupItemizedOverlay();
        loadFollowingList();
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            startActivity(new Intent(MapsActivity.this, FeedActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            finish();
        });

        navMap.setOnClickListener(v -> {});

        navChat.setOnClickListener(v -> {
            startActivity(new Intent(MapsActivity.this, ChatsActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            finish();
        });

        navProfile.setOnClickListener(v -> {
            startActivity(new Intent(MapsActivity.this, ProfileActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            finish();
        });
    }

    private void setupMap() {
        OnlineTileSourceBase tileSource = new XYTileSource(
                "OpenTopoMap",
                0, 18, 256,
                ".png",
                new String[] {
                        "https://a.tile.opentopomap.org/",
                        "https://b.tile.opentopomap.org/",
                        "https://c.tile.opentopomap.org/"
                }
        );

        mapView.setTileSource(tileSource);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(13.7942, -88.8965));
        mapView.setMinZoomLevel(5.0);
        mapView.setMaxZoomLevel(18.0);
    }

    private void setupItemizedOverlay() {
        itemizedOverlay = new ItemizedIconOverlay<OverlayItem>(overlayItems,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        String title = item.getTitle();
                        Post post = markerToPost.get(title);
                        if (post != null) {
                            showPostDialog(post);
                        }
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, mapView.getContext());

        mapView.getOverlays().add(itemizedOverlay);
    }

    private void setupMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
            myLocationOverlay.setDrawAccuracyEnabled(true);
            mapView.getOverlays().add(myLocationOverlay);
        }
    }

    private void checkPermissionsAndLoad() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        } else {
            setupMyLocation();
            loadPostsFromFollowing();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMyLocation();
        }
        loadPostsFromFollowing();
    }

    private void loadFollowingList() {
        new LoadFollowingTask().execute();
    }

    private void loadPostsFromFollowing() {
        new LoadPostsTask().execute();
    }

    // Método para generar un desplazamiento basado en el índice de publicación en la misma coordenada
    private GeoPoint getOffsetPosition(double lat, double lon, int offsetIndex) {
        // Desplazamiento de aproximadamente 0.0005 grados (unos 50 metros)
        double offsetAmount = 0.0005;

        // Distribuir en círculo alrededor del punto original
        double angle = offsetIndex * 45; // Ángulo en grados
        double radians = Math.toRadians(angle);
        double deltaLat = Math.cos(radians) * offsetAmount;
        double deltaLon = Math.sin(radians) * offsetAmount;

        return new GeoPoint(lat + deltaLat, lon + deltaLon);
    }

    private void addMarkerToMap(Post post, int sameCoordIndex) {
        if (post == null) return;

        if (post.isTieneCoordenadas() && post.getLatitud() != 0 && post.getLongitud() != 0) {
            GeoPoint point;

            if (sameCoordIndex > 0) {
                // Si hay más publicaciones en la misma coordenada, desplazar este marcador
                point = getOffsetPosition(post.getLatitud(), post.getLongitud(), sameCoordIndex);
            } else {
                point = new GeoPoint(post.getLatitud(), post.getLongitud());
            }

            String uniqueTitle = post.getId() + " - " + post.getTitulo();

            // Agregar indicador de que hay múltiples publicaciones en el mismo lugar
            String description = post.getUserName() + " - " + post.getCategoria();
            if (sameCoordIndex > 0) {
                description = "📌 " + description + " (cerca de otra publicación)";
            }

            OverlayItem overlayItem = new OverlayItem(
                    uniqueTitle,
                    post.getTitulo(),
                    description,
                    point
            );

            markerToPost.put(uniqueTitle, post);

            Drawable icon = createMarkerIcon(getMarkerColor(post.getCategoria()));
            overlayItem.setMarker(icon);

            overlayItems.add(overlayItem);
        }
    }

    private void refreshMapMarkers() {
        overlayItems.clear();
        markerToPost.clear();
        coordenadaCount.clear();

        // Primero contar cuántas publicaciones hay en cada coordenada
        for (Post post : postsConUbicacion) {
            if (post.isTieneCoordenadas() && post.getLatitud() != 0 && post.getLongitud() != 0) {
                String coordKey = String.format("%.6f,%.6f", post.getLatitud(), post.getLongitud());
                int count = coordenadaCount.getOrDefault(coordKey, 0);
                coordenadaCount.put(coordKey, count + 1);
            }
        }

        // Crear un mapa para llevar el conteo por coordenada mientras se agregan
        Map<String, Integer> currentIndex = new HashMap<>();

        // Agregar marcadores con desplazamiento si hay múltiples
        for (Post post : postsConUbicacion) {
            if (post.isTieneCoordenadas() && post.getLatitud() != 0 && post.getLongitud() != 0) {
                String coordKey = String.format("%.6f,%.6f", post.getLatitud(), post.getLongitud());
                int totalEnCoordenada = coordenadaCount.getOrDefault(coordKey, 0);
                int index = currentIndex.getOrDefault(coordKey, 0);

                if (totalEnCoordenada > 1) {
                    // Múltiples publicaciones en el mismo lugar - mostrar con desplazamiento
                    addMarkerToMap(post, index);
                } else {
                    // Solo una publicación - mostrar en el punto exacto
                    addMarkerToMap(post, 0);
                }
                currentIndex.put(coordKey, index + 1);
            }
        }

        itemizedOverlay.removeAllItems();
        for (OverlayItem item : overlayItems) {
            itemizedOverlay.addItem(item);
        }
        mapView.invalidate();
    }

    private int getMarkerColor(String categoria) {
        switch (categoria) {
            case "Tráfico": return Color.rgb(244, 67, 54);
            case "Seguridad": return Color.rgb(255, 152, 0);
            case "Emergencia": return Color.rgb(233, 30, 99);
            case "Mascotas": return Color.rgb(255, 193, 7);
            case "Eventos": return Color.rgb(76, 175, 80);
            case "Ayuda": return Color.rgb(0, 150, 136);
            case "Objetos": return Color.rgb(33, 150, 243);
            default: return Color.rgb(156, 39, 176);
        }
    }

    private Drawable createMarkerIcon(int color) {
        int size = 80;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        canvas.drawCircle(size / 2, size / 2, size / 2 - 4, paint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        canvas.drawCircle(size / 2, size / 2, size / 2 - 4, paint);

        return new android.graphics.drawable.BitmapDrawable(getResources(), bitmap);
    }

    private void showPostDialog(Post post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(post.getTitulo());

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_post_preview, null);
        TextView txtCategoria = dialogView.findViewById(R.id.txtCategoria);
        TextView txtContenido = dialogView.findViewById(R.id.txtContenido);
        TextView txtUsuario = dialogView.findViewById(R.id.txtUsuario);
        TextView txtUbicacion = dialogView.findViewById(R.id.txtUbicacion);
        TextView txtLikes = dialogView.findViewById(R.id.txtLikes);
        TextView txtComments = dialogView.findViewById(R.id.txtComments);

        txtCategoria.setText(post.getCategoria());
        txtContenido.setText(post.getContenido());
        txtUsuario.setText("📝 " + post.getUserName());
        txtUbicacion.setText("📍 " + post.getUbicacionPost());
        txtLikes.setText("❤️ " + post.getLikes());
        txtComments.setText("💬 " + (post.getComments() != null ? post.getComments().size() : 0));

        builder.setView(dialogView);
        builder.setPositiveButton("Ver publicación completa", (dialog, which) -> {
            Intent intent = new Intent(MapsActivity.this, FeedActivity.class);
            intent.putExtra("post_id", post.getId());
            startActivity(intent);
        });
        builder.setNegativeButton("Cerrar", null);
        builder.show();
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
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
                            if (!followingId.isEmpty()) {
                                following.add(followingId);
                            }
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
            followingIds = result;
            checkPermissionsAndLoad();
        }
    }

    private class LoadPostsTask extends AsyncTask<Void, Void, List<Post>> {
        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Post> doInBackground(Void... voids) {
            List<Post> postList = new ArrayList<>();

            if (followingIds.isEmpty()) {
                return postList;
            }

            try {
                for (String userId : followingIds) {
                    String urlStr = Configuracion.SERVIDOR + "/db_publicaciones/_design/publicaciones/_view/por_usuario?key=\"" + userId + "\"";
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
                                JSONObject doc = row.getJSONObject("value");
                                String docId = doc.getString("_id");
                                Post post = Post.fromJSON(doc, docId);

                                if (post.isTieneCoordenadas() && post.getLatitud() != 0 && post.getLongitud() != 0) {
                                    postList.add(post);
                                }
                            }
                        }
                    }
                }

                String myPostsUrl = Configuracion.SERVIDOR + "/db_publicaciones/_design/publicaciones/_view/por_usuario?key=\"" + currentUserId + "\"";
                URL myUrl = new URL(myPostsUrl);
                HttpURLConnection myConn = (HttpURLConnection) myUrl.openConnection();
                setBasicAuth(myConn);
                myConn.setRequestMethod("GET");
                myConn.setConnectTimeout(10000);
                myConn.setReadTimeout(10000);

                if (myConn.getResponseCode() == 200) {
                    InputStream in = myConn.getInputStream();
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

                            if (post.isTieneCoordenadas() && post.getLatitud() != 0 && post.getLongitud() != 0) {
                                boolean exists = false;
                                for (Post p : postList) {
                                    if (p.getId().equals(post.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) {
                                    postList.add(post);
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return postList;
        }

        @Override
        protected void onPostExecute(List<Post> result) {
            progressBar.setVisibility(View.GONE);

            if (result != null && !result.isEmpty()) {
                postsConUbicacion = result;
                refreshMapMarkers();

                // Contar cuántas coordenadas tienen múltiples publicaciones
                int duplicated = 0;
                for (int count : coordenadaCount.values()) {
                    if (count > 1) duplicated++;
                }

                if (duplicated > 0) {
                    Toast.makeText(MapsActivity.this, "📍 " + result.size() + " publicaciones.\n" + duplicated + " ubicaciones tienen múltiples publicaciones (separadas visualmente)", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MapsActivity.this, "📍 " + result.size() + " publicaciones de usuarios que sigues", Toast.LENGTH_LONG).show();
                }
            } else {
                if (followingIds.isEmpty()) {
                    Toast.makeText(MapsActivity.this, "⚠️ No sigues a ningún usuario. Sigue a otros usuarios para ver sus publicaciones en el mapa.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MapsActivity.this, "⚠️ No hay publicaciones con ubicación de los usuarios que sigues.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }
}