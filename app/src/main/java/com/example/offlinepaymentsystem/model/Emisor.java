package com.example.offlinepaymentsystem.model;
import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
/**
 * Representa a nuestro pagador semi-offline.
 **/
public class Emisor {

    private String address;
    private String hashActual;
    /**
     * Ejemplo:
     * 0xTiendaA: 30ETH
     * 0xTiendaB: 70ETH
     * */
    private Map<String, Double> whitelist;
    private long timestampUltimoPago;
    private boolean registrado;


    public Emisor() {
        this.whitelist = new HashMap<>();
        this.timestampUltimoPago =0;
        this.registrado = false;
    }

    public Emisor(String address, String HashActual){
        this.address = address;
        this.hashActual = hashActual;
        this.whitelist = new HashMap<>();
        this.timestampUltimoPago = 0;
        this.registrado = false;
    }



    /**
     * Getters y setters
     * */

    public String getAddress() {
        return this.address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getHashActual() {
        return this.hashActual;
    }

    public void setHashActual(String hashActual) {
        this.hashActual = hashActual;
    }

    public Map<String, Double> getWhitelist() {
        return this.whitelist;
    }

    public void setWhitelist(Map<String,Double> whitelist) {
        this.whitelist = whitelist;
    }

    public long getTimestampUltimoPago() {
        return this.timestampUltimoPago;
    }

    public void setTimestampUltimoPago(long timestampUltimoPago){
        this.timestampUltimoPago = timestampUltimoPago;
    }

    public boolean isRegistrado() {
        return registrado;
    }

    public void setRegistrado(boolean registrado) {
        this.registrado = registrado;
    }
    @NonNull
    @Override
    public String toString() {
        return "Emisor{" +
                "address='" + this.getAddress() + '\'' +
                ", hashActual='" + this.getHashActual()+ '\'' +
                ", whitelist=" + this.getWhitelist() +
                ", timestampUltimoPago=" + this.getTimestampUltimoPago() +
                ", registrado=" + this.isRegistrado() +
                '}';
    }
}

