package com.example.comunidadsv;

public class Configuracion {


    private static final String SERVER_IP = "192.168.1.8";  // <-- CAMBIA SOLO ESTA LÍNEA
    private static final String SERVER_PORT = "5984";
    public static final String USER = "josue";
    public static final String PASS = "IdJosue2004";

    public static final String SERVIDOR = "http://" + USER + ":" + PASS + "@" + SERVER_IP + ":" + SERVER_PORT;

    public static String getCurrentServerInfo() {
        return "Conectado a: " + SERVER_IP + ":" + SERVER_PORT;
    }
}