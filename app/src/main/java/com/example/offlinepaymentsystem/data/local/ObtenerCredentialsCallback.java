package com.example.offlinepaymentsystem.data.local;

public interface ObtenerCredentialsCallback {
    void onCredentialsObtenidos(org.web3j.crypto.Credentials credentials);
    void onError(String mensaje);
}