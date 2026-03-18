package com.example.offlinepaymentsystem.model;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;


public class Emisor {

    private String walletAddress;           // Address Ethereum
    private byte[] hashActual;              // Hash encadenado actual (bytes32)
    private byte[] deviceId;                // ID del dispositivo (bytes32)
    private long timestampUltimoPago;       // Timestamp del último pago
    private boolean registrado;             // ¿Está registrado en blockchain?

    /**
     * Whitelist: receptor → límite (en Wei como long)
     * Ejemplo:
     * 0xTiendaA: 30 ETH
     * 0xTiendaB: 70 ETH
     */
    private Map<String, Long> whitelist;

    public Emisor() {
        this.whitelist = new HashMap<>();
        this.timestampUltimoPago = 0;
        this.registrado = false;
    }

    public Emisor(String walletAddress, byte[] hashActual) {
        this.walletAddress = walletAddress;
        this.hashActual = hashActual;
        this.whitelist = new HashMap<>();
        this.timestampUltimoPago = 0;
        this.registrado = false;
    }

    // GETTERS Y SETTERS

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public byte[] getHashActual() {
        return hashActual;
    }

    public void setHashActual(byte[] hashActual) {
        this.hashActual = hashActual;
    }

    public byte[] getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(byte[] deviceId) {
        this.deviceId = deviceId;
    }

    public long getTimestampUltimoPago() {
        return timestampUltimoPago;
    }

    public void setTimestampUltimoPago(long timestampUltimoPago) {
        this.timestampUltimoPago = timestampUltimoPago;
    }

    public boolean isRegistrado() {
        return registrado;
    }

    public void setRegistrado(boolean registrado) {
        this.registrado = registrado;
    }

    public Map<String, Long> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(Map<String, Long> whitelist) {
        this.whitelist = whitelist;
    }

    // MÉTODOS AUXILIARES
    /*En hexadecimal es mucho más bonito para mostrárselo al usuario*/
    public String getHashActualHex() {
        if (hashActual == null) return "null";
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : hashActual) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public String getDeviceIdHex() {
        if (deviceId == null) return "null";
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : deviceId) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @NonNull
    @Override
    public String toString() {
        return "Emisor{" +
                "walletAddress='" + walletAddress + '\'' +
                ", hashActual=" + getHashActualHex() +
                ", deviceId=" + getDeviceIdHex() +
                ", timestampUltimoPago=" + timestampUltimoPago +
                ", registrado=" + registrado +
                ", whitelist=" + whitelist +
                '}';
    }
}
