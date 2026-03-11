package com.example.calculadora;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tempVal;
    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar el sensor de proximidad
        sensorProximidad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        iniciar();
    }

    @Override
    protected void onPause() {
        detener();
        super.onPause();
    }

    private void sensorProximidad() {
        tempVal = findViewById(R.id.lblSensorProximidad);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Verificar si existe el sensor de proximidad
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (sensor == null) {
            tempVal.setText("No dispones del sensor de proximidad");
            return; // No finalizamos la actividad, solo mostramos el mensaje
        }

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                double valor = sensorEvent.values[0];
                tempVal.setText("Proximidad: " + valor);

                // Cambiar color de fondo según la proximidad
                int color = Color.BLACK;
                if (valor <= 4) {
                    color = Color.WHITE;
                }
                getWindow().getDecorView().setBackgroundColor(color);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

        };
    }

    private void iniciar() {
        if (sensorManager != null && sensor != null && sensorEventListener != null) {
            sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void detener() {
        if (sensorManager != null && sensorEventListener != null) {
            sensorManager.unregisterListener(sensorEventListener);
        }
    }
}