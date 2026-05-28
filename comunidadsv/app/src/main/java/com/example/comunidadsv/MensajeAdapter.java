package com.example.comunidadsv;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MensajeAdapter extends RecyclerView.Adapter<MensajeAdapter.ViewHolder> {

    private Context context;
    private List<Mensaje> mensajes;
    private String currentUserId;

    public MensajeAdapter(Context context, List<Mensaje> mensajes, String currentUserId) {
        this.context = context;
        this.mensajes = mensajes;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_mensaje, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Mensaje mensaje = mensajes.get(position);
        boolean esMio = mensaje.getEmisorId().equals(currentUserId);

        // Configurar estilo según quién envió
        if (esMio) {
            holder.layoutMensaje.setGravity(Gravity.END);
            holder.txtMensaje.setBackgroundResource(R.drawable.bg_mensaje_mio);
            holder.txtMensaje.setTextColor(context.getColor(android.R.color.white));
            holder.layoutMensaje.setPadding(60, 8, 16, 8);
        } else {
            holder.layoutMensaje.setGravity(Gravity.START);
            holder.txtMensaje.setBackgroundResource(R.drawable.bg_mensaje_otro);
            holder.txtMensaje.setTextColor(context.getColor(android.R.color.black));
            holder.layoutMensaje.setPadding(16, 8, 60, 8);
        }

        holder.txtMensaje.setText(mensaje.getTexto());

        // Formato de hora
        String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(mensaje.getFecha()));
        holder.txtHora.setText(hora);

        // Indicador de leído (solo para mensajes propios)
        if (esMio) {
            if (mensaje.isLeido()) {
                holder.txtEstado.setText("✓✓");
            } else {
                holder.txtEstado.setText("✓");
            }
        } else {
            holder.txtEstado.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return mensajes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout layoutMensaje;
        TextView txtMensaje, txtHora, txtEstado;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutMensaje = itemView.findViewById(R.id.layoutMensaje);
            txtMensaje = itemView.findViewById(R.id.txtMensaje);
            txtHora = itemView.findViewById(R.id.txtHora);
            txtEstado = itemView.findViewById(R.id.txtEstado);
        }
    }
}