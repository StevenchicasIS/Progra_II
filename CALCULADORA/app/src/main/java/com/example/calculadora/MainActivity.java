package com.example.calculadora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText txtNum1, txtNum2;
    TextView lblRespuesta;
    Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        txtNum1 = findViewById(R.id.txtNum1);
        txtNum2 = findViewById(R.id.txtNum2);
        lblRespuesta = findViewById(R.id.lblRespuesta);
        btn = findViewById(R.id.btnCalcular);

        btn.setOnClickListener(v -> calcular());
    }

    private void calcular() {

        String n1 = txtNum1.getText().toString();
        String n2 = txtNum2.getText().toString();

        if (n1.isEmpty() || n2.isEmpty()) {
            lblRespuesta.setText("Respuesta: ingrese números");
            return;
        }

        double num1 = Double.parseDouble(n1);
        double num2 = Double.parseDouble(n2);

        double respuesta = num1 + num2;

        lblRespuesta.setText("Respuesta: " + respuesta);
    }
}