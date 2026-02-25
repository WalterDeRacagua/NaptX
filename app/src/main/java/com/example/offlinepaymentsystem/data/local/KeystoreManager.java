package com.example.offlinepaymentsystem.data.local;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentActivity;

import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;



public class KeystoreManager {
    private static final String TAG = "KeyStoreManager";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";

    private KeyStore keyStore;
    private Context context;

    public KeystoreManager(Context context){

        this.context = context;
        inicializarKeyStore();
    }

    private void inicializarKeyStore(){
        try{
            this.keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            this.keyStore.load(null);

            Log.d(TAG, "Android KeyStore inicializado correctamente");
        }catch (Exception e){
            Log.d(TAG, "Error al inicializar el keystore", e);
        }
    }

    public void generarClaves(String alias) throws Exception{

        if (claveExiste(alias)){
            Log.d(TAG, "Ya existe una clave con el alias " + alias);
            return;
        }

        //Usa el algoritmo de curva eliptica (eliptic curve (EC)).
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER);

        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN
                | KeyProperties.PURPOSE_VERIFY).setDigests(KeyProperties.DIGEST_SHA256).setUserAuthenticationRequired(
                        true).setUserAuthenticationValidityDurationSeconds(-1).build();

        keyPairGenerator.initialize(spec);

        KeyPair keyPair= keyPairGenerator.generateKeyPair();
        Log.d(TAG, "Par de claves generado exitosamente con alias: " + alias);
    }

    public boolean claveExiste(String alias) {
        try {
            return keyStore.containsAlias(alias);
        }catch (Exception e){
            Log.e(TAG, "Error al verificar al existencia de la clave", e);
            return false;
        }
    }

    public String obtenerAddress(String alias) throws Exception{

        if (!this.claveExiste(alias)){
            throw new Exception("No existe clave con alias " + alias);
        }

        //Obtener clave pública
        PublicKey publicKey = keyStore.getCertificate(alias).getPublicKey();
        java.security.interfaces.ECPublicKey ecPublicKey =
                (java.security.interfaces.ECPublicKey) publicKey;

        BigInteger x = ecPublicKey.getW().getAffineX();
        BigInteger y = ecPublicKey.getW().getAffineY();

        //Crear ECKeyPair de Web3j
        ECKeyPair ecKeyPair = new ECKeyPair(BigInteger.ZERO, new BigInteger(1, concatenarCoordenadas(x,y)));

        //Calcular address de Ethereum
        String address = "0x" + Keys.getAddress(ecKeyPair);

        Log.d(TAG, "Address calculada: " + address);

        return address;
    }

    private byte[] concatenarCoordenadas(BigInteger x, BigInteger y) {
        byte[] xBytes = x.toByteArray();
        byte[] yBytes = y.toByteArray();

        // Asegurar que cada coordenada tiene 32 bytes
        byte[] xPadded = new byte[32];
        byte[] yPadded = new byte[32];

        System.arraycopy(xBytes,
                Math.max(0, xBytes.length - 32),
                xPadded,
                Math.max(0, 32 - xBytes.length),
                Math.min(32, xBytes.length));

        System.arraycopy(yBytes,
                Math.max(0, yBytes.length - 32),
                yPadded,
                Math.max(0, 32 - yBytes.length),
                Math.min(32, yBytes.length));

        // Concatenar X + Y (64 bytes total)
        byte[] result = new byte[64];
        System.arraycopy(xPadded, 0, result, 0, 32);
        System.arraycopy(yPadded, 0, result, 32, 32);

        return result;
    }

    public void firmarMensaje(String alias, byte[] mensaje, FirmaCallback callback) {
        if (!claveExiste(alias)) {
            callback.onError("No existe clave con alias: " + alias);
            return;
        }

        try {
            // Obtener clave privada
            KeyStore.PrivateKeyEntry privateKeyEntry =
                    (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null);
            PrivateKey privateKey = privateKeyEntry.getPrivateKey();

            // Crear Signature
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);

            // Crear CryptoObject vinculado a la firma
            BiometricPrompt.CryptoObject cryptoObject =
                    new BiometricPrompt.CryptoObject(signature);

            // Configurar BiometricPrompt
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Autenticación requerida")
                    .setSubtitle("Confirma tu identidad para firmar")
                    .setNegativeButtonText("Cancelar")
                    .build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(
                    (FragmentActivity) context,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);

                            try {
                                // Obtener la Signature autenticada
                                Signature authenticatedSignature = result.getCryptoObject().getSignature();

                                // Firmar con la Signature autenticada
                                authenticatedSignature.update(mensaje);
                                byte[] firmaCruda = authenticatedSignature.sign();

                                byte[] firmaEthereum = convertirFirmaEthereum(firmaCruda);

                                Log.d(TAG, "Mensaje firmado exitosamente");
                                callback.onFirmaExitosa(firmaEthereum);

                            } catch (Exception e) {
                                Log.e(TAG, "Error al firmar", e);
                                callback.onError("Error al firmar: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            callback.onError("Error de autenticación: " + errString);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            callback.onError("Autenticación fallida");
                        }
                    }
            );

            // Mostrar el diálogo CON el CryptoObject
            biometricPrompt.authenticate(promptInfo, cryptoObject);

        } catch (Exception e) {
            Log.e(TAG, "Error al iniciar firma", e);
            callback.onError("Error al iniciar firma: " + e.getMessage());
        }
    }



    /**
     * Es necesario porque el formato DER de firma de Android es distinto al de Ethereum
     * Formato Ethereum tiene 65 bytes: [r:32] [s:32] [v:1]
     * r y s: Valores matemáticos de la firma (32 bytes cada uno)
     * v: Recovery ID (0, 1, 27 o 28) - Ayuda a recuperar la clave pública
     * */
    private byte[] convertirFirmaEthereum(byte[] firmaDER) throws Exception {
        // La firma DER de Android necesita convertirse al formato de Ethereum
        // Ethereum espera: [r (32 bytes)][s (32 bytes)][v (1 byte)] = 65 bytes

        // Parsear firma DER
        // Formato DER: 0x30 [longitud total] 0x02 [longitud r] [r] 0x02 [longitud s] [s]
        int offset = 3;
        int rLength = firmaDER[offset];
        offset++;

        byte[] r = new byte[32];
        System.arraycopy(firmaDER, offset + Math.max(0, rLength- 32), r, Math.max(0,32 - rLength), Math.min(32, rLength));
        offset +=rLength;

        offset++;
        int sLength =firmaDER[offset];
        offset++;

        byte[] s = new byte[32];
        System.arraycopy(firmaDER, offset + Math.max(0, sLength- 32), s, Math.max(0,32 - sLength), Math.min(32, sLength));

        byte v = 27;

        byte[] firmaEthereum = new byte[65];
        System.arraycopy(r, 0, firmaEthereum, 0, 32);
        System.arraycopy(s, 0, firmaEthereum, 32, 32);
        firmaEthereum[64]= v;

        return firmaEthereum;
    }

    public void eliminarClaves(String alias) throws Exception{
        if (!claveExiste(alias)){
            Log.w(TAG, "No existe clave con alias: " +alias);
            return;
        }

        keyStore.deleteEntry(alias);

        Log.d(TAG, "Clave eliminada correctamente: " +alias);
    }

}

