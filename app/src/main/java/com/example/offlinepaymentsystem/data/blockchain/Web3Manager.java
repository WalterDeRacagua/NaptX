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
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
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
import android.util.Log;

import com.example.offlinepaymentsystem.utils.Constants;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
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
     * Registra un emisor en el smart contract
     * Envía transacción que modifica el estado (requiere gas)
     */
    public String registrarEmisor(
            org.web3j.crypto.Credentials credentials,
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

            // 3. Obtener nonce de la cuenta (número de transacciones)
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
            BigInteger gasLimit = BigInteger.valueOf(300000); // Estimación conservadora

            // 6. Crear RawTransaction
            RawTransaction rawTransaction = RawTransaction.createTransaction(
                    nonceTransaccion,
                    gasPrice,
                    gasLimit,
                    Constants.CONTRACT_ADDRESS,
                    encodedFunction
            );

            // 7. Firmar transacción con credentials
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
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

            Log.d(TAG, "Transacción enviada");
            Log.d(TAG, "Transaction Hash: " + transactionHash);

            return transactionHash;

        } catch (Exception e) {
            Log.e(TAG, "Error al registrar emisor", e);
            return null;
        }
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
}

