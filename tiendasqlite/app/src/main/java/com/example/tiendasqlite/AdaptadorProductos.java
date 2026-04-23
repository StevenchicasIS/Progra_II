package com.example.tiendasqlite;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import de.hdodenhof.circleimageview.CircleImageView;

public class AdaptadorProductos extends RecyclerView.Adapter<AdaptadorProductos.ViewHolder> {

    private List<Producto> listaProductos = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void alHacerClick(Producto producto);
        void alMantenerClick(Producto producto);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setListaProductos(List<Producto> productos) {
        this.listaProductos = productos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_producto, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Producto producto = listaProductos.get(position);

        holder.txtCodigo.setText("📌 " + producto.getCodigo());
        holder.txtDescripcion.setText(producto.getDescripcion());
        holder.txtPrecio.setText("$" + String.format("%,.2f", producto.getPrecio()));

        // Mostrar la PRIMERA foto como portada
        List<byte[]> fotos = producto.getFotos();
        if (fotos != null && !fotos.isEmpty()) {
            UtilidadImagenes.cargarImagenRedondeada(holder.imgImagen, fotos.get(0));
        } else {
            holder.imgImagen.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.alHacerClick(producto);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.alMantenerClick(producto);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return listaProductos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView imgImagen;
        TextView txtCodigo, txtDescripcion, txtPrecio;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgImagen = itemView.findViewById(R.id.ivImagen);
            txtCodigo = itemView.findViewById(R.id.tvCodigo);
            txtDescripcion = itemView.findViewById(R.id.tvDescripcion);
            txtPrecio = itemView.findViewById(R.id.tvPrecio);
        }
    }
}