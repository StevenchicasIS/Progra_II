package com.example.comunidadsv;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private String id;
    private String userId;
    private String userName;
    private String userUbicacion;
    private String titulo;
    private String contenido;
    private long fecha;
    private int likes;
    private List<String> likedBy;
    private List<Comment> comments;
    private String categoria;
    private String ubicacionPost;
    private List<String> imagenesBase64;

    public Post() {
        this.likedBy = new ArrayList<>();
        this.comments = new ArrayList<>();
        this.imagenesBase64 = new ArrayList<>();
    }

    public static Post fromJSON(JSONObject json, String docId) {
        Post post = new Post();
        post.id = docId;
        post.userId = json.optString("userId");
        post.userName = json.optString("userName");
        post.userUbicacion = json.optString("userUbicacion");
        post.titulo = json.optString("titulo");
        post.contenido = json.optString("contenido");
        post.fecha = json.optLong("fecha");
        post.likes = json.optInt("likes", 0);
        post.categoria = json.optString("categoria");
        post.ubicacionPost = json.optString("ubicacion");

        JSONArray likedArray = json.optJSONArray("likedBy");
        if (likedArray != null) {
            for (int i = 0; i < likedArray.length(); i++) {
                post.likedBy.add(likedArray.optString(i));
            }
        }

        JSONArray commentsArray = json.optJSONArray("comments");
        if (commentsArray != null) {
            for (int i = 0; i < commentsArray.length(); i++) {
                JSONObject c = commentsArray.optJSONObject(i);
                if (c != null) {
                    post.comments.add(Comment.fromJSON(c));
                }
            }
        }

        JSONArray imagenesArray = json.optJSONArray("imagenesBase64");
        if (imagenesArray == null) {
            imagenesArray = json.optJSONArray("imagenes");
        }
        if (imagenesArray != null) {
            for (int i = 0; i < imagenesArray.length(); i++) {
                post.imagenesBase64.add(imagenesArray.optString(i));
            }
        }

        return post;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("tipo", "publicacion");
            json.put("userId", userId);
            json.put("userName", userName);
            json.put("userUbicacion", userUbicacion);
            json.put("titulo", titulo);
            json.put("contenido", contenido);
            json.put("fecha", fecha);
            json.put("likes", likes);
            json.put("categoria", categoria);
            json.put("ubicacion", ubicacionPost);
            json.put("likedBy", new JSONArray(likedBy));

            JSONArray commentsArray = new JSONArray();
            for (Comment c : comments) {
                commentsArray.put(c.toJSON());
            }
            json.put("comments", commentsArray);

            JSONArray imagenesArray = new JSONArray();
            for (String img : imagenesBase64) {
                imagenesArray.put(img);
            }
            json.put("imagenesBase64", imagenesArray);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserUbicacion() { return userUbicacion; }
    public void setUserUbicacion(String userUbicacion) { this.userUbicacion = userUbicacion; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public long getFecha() { return fecha; }
    public void setFecha(long fecha) { this.fecha = fecha; }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public List<String> getLikedBy() { return likedBy; }
    public List<Comment> getComments() { return comments; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getUbicacionPost() { return ubicacionPost; }
    public void setUbicacionPost(String ubicacionPost) { this.ubicacionPost = ubicacionPost; }
    public List<String> getImagenesBase64() { return imagenesBase64; }
    public void setImagenesBase64(List<String> imagenesBase64) { this.imagenesBase64 = imagenesBase64; }

    public boolean isLikedByUser(String userId) {
        return likedBy.contains(userId);
    }
}