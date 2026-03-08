package com.example.offlinepaymentsystem.data.local;

import android.content.Context;
import android.os.Build;
import androidx.biometric.BiometricPrompt;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;


import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class WalletManager {
    private static final String TAG = "WalletManager";
    private static final String KEYSTORE_PROVIDER= "AndroidKeyStore";
    //Nombre de la clave AES en el keystore
    private static final String AES_KEY_ALIAS= "naptx_wallet_cipher_key";
    //Nombre del fichero donde guardamos la clave de Ethereum cifrada
    private static  final String WALLET_FILE = "wallet_encrypted.dat";
    //Inicialización del vector IV para el cifrado, se genera aleatoriamente al cifrar y se necesita descifrar
    private static  final String IV_FILE = "wallet_iv.dat";
    //Longitud del IV
    private static final int GCM_IV_LENGTH =12;
    //TAG de autenticación que verifica que el cifrado no fue manipulado
    private static final int GCM_TAG_LENGTH =128;

    private Context context;
    private KeyStore keyStore;

    public WalletManager(Context context){
        this.context = context;
        try {
            Security.removeProvider("BC");
            Security.insertProviderAt(new org.bouncycastle.jce.provider.BouncyCastleProvider(), 1);
            Log.d(TAG, "BouncyCastle provider registrado");
        } catch (Exception e) {
            Log.e(TAG, "Error al registrar BouncyCastle", e);
        }
        this.inicializarKeyStore();
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

    /***
     * Méthod para verificar si una wallet ya existe
     */
    public boolean existeWallet(){
        File walletFile = new File(this.context.getFilesDir(), WALLET_FILE);
        boolean fileExists = walletFile.exists();

        try{
            boolean keyExists = keyStore.containsAlias(AES_KEY_ALIAS);
            return fileExists && keyExists;
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar si existe o no la Wallet", e);
            return false;
        }
    }

    /***
     * Méthod que utilizamos para generar una clave AES en el Keystore protegida con biometría
     */

    public void generarClaveAES() throws Exception {
        //Verificamos si ya existe
        if (this.keyStore.containsAlias(AES_KEY_ALIAS)){
            Log.e(TAG, "Error, la clave AES ya existe");
            return;
        }

        //Usamos el algoritmo AES y el proveedor es el android keystore.
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER);

        //Especifica cómo debe ser la clave. Modo de cifrado GCM (modo moderno de cifrado) que no require padding y requiere autenticación cada vez
        KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                AES_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(true).setUserAuthenticationValidityDurationSeconds(-1).build();

        //Crea la clave y la guarda en el KeyStore.
        keyGenerator.init(keySpec);
        keyGenerator.generateKey();
        Log.d(TAG, "Clave AES generada en Keystore con alias: " + AES_KEY_ALIAS);
    }

    /**
     * Crea una nueva wallet:
     * 1. Genera clave AES en Keystore
     * 2. Genera clave privada Ethereum
     * 3. Cifra la clave privada con biometría
     * 4. Guarda en fichero
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void crearWallet(CrearWalletCallback callback){
        try {
            if (this.existeWallet()){
                callback.onError("Ya existe la wallet");
                return;
            }

            //Genera la clave AES en el KeyStore
            generarClaveAES();

            //Generamos la clave privada de Ethereum
            ECKeyPair keyPair = Keys.createEcKeyPair();
            String privateKeyHex = keyPair.getPrivateKey().toString(16);
            String address = "0x" + Keys.getAddress(keyPair);

            Log.d(TAG, "Clave Ethereum ha sido generada con el address: " + address);

            // 4. Cifrar la clave privada con biometría
            cifrarYGuardarClave(privateKeyHex, address, callback);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Cifra la clave privada usando biometría y la guarda en fichero
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void cifrarYGuardarClave(String privateKey, String address, CrearWalletCallback callback) {
        try {
            // 1. Obtener clave AES del Keystore
            SecretKey secretKey = (SecretKey) keyStore.getKey(AES_KEY_ALIAS, null);

            // 2. Configurar Cipher para cifrar
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // 3. Crear CryptoObject para vincular la biometría con el cifrado.
            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

            // 4. Configurar BiometricPrompt, el dialogo que se muestra de cifrado
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Crear Wallet")
                    .setSubtitle("Confirma tu identidad para crear la wallet")
                    .setNegativeButtonText("Cancelar")
                    .build();

            // 5. Crear BiometricPrompt con callbacks
            BiometricPrompt biometricPrompt = new BiometricPrompt(
                    (FragmentActivity) context,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);


                            try {
                                // Obtener cipher autenticado
                                Cipher authenticatedCipher = result.getCryptoObject().getCipher();

                                // Cifrar la clave privada
                                byte[] encryptedKey = authenticatedCipher.doFinal(privateKey.getBytes());

                                // Obtener IV (Initialization Vector)
                                byte[] iv = authenticatedCipher.getIV();

                                // Guardar clave cifrada y IV en ficheros
                                guardarEnFichero(WALLET_FILE, encryptedKey);
                                guardarEnFichero(IV_FILE, iv);

                                Log.d(TAG, "Wallet creada y guardada exitosamente");
                                callback.onWalletCreada(address);

                            } catch (Exception e) {
                                Log.e(TAG, "Error al cifrar clave", e);
                                callback.onError("Error al cifrar: " + e.getMessage());
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

            // 6. Mostrar diálogo de biometría
            biometricPrompt.authenticate(promptInfo, cryptoObject);

        } catch (Exception e) {
            Log.e(TAG, "Error al preparar cifrado", e);
            callback.onError("Error al preparar cifrado: " + e.getMessage());
        }
    }

    /**
     * Guarda bytes en un fichero interno
     */
    private void guardarEnFichero(String nombreFichero, byte[] datos) throws Exception {
        File file = new File(context.getFilesDir(), nombreFichero);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(datos);
        fos.close();
        Log.d(TAG, "Guardado en fichero: " + nombreFichero + " (" + datos.length + " bytes)");
    }

    /**
     * Obtiene la address de la wallet sin necesidad de descifrar
     * La calculamos de nuevo desde la clave pública guardada
     * O simplemente la guardamos por separado al crear
     */
    public String obtenerAddress() {
        // Por ahora retornamos null
        // Lo implementaremos cuando guardemos la address por separado
        return null;
    }

    /**
     * Descifra la wallet y devuelve Credentials para firmar transacciones
     * Requiere biometría
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void obtenerCredentials(ObtenerCredentialsCallback callback) {

        Log.d(TAG, ">>> [obtenerCredentials] MÉTODO INICIADO");
        try {
            // 1. Verificar que existe wallet
            if (!existeWallet()) {
                Log.e(TAG, ">>> [obtenerCredentials] No existe wallet");
                callback.onError("No existe wallet");
                return;
            }

            Log.d(TAG, ">>> [obtenerCredentials] Wallet existe");
            // 2. Leer ficheros: lee la clave cifrada y el IV que guardamos al crear.
            byte[] encryptedKey = leerDeFichero(WALLET_FILE);
            byte[] iv = leerDeFichero(IV_FILE);

            Log.d(TAG, ">>> [obtenerCredentials] Ficheros leídos");
            // 3. Obtener clave AES del Keystore
            SecretKey secretKey = (SecretKey) keyStore.getKey(AES_KEY_ALIAS, null);

            Log.d(TAG, ">>> [obtenerCredentials] SecretKey obtenida");
            // 4. Configurar Cipher para descifrar
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            Log.d(TAG, ">>> [obtenerCredentials] Cipher configurado");

            // 5. Crear CryptoObject
            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

            Log.d(TAG, ">>> [obtenerCredentials] CryptoObject creado");
            // 6. Configurar BiometricPrompt
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Autenticación requerida")
                    .setSubtitle("Confirma tu identidad para usar la wallet")
                    .setNegativeButtonText("Cancelar")
                    .build();

            Log.d(TAG, ">>> [obtenerCredentials] PromptInfo creado");
            // 7. Crear BiometricPrompt con callbacks
            Log.d(TAG, ">>> [obtenerCredentials] A PUNTO de crear BiometricPrompt");
            BiometricPrompt biometricPrompt = new BiometricPrompt(
                    (FragmentActivity) context,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            Log.d(TAG, ">>> [obtenerCredentials] onAuthenticationSucceeded llamado");

                            try {
                                // Obtener cipher autenticado
                                Cipher authenticatedCipher = result.getCryptoObject().getCipher();
                                Log.d(TAG, ">>> [obtenerCredentials] Cipher obtenido");

                                // Descifrar la clave privada
                                byte[] decryptedKey = authenticatedCipher.doFinal(encryptedKey);
                                Log.d(TAG, ">>> [obtenerCredentials] Clave descifrada, tamaño: " + decryptedKey.length);

                                String privateKeyHex = new String(decryptedKey);

                                // Crear Credentials --> se usa para firmar transacciones en Web3j
                                Log.d(TAG, ">>> [obtenerCredentials] Creando Credentials...");
                                org.web3j.crypto.Credentials credentials =
                                        org.web3j.crypto.Credentials.create(privateKeyHex);

                                Log.d(TAG, ">>> [obtenerCredentials] Credentials creados");
                                Log.d(TAG, "Credentials obtenidos exitosamente");
                                Log.d(TAG, ">>> [obtenerCredentials] Llamando a callback.onCredentialsObtenidos()");
                                callback.onCredentialsObtenidos(credentials);

                                // Borrar clave de memoria RAM por seguridad
                                java.util.Arrays.fill(decryptedKey, (byte) 0);

                            } catch (Exception e) {
                                Log.e(TAG, ">>> [obtenerCredentials] EXCEPCIÓN", e);
                                Log.e(TAG, "Error al descifrar", e);
                                callback.onError("Error al descifrar: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            Log.e(TAG, ">>> [obtenerCredentials] onAuthenticationError llamado: " + errString);
                            callback.onError("Error de autenticación: " + errString);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            Log.e(TAG, ">>> [obtenerCredentials] onAuthenticationFailed llamado");
                            callback.onError("Autenticación fallida");
                        }
                    }
            );
            Log.d(TAG, ">>> [obtenerCredentials] BiometricPrompt creado");



            // 8. Mostrar diálogo de biometría
            Log.d(TAG, ">>> [obtenerCredentials] A PUNTO de llamar a authenticate()");
            biometricPrompt.authenticate(promptInfo, cryptoObject);
            Log.d(TAG, ">>> [obtenerCredentials] authenticate() LLAMADO");
        } catch (Exception e) {
            Log.e(TAG, ">>> [obtenerCredentials] EXCEPCIÓN CAPTURADA", e);
            Log.e(TAG, ">>> [obtenerCredentials] Mensaje: " + e.getMessage());
            Log.e(TAG, ">>> [obtenerCredentials] Clase: " + e.getClass().getName());
            Log.e(TAG, "Error al preparar descifrado", e);
            callback.onError("Error: " + e.getMessage());
        }
    }

    /**
     * Lee bytes desde un fichero interno
     */
    private byte[] leerDeFichero(String nombreFichero) throws Exception {
        File file = new File(context.getFilesDir(), nombreFichero);
        java.io.FileInputStream fis = new java.io.FileInputStream(file);
        byte[] datos = new byte[(int) file.length()];
        fis.read(datos);
        fis.close();
        Log.d(TAG, "Leído de fichero: " + nombreFichero + " (" + datos.length + " bytes)");
        return datos;
    }

    /**
     * Firma el mensaje de registro para el smart contract
     * Mensaje = keccak256(address, deviceId, timestamp, nonce)
     * Requiere biometría para descifrar la clave privada
     */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void firmarMensajeRegistro(
            String address,
            byte[] deviceId,
            long timestamp,
            long nonce,
            FirmarMensajeCallback callback
    ) {
        try {
            // 1. Verificar que existe wallet
            if (!existeWallet()) {
                callback.onError("No existe wallet");
                return;
            }

            // 2. Construir mensaje
            byte[] mensaje = construirMensajeRegistro(address, deviceId, timestamp, nonce);

            Log.d(TAG, "Mensaje a firmar (hex): " + Numeric.toHexString(mensaje));

            // 3. Leer ficheros cifrados
            byte[] encryptedKey = leerDeFichero(WALLET_FILE);
            byte[] iv = leerDeFichero(IV_FILE);

            // 4. Obtener clave AES del Keystore
            SecretKey secretKey = (SecretKey) keyStore.getKey(AES_KEY_ALIAS, null);

            // 5. Configurar Cipher para descifrar
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // 6. Crear CryptoObject
            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

            // 7. Configurar BiometricPrompt
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Firmar Registro")
                    .setSubtitle("Confirma tu identidad para firmar el registro")
                    .setNegativeButtonText("Cancelar")
                    .build();

            // 8. Crear BiometricPrompt con callbacks
            BiometricPrompt biometricPrompt = new BiometricPrompt(
                    (FragmentActivity) context,
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);

                            try {
                                // Descifrar clave privada
                                Cipher authenticatedCipher = result.getCryptoObject().getCipher();
                                byte[] decryptedKey = authenticatedCipher.doFinal(encryptedKey);
                                String privateKeyHex = new String(decryptedKey);

                                // Crear Credentials
                                org.web3j.crypto.Credentials credentials =
                                        org.web3j.crypto.Credentials.create(privateKeyHex);

                                // Firmar mensaje
                                org.web3j.crypto.Sign.SignatureData signatureData =
                                        org.web3j.crypto.Sign.signMessage(mensaje, credentials.getEcKeyPair(), false);

                                // Convertir a formato Ethereum (65 bytes)
                                byte[] firma = convertirFirmaEthereum(signatureData);

                                Log.d(TAG, "Mensaje firmado exitosamente");
                                Log.d(TAG, "Firma (hex): " + Numeric.toHexString(firma));

                                callback.onMensajeFirmado(firma);

                                // Borrar clave de memoria
                                java.util.Arrays.fill(decryptedKey, (byte) 0);

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

            // 9. Mostrar diálogo de biometría
            biometricPrompt.authenticate(promptInfo, cryptoObject);

        } catch (Exception e) {
            Log.e(TAG, "Error al preparar firma", e);
            callback.onError("Error: " + e.getMessage());
        }
    }

    /**
     * Construye el mensaje que debe firmar el emisor para registrarse
     * mensaje = keccak256(address, deviceId, timestamp, nonce)
     */
    private byte[] construirMensajeRegistro(String address, byte[] deviceId, long timestamp, long nonce) {
        try {
            // Convertir address a bytes (sin "0x")
            String addressSin0x = address.startsWith("0x") ? address.substring(2) : address;
            byte[] addressBytes = Numeric.hexStringToByteArray(addressSin0x);

            // Convertir timestamp a bytes32 (big-endian)
            byte[] timestampBytes = new byte[32];
            for (int i = 0; i < 8; i++) {
                timestampBytes[31 - i] = (byte) (timestamp >> (8 * i));
            }

            // Convertir nonce a bytes32 (big-endian)
            byte[] nonceBytes = new byte[32];
            for (int i = 0; i < 8; i++) {
                nonceBytes[31 - i] = (byte) (nonce >> (8 * i));
            }

            // Concatenar: address (20 bytes) + deviceId (32 bytes) + timestamp (32 bytes) + nonce (32 bytes)
            byte[] concatenado = new byte[addressBytes.length + deviceId.length + timestampBytes.length + nonceBytes.length];
            System.arraycopy(addressBytes, 0, concatenado, 0, addressBytes.length);
            System.arraycopy(deviceId, 0, concatenado, addressBytes.length, deviceId.length);
            System.arraycopy(timestampBytes, 0, concatenado, addressBytes.length + deviceId.length, timestampBytes.length);
            System.arraycopy(nonceBytes, 0, concatenado, addressBytes.length + deviceId.length + timestampBytes.length, nonceBytes.length);

            // Hash con Keccak256
            byte[] hash = Hash.sha3(concatenado);

            Log.d(TAG, "Mensaje construido - Address: " + address);
            Log.d(TAG, "Mensaje construido - DeviceId: " + Numeric.toHexString(deviceId));
            Log.d(TAG, "Mensaje construido - Timestamp: " + timestamp);
            Log.d(TAG, "Mensaje construido - Nonce: " + nonce);
            Log.d(TAG, "Mensaje construido - Hash: " + Numeric.toHexString(hash));

            return hash;

        } catch (Exception e) {
            Log.e(TAG, "Error al construir mensaje", e);
            throw new RuntimeException("Error al construir mensaje", e);
        }
    }

    /**
     * Convierte SignatureData de Web3j al formato Ethereum (65 bytes)
     * Formato: [r:32][s:32][v:1]
     */
    private byte[] convertirFirmaEthereum(org.web3j.crypto.Sign.SignatureData signatureData) {
        byte[] firma = new byte[65];

        // r (32 bytes)
        System.arraycopy(signatureData.getR(), 0, firma, 0, 32);

        // s (32 bytes)
        System.arraycopy(signatureData.getS(), 0, firma, 32, 32);

        // v (1 byte) - recovery id
        byte v = signatureData.getV()[0];
        if (v < 27) {
            v += 27;  // Convertir 0/1 a 27/28
        }
        firma[64] = v;

        return firma;
    }

    /**
     * Firma un pago para la fase de preapración
     * Mensaje: keccak256(hashUsado, amount, receptor, timestamp, nonce, deviceId)
     * Requiere biometría para descifrar la clave privada
     * */
    @RequiresApi(api = Build.VERSION_CODES.P)
    public void firmarPago( byte[] hashUsado, long amount, String receptor, long timestamp, long nonce, byte[] deviceId, FirmarMensajeCallback callback){
        try {

            //1. Verificamos que exista una wallet (lo que siempre verificamos)
            if (!existeWallet()){
                callback.onError("No existe wallet");
                return;
            }

            //2. Construimos el mensaje del pago
            byte[] mensaje = construirMensajePago(hashUsado, amount, receptor, timestamp, nonce, deviceId);

            //3. Leemos los ficheros encriptados para extraer clave privada
            byte[] encryptedKey = leerDeFichero(WALLET_FILE);
            byte[] iv = leerDeFichero(IV_FILE);

            //4. Obtenemos la clave AES del Keystore necesaria para desencriptar
            SecretKey secretKey = (SecretKey) keyStore.getKey(AES_KEY_ALIAS, null);

            //5. Configuramos el cipher para descifrar
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            //6. Crear cryptoObject
            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

            //7. Configuramos el biometrix prompt
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Firmar Pago").setSubtitle("Confirma tu identidad para firmar el pago")
                    .setNegativeButtonText("Cancelar").build();

            //8. Creamos el biometric prompt con callbacks

            BiometricPrompt biometricPrompt = new BiometricPrompt((FragmentActivity) context,
                    new BiometricPrompt.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result)
                        {
                            super.onAuthenticationSucceeded(result);

                            try {
                                Cipher authenticatedCipher = result.getCryptoObject().getCipher();
                                byte[] decriptedKey = authenticatedCipher.doFinal(encryptedKey);
                                String privateKeyHex = new String(decriptedKey);

                                Credentials credentials = Credentials.create(privateKeyHex);

                                Sign.SignatureData signatureData = Sign.signMessage(mensaje, credentials.getEcKeyPair(), false);

                                byte[] firma = convertirFirmaEthereum(signatureData);

                                callback.onMensajeFirmado(firma);

                                Arrays.fill(decriptedKey, (byte) 0);
                            } catch (Exception e) {
                                callback.onError("Error al firmar: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            callback.onError("Error de autenticación " + errString);
                        }

                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            callback.onError("Autenticación fallida");
                        }

                    });


            biometricPrompt.authenticate(promptInfo, cryptoObject);

        } catch (Exception e) {
            callback.onError("Error: " + e.getMessage());
        }
    }

    private byte[] construirMensajePago(byte[] hashUsado, long amount, String receptor, long timestamp, long nonce, byte[] deviceId ){
        //Transformo el address 0x.... a bytes sin el 0x
        byte[] receptorBytes = Numeric.hexStringToByteArray(receptor);

        //Convertimos amount a bytes32
        byte[] amountBytes = new byte[32];
        for (int i = 0; i < 8; i++) {
            amountBytes[31-i] = (byte) (amount >> (i*8));
        }

        // Convertimos timestamp a bytes32
        byte[] timestampBytes = new byte[32];
        for (int i = 0; i < 8; i++) {
            timestampBytes[31 - i] = (byte) (timestamp >> (i * 8));
        }

        // Convertimos nonce a bytes32
        byte[] nonceBytes = new byte[32];
        for (int i = 0; i < 8; i++) {
            nonceBytes[31 - i] = (byte) (nonce >> (i * 8));
        }

        byte[] mensaje = new byte[hashUsado.length + amountBytes.length + receptorBytes.length +
                timestampBytes.length + nonceBytes.length + deviceId.length];

        int offset = 0;
        System.arraycopy(hashUsado, 0, mensaje, offset, hashUsado.length);
        offset += hashUsado.length;

        System.arraycopy(amountBytes, 0, mensaje, offset, amountBytes.length);
        offset += amountBytes.length;

        System.arraycopy(receptorBytes, 0, mensaje, offset, receptorBytes.length);
        offset += receptorBytes.length;

        System.arraycopy(timestampBytes, 0, mensaje, offset, timestampBytes.length);
        offset += timestampBytes.length;

        System.arraycopy(nonceBytes, 0, mensaje, offset, nonceBytes.length);
        offset += nonceBytes.length;

        System.arraycopy(deviceId, 0, mensaje, offset, deviceId.length);

        return Hash.sha3(mensaje);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public void firmarConfiguracionWhitelist(
            String[] receptores,
            long[] limites,
            long timestamp,
            long nonce,
            FirmarMensajeCallback callback
    ){
        try {
            if (!existeWallet()){
                callback.onError("No existe wallet, por lo tanto no puedes firmar");
                return;
            }

            byte[] mensaje = construirMensajeWhitelist(receptores,limites,timestamp,nonce);

            // === DEBUG LOGS ===
            Log.d(TAG, "=== CONFIGURAR WHITELIST DEBUG ===");
            Log.d(TAG, "Número de receptores: " + receptores.length);
            for (int i = 0; i < receptores.length; i++) {
                Log.d(TAG, "Receptor[" + i + "]: " + receptores[i]);
                Log.d(TAG, "Límite[" + i + "]: " + limites[i] + " wei");
            }
            Log.d(TAG, "Timestamp: " + timestamp);
            Log.d(TAG, "Nonce: " + nonce);
            Log.d(TAG, "Mensaje construido (hex): " + Numeric.toHexString(mensaje));

            byte[] encryptedKey = leerDeFichero(WALLET_FILE);
            byte[] iv = leerDeFichero(IV_FILE);

            SecretKey secretKey = (SecretKey) keyStore.getKey(AES_KEY_ALIAS, null);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            BiometricPrompt.CryptoObject cryptoObject = new BiometricPrompt.CryptoObject(cipher);

            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Sincronizar Whitelist").setSubtitle("Confirma tu identidad para sincronizar con la Blockchain")
                    .setNegativeButtonText("Cancelar").build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(
                    (FragmentActivity) context,
                    new BiometricPrompt.AuthenticationCallback() {

                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            try {
                                Cipher authenticatedCipher = result.getCryptoObject().getCipher();
                                byte[] decryptedKey = authenticatedCipher.doFinal(encryptedKey);
                                String privateKeyHex = new String(decryptedKey);

                                Credentials credentials = Credentials.create(privateKeyHex);

                                Sign.SignatureData signatureData = Sign.signMessage(mensaje,credentials.getEcKeyPair(), false);

                                byte[] firma = convertirFirmaEthereum(signatureData);
                                Log.d(TAG, "Firma generada (hex): " + Numeric.toHexString(firma));
                                Log.d(TAG, "Address del firmante: " + credentials.getAddress());

                                callback.onMensajeFirmado(firma);

                                Arrays.fill(decryptedKey, (byte) 0);
                            } catch (Exception e) {
                                callback.onError("Error al firmar " + e.getMessage());
                            }

                        }

                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            callback.onError("Autenticación falllida " + errString);
                        }


                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            callback.onError("Error al firmar");
                        }
                    }
            );

            biometricPrompt.authenticate(promptInfo, cryptoObject);
        } catch (Exception e) {
            callback.onError("Error " + e.getMessage());
        }
    }

    private byte[] construirMensajeWhitelist(
            String[] receptores,
            long[] limites,
            long timestamp,
            long nonce
    ) {
        Log.d(TAG, ">>> construirMensajeWhitelist INICIO");
        Log.d(TAG, ">>> Parámetros recibidos:");
        Log.d(TAG, ">>> receptores.length = " + receptores.length);
        for (int i = 0; i < receptores.length; i++) {
            Log.d(TAG, ">>> receptores[" + i + "] = " + receptores[i]);
        }
        Log.d(TAG, ">>> limites.length = " + limites.length);
        for (int i = 0; i < limites.length; i++) {
            Log.d(TAG, ">>> limites[" + i + "] = " + limites[i]);
        }
        Log.d(TAG, ">>> timestamp = " + timestamp);
        Log.d(TAG, ">>> nonce = " + nonce);

        int totalReceptores = receptores.length * 32;
        int totalLimites = limites.length * 32;
        int totalSize = totalReceptores + totalLimites + 32 + 32;

        Log.d(TAG, ">>> totalSize calculado = " + totalSize);

        byte[] mensaje = new byte[totalSize];
        int offset = 0;

        // 1. Concatenar receptores (addresses)
        Log.d(TAG, ">>> Copiando receptores...");
        for (int i = 0; i < receptores.length; i++) {
            String receptor = receptores[i];
            Log.d(TAG, ">>> Procesando receptor[" + i + "] = " + receptor);

            byte[] receptorBytes = Numeric.hexStringToByteArray(receptor);
            Log.d(TAG, ">>> receptorBytes.length original = " + receptorBytes.length);

            // PADEAR A 32 BYTES (añadir 12 bytes de ceros al inicio)
            byte[] receptorPadded = new byte[32];
            System.arraycopy(receptorBytes, 0, receptorPadded, 12, 20);  // Copiar a partir del byte 12

            Log.d(TAG, ">>> receptorPadded (hex) = " + Numeric.toHexString(receptorPadded));

            System.arraycopy(receptorPadded, 0, mensaje, offset, 32);  // Copiar 32 bytes
            Log.d(TAG, ">>> Copiado a offset " + offset);
            offset += 32;  // Avanzar 32 bytes
        }

        // 2. Concatenar limites (uint256 cada uno)
        Log.d(TAG, ">>> Copiando límites...");
        for (int i = 0; i < limites.length; i++) {
            long limite = limites[i];
            Log.d(TAG, ">>> Procesando limite[" + i + "] = " + limite);

            byte[] limiteBytes = new byte[32];
            for (int j = 0; j < 8; j++) {
                limiteBytes[31 - j] = (byte) (limite >> (j * 8));
            }
            Log.d(TAG, ">>> limiteBytes (hex) = " + Numeric.toHexString(limiteBytes));

            System.arraycopy(limiteBytes, 0, mensaje, offset, 32);
            Log.d(TAG, ">>> Copiado a offset " + offset);
            offset += 32;
        }

        // 3. Concatenar timestamp
        Log.d(TAG, ">>> Copiando timestamp...");
        byte[] timestampBytes = new byte[32];
        for (int i = 0; i < 8; i++) {
            timestampBytes[31 - i] = (byte) (timestamp >> (i * 8));
        }
        Log.d(TAG, ">>> timestampBytes (hex) = " + Numeric.toHexString(timestampBytes));
        System.arraycopy(timestampBytes, 0, mensaje, offset, 32);
        offset += 32;

        // 4. Concatenar nonce
        Log.d(TAG, ">>> Copiando nonce...");
        byte[] nonceBytes = new byte[32];
        for (int i = 0; i < 8; i++) {
            nonceBytes[31 - i] = (byte) (nonce >> (i * 8));
        }
        Log.d(TAG, ">>> nonceBytes (hex) = " + Numeric.toHexString(nonceBytes));
        System.arraycopy(nonceBytes, 0, mensaje, offset, 32);

        Log.d(TAG, ">>> Mensaje COMPLETO antes del hash:");
        Log.d(TAG, ">>> " + Numeric.toHexString(mensaje));

        // 5. Hash Keccak256
        byte[] hash = Hash.sha3(mensaje);
        Log.d(TAG, ">>> Hash final: " + Numeric.toHexString(hash));

        return hash;
    }
}



