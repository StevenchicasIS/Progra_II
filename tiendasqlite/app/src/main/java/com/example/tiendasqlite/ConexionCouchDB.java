package com.example.tiendasqlite;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConexionCouchDB {

    private static final String TAG = "CouchDB";

    // ==================== PARA EMULADOR ====================
    // 10.0.2.2 es la IP del localhost de TU COMPUTADORA
    private static final String BASE_URL = "http://10.0.2.2:5984/";
    // =======================================================

    private static final String DB_NAME = "tienda_db";

    // ==================== AUTENTICACIÓN ====================
    // Usuario y contraseña por defecto de CouchDB
    // Si cambiaste la contraseña, modifícala aquí
    private static final String USER = "josue";
    private static final String PASSWORD = "IdJosue2004";
    // =======================================================

    /**
     * Verificar si hay conexión a internet
     */
    public static boolean hayInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Obtener el header de autenticación Basic
     */
    private static String getAuthHeader() {
        String credentials = USER + ":" + PASSWORD;
        byte[] encoded = Base64.encode(credentials.getBytes(), Base64.NO_WRAP);
        return "Basic " + new String(encoded);
    }

    /**
     * AsyncTask para obtener todos los productos desde CouchDB
     */
    public static class ObtenerProductosTask extends AsyncTask<Void, Void, String> {

        private OnProductosListener listener;

        public interface OnProductosListener {
            void onSuccess(JSONArray productos);
            void onError(String error);
        }

        public ObtenerProductosTask(OnProductosListener listener) {
            this.listener = listener;
        }

        @Override
        protected String doInBackground(Void... params) {
            HttpURLConnection urlConnection = null;
            try {
                // URL de la vista que creamos en CouchDB
                String urlString = BASE_URL + DB_NAME + "/_design/productos/_view/todos-productos";
                Log.d(TAG, "Conectando a: " + urlString);

                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);

                // ==================== AGREGAR AUTENTICACIÓN ====================
                urlConnection.setRequestProperty("Authorization", getAuthHeader());
                // ===============================================================

                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "Código de respuesta: " + responseCode);

                if (responseCode == 401) {
                    return "Error: Usuario o contraseña incorrectos. Verifica USER y PASSWORD en ConexionCouchDB.java";
                }

                if (responseCode != 200) {
                    return "Error: Código HTTP " + responseCode;
                }

                // Leer respuesta
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();

            } catch (Exception e) {
                Log.e(TAG, "Error GET: ", e);
                return "Error: " + e.getMessage();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (listener != null) {
                if (result == null || result.startsWith("Error")) {
                    listener.onError(result != null ? result : "Error desconocido");
                } else {
                    try {
                        JSONObject response = new JSONObject(result);
                        JSONArray rows = response.getJSONArray("rows");
                        JSONArray productos = new JSONArray();

                        for (int i = 0; i < rows.length(); i++) {
                            JSONObject row = rows.getJSONObject(i);
                            JSONObject doc = row.getJSONObject("value");

                            // Saltar documentos de diseño (no son productos)
                            if (!doc.has("_design") && doc.has("codigo")) {
                                productos.put(doc);
                            }
                        }
                        listener.onSuccess(productos);

                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando JSON: ", e);
                        listener.onError("Error al parsear JSON: " + e.getMessage());
                    }
                }
            }
        }
    }
}