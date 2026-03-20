package com.example.offlinepaymentsystem.utils;

import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.security.SecureRandom;

public class CryptoUtils {

    public static String convertirWeiAETH(long wei) {
        double eth = wei / (double) CryptoConstants.WEI_PER_ETH;
        return String.format("%.6f", eth);
    }

    public static long convertirETHaWei(String montoETH) {
        double eth = Double.parseDouble(montoETH);
        BigDecimal weiDecimal = new BigDecimal(eth)
                .multiply(new BigDecimal("1000000000000000000"));
        return weiDecimal.longValue();
    }

    public static long convertirETHaWei(double eth) {
        return (long) (eth * CryptoConstants.WEI_PER_ETH);
    }

    public static BigDecimal convertirWeiAETHBigDecimal(long wei) {
        return new BigDecimal(wei)
                .divide(new BigDecimal("1000000000000000000"));
    }

    public static String acortarHash(String hash, int length) {
        if (hash == null || hash.length() <= length) {
            return hash;
        }
        return hash.substring(0, length) + "...";
    }

    public static String acortarAddress(String address) {
        if (address == null || address.length() < 10) {
            return address;
        }
        return address.substring(0, 6) + "..." + address.substring(address.length() - 4);
    }

    public static String acortarAddressLargo(String address) {
        if (address == null || address.length() <= 20) {
            return address;
        }
        return address.substring(0, 10) + "..." + address.substring(address.length() - 8);
    }

    public static long generarNonce() {
        SecureRandom random = new SecureRandom();
        return Math.abs(random.nextLong());
    }

    /**
     * Convierte un long a bytes32 (big-endian) para Ethereum
     * @param value Valor long a convertir
     * @return Array de 32 bytes
     */
    public static byte[] longToBytes32(long value) {
        byte[] bytes = new byte[32];
        for (int i = 0; i < 8; i++) {
            bytes[31 - i] = (byte) (value >> (i * 8));
        }
        return bytes;
    }

    /**
     * Convierte un address Ethereum a bytes (20 bytes, sin padding)
     * @param address Address con o sin 0x
     * @return Array de 20 bytes
     */
    public static byte[] addressToBytes(String address) {
        String addressSin0x = address.startsWith("0x") ? address.substring(2) : address;
        return Numeric.hexStringToByteArray(addressSin0x);
    }

    /**
     * Convierte un address Ethereum a bytes32 (con padding de 12 ceros)
     * @param address Address con o sin 0x
     * @return Array de 32 bytes
     */
    public static byte[] addressToBytes32(String address) {
        byte[] addressBytes = addressToBytes(address);
        byte[] padded = new byte[32];
        System.arraycopy(addressBytes, 0, padded, 12, 20);
        return padded;
    }
    private CryptoUtils() {
        throw new AssertionError("No se puede instanciar esta clase");
    }
}