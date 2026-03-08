package com.example.offlinepaymentsystem.ui.emisor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;

public class EmisorActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "WalletPrefs";
    private static final String KEY_WALLET_ADDRESS="WALLET_ADDRESS";

    private TextView tvWelcome;
    private TextView tvAddress;
    private Button btnRegistrar;
    private Button btnGestionarWhitelist;
    private Button btnHacerPago;
    private Button btnVerEstado;

    private SharedPreferences prefs;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emisor);

        // Inicializar vistas
        tvWelcome = findViewById(R.id.tvWelcome);
        tvAddress = findViewById(R.id.tvAddress);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnGestionarWhitelist = findViewById(R.id.btnGestionarWhitelist);
        btnHacerPago = findViewById(R.id.btnHacerPago);
        btnVerEstado = findViewById(R.id.btnVerEstado);

        // Obtener address
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        address = prefs.getString(KEY_WALLET_ADDRESS, null);

        if (address == null) {
            tvWelcome.setText("No tienes wallet creada");
            tvAddress.setText("Crea una wallet primero desde el menú principal");
            btnRegistrar.setEnabled(false);
            btnGestionarWhitelist.setEnabled(false);
            btnHacerPago.setEnabled(false);
            btnVerEstado.setEnabled(false);
        } else {
            tvWelcome.setText("Bienvenido, Emisor");
            tvAddress.setText("Tu address:\n" + address);
        }

        // Botones
        btnRegistrar.setOnClickListener(v -> onRegistrarClicked());
        btnGestionarWhitelist.setOnClickListener(v -> onGestionarWhitelistClicked());
        btnHacerPago.setOnClickListener(v -> onHacerPagoClicked());
        btnVerEstado.setOnClickListener(v -> onVerEstadoClicked());
    }

    private void onRegistrarClicked() {
        Intent intent = new Intent(this, RegistrarEmisorActivity.class);
        startActivity(intent);
    }

    private void onGestionarWhitelistClicked() {
        Intent intent = new Intent(this, GestionarWhitelistActivity.class);
        startActivity(intent);
    }

    private void onHacerPagoClicked() {
        Intent intent = new Intent(this, GenerarPagoActivity.class);  // ← CAMBIADO
        startActivity(intent);
    }

    private void onVerEstadoClicked() {
        Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show();
        // TODO: Mostrar estado del emisor
    }
}