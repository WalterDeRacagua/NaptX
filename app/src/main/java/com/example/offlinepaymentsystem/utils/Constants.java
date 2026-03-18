package com.example.offlinepaymentsystem.utils;

public class Constants {

    // ========================================
    // BLOCKCHAIN - SEPOLIA TESTNET
    // ========================================
    /*Enlace a la testnet de sepolia, utilizo Infura*/
    public static final String SEPOLIA_RPC_URL = "https://sepolia.infura.io/v3/d7a0c48e7b534870a25179979decddc2";

    /*Timeout para las conexiones http (ms)*/
    public static final int HTTP_TIMEOUT =30000;

    public static final String CONTRACT_ADDRESS="0xC9861bdFBc22B829172Adf824bF8D708d4e8Aa00";

    // ========================================
    // ALMACENAMIENTO LOCAL
    // ========================================
    public static final String PREFS_NAME = "WalletPrefs";
    public static final String KEY_WALLET_ADDRESS="WALLET_ADDRESS";
    /** Nombre de la base de datos de Room */
    public static final String DATABASE_NAME = "offline_payment_db";

    /** Versión de la base de datos */
    public static final int DATABASE_VERSION = 2;
    private Constants() {
        throw new AssertionError("No se puede instanciar esta clase");
    }

}
