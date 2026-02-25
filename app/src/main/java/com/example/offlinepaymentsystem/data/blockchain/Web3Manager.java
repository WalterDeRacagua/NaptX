package com.example.offlinepaymentsystem.data.blockchain;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import com.example.offlinepaymentsystem.model.Emisor;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.util.Log;

import com.example.offlinepaymentsystem.utils.Constants;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

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
                    response.getValue(),
                    function.getOutputParameters()
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

}

