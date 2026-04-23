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
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText campoBuscar;
    private Button botonBuscar, botonAgregar, botonVerTodos;
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
}