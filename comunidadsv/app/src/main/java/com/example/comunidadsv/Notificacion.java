package com.example.comunidadsv;

import org.json.JSONObject;
import java.util.UUID;

public class Notificacion {
    private String id;
    private String usuarioId;      // Usuario que recibe la notificación
    private String tipo;           // "like", "comentario", "nueva_publicacion", "solicitud_aceptada"
    private String emisorId;       // Usuario que genera la notificación
    private String emisorNombre;
    private String emisorFoto;
    private String postId;         // ID de la publicación relacionada (si aplica)
    private String postTitulo;     // Título de la publicación (para mostrar)
    private String mensaje;
    private long fecha;
    private boolean leido;

    public Notificacion(String usuarioId, String tipo, String emisorId, String emisorNombre,
                        String emisorFoto, String postId, String postTitulo, String mensaje) {
        this.id = UUID.randomUUID().toString();
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.emisorId = emisorId;
        this.emisorNombre = emisorNombre;
        this.emisorFoto = emisorFoto;
        this.postId = postId;
        this.postTitulo = postTitulo;
        this.mensaje = mensaje;
        this.fecha = System.currentTimeMillis();
        this.leido = false;
    }

    public static Notificacion fromJSON(JSONObject json) {
        Notificacion n = new Notificacion(
                json.optString("usuarioId"),
                json.optString("tipo"),
                json.optString("emisorId"),
                json.optString("emisorNombre"),
                json.optString("emisorFoto"),
                json.optString("postId"),
                json.optString("postTitulo"),
                json.optString("mensaje")
        );
        n.id = json.optString("_id");
        n.fecha = json.optLong("fecha");
        n.leido = json.optBoolean("leido", false);
        return n;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("_id", id);
            json.put("tipo", "notificacion");
            json.put("usuarioId", usuarioId);
            json.put("tipoNotificacion", tipo);
            json.put("emisorId", emisorId);
            json.put("emisorNombre", emisorNombre);
            json.put("emisorFoto", emisorFoto);
            json.put("postId", postId);
            json.put("postTitulo", postTitulo);
            json.put("mensaje", mensaje);
            json.put("fecha", fecha);
            json.put("leido", leido);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getUsuarioId() { return usuarioId; }
    public String getTipo() { return tipo; }
    public String getEmisorId() { return emisorId; }
    public String getEmisorNombre() { return emisorNombre; }
    public String getEmisorFoto() { return emisorFoto; }
    public String getPostId() { return postId; }
    public String getPostTitulo() { return postTitulo; }
    public String getMensaje() { return mensaje; }
    public long getFecha() { return fecha; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}