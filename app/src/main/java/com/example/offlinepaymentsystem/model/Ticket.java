package com.example.offlinepaymentsystem.model;

import org.jetbrains.annotations.NotNull;

/**
 * Representa un pago concreto --> es lo que mandamos al pagador en forma de QR.
 */
public class Ticket {
    private String hashAnterior;
    private String hashNuevo;
    private  String receptor;
    private double amount;
    private long timestamp;
    private long nonce; //TODO:Para prevenir reply attacks veremos si lo elimino o no, creo que no
    private String firma; //Firma cryptográfica.
    private String emisor;
    private String idPago; //TODO: Creo que va a ser necesario para el sistema de 2 fases

    public Ticket() {
    }

    public Ticket(String hashAnterior, String receptor, double amount, long timestamp, long nonce, String emisor) {
        this.hashAnterior=hashAnterior;
        this.receptor = receptor;
        this.amount = amount;
        this.timestamp= timestamp;
        this.nonce = nonce;
        this.emisor = emisor;
    }

    public String getHashAnterior() {
        return hashAnterior;
    }

    public void setHashAnterior(String hashAnterior) {
        this.hashAnterior = hashAnterior;
    }

    public String getHashNuevo() {
        return hashNuevo;
    }

    public void setHashNuevo(String hashNuevo) {
        this.hashNuevo = hashNuevo;
    }

    public String getReceptor() {
        return receptor;
    }

    public void setReceptor(String receptor) {
        this.receptor = receptor;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }
    public String getFirma() {
        return firma;
    }

    public void setFirma(String firma) {
        this.firma = firma;
    }

    public String getEmisor() {
        return emisor;
    }

    public void setEmisor(String emisor) {
        this.emisor = emisor;
    }

    public String getIdPago() {
        return idPago;
    }

    public void setIdPago(String idPago) {
        this.idPago = idPago;
    }

    @NotNull
    @Override
    public String toString() {
        return "Ticket{" +
                "hashAnterior='" + hashAnterior + '\'' +
                ", hashNuevo='" + hashNuevo + '\'' +
                ", receptor='" + receptor + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                ", nonce=" + nonce +
                ", firma='" + firma + '\'' +
                ", emisor='" + emisor + '\'' +
                ", idPago='" + idPago + '\'' +
                '}';
    }


}
