package com.example.offlinepaymentsystem.model;

import androidx.annotation.NonNull;

/***
 * Representa al receptor desde el punto de vista del emisor. Solo tiene datos.
 */
public class WhitelistItem {
    private String direccion;
    private String nombre;
    private double limite;
    private double limiteOriginal; //TODO:Para saber cuánto se ha gastado, quizás lo quitamos
    private long timestampAgregado;

    public WhitelistItem(){
    }

    public WhitelistItem(String direccion, String nombre, double limite){
        this.direccion = direccion;
        this.nombre= nombre;
        this.limite = limite;
        this.limiteOriginal = limite;
        this.timestampAgregado = System.currentTimeMillis();
    }


    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getLimite() {
        return limite;
    }

    public void setLimite(double limite) {
        this.limite = limite;
    }

    @NonNull
    @Override
    public String toString() {
        return "WhitelistItem{" +
                "direccion='" + direccion + '\'' +
                ", nombre='" + nombre + '\'' +
                ", limite=" + limite +
                ", limiteOriginal=" + limiteOriginal +
                ", timestampAgregado=" + timestampAgregado +
                '}';
    }
}
