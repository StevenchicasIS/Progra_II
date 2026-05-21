package com.example.comunidadsv;

import org.json.JSONObject;
import java.util.UUID;

public class Comment {
    private String id;
    private String userId;
    private String userName;
    private String texto;
    private long fecha;

    public Comment(String userId, String userName, String texto, long fecha) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.userName = userName;
        this.texto = texto;
        this.fecha = fecha;
    }

    public Comment(String id, String userId, String userName, String texto, long fecha) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.texto = texto;
        this.fecha = fecha;
    }

    public static Comment fromJSON(JSONObject json) {
        String id = json.optString("id");
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        return new Comment(
                id,
                json.optString("userId"),
                json.optString("userName"),
                json.optString("texto"),
                json.optLong("fecha")
        );
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("userId", userId);
            json.put("userName", userName);
            json.put("texto", texto);
            json.put("fecha", fecha);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getTexto() { return texto; }
    public long getFecha() { return fecha; }
}