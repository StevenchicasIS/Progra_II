package com.example.comunidadsv;

import org.json.JSONObject;
import java.util.UUID;

public class Solicitud {
    private String id;
    private String emisorId;
    private String emisorNombre;
    private String emisorFoto;
    private String receptorId;
    private String estado;
    private long fecha;

    public Solicitud(String emisorId, String emisorNombre, String emisorFoto, String receptorId) {
        this.id = UUID.randomUUID().toString();
        this.emisorId = emisorId;
        this.emisorNombre = emisorNombre;
        this.emisorFoto = emisorFoto;
        this.receptorId = receptorId;
        this.estado = "pendiente";
        this.fecha = System.currentTimeMillis();
    }

    public static Solicitud fromJSON(JSONObject json) {
        Solicitud s = new Solicitud(
                json.optString("emisorId"),
                json.optString("emisorNombre"),
                json.optString("emisorFoto"),
                json.optString("receptorId")
        );
        s.id = json.optString("_id");
        s.estado = json.optString("estado", "pendiente");
        s.fecha = json.optLong("fecha");
        return s;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("_id", id);
            json.put("tipo", "solicitud_seguimiento");
            json.put("emisorId", emisorId);
            json.put("emisorNombre", emisorNombre);
            json.put("emisorFoto", emisorFoto);
            json.put("receptorId", receptorId);
            json.put("estado", estado);
            json.put("fecha", fecha);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    // Getters
    public String getId() { return id; }
    public String getEmisorId() { return emisorId; }
    public String getEmisorNombre() { return emisorNombre; }
    public String getEmisorFoto() { return emisorFoto; }
    public String getReceptorId() { return receptorId; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public long getFecha() { return fecha; }
}