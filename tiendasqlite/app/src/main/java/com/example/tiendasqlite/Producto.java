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
    private double costo;      // NUEVO
    private double ganancia;   // NUEVO (porcentaje)
    private int stock;         // NUEVO
    private List<byte[]> fotos;

    public Producto() {
        this.fotos = new ArrayList<>();
    }

    public Producto(String codigo, String descripcion, String marca,
                    String presentacion, double precio, double costo, int stock) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.marca = marca;
        this.presentacion = presentacion;
        this.precio = precio;
        this.costo = costo;
        this.stock = stock;
        this.ganancia = calcularGanancia(precio, costo);
        this.fotos = new ArrayList<>();
    }

    // Calcular porcentaje de ganancia
    public static double calcularGanancia(double precio, double costo) {
        if (costo <= 0) return 0;
        return ((precio - costo) / costo) * 100;
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
    public void setPrecio(double precio) {
        this.precio = precio;
        this.ganancia = calcularGanancia(precio, this.costo);
    }
    public double getCosto() { return costo; }
    public void setCosto(double costo) {
        this.costo = costo;
        this.ganancia = calcularGanancia(this.precio, costo);
    }
    public double getGanancia() { return ganancia; }
    public void setGanancia(double ganancia) { this.ganancia = ganancia; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public List<byte[]> getFotos() { return fotos; }
    public void setFotos(List<byte[]> fotos) { this.fotos = fotos; }
    public void agregarFoto(byte[] foto) { this.fotos.add(foto); }
}