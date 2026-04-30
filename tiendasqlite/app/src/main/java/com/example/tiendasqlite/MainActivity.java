package com.example.tiendasqlite;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText campoBuscar;
    private Button botonBuscar, botonAgregar, botonVerTodos, botonWebServices;
    private RecyclerView listaProductos;
    private AdaptadorProductos adaptador;
    private ProductoDAO dao;

    // Variable para el producto seleccionado en el menú contextual
    private Producto productoSeleccionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dao = new ProductoDAO(this);

        campoBuscar = findViewById(R.id.etBuscar);
        botonBuscar = findViewById(R.id.btnBuscar);
        botonAgregar = findViewById(R.id.btnAgregar);
        botonVerTodos = findViewById(R.id.btnVerTodos);
        botonWebServices = findViewById(R.id.btnWebServices);
        listaProductos = findViewById(R.id.rvProductos);

        listaProductos.setLayoutManager(new LinearLayoutManager(this));
        adaptador = new AdaptadorProductos();
        listaProductos.setAdapter(adaptador);

        // IMPORTANTE: Registrar el RecyclerView para menú contextual
        registerForContextMenu(listaProductos);

        adaptador.setOnItemClickListener(new AdaptadorProductos.OnItemClickListener() {
            @Override
            public void alHacerClick(Producto producto) {
                Intent intent = new Intent(MainActivity.this, EditarProductoActivity.class);
                intent.putExtra("producto_id", producto.getId());
                startActivity(intent);
            }

            @Override
            public void alMantenerClick(Producto producto) {
                // Guardar el producto seleccionado
                productoSeleccionado = producto;
                // Abrir el menú contextual
                openContextMenu(listaProductos);
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

    // ==================== MENÚ CONTEXTUAL ====================
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        if (productoSeleccionado != null) {
            menu.setHeaderTitle(productoSeleccionado.getDescripcion());
            menu.add(0, 1, 0, "✏️ Modificar producto");
            menu.add(0, 2, 0, "🗑️ Eliminar producto");
            menu.add(0, 3, 0, "📸 Ver todas las fotos");
            menu.add(0, 4, 0, "🌐 Sincronizar con nube");
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (productoSeleccionado == null) {
            return super.onContextItemSelected(item);
        }

        int id = item.getItemId();

        if (id == 1) {
            // Modificar producto
            Intent intent = new Intent(MainActivity.this, EditarProductoActivity.class);
            intent.putExtra("producto_id", productoSeleccionado.getId());
            startActivity(intent);
            return true;
        }
        else if (id == 2) {
            // Eliminar producto
            confirmarEliminacion(productoSeleccionado);
            return true;
        }
        else if (id == 3) {
            // Ver todas las fotos
            Intent intent = new Intent(MainActivity.this, AgregarFotosActivity.class);
            intent.putExtra("producto_id", productoSeleccionado.getId());
            startActivity(intent);
            return true;
        }
        else if (id == 4) {
            // Sincronizar con nube
            sincronizarProductoIndividual(productoSeleccionado);
            return true;
        }

        return super.onContextItemSelected(item);
    }

    private void confirmarEliminacion(Producto producto) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Producto")
                .setMessage("¿Eliminar " + producto.getDescripcion() + "?\n\nTambién se eliminará de la nube si existe")
                .setPositiveButton("Sí", (d, w) -> {
                    String cloudId = dao.getCloudId(producto.getId());
                    String cloudRev = dao.getCloudRev(producto.getId());

                    // Eliminar localmente
                    dao.eliminar(producto.getId());

                    // Eliminar en la nube si tiene cloud_id y hay internet
                    if (cloudId != null && !cloudId.isEmpty() && ConexionCouchDB.hayInternet(MainActivity.this)) {
                        eliminarEnNube(cloudId, cloudRev);
                    }

                    cargarProductos();
                    Toast.makeText(MainActivity.this, "Producto eliminado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void sincronizarProductoIndividual(Producto producto) {
        if (!ConexionCouchDB.hayInternet(this)) {
            Toast.makeText(this, "⚠️ Sin conexión a internet", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("codigo", producto.getCodigo());
            json.put("descripcion", producto.getDescripcion());
            json.put("marca", producto.getMarca() != null ? producto.getMarca() : "");
            json.put("presentacion", producto.getPresentacion() != null ? producto.getPresentacion() : "");
            json.put("precio", producto.getPrecio());
            json.put("timestamp", System.currentTimeMillis());

            Toast.makeText(this, "📤 Sincronizando " + producto.getCodigo() + "...", Toast.LENGTH_SHORT).show();

            new ConexionCouchDB.GuardarProductoTask(new ConexionCouchDB.GuardarProductoTask.OnGuardarListener() {
                @Override
                public void onSuccess(String cloudId, String cloudRev) {
                    dao.marcarComoSincronizado(producto.getId(), cloudId, cloudRev);
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "✅ Sincronizado: " + producto.getCodigo(), Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "❌ Error al sincronizar: " + error, Toast.LENGTH_SHORT).show());
                }
            }).execute(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==================== CONSULTAR WEBSERVICES (GET) ====================
    private void consultarWebServices() {
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

    // ==================== ELIMINAR EN LA NUBE (DELETE) ====================
    private void eliminarEnNube(String cloudId, String cloudRev) {
        new ConexionCouchDB.EliminarProductoCloudTask(new ConexionCouchDB.EliminarProductoCloudTask.OnEliminarListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "✅ Producto eliminado de la nube", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "⚠️ Error al eliminar de nube: " + error, Toast.LENGTH_SHORT).show());
            }
        }).execute(cloudId, cloudRev);
    }

    // ==================== SINCRONIZAR PRODUCTOS PENDIENTES ====================
    private void sincronizarPendientes() {
        if (!ConexionCouchDB.hayInternet(this)) {
            Toast.makeText(this, "⚠️ Sin conexión a internet", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "🔄 Sincronizando productos pendientes...", Toast.LENGTH_SHORT).show();

        List<Producto> pendientes = dao.obtenerProductosPendientes();

        if (pendientes.isEmpty()) {
            Toast.makeText(this, "No hay productos pendientes", Toast.LENGTH_SHORT).show();
            return;
        }

        for (Producto producto : pendientes) {
            try {
                JSONObject json = new JSONObject();
                json.put("codigo", producto.getCodigo());
                json.put("descripcion", producto.getDescripcion());
                json.put("marca", producto.getMarca());
                json.put("presentacion", producto.getPresentacion());
                json.put("precio", producto.getPrecio());
                json.put("timestamp", System.currentTimeMillis());

                new ConexionCouchDB.GuardarProductoTask(new ConexionCouchDB.GuardarProductoTask.OnGuardarListener() {
                    @Override
                    public void onSuccess(String cloudId, String cloudRev) {
                        dao.marcarComoSincronizado(producto.getId(), cloudId, cloudRev);
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "✅ Sincronizado: " + producto.getCodigo(), Toast.LENGTH_SHORT).show());
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "❌ Error: " + producto.getCodigo(), Toast.LENGTH_SHORT).show());
                    }
                }).execute(json.toString());

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}