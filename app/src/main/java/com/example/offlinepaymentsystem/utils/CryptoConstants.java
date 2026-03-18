package com.example.offlinepaymentsystem.utils;

public class CryptoConstants {
    // Blockchain
    public static final long SEPOLIA_CHAIN_ID = 11155111L;

    // Gas Limits
    public static final long GAS_LIMIT_REGISTER = 300000L;
    public static final long GAS_LIMIT_APPROVE = 100000L;
    public static final long GAS_LIMIT_WHITELIST = 500000L;
    public static final long GAS_LIMIT_PREPARAR_PAGO = 500000L;
    public static final long GAS_LIMIT_CONFIRMAR_PAGO = 500000L;

    // Receipt Wait Configuration
    public static final int MAX_INTENTOS_RECEIPT = 60;  // 60 intentos
    public static final int DELAY_ENTRE_INTENTOS_MS = 2000;  // 2 segundos
    public static final int TIMEOUT_REGISTRO_SEGUNDOS = 60;  // 60 segundos

    // Conversión Wei/ETH
    public static final long WEI_PER_ETH = 1000000000000000000L;

    // Event Signatures (para keccak256)
    public static final String EVENT_EMISOR_REGISTRADO = "EmisorRegistrado(address,bytes32,bytes32,uint256)";
    public static final String EVENT_PAGO_PREPARADO = "PagoPreparado(bytes32,address,address,uint256,bytes32,uint256)";
    public static final String EVENT_PAGO_CONFIRMADO = "PagoConfirmado(bytes32,address,address,uint256,bytes32,uint256)";

    // Private constructor para prevenir instanciación
    private CryptoConstants() {
        throw new AssertionError("No se puede instanciar esta clase");
    }
}
