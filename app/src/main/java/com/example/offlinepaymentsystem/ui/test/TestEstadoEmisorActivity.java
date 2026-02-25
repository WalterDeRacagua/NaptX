package com.example.offlinepaymentsystem.ui.test;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.blockchain.Web3Manager;
import com.example.offlinepaymentsystem.model.Emisor;

public class TestEstadoEmisorActivity extends AppCompatActivity {

    private static final String TAG = "TestEstadoEmisor";

    private Web3Manager web3Manager;
    private EditText etAddress;
    private Button btnConsultar;
    private TextView tvResultado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_estado_emisor);

        // Inicializar vistas
        etAddress = findViewById(R.id.etAddress);
        btnConsultar = findViewById(R.id.btnConsultar);
        tvResultado = findViewById(R.id.tvResultado);

        // Inicializar Web3Manager
        web3Manager = new Web3Manager(this);

        etAddress.setText("0xaB0060F161f19B62eE281Eee607F592B6f1d7007");

        // Botón consultar
        btnConsultar.setOnClickListener(v -> consultarEstado());
    }

    private void consultarEstado() {
        String address = etAddress.getText().toString().trim();

        // Validar que la address no esté vacía
        if (address.isEmpty()) {
            Toast.makeText(this, "Introduce una address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar formato básico (empieza con 0x y tiene 42 caracteres)
        if (!address.startsWith("0x") || address.length() != 42) {
            Toast.makeText(this, "Address inválida (debe empezar con 0x y tener 42 caracteres)", Toast.LENGTH_LONG).show();
            return;
        }

        tvResultado.setText("Consultando blockchain...\n");
        btnConsultar.setEnabled(false);

        // Ejecutar en hilo separado (red no puede ir en UI thread)
        new Thread(() -> {
            Emisor emisor = web3Manager.obtenerEstadoEmisor(address);

            // Volver al hilo UI para actualizar la interfaz
            runOnUiThread(() -> {
                btnConsultar.setEnabled(true);

                if (emisor != null) {
                    mostrarResultado(emisor);
                } else {
                    tvResultado.setText("Error al consultar el estado.\nVerifica los logs para más detalles.");
                    Toast.makeText(this, "Error al consultar", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void mostrarResultado(Emisor emisor) {
        StringBuilder resultado = new StringBuilder();

        resultado.append("=== ESTADO DEL EMISOR ===\n\n");

        resultado.append("Address:\n");
        resultado.append(emisor.getWalletAddress()).append("\n\n");

        resultado.append("Registrado:\n");
        resultado.append(emisor.isRegistrado() ? "✓ SÍ" : "✗ NO").append("\n\n");

        if (emisor.isRegistrado()) {
            resultado.append("Hash Actual:\n");
            resultado.append(emisor.getHashActualHex()).append("\n\n");

            resultado.append("Device ID:\n");
            resultado.append(emisor.getDeviceIdHex()).append("\n\n");

            resultado.append("Timestamp Último Pago:\n");
            if (emisor.getTimestampUltimoPago() == 0) {
                resultado.append("Nunca ha pagado\n");
            } else {
                resultado.append(emisor.getTimestampUltimoPago()).append("\n");
                // Convertir a fecha legible
                java.util.Date fecha = new java.util.Date(emisor.getTimestampUltimoPago() * 1000L);
                resultado.append("(").append(fecha.toString()).append(")\n");
            }
        } else {
            resultado.append("Este emisor NO está registrado en el contrato.\n");
        }

        tvResultado.setText(resultado.toString());

        Log.d(TAG, "Estado obtenido: " + emisor.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (web3Manager != null) {
            web3Manager.shutdown();
        }
    }
}