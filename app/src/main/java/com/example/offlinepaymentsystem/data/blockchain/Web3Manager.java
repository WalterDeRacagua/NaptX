package com.example.offlinepaymentsystem.data.blockchain;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.DynamicBytes;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import com.example.offlinepaymentsystem.model.Emisor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

import com.example.offlinepaymentsystem.utils.Constants;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class Web3Manager {

    private static final String TAG = "Web3Manager";
    private static  Web3Manager instance;
    private Web3j web3j;
    private Context context;
    private String contractABI;

    public Web3Manager(Context context) {
        this.context = context;
        this.initializeWeb3j();
        this.loadContractABI();
    }

    private void initializeWeb3j() {
        /**
         * connectTimeout: Tiempo máximo para conectar al servidor (30 segundos)
         * readTimeout: Tiempo máximo para leer la respuesta (30 segundos)
         * writeTimeout: Tiempo máximo para enviar datos (30 segundos)
         *
         * Nos va a permitir conectarnos a Sepolia.
         */
        OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(Constants.HTTP_TIMEOUT, TimeUnit.MILLISECONDS).readTimeout(Constants.HTTP_TIMEOUT, TimeUnit.MILLISECONDS).writeTimeout(Constants.HTTP_TIMEOUT,
                TimeUnit.MILLISECONDS).build();

        //El service nos permite conectar web3j con el cliente HTTP, le dice a qué URL enviar peticiones.
        this.web3j = Web3j.build(new HttpService(Constants.SEPOLIA_RPC_URL, httpClient));

        Log.d(TAG, "Web3j inicializado con Sepolia RPC");
    }

    private void loadContractABI(){
        try{
            InputStream is = context.getAssets().open("OfflinePaymentSystem.json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null){
                sb.append(line);
            }

            this.contractABI = sb.toString();

            reader.close();
            is.close();

            Log.d(TAG, "ABI del contrato cargado correctamente");
        } catch (IOException e) {
            Log.e(TAG, "Error al cargar la ABI del contrato", e);
            this.contractABI = null;
        }
    }

    public void shutdown(){
        if (web3j!= null){
            web3j.shutdown();
        }
    }

    //Verificar conexión a Sepolia.
    public boolean verificarConexion(){
        try{
            org.web3j.protocol.core.methods.response.EthBlockNumber blockNumber = this.web3j.ethBlockNumber().send();

            if (blockNumber.hasError()){
                Log.e(TAG, "Error al conectarnos con Sepolia: " + blockNumber.getError().getMessage());
                return false;
            }

            long numeroBloque = blockNumber.getBlockNumber().longValue();

            Log.d(TAG, "Conectado a Sepolia. Bloque actual: "+ numeroBloque);
            return true;
        }catch (Exception e){
            Log.e(TAG, "Error al verificar la cone: ", e);
            return false;
        }
    }


    /**
     * Obtener estado de un emisor desde el contrato
     * @param addressEmisor Address del emisor a consultar
     * @return Objeto Emisor con los datos, o null si hay error
     */
    public Emisor obtenerEstadoEmisor(String addressEmisor) {
        try {
            Function function = new Function(
                    "obtenerEstadoEmisor",
                    Arrays.asList(new Address(addressEmisor)),  // address
                    Arrays.asList(
                            new TypeReference<Bytes32>() {},      // hashActual
                            new TypeReference<Bool>() {},         // registrado
                            new TypeReference<Uint256>() {},      // timestampUltimoPago
                            new TypeReference<Bytes32>() {}       // deviceId
                    )
            );

            // 2. Encodear la función
            String encodedFunction = FunctionEncoder.encode(function);

            // 3. Crear transacción de lectura
            Transaction transaction = Transaction.createEthCallTransaction(
                    null,                           // from (no necesario para view)
                    Constants.CONTRACT_ADDRESS,     // to (nuestro contrato)
                    encodedFunction                 // data
            );

            // 4. Hacer la llamada
            EthCall response = web3j.ethCall(
                    transaction,
                    DefaultBlockParameterName.LATEST  // Usar el bloque más reciente
            ).send();

            // 5. Verificar errores
            if (response.hasError()) {
                Log.e(TAG, "Error en ethCall: " + response.getError().getMessage());
                return null;
            }

            // 6. Decodear la respuesta
            List<Type> results = FunctionReturnDecoder.decode(
                    response.getValue(),                //Respuesta en hexadecimal del SC
                    function.getOutputParameters()      //Tipos esperados de lo que devuelve
            );

            // 7. Extraer valores
            byte[] hashActual = (byte[]) results.get(0).getValue();
            boolean registrado = (boolean) results.get(1).getValue();
            BigInteger timestampUltimoPago = (BigInteger) results.get(2).getValue();
            byte[] deviceId = (byte[]) results.get(3).getValue();

            // 8. Crear objeto Emisor
            Emisor emisor = new Emisor();
            emisor.setWalletAddress(addressEmisor);
            emisor.setHashActual(hashActual);
            emisor.setRegistrado(registrado);
            emisor.setTimestampUltimoPago(timestampUltimoPago.longValue());
            emisor.setDeviceId(deviceId);

            Log.d(TAG, "Estado del emisor obtenido: registrado=" + registrado);

            return emisor;

        } catch (Exception e) {
            Log.e(TAG, "Error al obtener estado del emisor", e);
            return null;
        }
    }

    /**
     * Registra al emisor en el contrato
     * @return Array: [txHash, hashInicial]
     */
    public String[] registrarEmisor(
            Credentials credentials,
            byte[] deviceId,
            long timestamp,
            long nonce,
            byte[] firma
    ) {
        try {
            Log.d(TAG, "=== INICIANDO REGISTRO ===");
            Log.d(TAG, "Address: " + credentials.getAddress());
            Log.d(TAG, "DeviceId: " + Numeric.toHexString(deviceId));
            Log.d(TAG, "Timestamp: " + timestamp);
            Log.d(TAG, "Nonce: " + nonce);
            Log.d(TAG, "Firma: " + Numeric.toHexString(firma));

            // 1. Crear Function para registrar
            Function function = new Function(
                    "registrar",
                    Arrays.asList(
                            new Bytes32(deviceId),
                            new Uint256(timestamp),
                            new Uint256(nonce),
                            new DynamicBytes(firma)
                    ),
                    Collections.emptyList()
            );

            // 2. Encodear función
            String encodedFunction = FunctionEncoder.encode(function);
            Log.d(TAG, "Función encodeada: " + encodedFunction);

            // 3. Obtener nonce de la cuenta
            EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                    credentials.getAddress(),
                    DefaultBlockParameterName.LATEST
            ).send();

            BigInteger nonceTransaccion = ethGetTransactionCount.getTransactionCount();
            Log.d(TAG, "Nonce de transacción: " + nonceTransaccion);

            // 4. Obtener gas price
            EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
            BigInteger gasPrice = ethGasPrice.getGasPrice();
            Log.d(TAG, "Gas price: " + gasPrice);

            // 5. Estimar gas limit
            BigInteger gasLimit = BigInteger.valueOf(300000);

            // 6. Crear RawTransaction
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonceTransaccion,
                    gasPrice,
                    gasLimit,
                    Constants.CONTRACT_ADDRESS,
                    encodedFunction
            );

            // 7. Firmar transacción
            byte[] signedMessage = TransactionEncoder.signMessage(
                    rawTransaction,
                    11155111,  // Chain ID de Sepolia
                    credentials
            );
            String hexValue = Numeric.toHexString(signedMessage);
            Log.d(TAG, "Transacción firmada: " + hexValue);

            // 8. Enviar transacción
            EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

            if (ethSendTransaction.hasError()) {
                String error = ethSendTransaction.getError().getMessage();
                Log.e(TAG, "Error al enviar transacción: " + error);
                return null;
            }

            String transactionHash = ethSendTransaction.getTransactionHash();
            Log.d(TAG, "Transacción enviada - TX Hash: " + transactionHash);

            // 9. Esperar receipt y extraer hashInicial del evento
            Log.d(TAG, "Esperando receipt y extrayendo hashInicial...");
            String hashInicialHex = esperarYExtraerHashInicial(transactionHash);

            Log.d(TAG, "HashInicial obtenido: " + hashInicialHex);

            return new String[]{transactionHash, hashInicialHex};

        } catch (Exception e) {
            Log.e(TAG, "Error al registrar emisor", e);
            return null;
        }
    }

    /**
     * Espera el receipt de la transacción y extrae hashInicial del evento EmisorRegistrado
     */
    private String esperarYExtraerHashInicial(String txHash) throws Exception {

        Log.d(TAG, ">>> Esperando receipt de transacción: " + txHash);

        // Esperar hasta 60 segundos
        int intentos = 0;
        int maxIntentos = 30; // 30 intentos x 2 segundos = 60 segundos

        while (intentos < maxIntentos) {
            try {
                EthGetTransactionReceipt receiptResponse =
                        web3j.ethGetTransactionReceipt(txHash).send();

                if (receiptResponse.getTransactionReceipt().isPresent()) {

                    TransactionReceipt receipt = receiptResponse.getTransactionReceipt().get();

                    Log.d(TAG, "Receipt obtenido, buscando evento...");

                    // Extraer hashInicial del evento EmisorRegistrado
                    // event EmisorRegistrado(address indexed emisor, bytes32 deviceId, bytes32 hashInicial, uint256 timestamp)

                    for (org.web3j.protocol.core.methods.response.Log log : receipt.getLogs()) {

                        if (log.getTopics().isEmpty()) {
                            continue;
                        }

                        // El primer topic es el hash del evento
                        String eventSignature = log.getTopics().get(0);

                        // Hash del evento: keccak256("EmisorRegistrado(address,bytes32,bytes32,uint256)")
                        String expectedEventHash = Hash.sha3String(
                                "EmisorRegistrado(address,bytes32,bytes32,uint256)"
                        );

                        Log.d(TAG, "Event signature encontrado: " + eventSignature);
                        Log.d(TAG, "Event signature esperado: " + expectedEventHash);

                        if (eventSignature.equalsIgnoreCase(expectedEventHash)) {

                            Log.d(TAG, "Evento EmisorRegistrado encontrado!");

                            // Los datos no indexados están en log.getData()
                            // deviceId (bytes32), hashInicial (bytes32), timestamp (uint256)
                            String data = log.getData();

                            Log.d(TAG, "Datos del evento: " + data);

                            // Remover "0x"
                            if (data.startsWith("0x")) {
                                data = data.substring(2);
                            }

                            // Verificar que tenemos suficientes datos
                            if (data.length() < 192) { // 3 x 64 chars (3 x 32 bytes)
                                throw new Exception("Datos del evento incompletos: " + data.length() + " chars");
                            }

                            // deviceId = primeros 64 chars (32 bytes)
                            // hashInicial = siguientes 64 chars (32 bytes)
                            // timestamp = últimos 64 chars (32 bytes)

                            String hashInicialHex = "0x" + data.substring(64, 128);

                            Log.d(TAG, "HashInicial extraído del evento: " + hashInicialHex);

                            return hashInicialHex;
                        }
                    }

                    throw new Exception("No se encontró el evento EmisorRegistrado en el receipt");
                }

                Log.d(TAG, "Esperando confirmación... intento " + (intentos + 1) + "/" + maxIntentos);
                Thread.sleep(2000);
                intentos++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Interrupted while waiting for receipt");
            }
        }

        throw new Exception("Timeout: No se recibió el receipt en 60 segundos");
    }

    public String configurarWhitelist(
            Credentials credentials,
            String[] receptores,
            long[] limites,
            long timestamp,
            long nonce,
            byte [] firma
    ) throws Exception{
        // === DEBUG LOGS ===
        Log.d(TAG, "=== ENVIAR CONFIGURAR WHITELIST ===");
        Log.d(TAG, "Credentials address: " + credentials.getAddress());
        Log.d(TAG, "Número de receptores: " + receptores.length);
        for (int i = 0; i < receptores.length; i++) {
            Log.d(TAG, "Receptor[" + i + "]: " + receptores[i]);
            Log.d(TAG, "Límite[" + i + "]: " + limites[i]);
        }
        Log.d(TAG, "Timestamp: " + timestamp);
        Log.d(TAG, "Nonce: " + nonce);
        Log.d(TAG, "Firma (hex): " + Numeric.toHexString(firma));
        Log.d(TAG, "Firma length: " + firma.length + " bytes");
        List<org.web3j.abi.datatypes.Address> receptoresList = new ArrayList<>();
        for (String receptor: receptores) {
            receptoresList.add(new Address(receptor));
        }

        List<Uint256> limitesList = new ArrayList<>();
        for (long limite: limites) {
            limitesList.add(new Uint256(BigInteger.valueOf(limite)));
        }

        Function function = new Function(
                "configurarWhitelist",
                Arrays.asList(
                        new DynamicArray<>(Address.class, receptoresList),
                        new DynamicArray<>(Uint256.class, limitesList),
                        new Uint256(BigInteger.valueOf(timestamp)),
                        new Uint256(BigInteger.valueOf(nonce)),
                        new DynamicBytes(firma)
                ), Collections.emptyList());

        String encodedFunction = FunctionEncoder.encode(function);

        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).send();

        BigInteger txNonce = ethGetTransactionCount.getTransactionCount();

        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                txNonce,
                gasPrice,
                BigInteger.valueOf(500000), //Mayor limit para arrays
                Constants.CONTRACT_ADDRESS,
                encodedFunction
        );

        byte [] signedMessage = TransactionEncoder.signMessage(
                rawTransaction,
                11155111,
                credentials
        );

        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

        if (ethSendTransaction.hasError()) {
            throw new Exception("Error: " + ethSendTransaction.getError().getMessage());
        }

        return ethSendTransaction.getTransactionHash();
    }

    /**
     * Aprueba tokens para que el contrato pueda gastarlos
     */
    public String aprobarTokens(
            org.web3j.crypto.Credentials credentials,
            long amount
    ) throws Exception {

        Log.d(TAG, ">>> aprobarTokens() iniciado");
        Log.d(TAG, ">>> Amount a aprobar: " + amount);

        // Función approve(address spender, uint256 amount)
        Function function = new Function(
                "approve",
                Arrays.asList(
                        new Address(Constants.CONTRACT_ADDRESS),  // spender = el contrato
                        new Uint256(BigInteger.valueOf(amount))   // amount
                ),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);
        Log.d(TAG, "Función encodeada: " + encodedFunction);

        // Obtener nonce de transacción
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).send();
        BigInteger txNonce = ethGetTransactionCount.getTransactionCount();

        // Obtener gas price
        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();

        // Crear RawTransaction
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                txNonce,
                gasPrice,
                BigInteger.valueOf(100000),  // Gas limit para approve
                Constants.CONTRACT_ADDRESS,
                encodedFunction
        );

        // Firmar transacción
        byte[] signedMessage = TransactionEncoder.signMessage(
                rawTransaction,
                11155111,  // Chain ID de Sepolia
                credentials
        );
        String hexValue = Numeric.toHexString(signedMessage);

        // Enviar transacción
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

        if (ethSendTransaction.hasError()) {
            throw new Exception("Error: " + ethSendTransaction.getError().getMessage());
        }

        String transactionHash = ethSendTransaction.getTransactionHash();
        Log.d(TAG, "Approve Transaction Hash: " + transactionHash);

        return transactionHash;
    }

    public EthGetTransactionReceipt obtenerReceipt(String txHash) throws Exception {
        return web3j.ethGetTransactionReceipt(txHash).send();
    }

    /**
     * Prepara un pago en la Blockchain (llamado por el RECEPTOR)
     * */
    public String[] prepararPago(Credentials credentials, byte[] hashUsado, long amount, String receptor, long timestamp, long nonce, byte[] deviceId, byte[]firma) throws Exception {
        Log.d(TAG, "=== PREPARAR PAGO ===");
        Log.d(TAG, "HashUsado: " + Numeric.toHexString(hashUsado));
        Log.d(TAG, "Amount: " + amount);
        Log.d(TAG, "Receptor: " + receptor);
        Log.d(TAG, "Timestamp: " + timestamp);
        Log.d(TAG, "Nonce: " + nonce);
        Log.d(TAG, "DeviceId: " + Numeric.toHexString(deviceId));
        Log.d(TAG, "Firma: " + Numeric.toHexString(firma));

        //Preparamos la función que se va a lanzar a la blockchain
        Function function = new Function(
                "prepararPago",
                Arrays.asList(
                        new Bytes32(hashUsado),
                        new Uint256(amount),
                        new Address(receptor),
                        new Uint256(timestamp),
                        new Uint256(nonce),
                        new Bytes32(deviceId),
                        new DynamicBytes(firma)
                ), Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthGetTransactionCount ethGetTransactionCount = this.web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).send();

        BigInteger txNonce = ethGetTransactionCount.getTransactionCount();

        EthGasPrice ethGasPrice = this.web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                txNonce,
                gasPrice,
                BigInteger.valueOf(500000),
                Constants.CONTRACT_ADDRESS,
                encodedFunction
        );
        byte[] signedMessage = TransactionEncoder.signMessage(
                rawTransaction,
                11155111,
                credentials
        );
        String hexValue = Numeric.toHexString(signedMessage);

        // Enviar transacción
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

        if (ethSendTransaction.hasError()) {
            throw new Exception("Error: " + ethSendTransaction.getError().getMessage());
        }

        String transactionHash = ethSendTransaction.getTransactionHash();
        Log.d(TAG, "Transacción enviada - TX Hash: " + transactionHash);

        // Esperar receipt y extraer pagoId + hashPreparado del evento
        Log.d(TAG, "Esperando receipt y extrayendo datos del evento...");
        String[] resultado = esperarYExtraerPagoPreparado(transactionHash);

        Log.d(TAG, " PagoId: " + resultado[0]);
        Log.d(TAG, " HashPreparado: " + resultado[1]);

        return resultado;
    }


    private String[] esperarYExtraerPagoPreparado(String txHas) throws Exception {
        Log.d(TAG, ">>> Esperando la receipt de la transacción: " + txHas);

        int intentos =0;
        int maxIntentos= 60;

        while (intentos < maxIntentos){
            try{
                EthGetTransactionReceipt receiptResponse = this.web3j.ethGetTransactionReceipt(txHas).send();

                if (receiptResponse.getTransactionReceipt().isPresent()){
                    TransactionReceipt receipt = receiptResponse.getTransactionReceipt().get();

                    Log.d(TAG, "Receipt obtenido, buscando evento... ");

                    for (org.web3j.protocol.core.methods.response.Log log: receipt.getLogs()) {
                        if (log.getTopics().isEmpty()){
                            continue;
                        }

                        String eventSignature = log.getTopics().get(0);

                        String expectedEventHash = Hash.sha3String(
                                "PagoPreparado(bytes32,address,address,uint256,bytes32,uint256)"
                        );

                        Log.d(TAG, "Event signature encontrado: " + eventSignature);
                        Log.d(TAG, "Event signature esperadp: " + expectedEventHash);

                        if (eventSignature.equalsIgnoreCase(expectedEventHash)){
                            Log.d(TAG, "Evento preparado encontrado");

                            //Topics: [eventHash, pagoId, emisor, receptor];
                            String pagoId = log.getTopics().get(1);

                            // Data: amount (uint256), hashPreparado (bytes32), timestamp (uint256)
                            String data = log.getData();

                            if (data.startsWith("0x")) {
                                data = data.substring(2);
                            }

                            // amount = 0-64
                            // hashPreparado = 64-128
                            // timestamp = 128-192

                            if (data.length() < 192) {
                                throw new Exception("Datos del evento incompletos");
                            }

                            String hashPreparado = "0x" + data.substring(64, 128);

                            Log.d(TAG, "PagoId que pillamos: " + pagoId);
                            Log.d(TAG, "HashPreparado que pillamos: " + hashPreparado);

                            return new String[]{pagoId, hashPreparado};
                        }

                        throw new Exception("No hemos encontrado el evento PagoPreparado en el receipt.");
                    }

                    Log.d(TAG, "Esperando confirmación... intento " + (intentos + 1) + "/" + maxIntentos);
                    Thread.sleep(2000);
                    intentos++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Interrumpido mientras esperabamos la receipt.");
            }
        }
        throw new Exception("Error de timeout");
    }

    public String confirmarPago(Credentials credentials, byte[] pagoId, byte[] hashPreparado, byte[] firmaConfirmacion) throws  Exception {
        Log.d(TAG, "=== CONFIRMAR PAGO ===");
        Log.d(TAG, "PagoId: " + Numeric.toHexString(pagoId));
        Log.d(TAG, "HashPreparado: " + Numeric.toHexString(hashPreparado));
        Log.d(TAG, "FirmaConfirmacion: " + Numeric.toHexString(firmaConfirmacion));

        Function function = new Function(
                "confirmarPago",
                Arrays.asList(
                        new Bytes32(pagoId),
                        new Bytes32(hashPreparado),
                        new DynamicBytes(firmaConfirmacion)
                ), Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(),
                DefaultBlockParameterName.LATEST
        ).send();
        BigInteger txNonce = ethGetTransactionCount.getTransactionCount();

        EthGasPrice ethGasPrice = web3j.ethGasPrice().send();
        BigInteger gasPrice = ethGasPrice.getGasPrice();

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                txNonce,
                gasPrice,
                BigInteger.valueOf(500000),
                Constants.CONTRACT_ADDRESS,
                encodedFunction
        );

        byte[] signedMessage = TransactionEncoder.signMessage(
                rawTransaction,
                11155111,
                credentials
        );
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

        if (ethSendTransaction.hasError()){
            throw new Exception("Error: " + ethSendTransaction.getError().getMessage());
        }

        String transactionHash = ethSendTransaction.getTransactionHash();

        Log.d(TAG, "Transacción enviada - TX Hash" + transactionHash);
        Log.d(TAG, "Esperando receipt y extrayendo Hashfinal.");
        String hashFinal = esperarYExtraerPagoConfirmado(transactionHash);

        return hashFinal;
    }

    private String esperarYExtraerPagoConfirmado(String txHash) throws Exception {

        Log.d(TAG, ">>> Esperando receipt de transacción: " + txHash);

        int intentos = 0;
        int maxIntentos = 60; // 60 x 2s = 120 segundos

        while (intentos < maxIntentos) {
            try {
                EthGetTransactionReceipt receiptResponse =
                        web3j.ethGetTransactionReceipt(txHash).send();

                if (receiptResponse.getTransactionReceipt().isPresent()) {

                    TransactionReceipt receipt =
                            receiptResponse.getTransactionReceipt().get();

                    Log.d(TAG, "Receipt obtenido, buscando evento...");

                    // Buscar evento PagoConfirmado
                    // event PagoConfirmado(bytes32 indexed pagoId, address indexed emisor,
                    //                      address indexed receptor, uint256 amount,
                    //                      bytes32 hashFinal, uint256 timestamp)

                    for (org.web3j.protocol.core.methods.response.Log log : receipt.getLogs()) {

                        if (log.getTopics().isEmpty()) {
                            continue;
                        }

                        String eventSignature = log.getTopics().get(0);

                        // Hash del evento PagoConfirmado
                        String expectedEventHash = org.web3j.crypto.Hash.sha3String(
                                "PagoConfirmado(bytes32,address,address,uint256,bytes32,uint256)"
                        );

                        Log.d(TAG, "Event signature encontrado: " + eventSignature);
                        Log.d(TAG, "Event signature esperado: " + expectedEventHash);

                        if (eventSignature.equalsIgnoreCase(expectedEventHash)) {

                            Log.d(TAG, "Evento PagoConfirmado encontrado!");

                            // Data: amount (uint256), hashFinal (bytes32), timestamp (uint256)
                            String data = log.getData();

                            if (data.startsWith("0x")) {
                                data = data.substring(2);
                            }

                            // amount = 0-64
                            // hashFinal = 64-128
                            // timestamp = 128-192

                            if (data.length() < 192) {
                                throw new Exception("Datos del evento incompletos");
                            }

                            String hashFinal = "0x" + data.substring(64, 128);

                            Log.d(TAG, "HashFinal extraído: " + hashFinal);

                            return hashFinal;
                        }
                    }

                    throw new Exception("No se encontró el evento PagoConfirmado en el receipt");
                }

                Log.d(TAG, "Esperando confirmación... intento " + (intentos + 1) + "/" + maxIntentos);
                Thread.sleep(2000);
                intentos++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception("Interrupted while waiting for receipt");
            }
        }

        throw new Exception("Timeout: No se recibió el receipt en 120 segundos");
    }
}

