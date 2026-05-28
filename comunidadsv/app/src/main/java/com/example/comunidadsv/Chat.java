package com.example.comunidadsv;

import org.json.JSONObject;
import java.util.UUID;

public class Chat {
    private String id;
    private String usuario1Id;
    private String usuario1Nombre;
    private String usuario1Foto;
    private String usuario2Id;
    private String usuario2Nombre;
    private String usuario2Foto;
    private String ultimoMensaje;
    private long ultimoMensajeFecha;
    private String ultimoMensajeRemitenteId;
    private int noLeidosUsuario1;
    private int noLeidosUsuario2;

    public Chat(String usuario1Id, String usuario1Nombre, String usuario1Foto,
                String usuario2Id, String usuario2Nombre, String usuario2Foto) {
        this.id = UUID.randomUUID().toString();
        this.usuario1Id = usuario1Id;
        this.usuario1Nombre = usuario1Nombre;
        this.usuario1Foto = usuario1Foto;
        this.usuario2Id = usuario2Id;
        this.usuario2Nombre = usuario2Nombre;
        this.usuario2Foto = usuario2Foto;
        this.ultimoMensaje = "";
        this.ultimoMensajeFecha = System.currentTimeMillis();
        this.ultimoMensajeRemitenteId = "";
        this.noLeidosUsuario1 = 0;
        this.noLeidosUsuario2 = 0;
    }

    public static Chat fromJSON(JSONObject json) {
        Chat chat = new Chat(
                json.optString("usuario1Id"),
                json.optString("usuario1Nombre"),
                json.optString("usuario1Foto"),
                json.optString("usuario2Id"),
                json.optString("usuario2Nombre"),
                json.optString("usuario2Foto")
        );
        chat.id = json.optString("_id");
        chat.ultimoMensaje = json.optString("ultimoMensaje");
        chat.ultimoMensajeFecha = json.optLong("ultimoMensajeFecha");
        chat.ultimoMensajeRemitenteId = json.optString("ultimoMensajeRemitenteId");
        chat.noLeidosUsuario1 = json.optInt("noLeidosUsuario1", 0);
        chat.noLeidosUsuario2 = json.optInt("noLeidosUsuario2", 0);
        return chat;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("_id", id);
            json.put("tipo", "chat");
            json.put("usuario1Id", usuario1Id);
            json.put("usuario1Nombre", usuario1Nombre);
            json.put("usuario1Foto", usuario1Foto);
            json.put("usuario2Id", usuario2Id);
            json.put("usuario2Nombre", usuario2Nombre);
            json.put("usuario2Foto", usuario2Foto);
            json.put("ultimoMensaje", ultimoMensaje);
            json.put("ultimoMensajeFecha", ultimoMensajeFecha);
            json.put("ultimoMensajeRemitenteId", ultimoMensajeRemitenteId);
            json.put("noLeidosUsuario1", noLeidosUsuario1);
            json.put("noLeidosUsuario2", noLeidosUsuario2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getUsuario1Id() { return usuario1Id; }
    public String getUsuario1Nombre() { return usuario1Nombre; }
    public String getUsuario1Foto() { return usuario1Foto; }
    public String getUsuario2Id() { return usuario2Id; }
    public String getUsuario2Nombre() { return usuario2Nombre; }
    public String getUsuario2Foto() { return usuario2Foto; }
    public String getUltimoMensaje() { return ultimoMensaje; }
    public void setUltimoMensaje(String ultimoMensaje) { this.ultimoMensaje = ultimoMensaje; }
    public long getUltimoMensajeFecha() { return ultimoMensajeFecha; }
    public void setUltimoMensajeFecha(long ultimoMensajeFecha) { this.ultimoMensajeFecha = ultimoMensajeFecha; }
    public String getUltimoMensajeRemitenteId() { return ultimoMensajeRemitenteId; }
    public void setUltimoMensajeRemitenteId(String ultimoMensajeRemitenteId) { this.ultimoMensajeRemitenteId = ultimoMensajeRemitenteId; }
    public int getNoLeidosUsuario1() { return noLeidosUsuario1; }
    public void setNoLeidosUsuario1(int noLeidosUsuario1) { this.noLeidosUsuario1 = noLeidosUsuario1; }
    public int getNoLeidosUsuario2() { return noLeidosUsuario2; }
    public void setNoLeidosUsuario2(int noLeidosUsuario2) { this.noLeidosUsuario2 = noLeidosUsuario2; }

    public int getNoLeidos(String usuarioId) {
        if (usuarioId.equals(usuario1Id)) {
            return noLeidosUsuario1;
        } else {
            return noLeidosUsuario2;
        }
    }

    public void incrementarNoLeidos(String usuarioId) {
        if (usuarioId.equals(usuario1Id)) {
            noLeidosUsuario1++;
        } else {
            noLeidosUsuario2++;
        }
    }

    public void resetearNoLeidos(String usuarioId) {
        if (usuarioId.equals(usuario1Id)) {
            noLeidosUsuario1 = 0;
        } else {
            noLeidosUsuario2 = 0;
        }
    }
}