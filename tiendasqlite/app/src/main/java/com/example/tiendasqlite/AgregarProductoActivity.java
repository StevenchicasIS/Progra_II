package com.example.tiendasqlite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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

        Producto producto = new Producto(codigo, descripcion, marca, presentacion, precio);
        long id = dao.insertar(producto);

        if (id > 0) {
            Toast.makeText(this, "Producto guardado. Ahora agrega fotos", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, AgregarFotosActivity.class);
            intent.putExtra("producto_id", (int) id);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Error o código duplicado", Toast.LENGTH_SHORT).show();
        }
    }
}