package com.example.tiendasqlite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class ModificarProductoActivity extends AppCompatActivity {

    private EditText campoCodigo, campoDescripcion, campoMarca, campoPresentacion, campoPrecio;
    private LinearLayout contenedorFotosPreview;
    private Button botonAgregarFotos, botonActualizar, botonEliminar, botonCancelar;
    private ProductoDAO dao;
    private Producto productoActual;
    private int idProducto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modificar_producto);

        dao = new ProductoDAO(this);
        idProducto = getIntent().getIntExtra("producto_id", -1);

        if (idProducto == -1) {
            Toast.makeText(this, "Error: Producto no encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productoActual = dao.obtenerPorId(idProducto);
        if (productoActual == null) {
            Toast.makeText(this, "Error: Producto no existe", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        campoCodigo = findViewById(R.id.etCodigo);
        campoDescripcion = findViewById(R.id.etDescripcion);
        campoMarca = findViewById(R.id.etMarca);
        campoPresentacion = findViewById(R.id.etPresentacion);
        campoPrecio = findViewById(R.id.etPrecio);
        contenedorFotosPreview = findViewById(R.id.llFotosPreview);
        botonAgregarFotos = findViewById(R.id.btnAgregarFotos);
        botonActualizar = findViewById(R.id.btnActualizar);
        botonEliminar = findViewById(R.id.btnEliminar);
        botonCancelar = findViewById(R.id.btnCancelar);

        cargarDatosProducto();
        mostrarVistaPreviaFotos();

        botonAgregarFotos.setOnClickListener(v -> {
            Intent intent = new Intent(this, AgregarFotosActivity.class);
            intent.putExtra("producto_id", idProducto);
            startActivity(intent);
        });

        botonActualizar.setOnClickListener(v -> actualizarProducto());
        botonEliminar.setOnClickListener(v -> confirmarEliminacion());
        botonCancelar.setOnClickListener(v -> finish());
    }

    private void cargarDatosProducto() {
        campoCodigo.setText(productoActual.getCodigo());
        campoDescripcion.setText(productoActual.getDescripcion());
        campoMarca.setText(productoActual.getMarca());
        campoPresentacion.setText(productoActual.getPresentacion());
        campoPrecio.setText(String.valueOf(productoActual.getPrecio()));
    }

    private void mostrarVistaPreviaFotos() {
        List<byte[]> fotos = productoActual.getFotos();
        contenedorFotosPreview.removeAllViews();

        if (fotos != null && !fotos.isEmpty()) {
            int maxPreview = Math.min(fotos.size(), 4);

            for (int i = 0; i < maxPreview; i++) {
                CircleImageView img = new CircleImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        convertirDpAPx(60), convertirDpAPx(60));
                params.setMargins(convertirDpAPx(4), 0, convertirDpAPx(4), 0);
                img.setLayoutParams(params);
                img.setBorderWidth(convertirDpAPx(2));
                img.setBorderColor(getColor(R.color.primary));
                UtilidadImagenes.cargarImagenRedondeada(img, fotos.get(i));
                contenedorFotosPreview.addView(img);
            }

            if (fotos.size() > 4) {
                TextView tvMas = new TextView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        convertirDpAPx(60), convertirDpAPx(60));
                params.setMargins(convertirDpAPx(4), 0, convertirDpAPx(4), 0);
                tvMas.setLayoutParams(params);
                tvMas.setBackgroundColor(getColor(R.color.surface_variant));
                tvMas.setText("+" + (fotos.size() - 4));
                tvMas.setTextSize(14);
                tvMas.setGravity(android.view.Gravity.CENTER);
                tvMas.setTextColor(getColor(R.color.primary));
                contenedorFotosPreview.addView(tvMas);
            }
        } else {
            TextView textoVacio = new TextView(this);
            textoVacio.setText("📭 Sin fotos");
            textoVacio.setTextSize(12);
            textoVacio.setTextColor(getColor(R.color.text_hint));
            contenedorFotosPreview.addView(textoVacio);
        }
    }

    private void actualizarProducto() {
        String codigo = campoCodigo.getText().toString().trim();
        String descripcion = campoDescripcion.getText().toString().trim();
        String precioStr = campoPrecio.getText().toString().trim();

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

        Producto existe = dao.buscarPorCodigo(codigo);
        if (existe != null && existe.getId() != idProducto) {
            Toast.makeText(this, "Ya existe otro producto con ese código", Toast.LENGTH_SHORT).show();
            return;
        }

        productoActual.setCodigo(codigo);
        productoActual.setDescripcion(descripcion);
        productoActual.setMarca(campoMarca.getText().toString().trim());
        productoActual.setPresentacion(campoPresentacion.getText().toString().trim());
        productoActual.setPrecio(precio);

        int resultado = dao.actualizar(productoActual);
        if (resultado > 0) {
            Toast.makeText(this, "Producto actualizado", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEliminacion() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Producto")
                .setMessage("¿Eliminar " + productoActual.getDescripcion() + "?\n(Se eliminarán también todas sus fotos)")
                .setPositiveButton("Sí", (dialog, which) -> {
                    dao.eliminar(idProducto);
                    Toast.makeText(this, "Producto eliminado", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        productoActual = dao.obtenerPorId(idProducto);
        mostrarVistaPreviaFotos();
    }

    private int convertirDpAPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}