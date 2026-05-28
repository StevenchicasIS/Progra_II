package com.example.comunidadsv;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Outline;
import android.os.AsyncTask;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private Context context;
    private List<Post> posts;
    private String currentUserId;
    private OnPostActionListener listener;
    private Map<String, String> userPhotoCache = new HashMap<>();

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

        holder.txtUserName.setOnClickListener(v -> {
            if (!post.getUserId().equals(currentUserId)) {
                showFollowDialog(post);
            } else {
                Toast.makeText(context, "Eres tú mismo", Toast.LENGTH_SHORT).show();
            }
        });

        if (post.getCategoria() != null && !post.getCategoria().isEmpty()) {
            holder.txtCategoria.setText(post.getCategoria());
            holder.txtCategoria.setVisibility(View.VISIBLE);
        } else {
            holder.txtCategoria.setVisibility(View.GONE);
        }

        holder.txtTitulo.setText(post.getTitulo());
        holder.txtContenido.setText(post.getContenido());

        setupImagenes(holder, post);
        loadUserPhoto(holder.imgUserPhoto, post.getUserId());

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

    private void loadUserPhoto(ImageView imageView, String userId) {
        if (userPhotoCache.containsKey(userId)) {
            String fotoBase64 = userPhotoCache.get(userId);
            if (!fotoBase64.isEmpty()) {
                Bitmap bitmap = ImageUtils.base64ToBitmap(fotoBase64);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    return;
                }
            }
            imageView.setImageResource(R.drawable.ic_profile);
            return;
        }

        imageView.setImageResource(R.drawable.ic_profile);
        new LoadUserPhotoTask(imageView, userId).execute();
    }

    private class LoadUserPhotoTask extends AsyncTask<Void, Void, String> {
        private ImageView imageView;
        private String userId;

        LoadUserPhotoTask(ImageView imageView, String userId) {
            this.imageView = imageView;
            this.userId = userId;
        }

        @Override
        protected String doInBackground(Void... voids) {
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

        @Override
        protected void onPostExecute(String fotoBase64) {
            userPhotoCache.put(userId, fotoBase64);
            if (!fotoBase64.isEmpty()) {
                Bitmap bitmap = ImageUtils.base64ToBitmap(fotoBase64);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }

    private void showFollowDialog(Post post) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_enviar_solicitud, null);

        TextView txtNombreUsuario = dialogView.findViewById(R.id.txtNombreUsuario);
        TextView txtUbicacionUsuario = dialogView.findViewById(R.id.txtUbicacionUsuario);
        TextView txtMensaje = dialogView.findViewById(R.id.txtMensaje);
        Button btnEnviarSolicitud = dialogView.findViewById(R.id.btnEnviarSolicitud);
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);

        txtNombreUsuario.setText(post.getUserName());
        txtUbicacionUsuario.setText(post.getUserUbicacion());
        txtMensaje.setText("¿Quieres enviarle una solicitud para poder chatear?");

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        btnEnviarSolicitud.setOnClickListener(v -> {
            enviarSolicitud(post.getUserId(), post.getUserName());
            dialog.dismiss();
        });

        btnCancelar.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void enviarSolicitud(String receptorId, String receptorNombre) {
        SharedPreferences prefs = context.getSharedPreferences("ComunidadSV", Context.MODE_PRIVATE);
        String emisorId = prefs.getString("userId", "");
        String emisorNombre = prefs.getString("nombre", "Usuario");
        String emisorFoto = prefs.getString("fotoPerfil", "");

        new EnviarSolicitudTask().execute(emisorId, emisorNombre, emisorFoto, receptorId, receptorNombre);
    }

    private class EnviarSolicitudTask extends AsyncTask<String, Void, Boolean> {
        private String errorMsg = "";
        private String receptorNombre;

        @Override
        protected Boolean doInBackground(String... params) {
            String emisorId = params[0];
            String emisorNombre = params[1];
            String emisorFoto = params[2];
            String receptorId = params[3];
            receptorNombre = params[4];

            try {
                // Verificar si ya existe una solicitud pendiente
                String checkUrl = Configuracion.SERVIDOR + "/db_solicitudes/_design/solicitudes/_view/pendientes?key=\"" + receptorId + "\"";
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
                        errorMsg = "Ya tienes una solicitud pendiente para este usuario";
                        return false;
                    }
                }

                // Crear nueva solicitud con el nombre del EMISOR
                Solicitud solicitud = new Solicitud(emisorId, emisorNombre, emisorFoto, receptorId);
                String putUrl = Configuracion.SERVIDOR + "/db_solicitudes/" + solicitud.getId();
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
                writer.write(solicitud.toJSON().toString());
                writer.flush();
                writer.close();
                os.close();

                int responseCode = putConn.getResponseCode();
                return responseCode == 201 || responseCode == 202;

            } catch (Exception e) {
                errorMsg = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(context, "Solicitud enviada a " + receptorNombre, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "Error: " + errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setBasicAuth(HttpURLConnection conn) {
        String auth = Configuracion.USER + ":" + Configuracion.PASS;
        byte[] encoded = android.util.Base64.encode(auth.getBytes(), android.util.Base64.NO_WRAP);
        conn.setRequestProperty("Authorization", "Basic " + new String(encoded));
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