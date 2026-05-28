package com.example.comunidadsv;

import android.content.Context;
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
import java.io.BufferedReader;
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

    private String obtenerFotoUsuario(String userId) {
        try {
            String urlStr = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            setBasicAuth(conn);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject user = new JSONObject(sb.toString());
                return user.optString("fotoPerfil", "");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private String obtenerNombreUsuario(String userId) {
        try {
            String urlStr = Configuracion.SERVIDOR + "/db_usuarios/" + userId;
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            setBasicAuth(conn);
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == 200) {
                InputStream in = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject user = new JSONObject(sb.toString());
                return user.optString("nombre", "Usuario");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Usuario";
    }

    private void crearChat(String usuario1Id, String usuario2Id,
                           String usuario1Nombre, String usuario2Nombre,
                           String usuario1Foto, String usuario2Foto) {
        try {
            // Verificar si ya existe un chat entre estos usuarios
            String checkUrl = Configuracion.SERVIDOR + "/db_chats/_design/chats/_view/entre_usuarios?startkey=[\"" + usuario1Id + "\",\"" + usuario2Id + "\"]&endkey=[\"" + usuario2Id + "\",\"" + usuario1Id + "\"]";
            URL checkUrlObj = new URL(checkUrl);
            HttpURLConnection checkConn = (HttpURLConnection) checkUrlObj.openConnection();
            setBasicAuth(checkConn);
            checkConn.setRequestMethod("GET");
            checkConn.setConnectTimeout(10000);
            checkConn.setReadTimeout(10000);

            if (checkConn.getResponseCode() == 200) {
                InputStream in = checkConn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject response = new JSONObject(sb.toString());
                JSONArray rows = response.optJSONArray("rows");

                if (rows != null && rows.length() > 0) {
                    // Ya existe un chat, no crear otro
                    return;
                }
            }

            // Crear nuevo chat
            Chat chat = new Chat(usuario1Id, usuario1Nombre, usuario1Foto,
                    usuario2Id, usuario2Nombre, usuario2Foto);

            String putUrl = Configuracion.SERVIDOR + "/db_chats/" + chat.getId();
            URL putUrlObj = new URL(putUrl);
            HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
            setBasicAuth(putConn);
            putConn.setRequestMethod("PUT");
            putConn.setRequestProperty("Content-Type", "application/json");
            putConn.setDoOutput(true);
            putConn.setConnectTimeout(10000);
            putConn.setReadTimeout(10000);

            OutputStream os = putConn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(chat.toJSON().toString());
            writer.flush();
            writer.close();
            os.close();

            int responseCode = putConn.getResponseCode();
            if (responseCode == 201 || responseCode == 202) {
                android.util.Log.d("SolicitudAdapter", "Chat creado exitosamente: " + chat.getId());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                getConn.setConnectTimeout(10000);
                getConn.setReadTimeout(10000);

                String rev = "";
                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
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
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

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

                // Si se aceptó, crear la relación de seguimiento Y el chat
                if (aceptar && success) {
                    // 1. Crear relación de seguimiento
                    Follow follow = new Follow(solicitud.getReceptorId(), solicitud.getEmisorId());
                    String followUrl = Configuracion.SERVIDOR + "/db_seguidores/" + follow.getId();
                    URL followUrlObj = new URL(followUrl);
                    HttpURLConnection followConn = (HttpURLConnection) followUrlObj.openConnection();
                    setBasicAuth(followConn);
                    followConn.setRequestMethod("PUT");
                    followConn.setRequestProperty("Content-Type", "application/json");
                    followConn.setDoOutput(true);
                    followConn.setConnectTimeout(10000);
                    followConn.setReadTimeout(10000);

                    OutputStream os2 = followConn.getOutputStream();
                    BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(os2, "UTF-8"));
                    writer2.write(follow.toJSON().toString());
                    writer2.flush();
                    writer2.close();
                    os2.close();

                    int followResponse = followConn.getResponseCode();
                    boolean followSuccess = followResponse == 201 || followResponse == 202;

                    // 2. Crear chat automáticamente
                    if (followSuccess) {
                        // Obtener fotos actualizadas de ambos usuarios
                        String receptorFoto = obtenerFotoUsuario(solicitud.getReceptorId());
                        String emisorFoto = obtenerFotoUsuario(solicitud.getEmisorId());
                        String receptorNombre = obtenerNombreUsuario(solicitud.getReceptorId());
                        String emisorNombre = obtenerNombreUsuario(solicitud.getEmisorId());

                        // Crear el chat (receptor - emisor)
                        crearChat(solicitud.getReceptorId(), solicitud.getEmisorId(),
                                receptorNombre, emisorNombre,
                                receptorFoto, emisorFoto);
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