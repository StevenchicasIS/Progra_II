package com.example.calculadora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    TabHost tabHost;

    // TAB AGUA
    EditText txtMetros;
    TextView lblRespuestaAgua;
    Button btnCalcularAgua;

    // TAB AREA
    EditText txtValorArea;
    Spinner cboDe, cboA;
    TextView lblRespuestaArea;
    Button btnConvertirArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // TABHOST
        tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();

        TabHost.TabSpec spec1 = tabHost.newTabSpec("agua");
        spec1.setContent(R.id.tabAgua);
        spec1.setIndicator("Agua");
        tabHost.addTab(spec1);

        TabHost.TabSpec spec2 = tabHost.newTabSpec("area");
        spec2.setContent(R.id.tabArea);
        spec2.setIndicator("Área");
        tabHost.addTab(spec2);

        // REFERENCIAS TAB AGUA
        txtMetros = findViewById(R.id.txtMetros);
        lblRespuestaAgua = findViewById(R.id.lblRespuestaAgua);
        btnCalcularAgua = findViewById(R.id.btnCalcularAgua);

        btnCalcularAgua.setOnClickListener(v -> calcularAgua());

        // REFERENCIAS TAB AREA
        txtValorArea = findViewById(R.id.txtValorArea);
        cboDe = findViewById(R.id.cboDe);
        cboA = findViewById(R.id.cboA);
        lblRespuestaArea = findViewById(R.id.lblRespuestaArea);
        btnConvertirArea = findViewById(R.id.btnConvertirArea);

        btnConvertirArea.setOnClickListener(v -> convertirArea());
    }


    private void calcularAgua() {

        String valor = txtMetros.getText().toString();

        if(valor.isEmpty()){
            lblRespuestaAgua.setText("Ingrese metros consumidos");
            return;
        }

        int metros = Integer.parseInt(valor);

        double total;

        if(metros <= 18){
            total = 6;
        }
        else if(metros <= 28){
            total = 6 + (metros - 18) * 0.45;
        }
        else{
            total = 6 + (28 - 18) * 0.45 + (metros - 28) * 0.65;
        }

        lblRespuestaAgua.setText(String.format(Locale.US,"Total: $%.2f", total));
    }


    private void convertirArea(){

        String valor = txtValorArea.getText().toString();

        if(valor.isEmpty()){
            lblRespuestaArea.setText("Ingrese un valor");
            return;
        }

        double numero = Double.parseDouble(valor);

        String de = cboDe.getSelectedItem().toString();
        String a = cboA.getSelectedItem().toString();

        double enMetros = numero * factorA_M2(de);

        double resultado = enMetros / factorA_M2(a);

        lblRespuestaArea.setText(String.format(Locale.US,"Resultado: %.6f", resultado));
    }

    // FACTORES EL SALVADOR
    private double factorA_M2(String unidad){

        switch(unidad){

            case "Metro cuadrado":
                return 1;

            case "Pie cuadrado":
                return 0.09290304;

            case "Yarda cuadrada":
                return 0.83612736;

            case "Vara cuadrada":
                return 0.698896;

            case "Tarea":
                return 436.81;

            case "Manzana":
                return 6988.96;

            case "Hectárea":
                return 10000;

            default:
                return 1;
        }
    }
}