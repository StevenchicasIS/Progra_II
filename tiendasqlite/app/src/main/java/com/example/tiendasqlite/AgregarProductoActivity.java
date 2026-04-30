package com.example.tiendasqlite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;

public class AgregarProductoActivity extends AppCompatActivity {

    private EditText campoCodigo, campoDescripcion, campoMarca, campoPresentacion, campoPrecio;
    private Button botonGuardar, botonCancelar;
    private ProductoDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agregar_producto);

        dao = new ProductoDAO(this);

        campoCodigo = findViewById(R.id.etCodigo);
        campoDescripcion = findViewById(R.id.etDescripcion);
        campoMarca = findViewById(R.id.etMarca);
        campoPresentacion = findViewById(R.id.etPresentacion);
        campoPrecio = findViewById(R.id.etPrecio);
        botonGuardar = findViewById(R.id.btnGuardar);
        botonCancelar = findViewById(R.id.btnCancelar);

        botonGuardar.setOnClickListener(v -> guardarProducto());
        botonCancelar.setOnClickListener(v -> finish());
    }

    private void guardarProducto() {
        String codigo = campoCodigo.getText().toString().trim();
        String descripcion = campoDescripcion.getText().toString().trim();
        String precioStr = campoPrecio.getText().toString().trim();

        if (codigo.isEmpty() || descripcion.isEmpty() || precioStr.isEmpty()) {
            Toast.makeText(this, "Completa código, descripción y precio", Toast.LENGTH_SHORT).show();
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(precioStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        String marca = campoMarca.getText().toString().trim();
        String presentacion = campoPresentacion.getText().toString().trim();

        // 1. Guardar en SQLite (offline)
        Producto producto = new Producto(codigo, descripcion, marca, presentacion, precio);
        long idLocal = dao.insertar(producto);

        if (idLocal > 0) {
            Toast.makeText(this, "✅ Producto guardado localmente", Toast.LENGTH_SHORT).show();

            // 2. Enviar a CouchDB (nube) si hay internet
            if (ConexionCouchDB.hayInternet(this)) {
                enviarANube(producto, (int) idLocal);
            } else {
                Toast.makeText(this, "⚠️ Sin internet. Se sincronizará después", Toast.LENGTH_LONG).show();
            }

            // Ir a agregar fotos
            Intent intent = new Intent(this, AgregarFotosActivity.class);
            intent.putExtra("producto_id", (int) idLocal);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Error o código duplicado", Toast.LENGTH_SHORT).show();
        }
    }

    private void enviarANube(Producto producto, int idLocal) {
        try {
            JSONObject json = new JSONObject();
            json.put("codigo", producto.getCodigo());
            json.put("descripcion", producto.getDescripcion());
            json.put("marca", producto.getMarca() != null ? producto.getMarca() : "");
            json.put("presentacion", producto.getPresentacion() != null ? producto.getPresentacion() : "");
            json.put("precio", producto.getPrecio());
            json.put("timestamp", System.currentTimeMillis());

            Toast.makeText(this, "📤 Enviando a la nube...", Toast.LENGTH_SHORT).show();

            new ConexionCouchDB.GuardarProductoTask(new ConexionCouchDB.GuardarProductoTask.OnGuardarListener() {
                @Override
                public void onSuccess(String cloudId, String cloudRev) {
                    dao.marcarComoSincronizado(idLocal, cloudId, cloudRev);
                    runOnUiThread(() ->
                            Toast.makeText(AgregarProductoActivity.this,
                                    "✅ Producto sincronizado con la nube\nID: " + cloudId,
                                    Toast.LENGTH_LONG).show());
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(AgregarProductoActivity.this,
                                    "❌ Error en nube: " + error,
                                    Toast.LENGTH_LONG).show());
                }
            }).execute(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al preparar datos: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}