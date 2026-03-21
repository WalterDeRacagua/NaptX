package com.example.offlinepaymentsystem.ui.emisor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.utils.Constants;

public class EmisorActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvAddress;
    private Button btnRegistrar;
    private Button btnGestionarWhitelist;
    private Button btnHacerPago;
    private Button btnConfirmarPago;
    private Button btnActualizarHash;

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
        this.btnActualizarHash = findViewById(R.id.btnActualizarHash);

        // Obtener address
        this.prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        this.address = prefs.getString(Constants.KEY_WALLET_ADDRESS, null);

        if (this.address == null) {
            this.tvWelcome.setText("No tienes wallet creada");
            this.tvAddress.setText("Crea una wallet primero desde el menú principal");
            this.btnRegistrar.setEnabled(false);
            this.btnGestionarWhitelist.setEnabled(false);
            this.btnHacerPago.setEnabled(false);
            this.btnConfirmarPago.setEnabled(false);
            this.btnActualizarHash.setEnabled(false);
        } else {
            this.tvWelcome.setText("Bienvenido, Emisor");
            this.tvAddress.setText("Tu address:\n" + address);
        }

        this.btnRegistrar.setOnClickListener(v -> onRegistrarClicked());
        this.btnGestionarWhitelist.setOnClickListener(v -> onGestionarWhitelistClicked());
        this.btnHacerPago.setOnClickListener(v -> onHacerPagoClicked());
        this.btnConfirmarPago.setOnClickListener(v -> onConfirmarPagoClicked());
        this.btnActualizarHash.setOnClickListener(v -> onActualizarHashClicked());
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

    private void onActualizarHashClicked(){
        Intent intent = new Intent(this, ActualizarHashActivity.class);
        startActivity(intent);
    }

}