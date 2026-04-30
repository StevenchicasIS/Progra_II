package com.example.tiendasqlite;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class EditarProductoActivity extends AppCompatActivity {

    private EditText etCodigo, etDescripcion, etMarca, etPresentacion, etPrecio;
    private CircleImageView ivFotoPortada;
    private Button btnCambiarFoto, btnGuardarCambios, btnCancelar, btnVerTodasFotos;
    private ProductoDAO dao;
    private Producto productoActual;
    private int productoId;
    private byte[] nuevaFotoPortada = null;

    private static final int SOLICITUD_CAMARA = 1;
    private static final int SOLICITUD_GALERIA = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_producto);

        dao = new ProductoDAO(this);
        productoId = getIntent().getIntExtra("producto_id", -1);

        if (productoId == -1) {
            Toast.makeText(this, "Error: Producto no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productoActual = dao.obtenerPorId(productoId);
        if (productoActual == null) {
            Toast.makeText(this, "Error: Producto no existe", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etCodigo = findViewById(R.id.etCodigo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etMarca = findViewById(R.id.etMarca);
        etPresentacion = findViewById(R.id.etPresentacion);
        etPrecio = findViewById(R.id.etPrecio);
        ivFotoPortada = findViewById(R.id.ivFotoPortada);
        btnCambiarFoto = findViewById(R.id.btnCambiarFoto);
        btnGuardarCambios = findViewById(R.id.btnGuardarCambios);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnVerTodasFotos = findViewById(R.id.btnVerTodasFotos);

        cargarDatos();

        btnCambiarFoto.setOnClickListener(v -> mostrarOpcionesImagen());
        btnGuardarCambios.setOnClickListener(v -> guardarCambios());
        btnCancelar.setOnClickListener(v -> finish());
        btnVerTodasFotos.setOnClickListener(v -> {
            Intent intent = new Intent(this, AgregarFotosActivity.class);
            intent.putExtra("producto_id", productoId);
            startActivity(intent);
        });
    }

    private void cargarDatos() {
        etCodigo.setText(productoActual.getCodigo());
        etDescripcion.setText(productoActual.getDescripcion());
        etMarca.setText(productoActual.getMarca());
        etPresentacion.setText(productoActual.getPresentacion());
        etPrecio.setText(String.valueOf(productoActual.getPrecio()));

        List<byte[]> fotos = productoActual.getFotos();
        if (fotos != null && !fotos.isEmpty()) {
            UtilidadImagenes.cargarImagenRedondeada(ivFotoPortada, fotos.get(0));
        }
    }

    private void mostrarOpcionesImagen() {
        String[] opciones = {"📷 Tomar foto", "🖼️ Seleccionar de galería"};
        new AlertDialog.Builder(this)
                .setTitle("Cambiar foto principal")
                .setItems(opciones, (d, which) -> {
                    if (which == 0) {
                        abrirCamara();
                    } else {
                        abrirGaleria();
                    }
                })
                .show();
    }

    private void abrirCamara() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, SOLICITUD_CAMARA);
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, SOLICITUD_GALERIA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri imagenUri = data.getData();

            if (imagenUri != null) {
                try {
                    ContentResolver resolver = getContentResolver();
                    InputStream inputStream = resolver.openInputStream(imagenUri);
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                    if (bitmap != null) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                        nuevaFotoPortada = UtilidadImagenes.bitmapToBytes(bitmap);
                        ivFotoPortada.setImageBitmap(bitmap);
                        Toast.makeText(this, "Foto principal actualizada", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void guardarCambios() {
        String codigo = etCodigo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String precioStr = etPrecio.getText().toString().trim();

        if (codigo.isEmpty() || descripcion.isEmpty() || precioStr.isEmpty()) {
            Toast.makeText(this, "Código, descripción y precio son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar código duplicado
        Producto existe = dao.buscarPorCodigo(codigo);
        if (existe != null && existe.getId() != productoId) {
            Toast.makeText(this, "Ya existe otro producto con ese código", Toast.LENGTH_SHORT).show();
            return;
        }

        // Actualizar datos del producto
        productoActual.setCodigo(codigo);
        productoActual.setDescripcion(descripcion);
        productoActual.setMarca(etMarca.getText().toString().trim());
        productoActual.setPresentacion(etPresentacion.getText().toString().trim());
        productoActual.setPrecio(precio);

        // Guardar en SQLite
        int resultado = dao.actualizar(productoActual);

        if (resultado > 0) {
            Toast.makeText(this, "✅ Producto actualizado localmente", Toast.LENGTH_SHORT).show();

            // Actualizar en CouchDB si tiene cloud_id y hay internet
            if (ConexionCouchDB.hayInternet(this)) {
                actualizarEnNube();
            }

            finish();
        } else {
            Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizarEnNube() {
        String cloudId = dao.getCloudId(productoId);
        String cloudRev = dao.getCloudRev(productoId);

        if (cloudId == null || cloudId.isEmpty()) {
            Toast.makeText(this, "⚠️ Producto no está en la nube", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("_id", cloudId);
            json.put("_rev", cloudRev);
            json.put("codigo", productoActual.getCodigo());
            json.put("descripcion", productoActual.getDescripcion());
            json.put("marca", productoActual.getMarca());
            json.put("presentacion", productoActual.getPresentacion());
            json.put("precio", productoActual.getPrecio());
            json.put("timestamp", System.currentTimeMillis());

            Toast.makeText(this, "📤 Actualizando en la nube...", Toast.LENGTH_SHORT).show();

            new ConexionCouchDB.ActualizarProductoTask(new ConexionCouchDB.ActualizarProductoTask.OnActualizarListener() {
                @Override
                public void onSuccess(String nuevaRev) {
                    dao.marcarComoSincronizado(productoId, cloudId, nuevaRev);
                    runOnUiThread(() ->
                            Toast.makeText(EditarProductoActivity.this,
                                    "✅ Producto actualizado en la nube", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(EditarProductoActivity.this,
                                    "❌ Error en nube: " + error, Toast.LENGTH_LONG).show());
                }
            }).execute(cloudId, cloudRev, json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}