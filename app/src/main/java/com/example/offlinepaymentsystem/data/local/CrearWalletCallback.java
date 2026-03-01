package com.example.offlinepaymentsystem.data.local;

public interface CrearWalletCallback {
    void onWalletCreada(String address);
    void onError(String mensaje);
}