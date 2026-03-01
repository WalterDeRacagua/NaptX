package com.example.offlinepaymentsystem.data.local;

public interface FirmarMensajeCallback {
    void onMensajeFirmado(byte[] firma);
    void onError(String mensaje);
}
