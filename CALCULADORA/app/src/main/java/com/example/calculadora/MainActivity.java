package com.example.calculadora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    EditText txtNum1, txtNum2;
    TextView lblRespuesta;
    TextView tempVal;
    Button btn;
    RadioButton opt;

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

            tempVal = findViewById(R.id.txtNum1);
            Double num1 = Double.parseDouble(tempVal.getText().toString());

            tempVal  = findViewById(R.id.txtNum2);
            Double num2 = Double.parseDouble(tempVal.getText().toString());

            double respuesta = 0;

            opt = findViewById(R.id.optSuma);
            if (opt.isChecked()) {
                respuesta = num1 + num2;
            }

            opt = findViewById(R.id.optResta);
            if (opt.isChecked()) {
                respuesta = num1 - num2;
            }

            opt = findViewById(R.id.optMultiplicar);
            if (opt.isChecked()) {
                respuesta = num1 * num2;
            }

            opt = findViewById(R.id.optDividir);
            if (opt.isChecked()) {
                respuesta = num1 / num2;
            }

            opt = findViewById(R.id.optFactorial);
            if (opt.isChecked()) {
                double factorial = 1;
                int n = (int) Math.round(num1);
                for (int i = 1; i <= n; i++) {
                    factorial = factorial * i;
                }
                respuesta = factorial;
            }

            opt = findViewById(R.id.optPorcentaje);
            if (opt.isChecked()) {
                respuesta = (num1 * num2) / 100;
            }

            opt = findViewById(R.id.optPotencia);
            if (opt.isChecked()) {
                respuesta = Math.pow(num1, num2);
            }

            opt = findViewById(R.id.optRaiz);
            if (opt.isChecked()) {
                respuesta = Math.sqrt(num1);
            }

            tempVal = findViewById(R.id.lblRespuesta);
            tempVal.setText("Respuesta: "+ respuesta);
        }
    }

