package com.example.offlinepaymentsystem.ui.emisor;

import android.Manifest;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;

@RequiresApi(api = Build.VERSION_CODES.P)
public class ConfirmarPagoActivity extends AppCompatActivity {

    private static final String TAG = "ConfirmarPago";
    private static final int CAMERA_PERMISSION_REQUEST_CODE= 100;

    private TextView tvEstado;
    private Button btnEscanear;
    private TextView tvDatosPago;
    private LinearLayout layoutQRConfirmacion;
    private ImageView ivQRConfirmacion;
    private Button btnNuevoEscaneo;

    private WalletManager walletManager;

    private byte[] pagoId;
    private byte[] hashPreparado;

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
        this.setContentView(R.layout.activity_confirmar_pago);
        this.initViews();
        this.initData();
        this.setupListeners();
    }

    private void initViews(){
        this.tvEstado = findViewById(R.id.tvEstado);
        this.btnEscanear = findViewById(R.id.btnEscanear);
        this.tvDatosPago = findViewById(R.id.tvDatosPago);
        this.layoutQRConfirmacion = findViewById(R.id.layoutQRConfirmacion);
        this.ivQRConfirmacion = findViewById(R.id.ivQRConfirmacion);
        this.btnNuevoEscaneo = findViewById(R.id.btnNuevoEscaneo);
    }

    private void initData() {
        this.walletManager = new WalletManager(this);
        tvEstado.setText("Escanea el QR del receptor\n Contiene pagoId + hashPreparado");
    }

    private void setupListeners(){
        this.btnEscanear.setOnClickListener(v-> verificarPermisoYEscanear());
        this.btnNuevoEscaneo.setOnClickListener(v-> reiniciarEscaneo());
    }

    private void reiniciarEscaneo(){
        this.layoutQRConfirmacion.setVisibility(View.GONE);
        this.tvDatosPago.setVisibility(View.GONE);
        this.btnEscanear.setEnabled(false);
        this.tvEstado.setText("Escanea el QR del receptor");
    }

    private void verificarPermisoYEscanear(){
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults){
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


    private void procesarQR(String contenidoQR){

        try {
            Log.d(TAG, "QR 2 escaneado: "+ contenidoQR);

            JSONObject json = new JSONObject(contenidoQR);

            this.pagoId = Numeric.hexStringToByteArray(json.getString("pagoId"));
            this.hashPreparado = Numeric.hexStringToByteArray(json.getString("hashPreparado"));

            String datos = "Datos del pago:\n\n" +
                    "PagoId:\n" + Numeric.toHexString(pagoId).substring(0, 20) + "...\n\n" +
                    "HashPreparado:\n" + Numeric.toHexString(hashPreparado).substring(0, 20) + "...";

            this.tvDatosPago.setText(datos);
            this.tvDatosPago.setVisibility(View.VISIBLE);

            this.tvEstado.setText("QR válido\n\nCalculando hashFinal...");
            this.btnEscanear.setEnabled(false);

            this.calcularHashFinalYFirmar();
        } catch (Exception e) {
            Log.e(TAG, "Error al procesar QR", e);
            this.tvEstado.setText("Error al procesar QR: " +e.getMessage());
        }
    }

    private void calcularHashFinalYFirmar() {
        try {
            // Calcular hashFinal = keccak256(hashPreparado)
            byte[] hashFinal = Hash.sha3(hashPreparado);

            Log.d(TAG, "=== CALCULAR HASH FINAL (OFFLINE) ===");
            Log.d(TAG, "HashPreparado: " + Numeric.toHexString(hashPreparado));
            Log.d(TAG, "HashFinal: " + Numeric.toHexString(hashFinal));

            tvEstado.setText("HashFinal calculado\n\n Firmando confirmación...");

            // Firmar mensaje de confirmación con biometría
            firmarConfirmacion(hashFinal);

        } catch (Exception e) {
            Log.e(TAG, "Error al calcular hashFinal", e);
            tvEstado.setText("❌ Error al calcular hashFinal:\n" + e.getMessage());
            btnEscanear.setEnabled(true);
        }
    }

    private void firmarConfirmacion(byte[] hashFinal){
        this.walletManager.firmarConfirmacionPago(
                this.pagoId,
                this.hashPreparado,
                new FirmarMensajeCallback() {
                    @Override
                    public void onMensajeFirmado(byte[] firmaConfirmacion) {
                        runOnUiThread(()->{
                            tvEstado.setText("Confirmación firmada \n\n Generando QR 3");
                            generarQRConfirmacion(hashFinal, firmaConfirmacion);
                        });
                    }

                    @Override
                    public void onError(String mensaje) {
                        runOnUiThread(()->{
                            tvEstado.setText("Error al firmar: \n\n" +mensaje);
                            btnEscanear.setEnabled(false);
                        });
                    }
                }
        );
    }

    private void generarQRConfirmacion(byte[] hashFinal, byte[] firmaConfirmacion) {
        try {
            // Crear JSON con los datos para el QR #3
            JSONObject json = new JSONObject();
            json.put("pagoId", Numeric.toHexString(pagoId));
            json.put("hashFinal", Numeric.toHexString(hashFinal));
            json.put("firmaConfirmacion", Numeric.toHexString(firmaConfirmacion));

            String datosQR = json.toString();

            Log.d(TAG, "=== QR #3 GENERADO ===");
            Log.d(TAG, "JSON: " + datosQR);

            // Generar bitmap del QR
            Bitmap qrBitmap = generarQRBitmap(datosQR, 512, 512);

            // Mostrar QR
            ivQRConfirmacion.setImageBitmap(qrBitmap);
            layoutQRConfirmacion.setVisibility(View.VISIBLE);

            tvEstado.setText("CONFIRMACIÓN FIRMADA (OFFLINE)\n\n" +
                    "Muestra este QR al receptor\n" +
                    "para completar el pago");
            btnEscanear.setEnabled(false);

        } catch (JSONException | WriterException e) {
            Log.e(TAG, "Error al generar QR #3", e);
            tvEstado.setText("Error al generar QR:\n" + e.getMessage());
            btnEscanear.setEnabled(true);
        }
    }

    private Bitmap generarQRBitmap(String content, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y,
                        bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF
                );
            }
        }
        return bitmap;
    }

}

