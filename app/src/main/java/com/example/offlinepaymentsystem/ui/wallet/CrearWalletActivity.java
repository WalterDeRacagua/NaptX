package com.example.offlinepaymentsystem.ui.wallet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.local.KeystoreManager;

public class CrearWalletActivity extends AppCompatActivity {

    private static final String TAG = "CrearWallet";
    private static final String PREFS_NAME = "NaptxPrefs";
    private static final String KEY_WALLET_ADDRESS = "wallet_address";
    private static final String KEY_HAS_WALLET = "has_wallet";
    private static final String ALIAS_WALLET = "naptx_wallet_key";

    private KeystoreManager keystoreManager;
    private SharedPreferences prefs;

    private TextView tvEstado;
    private TextView tvAddress;
    private Button btnCrearWallet;
    private Button btnContinuar;

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

        // Inicializar KeystoreManager
        try {
            keystoreManager = new KeystoreManager(this);
        } catch (Exception e) {
            tvEstado.setText("Error al inicializar KeystoreManager");
            Log.e(TAG, "Error", e);
            return;
        }

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
        boolean tieneWallet = prefs.getBoolean(KEY_HAS_WALLET, false);
        String addressGuardada = prefs.getString(KEY_WALLET_ADDRESS, null);

        if (tieneWallet && addressGuardada != null) {
            // Ya tiene wallet
            mostrarWalletExistente(addressGuardada);
        } else {
            // No tiene wallet
            mostrarPantallaCreacion();
        }
    }

    private void mostrarWalletExistente(String address) {
        tvEstado.setText("Ya tienes una wallet creada");
        tvAddress.setText(address);
        tvAddress.setVisibility(TextView.VISIBLE);

        btnCrearWallet.setEnabled(false);
        btnCrearWallet.setText("✓ Wallet ya creada");

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

    private void crearWallet() {
        tvEstado.setText("Creando wallet...\nSe pedirá tu huella dactilar.");
        btnCrearWallet.setEnabled(false);

        try {
            // 1. Generar claves
            keystoreManager.generarClaves(ALIAS_WALLET);

            tvEstado.append("\n✓ Claves generadas");

            // 2. Obtener address
            String address = keystoreManager.obtenerAddress(ALIAS_WALLET);

            tvEstado.append("\n✓ Address obtenida");
            tvEstado.append("\n\n¡Wallet creada exitosamente!");

            // 3. Mostrar address
            tvAddress.setText(address);
            tvAddress.setVisibility(TextView.VISIBLE);

            // 4. Guardar en SharedPreferences
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_HAS_WALLET, true);
            editor.putString(KEY_WALLET_ADDRESS, address);
            editor.apply();

            tvEstado.append("\n✓ Wallet guardada");

            // 5. Habilitar continuar
            btnContinuar.setEnabled(true);

            // 6. Cambiar botón crear
            btnCrearWallet.setText("✓ Wallet creada");

            Log.d(TAG, "Wallet creada: " + address);
            Toast.makeText(this, "¡Wallet creada!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            tvEstado.setText("Error al crear wallet: " + e.getMessage());
            btnCrearWallet.setEnabled(true);
            btnCrearWallet.setText("🔐 Reintentar");
            Log.e(TAG, "Error al crear wallet", e);
            Toast.makeText(this, "Error al crear wallet", Toast.LENGTH_SHORT).show();
        }
    }
}