package com.example.tiendasqlite;

import java.util.ArrayList;
import java.util.List;

public class Producto {
    private int id;
    private String codigo;
    private String descripcion;
    private String marca;
    private String presentacion;
    private double precio;
    private List<byte[]> fotos;

    public Producto() {
        this.fotos = new ArrayList<>();
    }

    public Producto(String codigo, String descripcion, String marca,
                    String presentacion, double precio) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.marca = marca;
        this.presentacion = presentacion;
        this.precio = precio;
        this.fotos = new ArrayList<>();
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getPresentacion() { return presentacion; }
    public void setPresentacion(String presentacion) { this.presentacion = presentacion; }
    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }
    public List<byte[]> getFotos() { return fotos; }
    public void setFotos(List<byte[]> fotos) { this.fotos = fotos; }

    // Método para obtener la primera foto (portada)
    public byte[] getFotoPortada() {
        if (fotos != null && !fotos.isEmpty()) {
            return fotos.get(0);
        }
        return null;
    }
}