package com.example.offlinepaymentsystem.model;

import org.jetbrains.annotations.NotNull;

public class Receptor {
    private String address;
    private String nombre;
    private long timestampRegistro;
    private int pagosRecibidos; //TODO: Atributo que a lo mejor elimino porque no aporta nada.
    private double totalRecibido;
    private boolean registrado;

    public Receptor(){
        this.pagosRecibidos=0;
        this.totalRecibido=0.0;
        this.registrado= false;
    }

    public Receptor(String address, String nombre){
        this.address = address;
        this.nombre = nombre;
        this.timestampRegistro = System.currentTimeMillis();
        this.pagosRecibidos =0;
        this.totalRecibido =0.0;
        this.registrado = false;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public long getTimestampRegistro() {
        return timestampRegistro;
    }

    public void setTimestampRegistro(long timestampRegistro) {
        this.timestampRegistro = timestampRegistro;
    }

    public int getPagosRecibidos() {
        return pagosRecibidos;
    }

    public void setPagosRecibidos(int pagosRecibidos) {
        this.pagosRecibidos = pagosRecibidos;
    }

    public double getTotalRecibido() {
        return totalRecibido;
    }

    public void setTotalRecibido(double totalRecibido) {
        this.totalRecibido = totalRecibido;
    }

    public boolean isRegistrado() {
        return registrado;
    }

    public void setRegistrado(boolean registrado) {
        this.registrado = registrado;
    }

    @NotNull
    @Override
    public String toString() {
        return "Receptor{" +
                "address='" + address + '\'' +
                ", nombre='" + nombre + '\'' +
                ", timestampRegistro=" + timestampRegistro +
                ", pagosRecibidos=" + pagosRecibidos +
                ", totalRecibido=" + totalRecibido +
                ", registrado=" + registrado +
                '}';
    }
}
