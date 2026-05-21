package com.example.comunidadsv;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class SolicitudAdapter extends RecyclerView.Adapter<SolicitudAdapter.ViewHolder> {

    private Context context;
    private List<Solicitud> solicitudes;
    private String currentUserId;
    private OnSolicitudListener listener;

    public interface OnSolicitudListener {
        void onSolicitudAceptada();
        void onSolicitudRechazada();
    }

    public SolicitudAdapter(Context context, List<Solicitud> solicitudes, String currentUserId) {
        this.context = context;
        this.solicitudes = solicitudes;
        this.currentUserId = currentUserId;
        if (context instanceof OnSolicitudListener) {
            this.listener = (OnSolicitudListener) context;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_solicitud, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Solicitud s = solicitudes.get(position);

        holder.txtNombre.setText(s.getEmisorNombre());
        holder.txtMensaje.setText("Quiere seguirte");

        // Cargar foto si existe
        if (s.getEmisorFoto() != null && !s.getEmisorFoto().isEmpty()) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(s.getEmisorFoto());
            if (bitmap != null) {
                holder.imgFoto.setImageBitmap(bitmap);
            } else {
                holder.imgFoto.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_profile);
        }

        holder.btnAceptar.setOnClickListener(v -> {
            aceptarSolicitud(s, position);
        });

        holder.btnRechazar.setOnClickListener(v -> {
            rechazarSolicitud(s, position);
        });
    }

    private void aceptarSolicitud(Solicitud s, int position) {
        new ResponderSolicitudTask(true, s, position).execute();
    }

    private void rechazarSolicitud(Solicitud s, int position) {
        new ResponderSolicitudTask(false, s, position).execute();
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class ResponderSolicitudTask extends AsyncTask<Void, Void, Boolean> {
        private boolean aceptar;
        private Solicitud solicitud;
        private int position;
        private String errorMsg = "";

        ResponderSolicitudTask(boolean aceptar, Solicitud solicitud, int position) {
            this.aceptar = aceptar;
            this.solicitud = solicitud;
            this.position = position;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // Obtener el documento actual para tener la rev
                String getUrl = Configuracion.SERVIDOR + "/db_solicitudes/" + solicitud.getId();
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");

                String rev = "";
                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject doc = new JSONObject(sb.toString());
                    rev = doc.optString("_rev", "");
                }

                // Actualizar estado de la solicitud
                String urlStr = Configuracion.SERVIDOR + "/db_solicitudes/" + solicitud.getId();
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("PUT");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject doc = new JSONObject();
                doc.put("_id", solicitud.getId());
                doc.put("_rev", rev);
                doc.put("tipo", "solicitud_seguimiento");
                doc.put("emisorId", solicitud.getEmisorId());
                doc.put("emisorNombre", solicitud.getEmisorNombre());
                doc.put("emisorFoto", solicitud.getEmisorFoto());
                doc.put("receptorId", solicitud.getReceptorId());
                doc.put("estado", aceptar ? "aceptada" : "rechazada");
                doc.put("fecha", solicitud.getFecha());

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(doc.toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = conn.getResponseCode();
                boolean success = responseCode == 201 || responseCode == 202;

                // Si se aceptó, crear la relación de seguimiento
                if (aceptar && success) {
                    // Verificar si ya existe la relación
                    String checkUrl = Configuracion.SERVIDOR + "/db_seguidores/_design/seguidores/_view/por_seguidor?key=\"" + solicitud.getReceptorId() + "\"";
                    URL checkUrlObj = new URL(checkUrl);
                    HttpURLConnection checkConn = (HttpURLConnection) checkUrlObj.openConnection();
                    setBasicAuth(checkConn);
                    checkConn.setRequestMethod("GET");

                    boolean yaExiste = false;
                    if (checkConn.getResponseCode() == 200) {
                        InputStream in = checkConn.getInputStream();
                        java.io.BufferedReader reader = new java.io.BufferedReader(new InputStreamReader(in));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        JSONObject response = new JSONObject(sb.toString());
                        JSONArray rows = response.optJSONArray("rows");
                        if (rows != null) {
                            for (int i = 0; i < rows.length(); i++) {
                                JSONObject row = rows.getJSONObject(i);
                                String followingId = row.optString("value");
                                if (followingId.equals(solicitud.getEmisorId())) {
                                    yaExiste = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!yaExiste) {
                        Follow follow = new Follow(solicitud.getReceptorId(), solicitud.getEmisorId());
                        String followUrl = Configuracion.SERVIDOR + "/db_seguidores/" + follow.getId();
                        URL followUrlObj = new URL(followUrl);
                        HttpURLConnection followConn = (HttpURLConnection) followUrlObj.openConnection();
                        setBasicAuth(followConn);
                        followConn.setRequestMethod("PUT");
                        followConn.setRequestProperty("Content-Type", "application/json");
                        followConn.setDoOutput(true);

                        OutputStream os2 = followConn.getOutputStream();
                        BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(os2, "UTF-8"));
                        writer2.write(follow.toJSON().toString());
                        writer2.flush();
                        writer2.close();
                        os2.close();

                        followConn.getResponseCode();
                    }
                }

                return success;

            } catch (Exception e) {
                errorMsg = e.getMessage();
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                solicitudes.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, aceptar ? "Solicitud aceptada" : "Solicitud rechazada", Toast.LENGTH_SHORT).show();

                // Notificar al listener para actualizar el perfil
                if (listener != null && aceptar) {
                    listener.onSolicitudAceptada();
                }
            } else {
                Toast.makeText(context, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public int getItemCount() {
        return solicitudes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoto;
        TextView txtNombre, txtMensaje;
        Button btnAceptar, btnRechazar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.imgFoto);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtMensaje = itemView.findViewById(R.id.txtMensaje);
            btnAceptar = itemView.findViewById(R.id.btnAceptar);
            btnRechazar = itemView.findViewById(R.id.btnRechazar);
        }
    }
}