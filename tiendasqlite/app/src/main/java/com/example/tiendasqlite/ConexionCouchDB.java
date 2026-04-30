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


    private static final String BASE_URL = "http://10.0.2.2:5984/";
    // =======================================================

    private static final String DB_NAME = "josue";
    private static final String VIEW_NAME = "jimmy";;


    // Usuario y contraseña por defecto de CouchDB
    // Si se cambia  la contraseña, hay que modificarla  aquí
    private static final String USER = "josue";
    private static final String PASSWORD = "IdJosue2004";
    // =======================================================


    public static boolean hayInternet(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }


    private static String getAuthHeader() {
        String credentials = USER + ":" + PASSWORD;
        byte[] encoded = Base64.encode(credentials.getBytes(), Base64.NO_WRAP);
        return "Basic " + new String(encoded);
    }


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
                String urlString = BASE_URL + DB_NAME + "/_design/productos/_view/jimmy";
                Log.d(TAG, "Conectando a: " + urlString);

                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);
                urlConnection.setRequestProperty("Authorization", getAuthHeader());

                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "Código de respuesta GET: " + responseCode);

                if (responseCode == 401) {
                    return "Error: Usuario o contraseña incorrectos";
                }

                if (responseCode != 200) {
                    return "Error: Código HTTP " + responseCode;
                }

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


    public static class GuardarProductoTask extends AsyncTask<String, Void, String> {

        private OnGuardarListener listener;

        public interface OnGuardarListener {
            void onSuccess(String id, String rev);
            void onError(String error);
        }

        public GuardarProductoTask(OnGuardarListener listener) {
            this.listener = listener;
        }

        @Override
        protected String doInBackground(String... params) {
            String jsonData = params[0];
            HttpURLConnection urlConnection = null;
            try {
                // Generar ID único usando timestamp
                String id = "prod_" + System.currentTimeMillis();
                String urlString = BASE_URL + DB_NAME + "/" + id;

                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("PUT");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Authorization", getAuthHeader());
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);

                java.io.OutputStream os = urlConnection.getOutputStream();
                os.write(jsonData.getBytes());
                os.flush();
                os.close();

                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "Código de respuesta POST: " + responseCode);

                if (responseCode == 201 || responseCode == 202) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    return result.toString();
                } else {
                    return "Error: Código HTTP " + responseCode;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error POST: ", e);
                return "Error: " + e.getMessage();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (listener != null) {
                if (result != null && result.startsWith("Error")) {
                    listener.onError(result);
                } else {
                    try {
                        JSONObject response = new JSONObject(result);
                        String id = response.getString("id");
                        String rev = response.getString("rev");
                        listener.onSuccess(id, rev);
                    } catch (Exception e) {
                        listener.onError("Error al parsear respuesta: " + e.getMessage());
                    }
                }
            }
        }
    }

    // ==================== PUT - ACTUALIZAR PRODUCTO ====================

    /**
     * AsyncTask para ACTUALIZAR un producto en CouchDB (PUT)
     */
    public static class ActualizarProductoTask extends AsyncTask<String, Void, String> {

        private OnActualizarListener listener;

        public interface OnActualizarListener {
            void onSuccess(String rev);
            void onError(String error);
        }

        public ActualizarProductoTask(OnActualizarListener listener) {
            this.listener = listener;
        }

        @Override
        protected String doInBackground(String... params) {
            String cloudId = params[0];
            String cloudRev = params[1];
            String jsonData = params[2];
            HttpURLConnection urlConnection = null;
            try {
                String urlString = BASE_URL + DB_NAME + "/" + cloudId + "?rev=" + cloudRev;
                Log.d(TAG, "PUT URL: " + urlString);

                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("PUT");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setRequestProperty("Authorization", getAuthHeader());
                urlConnection.setDoOutput(true);
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);

                java.io.OutputStream os = urlConnection.getOutputStream();
                os.write(jsonData.getBytes());
                os.flush();
                os.close();

                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "Código de respuesta PUT: " + responseCode);

                if (responseCode == 201 || responseCode == 202) {
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    return result.toString();
                } else {
                    return "Error: Código HTTP " + responseCode;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error PUT: ", e);
                return "Error: " + e.getMessage();
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (listener != null) {
                if (result != null && result.startsWith("Error")) {
                    listener.onError(result);
                } else {
                    try {
                        JSONObject response = new JSONObject(result);
                        String rev = response.getString("rev");
                        listener.onSuccess(rev);
                    } catch (Exception e) {
                        listener.onError("Error al parsear respuesta: " + e.getMessage());
                    }
                }
            }
        }
    }

    // ==================== DELETE - ELIMINAR PRODUCTO ====================

    /**
     * AsyncTask para ELIMINAR un producto de CouchDB (DELETE)
     */
    public static class EliminarProductoCloudTask extends AsyncTask<String, Void, Boolean> {

        private OnEliminarListener listener;

        public interface OnEliminarListener {
            void onSuccess();
            void onError(String error);
        }

        public EliminarProductoCloudTask(OnEliminarListener listener) {
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String cloudId = params[0];
            String cloudRev = params[1];
            HttpURLConnection urlConnection = null;
            try {
                String urlString = BASE_URL + DB_NAME + "/" + cloudId + "?rev=" + cloudRev;
                Log.d(TAG, "DELETE URL: " + urlString);

                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("DELETE");
                urlConnection.setRequestProperty("Authorization", getAuthHeader());
                urlConnection.setConnectTimeout(15000);
                urlConnection.setReadTimeout(15000);

                int responseCode = urlConnection.getResponseCode();
                Log.d(TAG, "Código de respuesta DELETE: " + responseCode);

                return (responseCode == 200 || responseCode == 202);

            } catch (Exception e) {
                Log.e(TAG, "Error DELETE: ", e);
                return false;
            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (listener != null) {
                if (success) {
                    listener.onSuccess();
                } else {
                    listener.onError("Error al eliminar de la nube");
                }
            }
        }
    }
}