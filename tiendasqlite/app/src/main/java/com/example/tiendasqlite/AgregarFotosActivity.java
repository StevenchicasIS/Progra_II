package com.example.tiendasqlite;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import de.hdodenhof.circleimageview.CircleImageView;

public class AgregarFotosActivity extends AppCompatActivity {

    private TextView tvProductoInfo, tvCantidadFotos;
    private RecyclerView rvFotos;
    private Button btnAgregarFoto, btnFinalizar;
    private ProductoDAO dao;
    private Producto productoActual;
    private int idProducto;
    private List<byte[]> listaFotos;
    private FotoGridAdapter adapter;

    private static final int SOLICITUD_PERMISOS = 100;
    private static final int SOLICITUD_CAMARA = 101;
    private static final int SOLICITUD_GALERIA = 102;

    private String rutaFotoActual;
    private Uri uriFotoActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_fotos);

        dao = new ProductoDAO(this);
        idProducto = getIntent().getIntExtra("producto_id", -1);

        if (idProducto == -1) {
            Toast.makeText(this, "Error: Producto no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productoActual = dao.obtenerPorId(idProducto);
        listaFotos = dao.obtenerFotos(idProducto);

        tvProductoInfo = findViewById(R.id.tvProductoInfo);
        tvCantidadFotos = findViewById(R.id.tvCantidadFotos);
        rvFotos = findViewById(R.id.rvFotos);
        btnAgregarFoto = findViewById(R.id.btnAgregarFoto);
        btnFinalizar = findViewById(R.id.btnFinalizar);

        rvFotos.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new FotoGridAdapter(listaFotos, new FotoGridAdapter.OnFotoClickListener() {
            @Override
            public void onEliminarClick(int position) {
                confirmarEliminarFoto(position);
            }

            @Override
            public void onFotoClick(int position) {
                verFotoGrande(position);
            }
        });
        rvFotos.setAdapter(adapter);

        tvProductoInfo.setText(productoActual.getCodigo() + " - " + productoActual.getDescripcion());
        actualizarContadorFotos();

        btnAgregarFoto.setOnClickListener(v -> mostrarOpciones());
        btnFinalizar.setOnClickListener(v -> finish());
    }

    private void actualizarContadorFotos() {
        int cantidad = listaFotos.size();
        if (cantidad == 0) {
            tvCantidadFotos.setText("📭 Sin fotos");
        } else if (cantidad == 1) {
            tvCantidadFotos.setText("📸 1 foto");
        } else {
            tvCantidadFotos.setText("📸 " + cantidad + " fotos");
        }
    }

    private void mostrarOpciones() {
        String[] opciones = {"📷 Tomar foto con cámara", "🖼️ Seleccionar de galería"};
        new AlertDialog.Builder(this)
                .setTitle("Agregar foto")
                .setItems(opciones, (d, which) -> {
                    if (which == 0) {
                        verificarPermisosCamara();
                    } else {
                        verificarPermisosGaleria();
                    }
                })
                .show();
    }

    private void verificarPermisosCamara() {
        String[] permisos;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permisos = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        boolean todosPermisos = true;
        for (String permiso : permisos) {
            if (ContextCompat.checkSelfPermission(this, permiso) != PackageManager.PERMISSION_GRANTED) {
                todosPermisos = false;
                break;
            }
        }

        if (todosPermisos) {
            abrirCamara();
        } else {
            ActivityCompat.requestPermissions(this, permisos, SOLICITUD_PERMISOS);
        }
    }

    private void verificarPermisosGaleria() {
        String permiso;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permiso = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permiso = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permiso) == PackageManager.PERMISSION_GRANTED) {
            abrirGaleria();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permiso}, SOLICITUD_PERMISOS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SOLICITUD_PERMISOS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Se necesitan permisos", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            mostrarOpciones();
        }
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File archivoFoto = null;
            try {
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                File directorio = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
                archivoFoto = File.createTempFile("FOTO_" + timestamp, ".jpg", directorio);
                rutaFotoActual = archivoFoto.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (archivoFoto != null) {
                uriFotoActual = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider",
                        archivoFoto);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFotoActual);
                startActivityForResult(intent, SOLICITUD_CAMARA);
            }
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, SOLICITUD_GALERIA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Uri imagenUri = null;

            if (requestCode == SOLICITUD_CAMARA) {
                imagenUri = uriFotoActual;
            } else if (requestCode == SOLICITUD_GALERIA && data != null) {
                imagenUri = data.getData();
            }

            if (imagenUri != null) {
                guardarImagen(imagenUri);
            }
        }
    }

    private void guardarImagen(Uri imagenUri) {
        try {
            ContentResolver resolver = getContentResolver();
            InputStream inputStream = resolver.openInputStream(imagenUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap == null) {
                Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
                return;
            }

            bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
            byte[] bytesImagen = UtilidadImagenes.bitmapToBytes(bitmap);

            listaFotos.add(bytesImagen);
            dao.guardarFotoProducto(idProducto, bytesImagen);

            listaFotos = dao.obtenerFotos(idProducto);
            adapter.actualizarFotos(listaFotos);
            actualizarContadorFotos();

            Toast.makeText(this, "✅ Foto guardada", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEliminarFoto(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar foto")
                .setMessage("¿Eliminar esta foto?")
                .setPositiveButton("Sí", (d, w) -> {
                    try {
                        dao.eliminarFoto(idProducto, position);
                        listaFotos = dao.obtenerFotos(idProducto);
                        adapter.actualizarFotos(listaFotos);
                        actualizarContadorFotos();
                        Toast.makeText(this, "✅ Foto eliminada", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void verFotoGrande(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_foto_grande, null);
        CircleImageView ivGrande = view.findViewById(R.id.ivFotoGrande);
        UtilidadImagenes.cargarImagenRedondeada(ivGrande, listaFotos.get(position));
        builder.setView(view);
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        listaFotos = dao.obtenerFotos(idProducto);
        if (adapter != null) {
            adapter.actualizarFotos(listaFotos);
        }
        actualizarContadorFotos();
    }
}