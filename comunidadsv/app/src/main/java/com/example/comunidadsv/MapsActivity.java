package com.example.comunidadsv;

import android.Manifest;
import android.app.DatePickerDialog;
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MapsActivity extends AppCompatActivity {

    private static final String TAG = "MapsActivity";

    private MapView mapView;
    private ProgressBar progressBar;
    private LinearLayout navHome, navMap, navChat, navProfile;
    private Button btnFechaInicio, btnAplicarFiltro;
    private List<Post> todasLasPublicaciones = new ArrayList<>();
    private List<Post> publicacionesFiltradas = new ArrayList<>();
    private MyLocationNewOverlay myLocationOverlay;
    private String currentUserId;
    private Set<String> followingIds = new HashSet<>();

    private ItemizedIconOverlay<OverlayItem> itemizedOverlay;
    private ArrayList<OverlayItem> overlayItems = new ArrayList<>();
    private Map<String, List<Post>> markerToPosts = new HashMap<>();

    private long fechaInicio = 0;
    private Calendar calInicio;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue("ComunidadSV/1.0 (Android)");

        setContentView(R.layout.activity_maps_osm);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        mapView = findViewById(R.id.mapView);
        progressBar = findViewById(R.id.progressBar);
        navHome = findViewById(R.id.navHome);
        navMap = findViewById(R.id.navMap);
        navChat = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);
        btnFechaInicio = findViewById(R.id.btnFechaInicio);
        btnAplicarFiltro = findViewById(R.id.btnAplicarFiltro);

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        Log.d(TAG, "Current User ID: " + currentUserId);

        // Por defecto mostrar publicaciones de los últimos 30 días
        calInicio = Calendar.getInstance();
        calInicio.add(Calendar.DAY_OF_MONTH, -30);
        calInicio.set(Calendar.HOUR_OF_DAY, 0);
        calInicio.set(Calendar.MINUTE, 0);
        calInicio.set(Calendar.SECOND, 0);
        calInicio.set(Calendar.MILLISECOND, 0);
        fechaInicio = calInicio.getTimeInMillis();
        btnFechaInicio.setText("Desde: " + dateFormat.format(calInicio.getTime()));

        setupBottomNavigation();
        setupMap();
        setupItemizedOverlay();
        setupDateFilter();

        loadFollowingList();
    }

    private void setupDateFilter() {
        btnFechaInicio.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calInicio.set(year, month, dayOfMonth);
                        calInicio.set(Calendar.HOUR_OF_DAY, 0);
                        calInicio.set(Calendar.MINUTE, 0);
                        calInicio.set(Calendar.SECOND, 0);
                        calInicio.set(Calendar.MILLISECOND, 0);
                        fechaInicio = calInicio.getTimeInMillis();
                        btnFechaInicio.setText("Desde: " + dateFormat.format(calInicio.getTime()));
                        aplicarFiltroYActualizarMapa();
                    },
                    calInicio.get(Calendar.YEAR),
                    calInicio.get(Calendar.MONTH),
                    calInicio.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        btnAplicarFiltro.setOnClickListener(v -> {
            aplicarFiltroYActualizarMapa();
        });
    }

    private void aplicarFiltroYActualizarMapa() {
        publicacionesFiltradas.clear();

        for (Post post : todasLasPublicaciones) {
            if (post.getFecha() >= fechaInicio) {
                publicacionesFiltradas.add(post);
            }
        }

        refreshMapMarkers();

        String mensaje = "Mostrando " + publicacionesFiltradas.size() + " publicaciones";
        if (fechaInicio > 0) {
            mensaje += " desde " + dateFormat.format(calInicio.getTime());
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        Log.d(TAG, mensaje);
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
                        String coordKey = item.getTitle();
                        List<Post> postsEnUbicacion = markerToPosts.get(coordKey);
                        if (postsEnUbicacion != null) {
                            if (postsEnUbicacion.size() == 1) {
                                showPostDialog(postsEnUbicacion.get(0));
                            } else {
                                showMultiplePostsDialog(postsEnUbicacion);
                            }
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

    private void showMultiplePostsDialog(List<Post> posts) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Publicaciones en esta ubicación (" + posts.size() + ")");

        String[] opciones = new String[posts.size()];
        for (int i = 0; i < posts.size(); i++) {
            Post p = posts.get(i);
            String fecha = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(p.getFecha());
            opciones[i] = p.getUserName() + " - " + fecha + "\n" + p.getTitulo();
        }

        builder.setItems(opciones, (dialog, which) -> {
            showPostDialog(posts.get(which));
        });

        builder.setNegativeButton("Cerrar", null);
        builder.show();
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
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMyLocation();
        }
    }

    private void loadFollowingList() {
        new LoadFollowingTask().execute();
    }

    private GeoPoint getOffsetPosition(double lat, double lon, int offsetIndex, int totalEnCoordenada) {
        double radius = 0.0008;
        double angle = (360.0 / totalEnCoordenada) * offsetIndex;
        double radians = Math.toRadians(angle);
        double deltaLat = Math.cos(radians) * radius;
        double deltaLon = Math.sin(radians) * radius;
        return new GeoPoint(lat + deltaLat, lon + deltaLon);
    }

    private void addMarkerToMap(Post post, int index, int totalEnUbicacion) {
        if (post == null) return;

        if (post.isTieneCoordenadas() && post.getLatitud() != 0 && post.getLongitud() != 0) {
            GeoPoint point;

            if (totalEnUbicacion > 1) {
                point = getOffsetPosition(post.getLatitud(), post.getLongitud(), index, totalEnUbicacion);
            } else {
                point = new GeoPoint(post.getLatitud(), post.getLongitud());
            }

            String coordKey = String.format("%.6f,%.6f", post.getLatitud(), post.getLongitud());
            String uniqueTitle = coordKey + "_" + index;

            String fecha = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(post.getFecha());
            String description = post.getUserName() + " - " + post.getCategoria() + "\n" + fecha;

            OverlayItem overlayItem = new OverlayItem(
                    uniqueTitle,
                    post.getTitulo(),
                    description,
                    point
            );

            if (!markerToPosts.containsKey(coordKey)) {
                markerToPosts.put(coordKey, new ArrayList<>());
            }

            List<Post> postsEnUbicacion = markerToPosts.get(coordKey);
            boolean yaExiste = false;
            for (Post p : postsEnUbicacion) {
                if (p.getId().equals(post.getId())) {
                    yaExiste = true;
                    break;
                }
            }
            if (!yaExiste) {
                postsEnUbicacion.add(post);
            }

            Drawable icon = createMarkerIcon(getMarkerColor(post.getCategoria()), totalEnUbicacion > 1);
            overlayItem.setMarker(icon);
            overlayItems.add(overlayItem);
        }
    }

    private void refreshMapMarkers() {
        overlayItems.clear();
        markerToPosts.clear();

        Map<String, List<Post>> postsPorCoordenada = new HashMap<>();

        for (Post post : publicacionesFiltradas) {
            if (post.isTieneCoordenadas() && post.getLatitud() != 0 && post.getLongitud() != 0) {
                String coordKey = String.format("%.6f,%.6f", post.getLatitud(), post.getLongitud());
                if (!postsPorCoordenada.containsKey(coordKey)) {
                    postsPorCoordenada.put(coordKey, new ArrayList<>());
                }
                postsPorCoordenada.get(coordKey).add(post);
            }
        }

        for (Map.Entry<String, List<Post>> entry : postsPorCoordenada.entrySet()) {
            List<Post> postsEnCoordenada = entry.getValue();
            int total = postsEnCoordenada.size();
            for (int i = 0; i < total; i++) {
                addMarkerToMap(postsEnCoordenada.get(i), i, total);
            }
        }

        itemizedOverlay.removeAllItems();
        for (OverlayItem item : overlayItems) {
            itemizedOverlay.addItem(item);
        }
        mapView.invalidate();

        Log.d(TAG, "Marcadores actualizados: " + overlayItems.size());
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

    private Drawable createMarkerIcon(int color, boolean hasMultiple) {
        int size = hasMultiple ? 72 : 80;
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

        if (hasMultiple) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(size / 3);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText("📌", size / 2, size / 2 + (size / 6), paint);
        }

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
                following.add(currentUserId);

                String urlStr = Configuracion.SERVIDOR + "/db_seguidores/_design/seguidores/_view/por_seguidor?key=\"" + currentUserId + "\"";
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
                            String followingId = row.optString("value");
                            if (!followingId.isEmpty() && !followingId.equals(currentUserId)) {
                                following.add(followingId);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error en LoadFollowingTask", e);
            }
            return following;
        }

        @Override
        protected void onPostExecute(Set<String> result) {
            followingIds = result;
            checkPermissionsAndLoad();
            loadAllPosts();
        }
    }

    private void loadAllPosts() {
        new LoadAllPostsTask().execute();
    }

    private class LoadAllPostsTask extends AsyncTask<Void, Void, List<Post>> {
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

            for (String userId : followingIds) {
                try {
                    String encodedUserId = URLEncoder.encode("\"" + userId + "\"", "UTF-8");
                    String urlStr = Configuracion.SERVIDOR + "/db_publicaciones/_design/publicaciones/_view/todas_con_ubicacion?key=" + encodedUserId;

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
                                JSONObject doc = row.optJSONObject("value");

                                if (doc != null) {
                                    String docId = doc.getString("_id");
                                    Post post = Post.fromJSON(doc, docId);
                                    postList.add(post);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error cargando publicaciones para usuario " + userId, e);
                }
            }
            return postList;
        }

        @Override
        protected void onPostExecute(List<Post> result) {
            progressBar.setVisibility(View.GONE);

            if (result != null && !result.isEmpty()) {
                todasLasPublicaciones = result;
                aplicarFiltroYActualizarMapa();
                Toast.makeText(MapsActivity.this, "📍 " + result.size() + " publicaciones cargadas", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MapsActivity.this, "No hay publicaciones con ubicación", Toast.LENGTH_LONG).show();
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