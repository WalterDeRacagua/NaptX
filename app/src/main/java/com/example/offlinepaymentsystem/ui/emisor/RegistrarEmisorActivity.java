package com.example.offlinepaymentsystem.ui.emisor;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.blockchain.Web3Manager;
import com.example.offlinepaymentsystem.data.local.DeviceIdManager;
import com.example.offlinepaymentsystem.data.local.FirmarMensajeCallback;
import com.example.offlinepaymentsystem.data.local.ObtenerCredentialsCallback;
import com.example.offlinepaymentsystem.data.local.WalletManager;
import com.example.offlinepaymentsystem.model.Emisor;
import com.example.offlinepaymentsystem.utils.Constants;

import org.web3j.crypto.Credentials;
import org.web3j.utils.Numeric;

import java.util.Random;

@RequiresApi(api = Build.VERSION_CODES.P)
public class RegistrarEmisorActivity extends AppCompatActivity {

    private static final String TAG = "RegistrarEmisor";

    private WalletManager walletManager;
    private Web3Manager web3Manager;
    private SharedPreferences prefs;

    private TextView tvAddress;
    private TextView tvDeviceId;
    private TextView tvEstado;
    private Button btnRegistrar;

    private String address;
    private byte[] deviceId;
    private long timestamp;
    private long nonce;
    private byte[] firma;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_emisor);

        //Inicializo las vistas
        this.tvAddress = findViewById(R.id.tvAddress);
        this.tvDeviceId = findViewById(R.id.tvDeviceId);
        this.tvEstado = findViewById(R.id.tvEstado);
        this.btnRegistrar = findViewById(R.id.btnRegistrar);

        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        walletManager = new WalletManager(this);
        this.web3Manager = new Web3Manager(this);

        address = prefs.getString(Constants.KEY_WALLET_ADDRESS, null);

        if (address == null){
            tvEstado.setText("Error: no hay ninguna wallet creada");
            btnRegistrar.setEnabled(false);
            return;
        }

        this.deviceId = DeviceIdManager.obtenerDeviceId(this);

        mostrarDatos();

        verificarSiYaRegistrado();

        this.btnRegistrar.setOnClickListener(v-> prepararRegistro());

    }

    private void mostrarDatos() {
        tvAddress.setText(address);
        tvDeviceId.setText(Numeric.toHexString(deviceId));
        tvEstado.setText("Listo para registrar");
    }

    private void prepararRegistro(){
        tvEstado.setText("Preparando registro");
        btnRegistrar.setEnabled(false);

        this.timestamp = System.currentTimeMillis()/1000;

        nonce = new Random().nextLong() & Long.MAX_VALUE;

        firmarMensajeRegistro();
    }

    private void firmarMensajeRegistro() {
        tvEstado.setText("Firmando mensaje... \nSe pedirá tu huella.");
        Log.d(TAG, ">>> [RegistrarEmisor] firmarMensajeRegistro() INICIADO");

        walletManager.firmarMensajeRegistro(address, deviceId, timestamp, nonce, new FirmarMensajeCallback() {
            @Override
            public void onMensajeFirmado(byte[] firmaGenerada) {
                Log.d(TAG, ">>> [RegistrarEmisor] onMensajeFirmado LLAMADO");
                runOnUiThread(()->{
                    firma = firmaGenerada;
                    tvEstado.setText("Mensaje firmado\n\n Obteniendo credenciales...\nSe pedirá tu huella de nuevo.");

                    Log.d(TAG, ">>> [RegistrarEmisor] A PUNTO de llamar a obtenerCredentialsYEnviar()");

                    // DELAY DE 500ms ENTRE LOS BIOMETRICS
                    new android.os.Handler().postDelayed(() -> {
                        Log.d(TAG, ">>> [RegistrarEmisor] Después del delay, llamando a obtenerCredentialsYEnviar()");
                        obtenerCredentialsYEnviar();
                    }, 500);

                    Log.d(TAG, ">>> [RegistrarEmisor] Delay programado");
                });
            }

            @Override
            public void onError(String mensaje) {
                Log.e(TAG, ">>> [RegistrarEmisor] onMensajeFirmado - onError: " + mensaje);
                runOnUiThread(()->{
                    tvEstado.setText("Error al firmar el mensaje");
                    btnRegistrar.setEnabled(false);
                    Toast.makeText(RegistrarEmisorActivity.this, "Error: " + mensaje, Toast.LENGTH_LONG).show();
                });
            }
        });

        Log.d(TAG, ">>> [RegistrarEmisor] walletManager.firmarMensajeRegistro() LLAMADO");
    }
    private void obtenerCredentialsYEnviar(){
        Log.d(TAG, ">>> [RegistrarEmisor] obtenerCredentialsYEnviar() INICIADO");
        walletManager.obtenerCredentials(new ObtenerCredentialsCallback() {

            @Override
            public void onCredentialsObtenidos(Credentials credentials) {
                Log.d(TAG, ">>> [RegistrarEmisor] onCredentialsObtenidos LLAMADO");

                runOnUiThread(() -> {
                    tvEstado.setText("Credenciales obtenidas\n\n Enviando transacción a Sepolia...");
                });

                Log.d(TAG, ">>> [RegistrarEmisor] Creando hilo para enviarTransaccion()");

                new Thread(() -> {
                    Log.d(TAG, ">>> [RegistrarEmisor] Hilo iniciado, llamando a enviarTransaccion()");
                    enviarTransaccion(credentials);
                }).start();
            }

            @Override
            public void onError(String mensaje) {
                Log.e(TAG, ">>> [RegistrarEmisor] onCredentialsObtenidos - onError: " + mensaje);
                runOnUiThread(() -> {
                    tvEstado.setText("Error al obtener credenciales " + mensaje);
                    btnRegistrar.setEnabled(true);
                    Toast.makeText(RegistrarEmisorActivity.this, "Error: "+mensaje, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void enviarTransaccion(org.web3j.crypto.Credentials credentials) {
        Log.d(TAG, ">>> [RegistrarEmisor] enviarTransaccion() INICIADO");

        try {
            // Devuelve [txHash, hashInicial]
            String[] resultado = web3Manager.registrarEmisor(credentials, deviceId, timestamp, nonce, firma);

            if (resultado == null || resultado.length < 2) {
                Log.e(TAG, ">>> [RegistrarEmisor] Resultado nulo o incompleto");
                runOnUiThread(() -> {
                    tvEstado.setText("Error: No se recibió respuesta del contrato");
                    btnRegistrar.setEnabled(true);
                    Toast.makeText(this, "Error al registrar", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            String txHash = resultado[0];
            String hashInicialHex = resultado[1];

            Log.d(TAG, ">>> [RegistrarEmisor] registrarEmisor() retornó:");
            Log.d(TAG, ">>> TX Hash: " + txHash);
            Log.d(TAG, ">>> HashInicial: " + hashInicialHex);

            runOnUiThread(() -> {
                // Guardar hashActual en SharedPreferences
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("HASH_ACTUAL", hashInicialHex);
                editor.apply();

                Log.d(TAG, "HashActual guardado en SharedPreferences");

                tvEstado.setText("REGISTRO COMPLETO!\n\n" +
                        "Transaction Hash:\n" + txHash.substring(0, 20) + "...\n\n" +
                        "Hash Inicial:\n" + hashInicialHex.substring(0, 20) + "...\n\n");

                Toast.makeText(this, "Registro exitoso y hash guardado!", Toast.LENGTH_LONG).show();
            });

        } catch (Exception e) {
            Log.e(TAG, ">>> [RegistrarEmisor] EXCEPCIÓN en enviarTransaccion()", e);
            runOnUiThread(() -> {
                tvEstado.setText("Error:\n" + e.getMessage());
                btnRegistrar.setEnabled(true);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    private void verificarSiYaRegistrado() {
        tvEstado.setText("Verificando estado en blockchain...");
        btnRegistrar.setEnabled(false);

        new Thread(() -> {
            try {
                Emisor emisor = web3Manager.obtenerEstadoEmisor(address);

                runOnUiThread(() -> {
                    if (emisor != null && emisor.isRegistrado()) {
                        tvEstado.setText("Ya estás registrado en la blockchain\n\nNo es necesario registrarte de nuevo.");
                        btnRegistrar.setEnabled(false);
                        btnRegistrar.setText("Ya Registrado");
                    } else {
                        tvEstado.setText("Registrar en la blockchain");
                        btnRegistrar.setEnabled(true);
                    }
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    tvEstado.setText("No se pudo verificar el estado");
                    btnRegistrar.setEnabled(true);
                });
            }
        }).start();
    }

    private void obtenerYGuardarHashInicial(){
        this.tvEstado.setText("Registro exitoso \n\n Obteniendo hash inicial...");
    }
}
