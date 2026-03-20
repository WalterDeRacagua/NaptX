package com.example.offlinepaymentsystem.ui.receptor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.utils.Constants;

public class ReceptorActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvAddress;
    private Button btnEscanearPago;
    private Button btnCompletarPago;

    private SharedPreferences prefs;
    private String address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receptor);

        // Inicializar vistas
        tvWelcome = findViewById(R.id.tvWelcome);
        tvAddress = findViewById(R.id.tvAddress);
        btnEscanearPago = findViewById(R.id.btnEscanearPago);
        btnCompletarPago = findViewById(R.id.btnCompletarPago);

        // Obtener address
        prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        address = prefs.getString(Constants.KEY_WALLET_ADDRESS, null);

        if (address == null) {
            tvWelcome.setText("No tienes wallet creada");
            tvAddress.setText("Crea una wallet primero desde el menú principal");
            btnEscanearPago.setEnabled(false);
            btnCompletarPago.setEnabled(false);
        } else {
            tvWelcome.setText("Bienvenido, Receptor");
            tvAddress.setText("Tu address:\n" + address);
        }

        // Botones
        btnEscanearPago.setOnClickListener(v -> onEscanearPagoClicked());
        btnCompletarPago.setOnClickListener(v -> onCompletarPagoClicked());
    }

    private void onEscanearPagoClicked() {
        Intent intent = new Intent(this, EscanearPagoActivity.class);
        startActivity(intent);
    }

    private void onCompletarPagoClicked() {
        Intent intent = new Intent(this, CompletarPagoActivity.class);
        startActivity(intent);
    }
}