package com.rhsoft.velocityautomotive.Data;

public class Category {
    private int id;
    private String nombre;
    private String icono;

    // Constructor — crea una categoría con sus 3 datos
    public Category(int id, String nombre, String icono) {
        this.id     = id;
        this.nombre = nombre;
        this.icono  = icono;
    }
    // Getters — permiten leer los datos desde afuera
    public int    getId()     { return id; }
    public String getNombre() { return nombre; }
    public String getIcono()  { return icono; }
}