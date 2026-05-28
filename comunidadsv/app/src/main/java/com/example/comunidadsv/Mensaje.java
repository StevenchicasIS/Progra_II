package com.example.comunidadsv;

import org.json.JSONObject;
import java.util.UUID;

public class Mensaje {
    private String id;
    private String chatId;
    private String emisorId;
    private String emisorNombre;
    private String texto;
    private long fecha;
    private boolean leido;

    public Mensaje(String chatId, String emisorId, String emisorNombre, String texto) {
        this.id = UUID.randomUUID().toString();
        this.chatId = chatId;
        this.emisorId = emisorId;
        this.emisorNombre = emisorNombre;
        this.texto = texto;
        this.fecha = System.currentTimeMillis();
        this.leido = false;
    }

    public static Mensaje fromJSON(JSONObject json) {
        Mensaje m = new Mensaje(
                json.optString("chatId"),
                json.optString("emisorId"),
                json.optString("emisorNombre"),
                json.optString("texto")
        );
        m.id = json.optString("_id");
        m.fecha = json.optLong("fecha");
        m.leido = json.optBoolean("leido", false);
        return m;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("_id", id);
            json.put("tipo", "mensaje");
            json.put("chatId", chatId);
            json.put("emisorId", emisorId);
            json.put("emisorNombre", emisorNombre);
            json.put("texto", texto);
            json.put("fecha", fecha);
            json.put("leido", leido);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getChatId() { return chatId; }
    public String getEmisorId() { return emisorId; }
    public String getEmisorNombre() { return emisorNombre; }
    public String getTexto() { return texto; }
    public long getFecha() { return fecha; }
    public boolean isLeido() { return leido; }
    public void setLeido(boolean leido) { this.leido = leido; }
}