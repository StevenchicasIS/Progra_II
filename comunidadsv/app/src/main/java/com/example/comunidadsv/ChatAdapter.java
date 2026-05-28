package com.example.comunidadsv;

import android.app.AlertDialog;
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
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private Context context;
    private List<Chat> chats;
    private String currentUserId;
    private OnChatDeletedListener listener;

    public interface OnChatDeletedListener {
        void onChatDeleted();
    }

    public ChatAdapter(Context context, List<Chat> chats, String currentUserId) {
        this.context = context;
        this.chats = chats;
        this.currentUserId = currentUserId;
        if (context instanceof OnChatDeletedListener) {
            this.listener = (OnChatDeletedListener) context;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Chat chat = chats.get(position);

        // Determinar el otro usuario
        boolean esUsuario1 = chat.getUsuario1Id().equals(currentUserId);
        String otroNombre = esUsuario1 ? chat.getUsuario2Nombre() : chat.getUsuario1Nombre();
        String otroId = esUsuario1 ? chat.getUsuario2Id() : chat.getUsuario1Id();
        String otraFoto = esUsuario1 ? chat.getUsuario2Foto() : chat.getUsuario1Foto();
        int noLeidos = chat.getNoLeidos(currentUserId);

        holder.txtNombre.setText(otroNombre);

        // Negrita si hay mensajes no leídos
        if (noLeidos > 0) {
            holder.txtNombre.setTypeface(null, Typeface.BOLD);
            holder.txtUltimoMensaje.setTypeface(null, Typeface.BOLD);
            holder.txtUltimoMensaje.setTextColor(context.getColor(R.color.black));
        } else {
            holder.txtNombre.setTypeface(null, Typeface.NORMAL);
            holder.txtUltimoMensaje.setTypeface(null, Typeface.NORMAL);
            holder.txtUltimoMensaje.setTextColor(context.getColor(R.color.gray_text));
        }

        // Cargar foto
        if (!otraFoto.isEmpty()) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(otraFoto);
            if (bitmap != null) {
                holder.imgFoto.setImageBitmap(bitmap);
            } else {
                holder.imgFoto.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_profile);
        }

        // Último mensaje
        if (chat.getUltimoMensaje() != null && !chat.getUltimoMensaje().isEmpty()) {
            String remitente = "";
            if (chat.getUltimoMensajeRemitenteId().equals(currentUserId)) {
                remitente = "Tú: ";
            }
            holder.txtUltimoMensaje.setText(remitente + chat.getUltimoMensaje());
        } else {
            holder.txtUltimoMensaje.setText("Sin mensajes");
        }

        // Contador de mensajes no leídos
        if (noLeidos > 0) {
            holder.badgeNoLeidos.setVisibility(View.VISIBLE);
            holder.badgeNoLeidos.setText(String.valueOf(noLeidos));
        } else {
            holder.badgeNoLeidos.setVisibility(View.GONE);
        }

        // Fecha del último mensaje
        if (chat.getUltimoMensajeFecha() > 0) {
            holder.txtFecha.setText(getTimeAgo(chat.getUltimoMensajeFecha()));
        } else {
            holder.txtFecha.setText("");
        }

        // Click para abrir chat
        holder.itemView.setOnClickListener(v -> {
            // Resetear contador de no leídos
            new ResetearNoLeidosTask(chat, position).execute();

            Intent intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("chatId", chat.getId());
            intent.putExtra("otroUsuarioId", otroId);
            intent.putExtra("otroUsuarioNombre", otroNombre);
            intent.putExtra("otroUsuarioFoto", otraFoto);
            context.startActivity(intent);
        });

        // Long click para eliminar chat
        holder.itemView.setOnLongClickListener(v -> {
            mostrarDialogoEliminarChat(chat, position);
            return true;
        });
    }

    private void mostrarDialogoEliminarChat(Chat chat, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Eliminar chat")
                .setMessage("¿Estás seguro de que quieres eliminar este chat? Se borrarán todos los mensajes.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    new EliminarChatTask(chat, position).execute();
                })
                .setNegativeButton("Cancelar", null)
                .show();
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

    private class ResetearNoLeidosTask extends AsyncTask<Void, Void, Boolean> {
        private Chat chat;
        private int position;

        ResetearNoLeidosTask(Chat chat, int position) {
            this.chat = chat;
            this.position = position;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                String getUrl = Configuracion.SERVIDOR + "/db_chats/" + chat.getId();
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");

                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    JSONObject doc = new JSONObject(sb.toString());

                    chat.resetearNoLeidos(currentUserId);
                    if (currentUserId.equals(chat.getUsuario1Id())) {
                        doc.put("noLeidosUsuario1", 0);
                    } else {
                        doc.put("noLeidosUsuario2", 0);
                    }

                    String rev = doc.getString("_rev");
                    String putUrl = Configuracion.SERVIDOR + "/db_chats/" + chat.getId() + "?rev=" + rev;
                    URL putUrlObj = new URL(putUrl);
                    HttpURLConnection putConn = (HttpURLConnection) putUrlObj.openConnection();
                    setBasicAuth(putConn);
                    putConn.setRequestMethod("PUT");
                    putConn.setRequestProperty("Content-Type", "application/json");
                    putConn.setDoOutput(true);

                    java.io.OutputStream os = putConn.getOutputStream();
                    java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(os, "UTF-8");
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

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                notifyItemChanged(position);
            }
        }
    }

    private class EliminarChatTask extends AsyncTask<Void, Void, Boolean> {
        private Chat chat;
        private int position;
        private String errorMsg = "";

        EliminarChatTask(Chat chat, int position) {
            this.chat = chat;
            this.position = position;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                // 1. Obtener todos los mensajes del chat
                String mensajesUrl = Configuracion.SERVIDOR + "/db_mensajes/_design/mensajes/_view/por_chat?key=\"" + chat.getId() + "\"";
                URL url = new URL(mensajesUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                setBasicAuth(conn);
                conn.setRequestMethod("GET");

                if (conn.getResponseCode() == 200) {
                    InputStream in = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    org.json.JSONObject response = new org.json.JSONObject(sb.toString());
                    org.json.JSONArray rows = response.optJSONArray("rows");

                    // Eliminar cada mensaje
                    if (rows != null) {
                        for (int i = 0; i < rows.length(); i++) {
                            org.json.JSONObject row = rows.getJSONObject(i);
                            org.json.JSONObject doc = row.getJSONObject("value");
                            String mensajeId = doc.getString("_id");
                            String rev = doc.getString("_rev");

                            String deleteUrl = Configuracion.SERVIDOR + "/db_mensajes/" + mensajeId + "?rev=" + rev;
                            URL deleteUrlObj = new URL(deleteUrl);
                            HttpURLConnection deleteConn = (HttpURLConnection) deleteUrlObj.openConnection();
                            setBasicAuth(deleteConn);
                            deleteConn.setRequestMethod("DELETE");
                            deleteConn.getResponseCode();
                        }
                    }
                }

                // 2. Eliminar el chat
                String getUrl = Configuracion.SERVIDOR + "/db_chats/" + chat.getId();
                URL getUrlObj = new URL(getUrl);
                HttpURLConnection getConn = (HttpURLConnection) getUrlObj.openConnection();
                setBasicAuth(getConn);
                getConn.setRequestMethod("GET");

                if (getConn.getResponseCode() == 200) {
                    InputStream in = getConn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    org.json.JSONObject doc = new org.json.JSONObject(sb.toString());
                    String rev = doc.getString("_rev");

                    String deleteUrl = Configuracion.SERVIDOR + "/db_chats/" + chat.getId() + "?rev=" + rev;
                    URL deleteUrlObj = new URL(deleteUrl);
                    HttpURLConnection deleteConn = (HttpURLConnection) deleteUrlObj.openConnection();
                    setBasicAuth(deleteConn);
                    deleteConn.setRequestMethod("DELETE");
                    int responseCode = deleteConn.getResponseCode();
                    return responseCode == 200;
                }
            } catch (Exception e) {
                errorMsg = e.getMessage();
                e.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                chats.remove(position);
                notifyItemRemoved(position);
                Toast.makeText(context, "Chat eliminado", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onChatDeleted();
                }
            } else {
                Toast.makeText(context, "Error al eliminar: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoto;
        TextView txtNombre, txtUltimoMensaje, txtFecha;
        TextView badgeNoLeidos;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.imgFoto);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtUltimoMensaje = itemView.findViewById(R.id.txtUltimoMensaje);
            txtFecha = itemView.findViewById(R.id.txtFecha);
            badgeNoLeidos = itemView.findViewById(R.id.badgeNoLeidos);
        }
    }
}