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
        void alHacerClick(Producto producto);      //  Modificación
        void alMantenerClick(Producto producto);  //  Eliminación
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setListaProductos(List<Producto> productos) {
        this.listaProductos = productos;
        notifyDataSetChanged();
    }

    public Producto getProductoAtPosition(int position) {
        if (position >= 0 && position < listaProductos.size()) {
            return listaProductos.get(position);
        }
        return null;
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

        holder.itemView.setTag(position);
        holder.txtCodigo.setText("📌 " + producto.getCodigo());

        if (producto.getDescripcion() != null && !producto.getDescripcion().isEmpty()) {
            holder.txtDescripcion.setText(producto.getDescripcion());
        } else {
            holder.txtDescripcion.setText("Sin nombre");
        }

        if (producto.getMarca() != null && !producto.getMarca().isEmpty()) {
            holder.txtMarca.setText("🏷️ " + producto.getMarca());
            holder.txtMarca.setVisibility(View.VISIBLE);
        } else {
            holder.txtMarca.setVisibility(View.GONE);
        }


        holder.txtPrecio.setText("$" + String.format("%,.2f", producto.getPrecio()));

        holder.txtStock.setText("📦 Stock: " + producto.getStock());


        holder.txtGanancia.setText("📈 Ganancia: " + String.format("%.1f", producto.getGanancia()) + "%");


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
        TextView txtCodigo, txtDescripcion, txtMarca, txtPrecio, txtStock, txtGanancia;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgImagen = itemView.findViewById(R.id.ivImagen);
            txtCodigo = itemView.findViewById(R.id.tvCodigo);
            txtDescripcion = itemView.findViewById(R.id.tvDescripcion);
            txtMarca = itemView.findViewById(R.id.tvMarca);
            txtPrecio = itemView.findViewById(R.id.tvPrecio);
            txtStock = itemView.findViewById(R.id.tvStock);      // ✅ Campo stock
            txtGanancia = itemView.findViewById(R.id.tvGanancia); // ✅ Campo ganancia
        }
    }
}