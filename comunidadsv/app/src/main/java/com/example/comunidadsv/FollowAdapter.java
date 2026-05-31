package com.example.comunidadsv;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FollowAdapter extends RecyclerView.Adapter<FollowAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;

    public FollowAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_follow, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = userList.get(position);
        holder.txtNombre.setText(user.getNombre());
        holder.txtUbicacion.setText(user.getUbicacion());

        if (user.getFotoPerfil() != null && !user.getFotoPerfil().isEmpty()) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(user.getFotoPerfil());
            if (bitmap != null) {
                holder.imgFoto.setImageBitmap(bitmap);
            } else {
                holder.imgFoto.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.imgFoto.setImageResource(R.drawable.ic_profile);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userId", user.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFoto;
        TextView txtNombre, txtUbicacion;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgFoto = itemView.findViewById(R.id.imgFoto);
            txtNombre = itemView.findViewById(R.id.txtNombre);
            txtUbicacion = itemView.findViewById(R.id.txtUbicacion);
        }
    }
}