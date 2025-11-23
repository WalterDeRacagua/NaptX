package com.example.offlinepaymentsystem.utils;

public class Constants {
    /*Enlace a la testnet de sepolia, utilizo Infuar*/
    public static final String SEPOLIA_RPC_URL = "https://sepolia.infura.io/v3/d7a0c48e7b534870a25179979decddc2";

    /*Chain ID de Sepolia*/
    public static  final long SEPOLIA_CHAIN_ID = 11155111L;

    /*Gas limit por defecto que utilizaremos en nuestras transacciones */
    public static  final long GAS_LIMIT = 21000L;

    /*Timeout para las conexiones http en milisegundos*/
    public static final int HTTP_TIMEOUT =30000;

    /*Dirección del contrato FUTURO*/
    public static final String CONTRACT_ADDRESS="";

    /*Nombre del archivo de preferencias*/
    public static final String PREFERENCES_NAME = "OfflinePaymentPrefs";

    /* Nombre de la base de datos de SQL LITE que Room crea en nuestro dispositivo android*/
    public static final String DATABASE_NAME = "offline_payment_db";

    /* Versión de la base de datos */
    public static final int DATABASE_VERSION = 1;

    /* KEYS PARA PREFERENCIAS */
    /*ALMACENAMIENTO INTERNO DE ANDROID*/

    /*Wallet seleccionada por defecto*/
    public static final String PREF_SELECTED_WALLET = "selected_wallet";
    /*Lista de todas las wallets*/
    public static final String PREF_WALLET_LIST = "wallet_list";
    /*Detectar si es la primera vez que el usuario utiliza la app*/
    public static final String PREF_IS_FIRST_RUN = "is_first_run";

}
