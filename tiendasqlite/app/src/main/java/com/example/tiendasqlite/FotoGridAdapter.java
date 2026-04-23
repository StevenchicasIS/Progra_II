package com.example.tiendasqlite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class FotoGridAdapter extends RecyclerView.Adapter<FotoGridAdapter.FotoViewHolder> {

    private List<byte[]> listaFotos = new ArrayList<>();
    private OnFotoClickListener listener;

    public interface OnFotoClickListener {
        void onEliminarClick(int position);
        void onFotoClick(int position);
    }

    public FotoGridAdapter(List<byte[]> fotos, OnFotoClickListener listener) {
        this.listaFotos = fotos != null ? fotos : new ArrayList<>();
        this.listener = listener;
    }

    public void actualizarFotos(List<byte[]> nuevasFotos) {
        this.listaFotos = nuevasFotos != null ? nuevasFotos : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_foto_grid, parent, false);
        return new FotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        byte[] foto = listaFotos.get(position);

        // Cargar imagen
        UtilidadImagenes.cargarImagenRedondeada(holder.ivFoto, foto);

        // Mostrar número de orden
        holder.tvOrden.setText(String.valueOf(position + 1));

        // Click para eliminar
        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEliminarClick(position);
            }
        });

        // Click en la foto
        holder.ivFoto.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFotoClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaFotos.size();
    }

    static class FotoViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivFoto;
        ImageView btnEliminar;
        TextView tvOrden;

        public FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFoto);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
            tvOrden = itemView.findViewById(R.id.tvOrden);
        }
    }
}