package com.example.offlinepaymentsystem.utils;

public class Constants {

    // ========================================
    // BLOCKCHAIN - SEPOLIA TESTNET
    // ========================================
    /*Enlace a la testnet de sepolia, utilizo Infura*/
    public static final String SEPOLIA_RPC_URL = "https://sepolia.infura.io/v3/d7a0c48e7b534870a25179979decddc2";

    /*Chain ID de Sepolia*/
    public static  final long SEPOLIA_CHAIN_ID = 11155111L;

    /*Gas limit por defecto que utilizaremos en nuestras transacciones */
    public static  final long GAS_LIMIT = 300000L;

    public static final long GAS_PRICE = 20000000000L;  // 20 Gwei

    /*Timeout para las conexiones http (ms)*/
    public static final int HTTP_TIMEOUT =30000;

    public static final String CONTRACT_ADDRESS="";

    // ========================================
    // ALMACENAMIENTO LOCAL
    // ========================================

    /*Nombre del archivo de preferencias*/
    public static final String PREFERENCES_NAME = "OfflinePaymentPrefs";

    /* Nombre de la base de datos de Room (si la usamos)*/
    public static final String DATABASE_NAME = "offline_payment_db";

    /* Versión de la base de datos */
    public static final int DATABASE_VERSION = 1;

    // ========================================
    // KEYS PARA SHAREDPREFERENCES
    // ========================================

    // Datos del Emisor (Pagador)
    public static final String PREF_EMISOR_ADDRESS = "emisor_address";
    public static final String PREF_EMISOR_HASH_ACTUAL = "emisor_hash_actual";
    public static final String PREF_EMISOR_REGISTRADO = "emisor_registrado";
    public static final String PREF_EMISOR_WHITELIST = "emisor_whitelist";
    public static final String PREF_EMISOR_TIMESTAMP = "emisor_timestamp";

    // Datos del Receptor
    public static final String PREF_RECEPTOR_ADDRESS = "receptor_address";
    public static final String PREF_RECEPTOR_NOMBRE = "receptor_nombre";
    public static final String PREF_RECEPTOR_REGISTRADO = "receptor_registrado";
    public static final String PREF_RECEPTOR_PAGOS_RECIBIDOS = "receptor_pagos_recibidos";
    public static final String PREF_RECEPTOR_TOTAL_RECIBIDO = "receptor_total_recibido";

    // General
    public static final String PREF_IS_FIRST_RUN = "is_first_run";
    public static final String PREF_SELECTED_MODE = "selected_mode";  // "PAGADOR" o "RECEPTOR"
    public static final String PREF_SELECTED_WALLET = "selected_wallet";
    /*Lista de todas las wallets*/
    public static final String PREF_WALLET_LIST = "wallet_list";

    // ========================================
    // SISTEMA DE PAGOS OFFLINE
    // ========================================

    public static final long PAGO_TIMEOUT_MS= 2 * 60* 1000;
    public static final long TICKET_VALIDITY_MS= 3 * 60* 1000;
    public static final int ETH_DECIMALS = 18;
    public static final String WEI_PER_ETH = "1000000000000000000";

    // ========================================
    // QR CODES
    // ========================================
    public static final int QR_CODE_SIZE = 512;
    public static final String QR_PAYMENT_PREFIX = "NAPTX_PAY:";
    public static final String QR_RESPONSE_PREFIX = "NAPTX_RES:";
    public static final String QR_CONFIRM_PREFIX = "NAPTX_CONF:";

    // ========================================
    // ANDROID KEYSTORE
    // ========================================
    /** Alias de la clave del emisor en Keystore */
    public static final String KEYSTORE_ALIAS_EMISOR = "naptx_emisor_key";

    /** Alias de la clave del receptor en Keystore */
    public static final String KEYSTORE_ALIAS_RECEPTOR = "naptx_receptor_key";

    /** Provider del Keystore */
    public static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    // ========================================
    // LÍMITES Y VALIDACIONES
    // ========================================

    /** Monto mínimo de pago en ETH */
    public static final double MIN_PAYMENT_AMOUNT = 0.0001;

    /** Monto máximo de pago en ETH */
    public static final double MAX_PAYMENT_AMOUNT = 1000.0;

    /** Límite máximo de whitelist en ETH */
    public static final double MAX_WHITELIST_LIMIT = 10000.0;

    /** Número máximo de receptores en whitelist */
    public static final int MAX_WHITELIST_ITEMS = 50;

    // ========================================
    // CÓDIGOS DE REQUEST (para Activities)
    // ========================================

    /** Request code para escanear QR */
    public static final int REQUEST_CODE_SCAN_QR = 1001;

    /** Request code para permisos de cámara */
    public static final int REQUEST_CODE_CAMERA_PERMISSION = 1002;

    /** Request code para biometría */
    public static final int REQUEST_CODE_BIOMETRIC = 1003;

    // ========================================
    // ESTADOS Y MODOS
    // ========================================

    /** Modo Pagador */
    public static final String MODE_PAGADOR = "PAGADOR";

    /** Modo Receptor */
    public static final String MODE_RECEPTOR = "RECEPTOR";

    // ========================================
    // LOGGING Y DEBUG
    // ========================================

    /** Tag para logs */
    public static final String LOG_TAG = "NaptX";

    /** ¿Habilitar logs de debug? */
    public static final boolean DEBUG_MODE = true;

    // ========================================
    // URLS Y RECURSOS EXTERNOS
    // ========================================

    /** Etherscan Sepolia (para ver transacciones) */
    public static final String ETHERSCAN_SEPOLIA_URL = "https://sepolia.etherscan.io/tx/";

    /** Faucet de Sepolia (para obtener ETH de prueba) */
    public static final String SEPOLIA_FAUCET_URL = "https://sepoliafaucet.com/";

    // ========================================
    // CONSTRUCTOR PRIVADO
    // (Para evitar que se instancie esta clase)
    // ========================================

    private Constants() {
        throw new AssertionError("No se puede instanciar esta clase");
    }

}
