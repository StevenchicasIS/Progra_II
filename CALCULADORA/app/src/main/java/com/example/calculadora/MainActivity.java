package com.example.calculadora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    EditText txtNum1, txtNum2;
    TextView lblRespuesta;
    TextView tempVal;
    Button btn;

    Spinner spn;

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

        tempVal = findViewById(R.id.txtNum2);
        Double num2 = Double.parseDouble(tempVal.getText().toString());

        double respuesta = 0;

        spn = findViewById(R.id.cboOpciones);
        switch (spn.getSelectedItemPosition()){
            case 0: //suma
                respuesta = num1 + num2;
                break;
            case 1: //Resta
                respuesta = num1 - num2;
                break;
            case 2: //Multiplicacion
                respuesta = num1 * num2;
                break;
            case 3: //division
                respuesta = num1 / num2;
                break;
        }

        tempVal = findViewById(R.id.lblRespuesta);
        tempVal.setText("Respuesta: "+ respuesta);
    }
}