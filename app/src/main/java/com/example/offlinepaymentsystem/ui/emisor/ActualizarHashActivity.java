package com.example.offlinepaymentsystem.ui.emisor;

import android.Manifest;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.utils.Constants;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONObject;

@RequiresApi(api = Build.VERSION_CODES.P)
public class ActualizarHashActivity extends AppCompatActivity {

    private static final String TAG = "ActualizarHash";
    private static final int CAMERA_PERMISSION_REQUEST_CODE= 100;

    private TextView tvEstado;
    private Button btnEscanearQR4;
    private Button btnCalcularManual;

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show();
                } else {
                    procesarQR4(result.getContents());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        this.setContentView(R.layout.activity_confirmar_pago);
        this.initViews();
        this.setupListeners();
    }

    private void initViews() {
        tvEstado = findViewById(R.id.tvEstado);
        btnEscanearQR4 = findViewById(R.id.btnEscanearQR4);
        btnCalcularManual = findViewById(R.id.btnCalcularManual);
    }

    private void setupListeners() {
        btnEscanearQR4.setOnClickListener(v -> verificarPermisoYEscanear());
        btnCalcularManual.setOnClickListener(v -> mostrarDialogoCalcularManual());
    }


    private void verificarPermisoYEscanear() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            escanearQR4();
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
                escanearQR4();
            } else {
                Toast.makeText(this,
                        "Se necesita permiso de cámara para escanear QR",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void escanearQR4() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Escanea el QR 4 del receptor");
        options.setCameraId(0);
        options.setBeepEnabled(true);
        options.setBarcodeImageEnabled(false);
        options.setOrientationLocked(true);

        barcodeLauncher.launch(options);
    }

    private void procesarQR4(String contenidoQR) {
        try {
            JSONObject json = new JSONObject(contenidoQR);

            if (!json.has("tipo") || !json.getString("tipo").equals("hashFinal")) {
                Toast.makeText(this, "QR inválido. Escanea el QR 4 del receptor",
                        Toast.LENGTH_LONG).show();
                return;
            }

            String hashFinal = json.getString("hashFinal");

            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("HASH_ACTUAL", hashFinal);
            editor.apply();

            tvEstado.setText("Hash actualizado correctamente\n\n" +
                    "Hash: " + hashFinal.substring(0, 20) + "...\n\n" +
                    "Tu wallet está sincronizada");

            Toast.makeText(this, "Wallet sincronizada", Toast.LENGTH_LONG).show();

            btnEscanearQR4.setEnabled(false);
            btnCalcularManual.setEnabled(false);
        } catch (Exception e) {
            Toast.makeText(this, "Error: QR inválido", Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarDialogoCalcularManual() {
        new AlertDialog.Builder(this)
                .setTitle("Advertencia")
                .setMessage("SOLO usa esta opción si:\n\n" +
                        "El receptor confirmó el pago\n" +
                        "El QR 4 se perdió\n" +
                        "No puedes conseguir el QR 4\n\n" +
                        "Si el pago NO se confirmó en blockchain, " +
                        "esto DESINCRONIZARÁ tu wallet.\n\n" +
                        "¿Continuar?")
                .setPositiveButton("Sí, estoy seguro", (dialog, which) -> {
                    ejecutarCalculoManual();
                })
                .setNegativeButton("Cancelar", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void ejecutarCalculoManual() {
        try {
            SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
            String hashFinalCalculado = prefs.getString("KEY_HASH_FINAL_CALCULADO", null);

            if (hashFinalCalculado == null) {
                throw new Exception("No se encontró hash calculado.\n\n" +
                        "Debes haber confirmado un pago antes de usar esta opción.");
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("HASH_ACTUAL", hashFinalCalculado);
            editor.apply();

            tvEstado.setText("Hash actualizado manualmente\n\n" +
                    "Hash: " + hashFinalCalculado.substring(0, 20) + "...\n\n" +
                    "IMPORTANTE: Verifica que el pago\nse confirmó en blockchain");

            Toast.makeText(this, "Hash actualizado", Toast.LENGTH_LONG).show();

            btnEscanearQR4.setEnabled(false);
            btnCalcularManual.setEnabled(false);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
