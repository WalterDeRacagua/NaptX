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
    private Button btnConfirmarPago;
    private Button btnVerEstado;

    private SharedPreferences prefs;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_emisor);

        // Inicializar vistas
        this.tvWelcome = findViewById(R.id.tvWelcome);
        this.tvAddress = findViewById(R.id.tvAddress);
        this.btnRegistrar = findViewById(R.id.btnRegistrar);
        this.btnGestionarWhitelist = findViewById(R.id.btnGestionarWhitelist);
        this.btnHacerPago = findViewById(R.id.btnHacerPago);
        this.btnConfirmarPago = findViewById(R.id.btnConfirmarPago);
        this.btnVerEstado = findViewById(R.id.btnVerEstado);

        // Obtener address
        this.prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        this.address = prefs.getString(KEY_WALLET_ADDRESS, null);

        if (this.address == null) {
            this.tvWelcome.setText("No tienes wallet creada");
            this.tvAddress.setText("Crea una wallet primero desde el menú principal");
            this.btnRegistrar.setEnabled(false);
            this.btnGestionarWhitelist.setEnabled(false);
            this.btnHacerPago.setEnabled(false);
            this.btnConfirmarPago.setEnabled(false);
            this.btnVerEstado.setEnabled(false);
        } else {
            this.tvWelcome.setText("Bienvenido, Emisor");
            this.tvAddress.setText("Tu address:\n" + address);
        }

        // Botones
        this.btnRegistrar.setOnClickListener(v -> onRegistrarClicked());
        this.btnGestionarWhitelist.setOnClickListener(v -> onGestionarWhitelistClicked());
        this.btnHacerPago.setOnClickListener(v -> onHacerPagoClicked());
        this.btnConfirmarPago.setOnClickListener(v -> onConfirmarPagoClicked());
        this.btnVerEstado.setOnClickListener(v -> onVerEstadoClicked());
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
        Intent intent = new Intent(this, GenerarPagoActivity.class);
        startActivity(intent);
    }

    private void onConfirmarPagoClicked() {
        Intent intent = new Intent(this, ConfirmarPagoActivity.class);
        startActivity(intent);
    }

    private void onVerEstadoClicked() {
        Toast.makeText(this, "Función en desarrollo", Toast.LENGTH_SHORT).show();
        // TODO: Mostrar estado del emisor
    }
}