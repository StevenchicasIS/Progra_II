package com.example.tiendasqlite;

public class Foto {
    private int id;
    private int productoId;
    private byte[] imagen;

    public Foto() {}

    public Foto(int productoId, byte[] imagen) {
        this.productoId = productoId;
        this.imagen = imagen;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public byte[] getImagen() { return imagen; }
    public void setImagen(byte[] imagen) { this.imagen = imagen; }
}