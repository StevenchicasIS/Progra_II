package com.example.comunidadsv;

import android.os.AsyncTask;
import org.json.JSONObject;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class CrearNotificacionTask extends AsyncTask<Notificacion, Void, Boolean> {

    private static CrearNotificacionTask instance;

    public static CrearNotificacionTask getInstance() {
        if (instance == null) {
            instance = new CrearNotificacionTask();
        }
        return instance;
    }

    @Override
    protected Boolean doInBackground(Notificacion... params) {
        Notificacion notificacion = params[0];

        try {
            String urlStr = Configuracion.SERVIDOR + "/db_notificaciones/" + notificacion.getId();
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            setBasicAuth(conn);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(notificacion.toJSON().toString());
            writer.flush();
            writer.close();
            os.close();

            int responseCode = conn.getResponseCode();
            return responseCode == 201 || responseCode == 202;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }
}