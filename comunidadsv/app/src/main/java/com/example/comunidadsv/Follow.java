package com.example.comunidadsv;

import org.json.JSONObject;
import java.util.UUID;

public class Follow {
    private String id;
    private String followerId;  // Usuario que sigue
    private String followingId; // Usuario seguido
    private long fecha;

    public Follow(String followerId, String followingId) {
        this.id = UUID.randomUUID().toString();
        this.followerId = followerId;
        this.followingId = followingId;
        this.fecha = System.currentTimeMillis();
    }

    public Follow(String id, String followerId, String followingId, long fecha) {
        this.id = id;
        this.followerId = followerId;
        this.followingId = followingId;
        this.fecha = fecha;
    }

    public static Follow fromJSON(JSONObject json) {
        return new Follow(
                json.optString("_id"),
                json.optString("followerId"),
                json.optString("followingId"),
                json.optLong("fecha")
        );
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("_id", id);
            json.put("tipo", "follow");
            json.put("followerId", followerId);
            json.put("followingId", followingId);
            json.put("fecha", fecha);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    // Getters
    public String getId() { return id; }
    public String getFollowerId() { return followerId; }
    public String getFollowingId() { return followingId; }
    public long getFecha() { return fecha; }
}