package com.example.comunidadsv;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.ViewHolder> {

    private Context context;
    private List<Notificacion> notificaciones;
    private String currentUserId;

    public NotificacionAdapter(Context context, List<Notificacion> notificaciones, String currentUserId) {
        this.context = context;
        this.notificaciones = notificaciones;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notificacion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notificacion n = notificaciones.get(position);

        // Negrita si no está leída
        if (!n.isLeido()) {
            holder.txtMensaje.setTypeface(null, Typeface.BOLD);
            holder.txtFecha.setTypeface(null, Typeface.BOLD);
            holder.txtMensaje.setTextColor(context.getColor(android.R.color.black));
        } else {
            holder.txtMensaje.setTypeface(null, Typeface.NORMAL);
            holder.txtFecha.setTypeface(null, Typeface.NORMAL);
            holder.txtMensaje.setTextColor(context.getColor(android.R.color.darker_gray));
        }

        // Icono según tipo
        switch (n.getTipo()) {
            case "like":
                holder.imgIcono.setImageResource(R.drawable.ic_like);
                holder.imgIcono.setColorFilter(context.getColor(R.color.green_primary));
                break;
            case "comentario":
                holder.imgIcono.setImageResource(R.drawable.ic_comment);
                holder.imgIcono.setColorFilter(context.getColor(R.color.green_primary));
                break;
            case "nueva_publicacion":
                holder.imgIcono.setImageResource(R.drawable.ic_add);
                holder.imgIcono.setColorFilter(context.getColor(R.color.green_primary));
                break;
            case "solicitud_aceptada":
                holder.imgIcono.setImageResource(R.drawable.ic_person_add);
                holder.imgIcono.setColorFilter(context.getColor(R.color.green_primary));
                break;
            default:
                holder.imgIcono.setImageResource(R.drawable.ic_notification);
                holder.imgIcono.setColorFilter(context.getColor(R.color.green_primary));
                break;
        }

        holder.txtMensaje.setText(n.getMensaje());
        holder.txtFecha.setText(getTimeAgo(n.getFecha()));

        // Cargar foto del emisor
        if (n.getEmisorFoto() != null && !n.getEmisorFoto().isEmpty()) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(n.getEmisorFoto());
            if (bitmap != null) {
                holder.imgFoto.setImageBitmap(bitmap);
            } else {
                holder.imgFoto.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_profile);
        }

        // Click para marcar como leído y abrir la publicación si tiene postId
        holder.itemView.setOnClickListener(v -> {
            marcarComoLeido(n, position);
            if (n.getPostId() != null && !n.getPostId().isEmpty()) {
                abrirPublicacion(n.getPostId());
            }
        });
    }

    private void marcarComoLeido(Notificacion n, int position) {
        if (!n.isLeido()) {
            n.setLeido(true);
            new MarcarNotificacionLeidaTask(n).execute();
            notifyItemChanged(position);
        }
    }

    private void abrirPublicacion(String postId) {
        Intent intent = new Intent(context, FeedActivity.class);
        intent.putExtra("post_id", postId);
        context.startActivity(intent);
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            return "ahora";
        } else if (diff < 3600000) {
            return "hace " + (diff / 60000) + "m";
        } else if (diff < 86400000) {
            return "hace " + (diff / 3600000) + "h";
        } else {
            return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date(timestamp));
        }
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
    }

    private class MarcarNotificacionLeidaTask extends AsyncTask<Void, Void, Boolean> {
        private Notificacion notificacion;

        MarcarNotificacionLeidaTask(Notificacion notificacion) {
            this.notificacion = notificacion;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String getUrl = Configuracion.SERVIDOR + "/db_notificaciones/" + notificacion.getId();
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");
                getConn.setConnectTimeout(10000);
                getConn.setReadTimeout(10000);

                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject doc = new JSONObject(sb.toString());

                    doc.put("leido", true);
                    String rev = doc.getString("_rev");

                    String putUrl = Configuracion.SERVIDOR + "/db_notificaciones/" + notificacion.getId() + "?rev=" + rev;
                    URL putUrlObj = new URL(putUrl);
                    HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                    setBasicAuth(putConn);
                    putConn.setRequestMethod("PUT");
                    putConn.setRequestProperty("Content-Type", "application/json");
                    putConn.setDoOutput(true);
                    putConn.setConnectTimeout(10000);
                    putConn.setReadTimeout(10000);

                    OutputStream os = putConn.getOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                    writer.write(doc.toString());
                    writer.flush();
                    writer.close();
                    os.close();

                    return putConn.getResponseCode() == 201 || putConn.getResponseCode() == 202;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return notificaciones.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoto, imgIcono;
        TextView txtMensaje, txtFecha;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.imgFoto);
            imgIcono = itemView.findViewById(R.id.imgIcono);
            txtMensaje = itemView.findViewById(R.id.txtMensaje);
            txtFecha = itemView.findViewById(R.id.txtFecha);
        }
    }
}