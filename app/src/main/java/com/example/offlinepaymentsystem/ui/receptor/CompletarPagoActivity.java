package com.example.offlinepaymentsystem.ui.receptor;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.offlinepaymentsystem.utils.Constants;
import com.example.offlinepaymentsystem.utils.QRCodeHelper;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Credentials;
import org.web3j.utils.Numeric;

@RequiresApi(api = Build.VERSION_CODES.P)
public class CompletarPagoActivity extends AppCompatActivity {

    private static final String TAG = "CompletarPago";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    // UI
    private TextView tvEstado;
    private Button btnEscanear;
    private TextView tvDatos;
    private Button btnNuevoEscaneo;
    private LinearLayout layoutQR4;
    private ImageView ivQR4;

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
        this.tvEstado = findViewById(R.id.tvEstado);
        this.btnEscanear = findViewById(R.id.btnEscanear);
        this.tvDatos = findViewById(R.id.tvDatos);
        this.btnNuevoEscaneo = findViewById(R.id.btnNuevoEscaneo);
        this.layoutQR4 = findViewById(R.id.layoutQR4);
        this.ivQR4 = findViewById(R.id.ivQR4);
    }

    private void initData() {
        walletManager = new WalletManager(this);
        web3Manager = new Web3Manager(this);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        addressReceptor = prefs.getString(Constants.KEY_WALLET_ADDRESS, null);

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
        this.tvDatos.setVisibility(View.GONE);
        this.layoutQR4.setVisibility(View.GONE);
        this.btnNuevoEscaneo.setVisibility(View.GONE);
        this.btnEscanear.setEnabled(true);
        this.tvEstado.setText("Escanea el QR 3 del emisor");
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
            Log.d(TAG, "Confirmar pago - Receptor: " + credentials.getAddress());

            String hashFinal = web3Manager.confirmarPago(
                    credentials,
                    pagoId,
                    hashPreparado,
                    firmaConfirmacion
            );

            Log.d(TAG, "Pago confirmado en blockchain");
            Log.d(TAG, "HashFinal devuelto: " + hashFinal);

            runOnUiThread(() -> {
                generarQR4(hashFinal);
            });

        } catch (Exception e) {
            Log.e(TAG, "Error al confirmar pago", e);
            runOnUiThread(() -> {
                tvEstado.setText("Error al confirmar pago:\n" + e.getMessage());
                btnEscanear.setEnabled(true);
            });
        }
    }

    private void generarQR4(String hashFinal) {
        try {
            Log.d(TAG, "Generando QR 4 con hashFinal: " + hashFinal);

            // Crear JSON para QR 4
            JSONObject json = new JSONObject();
            json.put("tipo", "hashFinal");
            json.put("hashFinal", hashFinal);

            String datosQR = json.toString();

            // Generar bitmap del QR
            Bitmap qrBitmap = QRCodeHelper.generarQRBitmap(datosQR, 512, 512);

            ivQR4.setImageBitmap(qrBitmap);
            layoutQR4.setVisibility(View.VISIBLE);

            // Ocultar elementos anteriores
            tvEstado.setVisibility(View.GONE);
            tvDatos.setVisibility(View.GONE);
            btnEscanear.setVisibility(View.GONE);

            // Mostrar botón para nuevo pago
            btnNuevoEscaneo.setVisibility(View.VISIBLE);

            Toast.makeText(this, "¡Pago completado!", Toast.LENGTH_LONG).show();

            Log.d(TAG, "QR 4 generado y mostrado correctamente");

        } catch (JSONException | WriterException e) {
            Log.e(TAG, "Error al generar QR 4", e);

            // Aunque falle el QR, mostrar mensaje de éxito
            tvEstado.setText("¡PAGO COMPLETADO!\n\n" +
                    "Fondos transferidos exitosamente\n\n" +
                    "HashFinal:\n" + hashFinal.substring(0, 20) + "...\n\n" +
                    "Error al generar QR 4");

            tvDatos.setVisibility(View.GONE);
            btnNuevoEscaneo.setVisibility(View.VISIBLE);

            Toast.makeText(this, "Pago completado pero error al generar QR 4",
                    Toast.LENGTH_LONG).show();
        }
    }
}