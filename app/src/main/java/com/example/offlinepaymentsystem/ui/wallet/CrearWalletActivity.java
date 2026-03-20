package com.example.offlinepaymentsystem.ui.wallet;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.local.CrearWalletCallback;
import com.example.offlinepaymentsystem.data.local.WalletManager;

public class CrearWalletActivity extends AppCompatActivity {

    private static final String TAG = "CrearWallet";
    private static final String PREFS_NAME = "WalletPrefs";
    private static final String KEY_WALLET_ADDRESS="WALLET_ADDRESS";
    private static final String KEY_HAS_WALLET = "has_wallet";
    private static final String ALIAS_WALLET = "naptx_wallet_key";

    private WalletManager walletManager;
    private SharedPreferences prefs;

    private TextView tvEstado;
    private TextView tvAddress;
    private Button btnCrearWallet;
    private Button btnContinuar;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crear_wallet);

        // Inicializar vistas
        tvEstado = findViewById(R.id.tvEstado);
        tvAddress = findViewById(R.id.tvAddress);
        btnCrearWallet = findViewById(R.id.btnCrearWallet);
        btnContinuar = findViewById(R.id.btnContinuar);

        // Inicializar SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Inicializar WalletManager
        this.walletManager = new WalletManager(this);

        // Verificar si ya existe wallet
        verificarWalletExistente();

        // Botones
        btnCrearWallet.setOnClickListener(v -> crearWallet());
        btnContinuar.setOnClickListener(v -> {
            Toast.makeText(this, "Continuando a la app...", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void verificarWalletExistente() {
        boolean existeWallet = walletManager.existeWallet();
        String addressGuardada = prefs.getString(KEY_WALLET_ADDRESS, null);

        if (addressGuardada != null && !existeWallet) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            addressGuardada = null;

            Toast.makeText(this,
                    "Datos inconsistentes detectados. Por favor, crea una nueva wallet.",
                    Toast.LENGTH_LONG).show();
        }

        if (existeWallet && addressGuardada != null) {
            mostrarWalletExistente(addressGuardada);
        } else {
            mostrarPantallaCreacion();
        }
    }

    private void mostrarWalletExistente(String address) {
        tvEstado.setText("Ya tienes una wallet creada");
        tvAddress.setText(address);
        tvAddress.setVisibility(TextView.VISIBLE);

        btnCrearWallet.setEnabled(false);
        btnCrearWallet.setText("Wallet ya creada");

        btnContinuar.setEnabled(true);

        Log.d(TAG, "Wallet existente: " + address);
    }

    private void mostrarPantallaCreacion() {
        tvEstado.setText("No tienes ninguna wallet.\nCrea una para empezar.");
        tvAddress.setVisibility(TextView.GONE);

        btnCrearWallet.setEnabled(true);
        btnCrearWallet.setText("Crear Wallet");

        btnContinuar.setEnabled(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void crearWallet() {
        tvEstado.setText("Creando wallet...\nSe pedirá tu huella.");
        btnCrearWallet.setEnabled(false);

        walletManager.crearWallet(new CrearWalletCallback() {
            @Override
            public void onWalletCreada(String address) {
                Log.d(TAG, "========================================");
                Log.d(TAG, "WALLET CREADA EXITOSAMENTE");
                Log.d(TAG, "========================================");
                Log.d(TAG, "ADDRESS: " + address);
                Log.d(TAG, "========================================");
                runOnUiThread(()->{
                    tvEstado.setText("Wallet creada exitosamente");
                    tvAddress.setText(address);
                    tvAddress.setVisibility(TextView.VISIBLE);

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(KEY_HAS_WALLET, true);
                    editor.putString(KEY_WALLET_ADDRESS, address);
                    editor.apply();

                    btnContinuar.setEnabled(true);
                    btnCrearWallet.setText("Creada exitosamente");

                    Log.d(TAG, "Wallet creada: " + address);
                    Toast.makeText(CrearWalletActivity.this, "¡Wallet creada!", Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(String mensaje) {
                runOnUiThread(() -> {
                    tvEstado.setText("Error al crear wallet:\n" + mensaje);
                    btnCrearWallet.setEnabled(true);
                    btnCrearWallet.setText("Reintentar");

                    Log.e(TAG, "Error: " + mensaje);
                    Toast.makeText(CrearWalletActivity.this, "Error: " + mensaje, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}