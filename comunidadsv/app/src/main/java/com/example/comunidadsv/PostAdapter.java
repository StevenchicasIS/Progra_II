package com.example.comunidadsv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> posts;
    private String currentUserId;
    private OnPostActionListener listener;

    public interface OnPostActionListener {
        void onLikeClicked(Post post, int position);
        void onCommentAdded(Post post, int position, String commentText);
        void onShareClicked(Post post);
        void onCommentDeleted(Post post, int position, int commentIndex);
    }

    public PostAdapter(Context context, List<Post> posts, String currentUserId, OnPostActionListener listener) {
        this.context = context;
        this.posts = posts;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);

        holder.txtUserName.setText(post.getUserName());
        holder.txtUbicacion.setText(post.getUserUbicacion());
        holder.txtTiempo.setText(getTimeAgo(post.getFecha()));

        // Mostrar foto de perfil del usuario
        setupUserPhoto(holder, post);

        if (post.getCategoria() != null && !post.getCategoria().isEmpty()) {
            holder.txtCategoria.setText(post.getCategoria());
            holder.txtCategoria.setVisibility(View.VISIBLE);
        } else {
            holder.txtCategoria.setVisibility(View.GONE);
        }

        holder.txtTitulo.setText(post.getTitulo());
        holder.txtContenido.setText(post.getContenido());

        setupImagenes(holder, post);

        holder.txtLikes.setText(String.valueOf(post.getLikes()));
        if (post.isLikedByUser(currentUserId)) {
            holder.imgLike.setImageResource(R.drawable.ic_like);
            holder.imgLike.setColorFilter(context.getColor(R.color.green_primary));
        } else {
            holder.imgLike.setImageResource(R.drawable.ic_like);
            holder.imgLike.setColorFilter(context.getColor(R.color.gray_text));
        }

        int commentCount = post.getComments() != null ? post.getComments().size() : 0;
        holder.txtComments.setText(String.valueOf(commentCount));

        setupCommentsSection(holder, post, position);

        holder.layoutLike.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLikeClicked(post, position);
            }
        });

        holder.layoutComment.setOnClickListener(v -> {
            toggleCommentInput(holder, post, position);
        });

        holder.layoutShare.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShareClicked(post);
            }
        });

        holder.btnEnviarComentario.setOnClickListener(v -> {
            String commentText = holder.edtNuevoComentario.getText().toString().trim();
            if (!commentText.isEmpty() && listener != null) {
                listener.onCommentAdded(post, position, commentText);
                holder.edtNuevoComentario.setText("");
                holder.commentInputContainer.setVisibility(View.GONE);
            } else {
                Toast.makeText(context, "Escribe un comentario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUserPhoto(PostViewHolder holder, Post post) {
        SharedPreferences prefs = context.getSharedPreferences("ComunidadSV", Context.MODE_PRIVATE);
        String fotoBase64 = prefs.getString("fotoPerfil", "");

        if (!fotoBase64.isEmpty()) {
            Bitmap bitmap = ImageUtils.base64ToBitmap(fotoBase64);
            if (bitmap != null) {
                holder.imgUserPhoto.setImageBitmap(bitmap);
            } else {
                holder.imgUserPhoto.setImageResource(R.drawable.ic_profile);
            }
        } else {
            holder.imgUserPhoto.setImageResource(R.drawable.ic_profile);
        }
    }

    private void setupImagenes(PostViewHolder holder, Post post) {
        List<String> imagenesBase64 = post.getImagenesBase64();

        if (imagenesBase64 == null || imagenesBase64.isEmpty()) {
            holder.imagenesContainer.setVisibility(View.GONE);
            return;
        }

        holder.imagenesContainer.setVisibility(View.VISIBLE);
        holder.imagenesLayout.removeAllViews();

        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int imageSize;
        int margin = 0;

        if (imagenesBase64.size() == 1) {
            imageSize = (int) (screenWidth * 0.9);
            margin = 0;
        } else if (imagenesBase64.size() == 2) {
            imageSize = (int) (screenWidth * 0.45);
            margin = 8;
        } else {
            imageSize = (int) (screenWidth * 0.4);
            margin = 6;
        }

        for (int i = 0; i < imagenesBase64.size(); i++) {
            String base64 = imagenesBase64.get(i);
            ImageView imageView = new ImageView(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(imageSize, imageSize);

            if (i < imagenesBase64.size() - 1) {
                params.setMargins(0, 0, margin, 0);
            }

            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            imageView.setClipToOutline(true);
            imageView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), 24);
                }
            });

            imageView.setBackgroundResource(R.drawable.edittext_style);

            Bitmap bitmap = ImageUtils.base64ToBitmap(base64);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            } else {
                imageView.setImageResource(R.drawable.ic_camera);
            }

            final String finalBase64 = base64;
            imageView.setOnClickListener(v -> {
                showFullScreenImage(finalBase64);
            });

            holder.imagenesLayout.addView(imageView);
        }
    }

    private void showFullScreenImage(String base64) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Ver imagen");

        ImageView fullImageView = new ImageView(context);
        fullImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullImageView.setPadding(16, 16, 16, 16);

        Bitmap bitmap = ImageUtils.base64ToBitmap(base64);
        if (bitmap != null) {
            fullImageView.setImageBitmap(bitmap);
        } else {
            fullImageView.setImageResource(R.drawable.ic_camera);
        }

        builder.setView(fullImageView);
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    private void setupCommentsSection(PostViewHolder holder, Post post, int postPosition) {
        holder.commentsContainer.removeAllViews();

        List<Comment> comments = post.getComments();
        if (comments == null || comments.isEmpty()) {
            holder.commentsContainer.setVisibility(View.GONE);
            holder.txtVerMasComentarios.setVisibility(View.GONE);
            return;
        }

        holder.commentsContainer.setVisibility(View.VISIBLE);

        int start = Math.max(0, comments.size() - 2);
        for (int i = start; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            final int commentIndex = i;

            LinearLayout commentLayout = new LinearLayout(context);
            commentLayout.setOrientation(LinearLayout.HORIZONTAL);
            commentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            commentLayout.setPadding(0, 6, 0, 6);

            TextView commentView = new TextView(context);
            commentView.setText(String.format("%s: %s", comment.getUserName(), comment.getTexto()));
            commentView.setTextSize(13);
            commentView.setTextColor(context.getColor(R.color.text_secondary));
            commentView.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            ));

            commentLayout.addView(commentView);

            boolean isCommentOwner = comment.getUserId().equals(currentUserId);

            if (isCommentOwner) {
                TextView deleteText = new TextView(context);
                deleteText.setText(" Eliminar");
                deleteText.setTextSize(12);
                deleteText.setTextColor(context.getColor(R.color.red));
                deleteText.setPadding(8, 0, 0, 0);
                deleteText.setOnClickListener(v -> {
                    showDeleteCommentDialog(post, postPosition, commentIndex);
                });
                commentLayout.addView(deleteText);
            }

            holder.commentsContainer.addView(commentLayout);
        }

        if (comments.size() > 2) {
            holder.txtVerMasComentarios.setVisibility(View.VISIBLE);
            holder.txtVerMasComentarios.setOnClickListener(v -> {
                showAllCommentsDialog(post, postPosition);
            });
        } else {
            holder.txtVerMasComentarios.setVisibility(View.GONE);
        }
    }

    private void showDeleteCommentDialog(Post post, int postPosition, int commentIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Eliminar comentario");
        builder.setMessage("¿Estás seguro de que quieres eliminar este comentario?");
        builder.setPositiveButton("Eliminar", (dialog, which) -> {
            if (listener != null) {
                listener.onCommentDeleted(post, postPosition, commentIndex);
            }
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void showAllCommentsDialog(Post post, int postPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Todos los comentarios");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        for (int i = 0; i < post.getComments().size(); i++) {
            Comment comment = post.getComments().get(i);
            final int commentIndex = i;

            LinearLayout commentLayout = new LinearLayout(context);
            commentLayout.setOrientation(LinearLayout.HORIZONTAL);
            commentLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView commentView = new TextView(context);
            String fechaStr = new SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(new Date(comment.getFecha()));
            commentView.setText(String.format("%s: %s\n%s", comment.getUserName(), comment.getTexto(), fechaStr));
            commentView.setTextSize(13);
            commentView.setTextColor(context.getColor(R.color.text_secondary));
            commentView.setPadding(0, 10, 0, 10);
            commentView.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            ));

            commentLayout.addView(commentView);

            boolean isCommentOwner = comment.getUserId().equals(currentUserId);
            if (isCommentOwner) {
                TextView deleteText = new TextView(context);
                deleteText.setText("Eliminar");
                deleteText.setTextSize(12);
                deleteText.setTextColor(context.getColor(R.color.red));
                deleteText.setPadding(8, 0, 0, 0);
                deleteText.setOnClickListener(v -> {
                    showDeleteCommentDialog(post, postPosition, commentIndex);
                });
                commentLayout.addView(deleteText);
            }

            layout.addView(commentLayout);

            View divider = new View(context);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(context.getColor(R.color.gray_light));
            layout.addView(divider);
        }

        builder.setView(layout);
        builder.setPositiveButton("Cerrar", null);
        builder.show();
    }

    private void toggleCommentInput(PostViewHolder holder, Post post, int position) {
        if (holder.commentInputContainer.getVisibility() == View.VISIBLE) {
            holder.commentInputContainer.setVisibility(View.GONE);
        } else {
            holder.commentInputContainer.setVisibility(View.VISIBLE);
            holder.edtNuevoComentario.requestFocus();
        }
    }

    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            return "hace " + (diff / 1000) + "s";
        } else if (diff < 3600000) {
            return "hace " + (diff / 60000) + "m";
        } else if (diff < 86400000) {
            return "hace " + (diff / 3600000) + "h";
        } else if (diff < 604800000) {
            return "hace " + (diff / 86400000) + "d";
        } else {
            return new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date(timestamp));
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updatePost(int position, Post updatedPost) {
        posts.set(position, updatedPost);
        notifyItemChanged(position);
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imgUserPhoto;
        TextView txtUserName, txtUbicacion, txtTiempo, txtCategoria;
        TextView txtTitulo, txtContenido, txtLikes, txtComments;
        TextView txtVerMasComentarios;
        ImageView imgLike;
        LinearLayout layoutLike, layoutComment, layoutShare;
        LinearLayout commentsContainer, commentInputContainer;
        EditText edtNuevoComentario;
        Button btnEnviarComentario;
        HorizontalScrollView imagenesContainer;
        LinearLayout imagenesLayout;

        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUserPhoto = itemView.findViewById(R.id.imgUserPhoto);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtUbicacion = itemView.findViewById(R.id.txtUbicacion);
            txtTiempo = itemView.findViewById(R.id.txtTiempo);
            txtCategoria = itemView.findViewById(R.id.txtCategoria);
            txtTitulo = itemView.findViewById(R.id.txtTitulo);
            txtContenido = itemView.findViewById(R.id.txtContenido);
            txtLikes = itemView.findViewById(R.id.txtLikes);
            txtComments = itemView.findViewById(R.id.txtComments);
            txtVerMasComentarios = itemView.findViewById(R.id.txtVerMasComentarios);
            imgLike = itemView.findViewById(R.id.imgLike);
            layoutLike = itemView.findViewById(R.id.layoutLike);
            layoutComment = itemView.findViewById(R.id.layoutComment);
            layoutShare = itemView.findViewById(R.id.layoutShare);
            commentsContainer = itemView.findViewById(R.id.commentsContainer);
            commentInputContainer = itemView.findViewById(R.id.commentInputContainer);
            edtNuevoComentario = itemView.findViewById(R.id.edtNuevoComentario);
            btnEnviarComentario = itemView.findViewById(R.id.btnEnviarComentario);
            imagenesContainer = itemView.findViewById(R.id.imagenesContainer);
            imagenesLayout = itemView.findViewById(R.id.imagenesLayout);
        }
    }
}