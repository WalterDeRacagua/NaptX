package com.example.offlinepaymentsystem.data.local;

public interface FirmaCallback {
    void onFirmaExitosa(byte[] firma);
    void onError(String mensaje);
}