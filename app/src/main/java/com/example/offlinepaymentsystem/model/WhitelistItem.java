package com.example.offlinepaymentsystem.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "whitelist")
public class WhitelistItem {
    @PrimaryKey
    @NonNull
    private String direccion;

    private String nombre;
    private long limite;
    private long timestampAgregado;


    public WhitelistItem(){
    }

    public WhitelistItem(String direccion, String nombre, long limite){
        this.direccion = direccion;
        this.nombre= nombre;
        this.limite = limite;
        this.timestampAgregado = System.currentTimeMillis();
    }


    @NonNull
    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(@NonNull String direccion) {
        this.direccion = direccion;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getLimite() {
        return limite;
    }

    public void setLimite(long limite) {
        this.limite = limite;
    }

    public long getTimestampAgregado() {
        return timestampAgregado;
    }

    public void setTimestampAgregado(long timestampAgregado) {
        this.timestampAgregado = timestampAgregado;
    }


    @NonNull
    @Override
    public String toString() {
        return "WhitelistItem{" +
                "direccion='" + direccion + '\'' +
                ", nombre='" + nombre + '\'' +
                ", limite=" + limite +
                ", timestampAgregado=" + timestampAgregado +
                '}';
    }
}
