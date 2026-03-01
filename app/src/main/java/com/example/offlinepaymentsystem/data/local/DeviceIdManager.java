package com.example.offlinepaymentsystem.data.local;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.nio.charset.StandardCharsets;

public class DeviceIdManager {

    private static final String TAG = "DeviceIdManager";

    /**
     * Obtiene el deviceId único del dispositivo
     * Usa Android ID (único por app + usuario + dispositivo)
     * Lo hashea con Keccak256 para obtener bytes32
     *
     * LIMITACIONES (documentadas en TFG):
     * - Cambia si se resetea de fábrica
     * - Cambia si se firma la app con diferente certificado
     * - Diferente para cada usuario en dispositivos multi-usuario
     * - No garantizado único en todos los casos
     */
    public static byte[] obtenerDeviceId(Context context) {
        try {
            // Obtener Android ID
            String androidId = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            Log.d(TAG, "Android ID: " + androidId);

            // Hashear con Keccak256 para obtener bytes32
            byte[] hash = Hash.sha3(androidId.getBytes(StandardCharsets.UTF_8));

            Log.d(TAG, "DeviceId (hex): " + Numeric.toHexString(hash));

            return hash;

        } catch (Exception e) {
            Log.e(TAG, "Error al obtener deviceId", e);
            return new byte[32]; // Devuelve bytes vacíos en caso de error
        }
    }

    /**
     * Convierte deviceId a String hexadecimal para mostrar
     */
    public static String obtenerDeviceIdHex(Context context) {
        byte[] deviceId = obtenerDeviceId(context);
        return Numeric.toHexString(deviceId);
    }
}