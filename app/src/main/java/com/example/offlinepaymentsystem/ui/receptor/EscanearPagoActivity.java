package com.example.offlinepaymentsystem.ui.receptor;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.blockchain.Web3Manager;
import com.example.offlinepaymentsystem.data.local.ObtenerCredentialsCallback;
import com.example.offlinepaymentsystem.data.local.WalletManager;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;
import org.web3j.utils.Numeric;

@RequiresApi(api= Build.VERSION_CODES.P)
public class EscanearPagoActivity extends AppCompatActivity {

    private static final String TAG = "EscanearPago";
    private static final String PREFS_NAME = "WalletPrefs";
    private static final String KEY_WALLET_ADDRESS = "WALLET_ADDRESS";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                } else {
                    procesarQR(result.getContents());
                }
            }
    );

    private TextView tvEstado;
    private Button btnEscanear;
    private TextView tvDatos;

    private WalletManager walletManager;
    private Web3Manager web3Manager;
    private String addressReceptor;

    // Datos del QR
    private byte[] hashUsado;
    private long amount;
    private String receptor;
    private long timestamp;
    private long nonce;
    private byte[] deviceId;
    private byte[] firma;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_escanear_pago);

        this.initViews();
        this.initData();
        this.setupListeners();
    }

    private void initViews() {
        tvEstado = findViewById(R.id.tvEstado);
        btnEscanear = findViewById(R.id.btnEscanear);
        tvDatos = findViewById(R.id.tvDatos);
    }

    private void initData() {
        walletManager = new WalletManager(this);
        web3Manager = new Web3Manager(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        addressReceptor = prefs.getString(KEY_WALLET_ADDRESS, null);

        if (addressReceptor == null) {
            tvEstado.setText("Error: No hay wallet creada");
            btnEscanear.setEnabled(false);
            return;
        }

        tvEstado.setText("Listo para escanear pago del emisor");
    }

    private void setupListeners() {
        btnEscanear.setOnClickListener(v -> verificarPermisoYEscanear());
    }

    private void verificarPermisoYEscanear() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            escanearQR();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permiso concedido, escanear
                escanearQR();
            } else {
                // Permiso denegado
                Toast.makeText(this,
                        "Se necesita permiso de cámara para escanear QR",
                        Toast.LENGTH_LONG).show();
                tvEstado.setText("Permiso de cámara denegado");
            }
        }
    }


    private void escanearQR() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Escanea el QR del emisor");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }


    private void procesarQR(String contenidoQR) {
        try {
            Log.d(TAG, "QR escaneado: " + contenidoQR);

            // Parsear JSON
            JSONObject json = new JSONObject(contenidoQR);

            hashUsado = Numeric.hexStringToByteArray(json.getString("hashUsado"));
            amount = json.getLong("amount");
            receptor = json.getString("receptor");
            timestamp = json.getLong("timestamp");
            nonce = json.getLong("nonce");
            deviceId = Numeric.hexStringToByteArray(json.getString("deviceId"));
            firma = Numeric.hexStringToByteArray(json.getString("firma"));

            // Mostrar datos
            String datos = "Pago recibido:\n\n" +
                    "Monto: " + convertirWeiAETH(amount) + " ETH\n" +
                    "Receptor: " + receptor + "\n" +
                    "Timestamp: " + timestamp;

            tvDatos.setText(datos);
            tvDatos.setVisibility(View.VISIBLE);

            // Validar que YO soy el receptor
            if (!receptor.equalsIgnoreCase(addressReceptor)) {
                tvEstado.setText("Error: Este pago no es para ti\n\n" +
                        "Receptor esperado: " + addressReceptor + "\n" +
                        "Receptor en QR: " + receptor);
                return;
            }

            tvEstado.setText("Pago válido\n\n Obteniendo credentials...");
            btnEscanear.setEnabled(false);

            obtenerCredentialsYPrepararPago();

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar QR", e);
            tvEstado.setText("Error al procesar QR:\n" + e.getMessage());
        }
    }


    private void obtenerCredentialsYPrepararPago() {
        walletManager.obtenerCredentials(new ObtenerCredentialsCallback() {
            @Override
            public void onCredentialsObtenidos(org.web3j.crypto.Credentials credentials) {
                runOnUiThread(() -> {
                    tvEstado.setText("Credentials obtenidos\n\nEnviando a blockchain...");
                });

                new Thread(() -> {
                    enviarPrepararPago(credentials);
                }).start();
            }

            @Override
            public void onError(String mensaje) {
                runOnUiThread(() -> {
                    tvEstado.setText("Error al obtener credentials:\n" + mensaje);
                    btnEscanear.setEnabled(true);
                });
            }
        });
    }

    private void enviarPrepararPago(org.web3j.crypto.Credentials credentials) {
        try {
            String[] resultado = web3Manager.prepararPago(
                    credentials,
                    hashUsado,
                    amount,
                    receptor,
                    timestamp,
                    nonce,
                    deviceId,
                    firma
            );

            String pagoId = resultado[0];
            String hashPreparado = resultado[1];

            runOnUiThread(() -> {
                tvEstado.setText("PAGO PREPARADO EN BLOCKCHAIN!\n\n" +
                        "PagoId:\n" + pagoId + "\n\n" +
                        "HashPreparado:\n" + hashPreparado.substring(0, 20) + "...\n\n" +
                        "Ahora genera el QR para que el emisor confirme");

                // TODO: Generar QR con pagoId + hashPreparado para el emisor
                btnEscanear.setEnabled(true);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al preparar pago", e);
            runOnUiThread(() -> {
                tvEstado.setText("Error al preparar pago:\n" + e.getMessage());
                btnEscanear.setEnabled(true);
            });
        }
    }

    private String convertirWeiAETH(long wei) {
        double eth = wei / 1000000000000000000.0;
        return String.format("%.6f", eth);
    }

}

