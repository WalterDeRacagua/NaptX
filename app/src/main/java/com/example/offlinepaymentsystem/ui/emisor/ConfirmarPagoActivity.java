package com.example.offlinepaymentsystem.ui.emisor;

import android.Manifest;
import android.app.usage.ConfigurationStats;
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
import com.example.offlinepaymentsystem.data.local.FirmarMensajeCallback;
import com.example.offlinepaymentsystem.data.local.WalletManager;
import com.example.offlinepaymentsystem.data.repository.PagoRepository;
import com.example.offlinepaymentsystem.data.repository.RepositoryCallback;
import com.example.offlinepaymentsystem.data.repository.WhitelistRepository;
import com.example.offlinepaymentsystem.model.PagoPendiente;
import com.example.offlinepaymentsystem.utils.Constants;
import com.example.offlinepaymentsystem.utils.QRCodeHelper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

import java.util.Collections;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.P)
public class ConfirmarPagoActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmarPago";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private TextView tvEstado;
    private Button btnEscanear;
    private TextView tvDatosPago;
    private LinearLayout layoutQRConfirmacion;
    private ImageView ivQRConfirmacion;
    private Button btnNuevoEscaneo;

    private WalletManager walletManager;
    private PagoRepository pagoRepository;

    private byte[] pagoId;
    private byte[] hashPreparado;

    private PagoPendiente pagoLocal;

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
        setContentView(R.layout.activity_confirmar_pago);

        initViews();
        initData();
        setupListeners();
    }

    private void initViews() {
        tvEstado = findViewById(R.id.tvEstado);
        btnEscanear = findViewById(R.id.btnEscanear);
        tvDatosPago = findViewById(R.id.tvDatosPago);
        layoutQRConfirmacion = findViewById(R.id.layoutQRConfirmacion);
        ivQRConfirmacion = findViewById(R.id.ivQRConfirmacion);
        btnNuevoEscaneo = findViewById(R.id.btnNuevoEscaneo);
    }

    private void initData() {
        walletManager = new WalletManager(this);
        pagoRepository = new PagoRepository(this);
        tvEstado.setText("Escanea el QR del receptor\nCcontiene pagoId + hashPreparado");
    }

    private void setupListeners() {
        btnEscanear.setOnClickListener(v -> verificarPermisoYEscanear());
        btnNuevoEscaneo.setOnClickListener(v -> reiniciarEscaneo());
    }

    private void reiniciarEscaneo() {
        layoutQRConfirmacion.setVisibility(View.GONE);
        tvDatosPago.setVisibility(View.GONE);
        btnEscanear.setEnabled(true);
        tvEstado.setText("Escanea el QR del receptor");
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
        options.setPrompt("Escanea el QR del receptor");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);

        barcodeLauncher.launch(options);
    }

    private void procesarQR(String contenidoQR) {
        try {
            Log.d(TAG, "QR #2 escaneado: " + contenidoQR);

            // Parsear JSON del QR #2
            JSONObject json = new JSONObject(contenidoQR);

            pagoId = Numeric.hexStringToByteArray(json.getString("pagoId"));
            hashPreparado = Numeric.hexStringToByteArray(json.getString("hashPreparado"));
            long timestampPreparacion = json.getLong("timestampPreparacion");

            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putLong("TIMESTAMP_PREPARACION", timestampPreparacion).apply();

            // Mostrar datos
            String datos = "Datos del pago:\n\n" +
                    "PagoId:\n" + Numeric.toHexString(pagoId).substring(0, 20) + "...\n\n" +
                    "HashPreparado:\n" + Numeric.toHexString(hashPreparado).substring(0, 20) + "...";

            tvDatosPago.setText(datos);
            tvDatosPago.setVisibility(View.VISIBLE);

            tvEstado.setText("QR válido\n\n Buscando pago en BD local...");
            btnEscanear.setEnabled(false);

            buscarPagoLocal();

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar QR", e);
            tvEstado.setText("Error al procesar QR:\n" + e.getMessage());
        }
    }

    /**
     * Busca el pago en la base de datos local usando el pagoId
     */
    private void buscarPagoLocal() {
        tvEstado.setText("QR válido\n\nBuscando pago en BD local...");

        pagoRepository.obtenerPagosPreparados(new RepositoryCallback<List<PagoPendiente>>() {
            @Override
            public void onSuccess(java.util.List<PagoPendiente> pagos) {
                runOnUiThread(() -> {
                    if (pagos == null || pagos.isEmpty()) {
                        tvEstado.setText("No hay pagos en BD local");
                        btnEscanear.setEnabled(true);
                        return;
                    }

                    // Ordenar por timestamp (más reciente primero)
                    Collections.sort(pagos, (p1, p2) ->
                            Long.compare(p2.getTimestampPreparacion(), p1.getTimestampPreparacion())
                    );

                    // Tomar el más reciente en estado PREPARADO
                    PagoPendiente pagoEncontrado = null;
                    for (PagoPendiente pago : pagos) {
                        if (pago.getEstado() == PagoPendiente.Estado.PREPARADO) {
                            pagoEncontrado = pago;
                            break;
                        }
                    }

                    if (pagoEncontrado == null) {
                        tvEstado.setText("No hay pagos preparados");
                        btnEscanear.setEnabled(true);
                        return;
                    }

                    pagoLocal = pagoEncontrado;

                    // Actualizar con datos reales del contrato
                    pagoLocal.setPagoId(Numeric.toHexString(pagoId));
                    pagoLocal.setHashPreparado(hashPreparado);

                    pagoRepository.actualizar(pagoLocal, new RepositoryCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Log.d(TAG, "Pago actualizado");
                        }
                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "Error: " + message);
                        }
                    });

                    Log.d(TAG, "Pago encontrado y actualizado");
                    tvEstado.setText("Pago encontrado\n\nCalculando hashFinal...");
                    calcularHashFinalYFirmar();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    tvEstado.setText("Error: " + message);
                    btnEscanear.setEnabled(true);
                });
            }
        });
    }

    private void calcularHashFinalYFirmar() {
        try {
            // Extraer datos del pago local
            byte[] hashUsado = pagoLocal.getHashUsado();
            long amount = pagoLocal.getAmount();
            String receptor = pagoLocal.getReceptor();
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            long timestamp = prefs.getLong("TIMESTAMP_PREPARACION", 0);

            if (timestamp == 0) {
                tvEstado.setText("Error: No se encontró timestamp del contrato");
                btnEscanear.setEnabled(true);
                return;
            }

            // Convertir amount a bytes32
            byte[] amountBytes = new byte[32];
            for (int i = 0; i < 8; i++) {
                amountBytes[31 - i] = (byte) (amount >> (i * 8));
            }

            // Convertir receptor a bytes (sin 0x)
            byte[] receptorBytes = Numeric.hexStringToByteArray(receptor);

            // Convertir timestamp a bytes32
            byte[] timestampBytes = new byte[32];
            for (int i = 0; i < 8; i++) {
                timestampBytes[31 - i] = (byte) (timestamp >> (i * 8));
            }

            // Palabra "confirmado"
            byte[] palabraConfirmado = "confirmado".getBytes();

            // Concatenar: hashUsado + amount + receptor + timestamp + "confirmado"
            byte[] mensaje = new byte[hashUsado.length + amountBytes.length +
                    receptorBytes.length + timestampBytes.length +
                    palabraConfirmado.length];

            int offset = 0;
            System.arraycopy(hashUsado, 0, mensaje, offset, hashUsado.length);
            offset += hashUsado.length;

            System.arraycopy(amountBytes, 0, mensaje, offset, amountBytes.length);
            offset += amountBytes.length;

            System.arraycopy(receptorBytes, 0, mensaje, offset, receptorBytes.length);
            offset += receptorBytes.length;

            System.arraycopy(timestampBytes, 0, mensaje, offset, timestampBytes.length);
            offset += timestampBytes.length;

            System.arraycopy(palabraConfirmado, 0, mensaje, offset, palabraConfirmado.length);

            // Calcular hashFinal = keccak256(mensaje)
            byte[] hashFinal = Hash.sha3(mensaje);

            Log.d(TAG, "=== CALCULAR HASH FINAL (OFFLINE) ===");
            Log.d(TAG, "HashUsado: " + Numeric.toHexString(hashUsado));
            Log.d(TAG, "Amount: " + amount + " (" + Numeric.toHexString(amountBytes) + ")");
            Log.d(TAG, "Receptor: " + receptor);
            Log.d(TAG, "Timestamp: " + timestamp + " (" + Numeric.toHexString(timestampBytes) + ")");
            Log.d(TAG, "Mensaje concatenado: " + Numeric.toHexString(mensaje));
            Log.d(TAG, "HashFinal: " + Numeric.toHexString(hashFinal));

            tvEstado.setText("HashFinal calculado\n\nFirmando confirmación...");

            // Firmar mensaje de confirmación con biometría
            firmarConfirmacion(hashFinal);

        } catch (Exception e) {
            Log.e(TAG, "Error al calcular hashFinal", e);
            tvEstado.setText("Error al calcular hashFinal:\n" + e.getMessage());
            btnEscanear.setEnabled(true);
        }
    }

    private void firmarConfirmacion(byte[] hashFinal) {
        walletManager.firmarConfirmacionPago(
                pagoId,
                hashPreparado,
                new FirmarMensajeCallback() {
                    @Override
                    public void onMensajeFirmado(byte[] firmaConfirmacion) {
                        runOnUiThread(() -> {
                            tvEstado.setText("Confirmación firmada\n\nGenerando QR 3...");
                            generarQRConfirmacion(hashFinal, firmaConfirmacion);
                        });
                    }

                    @Override
                    public void onError(String mensaje) {
                        runOnUiThread(() -> {
                            tvEstado.setText("Error al firmar:\n" + mensaje);
                            btnEscanear.setEnabled(true);
                        });
                    }
                }
        );
    }

    private void generarQRConfirmacion(byte[] hashFinal, byte[] firmaConfirmacion) {
        try {
            // PASO 1: Guardar hashFinal como próximo hashUsado
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            prefs.edit()
                    .putString("HASH_ACTUAL", Numeric.toHexString(hashFinal))
                    .apply();

            Log.d(TAG, "HashFinal guardado como próximo hashUsado: " +
                    Numeric.toHexString(hashFinal));

            decrementarLimiteWhitelist();

            // PASO 2: Crear QR #3 SIN hashFinal
            JSONObject json = new JSONObject();
            json.put("pagoId", Numeric.toHexString(pagoId));
            json.put("hashPreparado", Numeric.toHexString(hashPreparado));
            json.put("firmaConfirmacion", Numeric.toHexString(firmaConfirmacion));
            // NO incluir hashFinal en el QR

            String datosQR = json.toString();

            Log.d(TAG, "=== QR 3 GENERADO ===");
            Log.d(TAG, "JSON: " + datosQR);

            // Generar bitmap del QR
            Bitmap qrBitmap = QRCodeHelper.generarQRBitmap(datosQR, 512, 512);

            // Mostrar QR
            ivQRConfirmacion.setImageBitmap(qrBitmap);
            layoutQRConfirmacion.setVisibility(View.VISIBLE);

            tvEstado.setText("CONFIRMACIÓN FIRMADA (OFFLINE)\n\n" +
                    "Muestra este QR al receptor\n" +
                    "para completar el pago\n\n" +
                    "HashFinal guardado para próximo pago");
            btnEscanear.setEnabled(false);

        } catch (JSONException | WriterException e) {
            Log.e(TAG, "Error al generar QR #3", e);
            tvEstado.setText("Error al generar QR:\n" + e.getMessage());
            btnEscanear.setEnabled(true);
        }
    }

    private void decrementarLimiteWhitelist() {
        // Obtener datos del pago
        String receptorAddress = pagoLocal.getReceptor();
        long amount = pagoLocal.getAmount();

        WhitelistRepository whitelistRepo = new WhitelistRepository(this);

        whitelistRepo.decrementarLimite(receptorAddress, amount, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Límite de whitelist decrementado correctamente");
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error al decrementar límite: " + message);
            }
        });
    }
}