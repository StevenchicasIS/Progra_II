package com.example.tiendasqlite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText campoBuscar;
    private Button botonBuscar, botonAgregar, botonVerTodos, botonWebServices; // NUEVO
    private RecyclerView listaProductos;
    private AdaptadorProductos adaptador;
    private ProductoDAO dao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = new ProductoDAO(this);

        campoBuscar = findViewById(R.id.etBuscar);
        botonBuscar = findViewById(R.id.btnBuscar);
        botonAgregar = findViewById(R.id.btnAgregar);
        botonVerTodos = findViewById(R.id.btnVerTodos);
        botonWebServices = findViewById(R.id.btnWebServices); // NUEVO
        listaProductos = findViewById(R.id.rvProductos);

        listaProductos.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new AdaptadorProductos();
        listaProductos.setAdapter(adaptador);

        adaptador.setOnItemClickListener(new AdaptadorProductos.OnItemClickListener() {
            @Override
            public void alHacerClick(Producto producto) {
                Intent intent = new Intent(MainActivity.this, EditarProductoActivity.class);
                intent.putExtra("producto_id", producto.getId());
                startActivity(intent);
            }

            @Override
            public void alMantenerClick(Producto producto) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Eliminar Producto")
                        .setMessage("¿Eliminar " + producto.getDescripcion() + "?")
                        .setPositiveButton("Sí", (d, w) -> {
                            dao.eliminar(producto.getId());
                            cargarProductos();
                            Toast.makeText(MainActivity.this, "Producto eliminado", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

        botonAgregar.setOnClickListener(v -> {
            startActivity(new Intent(this, AgregarProductoActivity.class));
        });

        botonVerTodos.setOnClickListener(v -> {
            cargarProductos();
            campoBuscar.setText("");
        });

        botonBuscar.setOnClickListener(v -> {
            String texto = campoBuscar.getText().toString().trim();
            if (texto.isEmpty()) {
                cargarProductos();
            } else {
                List<Producto> resultados = dao.buscar(texto);
                adaptador.setListaProductos(resultados);
                if (resultados.isEmpty()) {
                    Toast.makeText(this, "No encontrado", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ==================== NUEVO: BOTÓN WEBSERVICES ====================
        botonWebServices.setOnClickListener(v -> {
            consultarWebServices();
        });

        cargarProductos();
    }

    private void cargarProductos() {
        List<Producto> productos = dao.obtenerTodos();
        adaptador.setListaProductos(productos);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarProductos();
    }

    // ==================== NUEVO: CONSULTAR WEBSERVICES (COUCHDB) ====================
    private void consultarWebServices() {
        // Verificar conexión a internet
        if (!ConexionCouchDB.hayInternet(this)) {
            Toast.makeText(this, "⚠️ Sin conexión a internet", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "🌐 Consultando WebServices...", Toast.LENGTH_SHORT).show();

        new ConexionCouchDB.ObtenerProductosTask(new ConexionCouchDB.ObtenerProductosTask.OnProductosListener() {
            @Override
            public void onSuccess(JSONArray productosCloud) {
                runOnUiThread(() -> {
                    try {
                        int cantidad = productosCloud.length();

                        if (cantidad == 0) {
                            new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("🌐 WebServices - CouchDB")
                                    .setMessage("No hay productos en la nube.\n\nAgrega algunos en CouchDB: http://localhost:5984/_utils/")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }

                        StringBuilder mensaje = new StringBuilder();
                        mensaje.append("📦 Productos en la nube:\n\n");

                        for (int i = 0; i < cantidad && i < 10; i++) {
                            JSONObject prod = productosCloud.getJSONObject(i);
                            mensaje.append("• ").append(prod.getString("codigo"))
                                    .append(" - ").append(prod.getString("descripcion"))
                                    .append("\n  💰 $").append(prod.getDouble("precio"))
                                    .append("\n\n");
                        }

                        if (cantidad > 10) {
                            mensaje.append("... y ").append(cantidad - 10).append(" productos más");
                        }

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("🌐 WebServices - CouchDB")
                                .setMessage(mensaje.toString())
                                .setPositiveButton("OK", null)
                                .show();

                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "❌ Error: " + error, Toast.LENGTH_LONG).show());
            }
        }).execute();
    }
}