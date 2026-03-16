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
import org.web3j.crypto.Credentials;
import org.web3j.utils.Numeric;

@RequiresApi(api = Build.VERSION_CODES.P)
public class CompletarPagoActivity extends AppCompatActivity {

    private static final String TAG = "CompletarPago";
    private static final String PREFS_NAME = "WalletPrefs";
    private static final String KEY_WALLET_ADDRESS = "WALLET_ADDRESS";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // UI
    private TextView tvEstado;
    private Button btnEscanear;
    private TextView tvDatos;
    private Button btnNuevoEscaneo;

    // Managers
    private WalletManager walletManager;
    private Web3Manager web3Manager;
    private String addressReceptor;

    // Datos del QR #3
    private byte[] pagoId;
    private byte[] hashPreparado;
    private byte[] firmaConfirmacion;

    // Scanner launcher
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completar_pago);

        initViews();
        initData();
        setupListeners();
    }

    private void initViews() {
        tvEstado = findViewById(R.id.tvEstado);
        btnEscanear = findViewById(R.id.btnEscanear);
        tvDatos = findViewById(R.id.tvDatos);
        btnNuevoEscaneo = findViewById(R.id.btnNuevoEscaneo);
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

        tvEstado.setText("Escanea el QR 3 del emisor\n Contiene la confirmación firmada");
    }

    private void setupListeners() {
        btnEscanear.setOnClickListener(v -> verificarPermisoYEscanear());
        btnNuevoEscaneo.setOnClickListener(v -> reiniciarEscaneo());
    }

    private void reiniciarEscaneo() {
        tvDatos.setVisibility(View.GONE);
        btnNuevoEscaneo.setVisibility(View.GONE);
        btnEscanear.setEnabled(true);
        tvEstado.setText("Escanea el QR 3 del emisor");
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
                escanearQR();
            } else {
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
        options.setPrompt("Escanea el QR 3 del emisor");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);

        barcodeLauncher.launch(options);
    }

    private void procesarQR(String contenidoQR) {
        try {
            Log.d(TAG, "QR #3 escaneado: " + contenidoQR);

            // Parsear JSON del QR #3
            JSONObject json = new JSONObject(contenidoQR);

            pagoId = Numeric.hexStringToByteArray(json.getString("pagoId"));
            hashPreparado = Numeric.hexStringToByteArray(json.getString("hashPreparado"));
            firmaConfirmacion = Numeric.hexStringToByteArray(json.getString("firmaConfirmacion"));

            Log.d(TAG, "QR 3 parseado correctamente");
            Log.d(TAG, "PagoId: " + Numeric.toHexString(pagoId));
            Log.d(TAG, "HashPreparado: " + Numeric.toHexString(hashPreparado));
            Log.d(TAG, "FirmaConfirmacion: " + Numeric.toHexString(firmaConfirmacion));

            // Mostrar datos
            String datos = "Confirmación recibida:\n\n" +
                    "PagoId:\n" + Numeric.toHexString(pagoId).substring(0, 20) + "...\n\n" +
                    "HashPreparado:\n" + Numeric.toHexString(hashPreparado).substring(0, 20) + "...";

            tvDatos.setText(datos);
            tvDatos.setVisibility(View.VISIBLE);

            tvEstado.setText("QR válido\n\n Obteniendo credentials...");

            // Continuar con confirmación
            obtenerCredentialsYConfirmar();

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar QR", e);
            tvEstado.setText("Error al procesar QR:\n" + e.getMessage());
        }
    }

    /**
     * Obtiene credentials con biometría y confirma el pago
     */
    private void obtenerCredentialsYConfirmar() {
        btnEscanear.setEnabled(false);

        walletManager.obtenerCredentials(new ObtenerCredentialsCallback() {
            @Override
            public void onCredentialsObtenidos(org.web3j.crypto.Credentials credentials) {
                runOnUiThread(() -> {
                    tvEstado.setText("Credentials obtenidos\n\n Confirmando pago en blockchain...");
                });

                // Ejecutar en hilo separado
                new Thread(() -> {
                    enviarConfirmarPago(credentials);
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

    private void enviarConfirmarPago(Credentials credentials) {
        try {
            Log.d(TAG, "=== CONFIRMAR PAGO EN BLOCKCHAIN ===");
            Log.d(TAG, "Receptor: " + credentials.getAddress());
            Log.d(TAG, "PagoId: " + Numeric.toHexString(pagoId));
            Log.d(TAG, "HashPreparado: " + Numeric.toHexString(hashPreparado));

            String hashFinal = web3Manager.confirmarPago(
                    credentials,
                    pagoId,
                    hashPreparado,
                    firmaConfirmacion
            );

            Log.d(TAG, "Pago confirmado en blockchain");
            Log.d(TAG, "HashFinal devuelto: " + hashFinal);

            runOnUiThread(() -> {
                tvEstado.setText("¡PAGO COMPLETADO!\n\n" +
                        "Fondos transferidos exitosamente\n\n" +
                        "HashFinal:\n" + hashFinal.substring(0, 20) + "...\n\n" +
                        "Verifica la transacción en Etherscan");

                tvDatos.setVisibility(View.GONE);
                btnNuevoEscaneo.setVisibility(View.VISIBLE);

                Toast.makeText(CompletarPagoActivity.this,
                        "¡Pago completado!",
                        Toast.LENGTH_LONG).show();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al confirmar pago", e);
            runOnUiThread(() -> {
                tvEstado.setText("Error al confirmar pago:\n" + e.getMessage());
                btnEscanear.setEnabled(true);
            });
        }
    }
}