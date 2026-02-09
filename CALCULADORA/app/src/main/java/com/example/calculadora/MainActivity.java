package com.example.calculadora;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    EditText txtNum1, txtNum2;
    TextView lblRespuesta;
    TextView tempVal;
    Button btn;

    RadioGroup radioGroup;
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

        tempVal = findViewById(R.id.txtNum2);
        Double num2 = Double.parseDouble(tempVal.getText().toString());

        double respuesta = 0;

        radioGroup = findViewById(R.id.optOpciones);

        if(radioGroup.getCheckedRadioButtonId()==R.id.optSuma) {
            respuesta = num1 + num2;
        }
        if(radioGroup.getCheckedRadioButtonId()==R.id.optResta) {
            respuesta = num1 - num2;
        }
        if(radioGroup.getCheckedRadioButtonId()==R.id.optMultiplicar) {
            respuesta = num1 * num2;
        }
        if(radioGroup.getCheckedRadioButtonId()==R.id.optDividir) {
            respuesta = num1 / num2;
        }
        if(radioGroup.getCheckedRadioButtonId()==R.id.optFactorial) {
            respuesta = 1;
            for(int i = 1; i <= num1; i++) {
                respuesta = respuesta * i;
            }
        }

        if(radioGroup.getCheckedRadioButtonId()==R.id.optPorcentaje) {
            respuesta = (num1 / 100) * num2;
        }
        if(radioGroup.getCheckedRadioButtonId()==R.id.optPotencia) {
            respuesta = Math.pow(num1, num2);
        }
        if(radioGroup.getCheckedRadioButtonId()==R.id.optRaiz) {
            respuesta = Math.sqrt(num1);
        }

        String respuestaTexto;

        if (respuesta % 1 == 0) {
            respuestaTexto = String.valueOf((int) respuesta);
        } else {
            respuestaTexto = String.format(Locale.US, "%.4f", respuesta);
            respuestaTexto = respuestaTexto.replaceAll("0*$", "").replaceAll("\\.$", "");
        }

        tempVal = findViewById(R.id.lblRespuesta);
        tempVal.setText("Respuesta: "+ respuestaTexto);
    }
}