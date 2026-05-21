package com.example.comunidadsv;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ProgressBar progressBar;
    private LinearLayout navHome, navMap, navChat, navProfile;
    private Toolbar toolbar;
    private List<Post> postsConUbicacion = new ArrayList<>();
    private HashMap<Marker, Post> markerPostMap = new HashMap<>();
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        progressBar = findViewById(R.id.progressBar);
        navHome = findViewById(R.id.navHome);
        navMap = findViewById(R.id.navMap);
        navChat = findViewById(R.id.navChat);
        navProfile = findViewById(R.id.navProfile);

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        currentUserId = prefs.getString("userId", "");

        setupBottomNavigation();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadPostsWithLocation();
    }

    private void setupBottomNavigation() {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, FeedActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        navMap.setOnClickListener(v -> {
            // Ya estamos en mapa
        });

        navChat.setOnClickListener(v -> {
            Toast.makeText(this, "Próximamente: Chat", Toast.LENGTH_SHORT).show();
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, ProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });
    }

    private void loadPostsWithLocation() {
        new LoadPostsTask().execute();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Centrar en El Salvador
        LatLng elSalvador = new LatLng(13.7942, -88.8965);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(elSalvador, 8));

        mMap.setOnMarkerClickListener(marker -> {
            Post post = markerPostMap.get(marker);
            if (post != null) {
                showPostDialog(post);
            }
            return true;
        });
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

    private void addMarkerToMap(Post post) {
        try {
            LatLng location = getLocationFromAddress(post.getUbicacionPost());
            if (location != null) {
                float markerColor = getMarkerColor(post.getCategoria());

                MarkerOptions markerOptions = new MarkerOptions()
                        .position(location)
                        .title(post.getTitulo())
                        .snippet(post.getUserName() + " - " + post.getCategoria())
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor));

                Marker marker = mMap.addMarker(markerOptions);
                if (marker != null) {
                    markerPostMap.put(marker, post);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private float getMarkerColor(String categoria) {
        switch (categoria) {
            case "Tráfico": return BitmapDescriptorFactory.HUE_RED;
            case "Seguridad": return BitmapDescriptorFactory.HUE_ORANGE;
            case "Emergencia": return BitmapDescriptorFactory.HUE_RED;
            case "Mascotas": return BitmapDescriptorFactory.HUE_YELLOW;
            case "Eventos": return BitmapDescriptorFactory.HUE_GREEN;
            case "Ayuda": return BitmapDescriptorFactory.HUE_CYAN;
            case "Objetos": return BitmapDescriptorFactory.HUE_BLUE;
            default: return BitmapDescriptorFactory.HUE_VIOLET;
        }
    }

    private LatLng getLocationFromAddress(String address) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(address, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address location = addresses.get(0);
                return new LatLng(location.getLatitude(), location.getLongitude());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
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

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
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
                            if (post.getUbicacionPost() != null && !post.getUbicacionPost().isEmpty()) {
                                postList.add(post);
                            }
                        }
                    }
                } else {
                    errorMsg = "Error HTTP: " + responseCode;
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
                postsConUbicacion = result;
                for (Post post : postsConUbicacion) {
                    addMarkerToMap(post);
                }
                if (postsConUbicacion.isEmpty()) {
                    Toast.makeText(MapsActivity.this, "No hay publicaciones con ubicación", Toast.LENGTH_SHORT).show();
                }
            } else if (!errorMsg.isEmpty()) {
                Toast.makeText(MapsActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}