package com.example.calculadora;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Spinner spnTipo, spnDe, spnA;
    EditText txtCantidad;
    TextView lblResultado;
    Button btnConvertir;

    DecimalFormat df;


    Double valores[][] = {

            //  MONEDAS
            {1.0, 1.09, 1.13, 0.040, 0.027, 0.0020, 1.28},
            // LONGITUD
            {1.0, 0.001, 0.01, 0.0254, 0.3048, 0.9144, 1000.0},
            // MASA
            {1.0, 0.001, 1000.0,  0.453592, 0.0283495, 11.3398, 45.3592},
            //  VOLUMEN
            {1.0, 0.001, 1000.0, 3.78541, 4.54609, 0.236588, 0.0295735},
            //ALMACENAMIENTO
            {1.0, 1024.0, 1048576.0, 1073741824.0, 1099511627776.0, 1125899906842624.0, 0.125},
            //  TIEMPO
            {1.0, 60.0, 3600.0, 86400.0, 604800.0, 2629800.0, 31557600.0},
            //TRANSFERENCIA DE DATOS
            {1.0, 1000.0, 1000000.0, 1000000000.0, 1000000000000.0, 8.0, 8000000.0}


    };

    String etiquetas[][] = {

            {"USD", "Euro", "Quetzal", "Lempira", "Córdoba", "Colón CR", "Libra"},
            {"Metro", "Milímetro", "Centímetro", "Pulgada", "Pie", "Yarda", "Kilómetro"},
            {"Kilogramo", "Gramo", "Tonelada", "Libra", "Onza", "Arroba", "Quintal"},
            {"Litro", "Mililitro", "Metro³", "Galón US", "Galón UK", "Taza", "Onza líquida"},
            {"Byte", "Kilobyte", "Megabyte", "Gigabyte", "Terabyte", "Petabyte", "Bit"},
            {"Segundo", "Minuto", "Hora", "Día", "Semana", "Mes", "Año"},
            {"bps", "Kbps", "Mbps", "Gbps", "Tbps", "Bps", "MBps"}


    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        df = new DecimalFormat("#,##0.######", symbols);

        spnTipo = findViewById(R.id.spnTipo);
        spnDe = findViewById(R.id.spnDe);
        spnA = findViewById(R.id.spnA);
        txtCantidad = findViewById(R.id.txtCantidad);
        lblResultado = findViewById(R.id.lblResultado);
        btnConvertir = findViewById(R.id.btnConvertir);

        cambiarOpciones(0);

        spnTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                cambiarOpciones(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnConvertir.setOnClickListener(v -> convertir());
    }

    private void cambiarOpciones(int tipo) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                etiquetas[tipo]
        );
        spnDe.setAdapter(adapter);
        spnA.setAdapter(adapter);
    }

    private void convertir() {
        String input = txtCantidad.getText().toString().trim();

        if (input.isEmpty()) {
            txtCantidad.setError("Ingrese un valor");
            Toast.makeText(this, "Debe ingresar una cantidad", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int tipo = spnTipo.getSelectedItemPosition();
            int de = spnDe.getSelectedItemPosition();
            int a = spnA.getSelectedItemPosition();

            double cantidad = Double.parseDouble(input);

            if (tipo >= valores.length ||
                    de >= valores[tipo].length ||
                    a >= valores[tipo].length) {
                Toast.makeText(this, "Error interno en unidades", Toast.LENGTH_SHORT).show();
                return;
            }


            double resultado = cantidad * valores[tipo][de] / valores[tipo][a];

            lblResultado.setText(
                    df.format(cantidad) + " " + etiquetas[tipo][de] + " = " +
                            df.format(resultado) + " " + etiquetas[tipo][a]
            );

        } catch (NumberFormatException e) {
            txtCantidad.setError("Número inválido");
            Toast.makeText(this, "Ingrese un número válido", Toast.LENGTH_SHORT).show();
        }
    }
}
