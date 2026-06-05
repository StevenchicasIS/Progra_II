package com.example.comunidadsv;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FotoPreviewAdapter extends RecyclerView.Adapter<FotoPreviewAdapter.ViewHolder> {

    private List<Uri> fotos;
    private OnFotoDeleteListener listener;

    public interface OnFotoDeleteListener {
        void onDelete(int position);
    }

    public FotoPreviewAdapter(OnFotoDeleteListener listener) {
        this.fotos = new ArrayList<>();
        this.listener = listener;
    }

    public void setFotos(List<Uri> fotos) {
        this.fotos = fotos;
        notifyDataSetChanged();
    }

    public void addFoto(Uri uri) {
        fotos.add(uri);
        notifyItemInserted(fotos.size() - 1);
    }

    public void removeFoto(int position) {
        fotos.remove(position);
        notifyItemRemoved(position);
    }

    public List<Uri> getFotos() {
        return fotos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foto_preview, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = fotos.get(position);
        holder.imgFoto.setImageURI(uri);

        holder.btnEliminar.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fotos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoto, btnEliminar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.imgFoto);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}