package com.example.comunidadsv;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CreatePostActivity extends AppCompatActivity {

    private EditText edtTitulo, edtDescripcion, edtUbicacion;
    private TextView txtContador, txtEstadoGps;
    private Button btnPublicar;
    private ProgressBar progressBar;
    private GridLayout gridCategorias;

    private String categoriaSeleccionada = "";
    private String userId, userName, userEmail;
    private List<Uri> imagenesSeleccionadas = new ArrayList<>();
    private List<String> imagenesBase64 = new ArrayList<>();
    private List<Bitmap> imagenesBitmap = new ArrayList<>();
    private LocationManager locationManager;
    private LocationListener locationListener;

    // Variables para coordenadas
    private double latitudActual = 0;
    private double longitudActual = 0;
    private boolean tieneCoordenadas = false;
    private boolean obteniendoUbicacion = false;

    // Moderador de contenido
    private ContentModerator contentModerator;

    // RecyclerView para fotos
    private RecyclerView rvFotosPreview;
    private FotoPreviewAdapter fotoAdapter;
    private LinearLayout btnAgregarFotoLayout, btnTomarFotoLayout;

    // Constantes
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_IMAGE_CAMERA = 3;

    private TextView[] categorias = new TextView[8];
    private int[] categoriaIds = {
            R.id.catTrafico, R.id.catSeguridad, R.id.catEmergencia,
            R.id.catMascotas, R.id.catEventos, R.id.catAyuda,
            R.id.catObjetos, R.id.catOtros
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        // Inicializar moderador de contenido
        contentModerator = new ContentModerator(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtTitulo = findViewById(R.id.edtTitulo);
        edtDescripcion = findViewById(R.id.edtDescripcion);
        edtUbicacion = findViewById(R.id.edtUbicacion);
        txtContador = findViewById(R.id.txtContador);
        btnPublicar = findViewById(R.id.btnPublicar);
        progressBar = findViewById(R.id.progressBar);
        gridCategorias = findViewById(R.id.gridCategorias);
        txtEstadoGps = findViewById(R.id.txtEstadoGps);

        // Inicializar RecyclerView para fotos
        rvFotosPreview = findViewById(R.id.rvFotosPreview);
        rvFotosPreview.setLayoutManager(new GridLayoutManager(this, 2));

        fotoAdapter = new FotoPreviewAdapter(position -> {
            // Eliminar foto
            if (position < imagenesSeleccionadas.size()) {
                imagenesSeleccionadas.remove(position);
                imagenesBase64.remove(position);
                if (position < imagenesBitmap.size()) {
                    imagenesBitmap.remove(position);
                }
                fotoAdapter.removeFoto(position);
                Toast.makeText(this, "Foto eliminada", Toast.LENGTH_SHORT).show();
            }
        });
        rvFotosPreview.setAdapter(fotoAdapter);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        for (int i = 0; i < categoriaIds.length; i++) {
            categorias[i] = findViewById(categoriaIds[i]);
            final int index = i;
            categorias[i].setOnClickListener(v -> seleccionarCategoria(index));
        }

        edtDescripcion.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                txtContador.setText(length + "/500");
                if (length > 500) {
                    edtDescripcion.setError("Máximo 500 caracteres");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        SharedPreferences prefs = getSharedPreferences("ComunidadSV", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        userName = prefs.getString("nombre", "Usuario");
        userEmail = prefs.getString("email", "");

        ImageView btnMiUbicacion = findViewById(R.id.btnMiUbicacion);
        btnMiUbicacion.setOnClickListener(v -> obtenerUbicacionActual());

        // Botones para agregar fotos (ahora son LinearLayouts)
        btnAgregarFotoLayout = findViewById(R.id.btnAgregarFoto);
        btnTomarFotoLayout = findViewById(R.id.btnTomarFoto);

        btnAgregarFotoLayout.setOnClickListener(v -> seleccionarImagen());
        btnTomarFotoLayout.setOnClickListener(v -> tomarFoto());

        btnPublicar.setOnClickListener(v -> publicar());

        obtenerUbicacionActual();
    }

    private void seleccionarCategoria(int index) {
        for (TextView cat : categorias) {
            cat.setBackgroundResource(R.drawable.category_unselected);
            cat.setTextColor(getColor(R.color.gray_text));
        }

        categorias[index].setBackgroundResource(R.drawable.category_selected);
        categorias[index].setTextColor(getColor(android.R.color.white));

        String[] nombresCategorias = {"Tráfico", "Seguridad", "Emergencia", "Mascotas",
                "Eventos", "Ayuda", "Objetos", "Otros"};
        categoriaSeleccionada = nombresCategorias[index];
    }

    private void obtenerUbicacionActual() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mostrarDialogoActivarGPS();
            return;
        }

        iniciarGPS();
    }

    private void mostrarDialogoActivarGPS() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("GPS desactivado")
                .setMessage("Para obtener tu ubicación automática, necesitas activar el GPS. ¿Quieres activarlo?")
                .setPositiveButton("Activar GPS", (dialog, which) -> {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                })
                .setNegativeButton("Escribir manualmente", (dialog, which) -> {
                    txtEstadoGps.setVisibility(View.GONE);
                    edtUbicacion.setHint("Escribe tu ubicación manualmente");
                    Toast.makeText(this, "Puedes escribir tu ubicación manualmente", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void iniciarGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        obteniendoUbicacion = true;
        txtEstadoGps.setVisibility(View.VISIBLE);
        txtEstadoGps.setText("🔄 Obteniendo ubicación...");
        txtEstadoGps.setTextColor(getColor(R.color.gray_text));

        Location lastLocation = null;
        try {
            lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation == null) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (lastLocation != null && lastLocation.getAccuracy() < 100) {
            latitudActual = lastLocation.getLatitude();
            longitudActual = lastLocation.getLongitude();
            tieneCoordenadas = true;
            obteniendoUbicacion = false;
            txtEstadoGps.setText("✅ Ubicación obtenida!");
            txtEstadoGps.setTextColor(getColor(R.color.green_primary));
            new ObtenerDireccionTask().execute(latitudActual, longitudActual);
            return;
        }

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (obteniendoUbicacion) {
                    latitudActual = location.getLatitude();
                    longitudActual = location.getLongitude();
                    tieneCoordenadas = true;
                    obteniendoUbicacion = false;
                    txtEstadoGps.setText("✅ Ubicación obtenida!");
                    txtEstadoGps.setTextColor(getColor(R.color.green_primary));
                    new ObtenerDireccionTask().execute(latitudActual, longitudActual);

                    try {
                        locationManager.removeUpdates(locationListener);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onProviderDisabled(String provider) {
                if (provider.equals(LocationManager.GPS_PROVIDER) && obteniendoUbicacion) {
                    txtEstadoGps.setText("⚠️ GPS desactivado");
                    txtEstadoGps.setTextColor(getColor(R.color.red));
                }
            }

            @Override
            public void onProviderEnabled(String provider) {
                if (provider.equals(LocationManager.GPS_PROVIDER) && obteniendoUbicacion) {
                    txtEstadoGps.setText("🔄 GPS activado, buscando...");
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}
        };

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000, 5, locationListener
            );

            new android.os.Handler().postDelayed(() -> {
                if (obteniendoUbicacion) {
                    obteniendoUbicacion = false;
                    if (latitudActual == 0 && longitudActual == 0) {
                        txtEstadoGps.setText("⚠️ No se pudo obtener ubicación");
                        txtEstadoGps.setTextColor(getColor(R.color.red));
                        Toast.makeText(CreatePostActivity.this,
                                "No se pudo obtener ubicación. Puedes escribirla manualmente.",
                                Toast.LENGTH_LONG).show();
                    }
                    try {
                        locationManager.removeUpdates(locationListener);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                }
            }, 15000);

        } catch (SecurityException e) {
            e.printStackTrace();
            txtEstadoGps.setText("❌ Error de permisos");
            obteniendoUbicacion = false;
        }
    }

    private void tomarFoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_IMAGE_CAMERA);
            return;
        }

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAMERA);
        } else {
            Toast.makeText(this, "No se encontró una aplicación de cámara", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso concedido, obteniendo ubicación...", Toast.LENGTH_SHORT).show();
                obtenerUbicacionActual();
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_LONG).show();
                edtUbicacion.setHint("Escribe tu ubicación manualmente");
                tieneCoordenadas = false;
            }
        }

        if (requestCode == REQUEST_IMAGE_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                tomarFoto();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ObtenerDireccionTask extends AsyncTask<Double, Void, String> {
        @Override
        protected String doInBackground(Double... params) {
            double lat = params[0];
            double lon = params[1];

            Geocoder geocoder = new Geocoder(CreatePostActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String direccion = "";
                    if (address.getSubLocality() != null) direccion += address.getSubLocality();
                    if (address.getLocality() != null) {
                        if (!direccion.isEmpty()) direccion += ", ";
                        direccion += address.getLocality();
                    }
                    if (direccion.isEmpty() && address.getAddressLine(0) != null) {
                        direccion = address.getAddressLine(0);
                    }
                    if (direccion.isEmpty()) {
                        direccion = String.format("%.4f, %.4f", lat, lon);
                    }
                    return direccion;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return String.format("%.4f, %.4f", lat, lon);
        }

        @Override
        protected void onPostExecute(String direccion) {
            edtUbicacion.setText(direccion);
        }
    }

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void procesarImagen(Uri imagenUri) {
        if (imagenesSeleccionadas.size() < 4) {
            imagenesSeleccionadas.add(imagenUri);
            fotoAdapter.addFoto(imagenUri);

            String base64 = ImageUtils.uriToBase64(this, imagenUri);
            if (base64 != null) {
                imagenesBase64.add(base64);
                Bitmap bitmap = ImageUtils.base64ToBitmap(base64);
                if (bitmap != null) {
                    imagenesBitmap.add(bitmap);
                }
            }

            // Scroll al final del RecyclerView
            rvFotosPreview.smoothScrollToPosition(fotoAdapter.getItemCount() - 1);
        } else {
            Toast.makeText(this, "Máximo 4 fotos", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            Uri imagenUri = data.getData();
            procesarImagen(imagenUri);
        }

        if (requestCode == REQUEST_IMAGE_CAMERA && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            if (imageBitmap != null) {
                String path = MediaStore.Images.Media.insertImage(getContentResolver(), imageBitmap, "Foto_" + System.currentTimeMillis(), null);
                if (path != null) {
                    Uri imagenUri = Uri.parse(path);
                    procesarImagen(imagenUri);
                } else {
                    Toast.makeText(this, "Error al procesar la foto", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void publicar() {
        String titulo = edtTitulo.getText().toString().trim();
        String descripcion = edtDescripcion.getText().toString().trim();
        String ubicacion = edtUbicacion.getText().toString().trim();

        if (titulo.isEmpty()) {
            edtTitulo.setError("Escribe un título");
            return;
        }
        if (descripcion.isEmpty()) {
            edtDescripcion.setError("Escribe una descripción");
            return;
        }
        if (descripcion.length() > 500) {
            edtDescripcion.setError("Máximo 500 caracteres");
            return;
        }
        if (categoriaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Selecciona una categoría", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ubicacion.isEmpty()) {
            edtUbicacion.setError("Agrega una ubicación");
            return;
        }

        ContentModerator.ModerationResult titleCheck = contentModerator.analyzeText(titulo);
        if (!titleCheck.isAppropriate) {
            mostrarDialogoModeracion(titleCheck.reason);
            return;
        }

        ContentModerator.ModerationResult contentCheck = contentModerator.analyzeText(descripcion);
        if (!contentCheck.isAppropriate) {
            mostrarDialogoModeracion(contentCheck.reason);
            return;
        }

        if (imagenesBitmap != null && !imagenesBitmap.isEmpty()) {
            progressBar.setVisibility(View.VISIBLE);
            btnPublicar.setEnabled(false);
            verificarImagenesSecuencialmente(0, titulo, descripcion, ubicacion);
        } else {
            procederConPublicacion(titulo, descripcion, ubicacion);
        }
    }

    private void verificarImagenesSecuencialmente(int index, String titulo, String descripcion, String ubicacion) {
        if (index >= imagenesBitmap.size()) {
            procederConPublicacion(titulo, descripcion, ubicacion);
            return;
        }

        Bitmap bitmap = imagenesBitmap.get(index);
        if (bitmap == null) {
            verificarImagenesSecuencialmente(index + 1, titulo, descripcion, ubicacion);
            return;
        }

        contentModerator.analyzeImage(bitmap, new ContentModerator.ModerationCallback() {
            @Override
            public void onApproved() {
                verificarImagenesSecuencialmente(index + 1, titulo, descripcion, ubicacion);
            }

            @Override
            public void onRejected(String reason) {
                progressBar.setVisibility(View.GONE);
                btnPublicar.setEnabled(true);
                mostrarDialogoModeracion("Imagen inapropiada: " + reason);
            }
        });
    }

    private void procederConPublicacion(String titulo, String descripcion, String ubicacion) {
        if (!tieneCoordenadas || latitudActual == 0 || longitudActual == 0) {
            Toast.makeText(this, "Buscando coordenadas...", Toast.LENGTH_SHORT).show();
            new GeocodificarDireccionTask().execute(ubicacion, titulo, descripcion);
        } else {
            new CrearPublicacionTask().execute(titulo, descripcion, ubicacion);
        }
    }

    private void mostrarDialogoModeracion(String razon) {
        new AlertDialog.Builder(this)
                .setTitle("Contenido no permitido")
                .setMessage("❌ Tu publicación ha sido detectada como contenido inapropiado.\n\n" +
                        "Motivo: " + razon + "\n\n" +
                        "Por favor, respeta las normas de la comunidad.")
                .setPositiveButton("Entendido", null)
                .show();
    }

    private class GeocodificarDireccionTask extends AsyncTask<String, Void, double[]> {
        private String titulo, descripcion, ubicacion;

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnPublicar.setEnabled(false);
        }

        @Override
        protected double[] doInBackground(String... params) {
            ubicacion = params[0];
            titulo = params[1];
            descripcion = params[2];

            Geocoder geocoder = new Geocoder(CreatePostActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocationName(ubicacion + ", El Salvador", 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    return new double[]{address.getLatitude(), address.getLongitude()};
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return new double[]{0, 0};
        }

        @Override
        protected void onPostExecute(double[] coordenadas) {
            if (coordenadas[0] != 0 && coordenadas[1] != 0) {
                latitudActual = coordenadas[0];
                longitudActual = coordenadas[1];
                tieneCoordenadas = true;
                new CrearPublicacionTask().execute(titulo, descripcion, ubicacion);
            } else {
                progressBar.setVisibility(View.GONE);
                btnPublicar.setEnabled(true);
                Toast.makeText(CreatePostActivity.this,
                        "No se encontraron coordenadas para esta ubicación.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class CrearPublicacionTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";
        private String ubicacionTexto = "";

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            btnPublicar.setEnabled(false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String titulo = params[0];
            String descripcion = params[1];
            ubicacionTexto = params[2];

            try {
                JSONObject publicacion = new JSONObject();
                publicacion.put("tipo", "publicacion");
                publicacion.put("userId", userId);
                publicacion.put("userName", userName);
                publicacion.put("userEmail", userEmail);
                publicacion.put("titulo", titulo);
                publicacion.put("contenido", descripcion);
                publicacion.put("categoria", categoriaSeleccionada);
                publicacion.put("ubicacion", ubicacionTexto);
                publicacion.put("fecha", System.currentTimeMillis());
                publicacion.put("likes", 0);
                publicacion.put("likedBy", new org.json.JSONArray());
                publicacion.put("comments", new org.json.JSONArray());
                publicacion.put("latitud", latitudActual);
                publicacion.put("longitud", longitudActual);
                publicacion.put("tieneCoordenadas", tieneCoordenadas && latitudActual != 0);

                org.json.JSONArray imagenesArray = new org.json.JSONArray();
                for (String base64 : imagenesBase64) {
                    imagenesArray.put(base64);
                }
                publicacion.put("imagenesBase64", imagenesArray);

                String postId = UUID.randomUUID().toString();
                String urlStr = Configuracion.SERVIDOR + "/db_publicaciones/" + postId;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(publicacion.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();
                return responseCode == 201 || responseCode == 202;

            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            progressBar.setVisibility(View.GONE);
            btnPublicar.setEnabled(true);

            if (success) {
                String mensaje = "¡Publicación creada exitosamente! ✅";
                if (tieneCoordenadas && latitudActual != 0) {
                    mensaje += " 📍 Aparecerá en el mapa";
                }
                Toast.makeText(CreatePostActivity.this, mensaje, Toast.LENGTH_LONG).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(CreatePostActivity.this, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        if (imagenesBitmap != null) {
            for (Bitmap bmp : imagenesBitmap) {
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
            }
            imagenesBitmap.clear();
        }
    }
}