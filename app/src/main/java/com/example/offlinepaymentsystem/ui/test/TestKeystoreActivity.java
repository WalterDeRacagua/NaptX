package com.example.offlinepaymentsystem.ui.test;
import com.example.offlinepaymentsystem.data.local.FirmaCallback;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.local.KeystoreManager;

public class TestKeystoreActivity extends AppCompatActivity {

    private static final String TAG = "TestKeystore";
    private static final String ALIAS = "naptx_test_key";

    private KeystoreManager keystoreManager;
    private TextView tvStatus;
    private Button btnGenerarClaves;
    private Button btnObtenerAddress;
    private Button btnFirmarMensaje;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_keystore);

        // Inicializar vistas
        tvStatus = findViewById(R.id.tvStatus);
        btnGenerarClaves = findViewById(R.id.btnGenerarClaves);
        btnObtenerAddress = findViewById(R.id.btnObtenerAddress);
        btnFirmarMensaje = findViewById(R.id.btnFirmarMensaje);

        // Inicializar KeystoreManager
        try {
            keystoreManager = new KeystoreManager(this);
            tvStatus.append("✓ KeystoreManager inicializado\n\n");
        } catch (Exception e) {
            tvStatus.append("✗ Error al inicializar KeystoreManager\n");
            Log.e(TAG, "Error", e);
            return;
        }

        // Verificar si ya existen claves
        verificarClavesExistentes();

        // Botones
        btnGenerarClaves.setOnClickListener(v -> generarClaves());
        btnObtenerAddress.setOnClickListener(v -> obtenerAddress());
        btnFirmarMensaje.setOnClickListener(v -> firmarMensaje());
    }

    private void verificarClavesExistentes() {
        try {
            boolean existen = keystoreManager.claveExiste(ALIAS);
            if (existen) {
                tvStatus.append("⚠️ Ya existen claves con alias: " + ALIAS + "\n");
                tvStatus.append("Puedes obtener la address o firmar.\n\n");
                btnObtenerAddress.setEnabled(true);
                btnFirmarMensaje.setEnabled(true);
            } else {
                tvStatus.append("ℹ️ No hay claves generadas aún.\n");
                tvStatus.append("Genera claves primero.\n\n");
                btnObtenerAddress.setEnabled(false);
                btnFirmarMensaje.setEnabled(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al verificar claves", e);
        }
    }

    private void generarClaves() {
        tvStatus.append("\n--- GENERANDO CLAVES ---\n");
        tvStatus.append("Se abrirá el diálogo de biometría...\n");

        try {
            // Esto pedirá la huella dactilar
            keystoreManager.generarClaves(ALIAS);

            tvStatus.append("✓ Claves generadas exitosamente\n");
            tvStatus.append("Alias: " + ALIAS + "\n\n");

            Toast.makeText(this, "¡Claves generadas!", Toast.LENGTH_SHORT).show();

            // Habilitar otros botones
            btnObtenerAddress.setEnabled(true);
            btnFirmarMensaje.setEnabled(true);

        } catch (Exception e) {
            tvStatus.append("✗ Error al generar claves\n");
            tvStatus.append("Error: " + e.getMessage() + "\n\n");
            Log.e(TAG, "Error al generar claves", e);
            Toast.makeText(this, "Error al generar claves", Toast.LENGTH_SHORT).show();
        }
    }

    private void obtenerAddress() {
        tvStatus.append("\n--- OBTENIENDO ADDRESS ---\n");

        try {
            String address = keystoreManager.obtenerAddress(ALIAS);

            tvStatus.append("✓ Address obtenida:\n");
            tvStatus.append(address + "\n\n");

            Log.d(TAG, "Address: " + address);
            Toast.makeText(this, "Address copiada a logs", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            tvStatus.append("✗ Error al obtener address\n");
            tvStatus.append("Error: " + e.getMessage() + "\n\n");
            Log.e(TAG, "Error al obtener address", e);
        }
    }

    private void firmarMensaje() {
        tvStatus.append("\n--- FIRMANDO MENSAJE ---\n");
        tvStatus.append("Se abrirá el diálogo de biometría...\n");

        // Mensaje de prueba
        String mensajeTexto = "Hola desde NaptX - Test de firma";
        byte[] mensaje = mensajeTexto.getBytes();

        tvStatus.append("Mensaje: " + mensajeTexto + "\n");

        try {
            // Esto pedirá la huella dactilar
            keystoreManager.firmarMensaje(ALIAS, mensaje, new FirmaCallback() {
                @Override
                public void onFirmaExitosa(byte[] firma) {
                    // Esto se ejecuta cuando la firma es exitosa
                    runOnUiThread(() -> {
                        tvStatus.append("✓ Mensaje firmado exitosamente\n");
                        tvStatus.append("Longitud firma: " + firma.length + " bytes\n");

                        // Mostrar firma en hexadecimal
                        StringBuilder firmaHex = new StringBuilder("0x");
                        for (int i = 0; i < Math.min(firma.length, 32); i++) {
                            firmaHex.append(String.format("%02x", firma[i]));
                        }
                        if (firma.length > 32) {
                            firmaHex.append("...");
                        }

                        tvStatus.append("Firma: " + firmaHex.toString() + "\n\n");

                        Log.d(TAG, "Firma generada: " + firma.length + " bytes");
                        Toast.makeText(TestKeystoreActivity.this, "¡Mensaje firmado!", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(String mensaje) {
                    // Esto se ejecuta cuando hay error
                    runOnUiThread(() -> {
                        tvStatus.append("✗ Error al firmar mensaje\n");
                        tvStatus.append("Error: " + mensaje + "\n\n");
                        Log.e(TAG, "Error al firmar: " + mensaje);
                        Toast.makeText(TestKeystoreActivity.this, "Error: " + mensaje, Toast.LENGTH_SHORT).show();
                    });
                }
            });

        } catch (Exception e) {
            tvStatus.append("✗ Error al iniciar firma\n");
            tvStatus.append("Error: " + e.getMessage() + "\n\n");
            Log.e(TAG, "Error al iniciar firma", e);
            Toast.makeText(this, "Error al iniciar firma", Toast.LENGTH_SHORT).show();
        }
    }
}