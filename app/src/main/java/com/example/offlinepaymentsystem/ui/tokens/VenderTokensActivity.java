package com.example.offlinepaymentsystem.ui.tokens;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.blockchain.Web3Manager;
import com.example.offlinepaymentsystem.data.local.ObtenerCredentialsCallback;
import com.example.offlinepaymentsystem.data.local.WalletManager;
import com.example.offlinepaymentsystem.utils.Constants;
import com.example.offlinepaymentsystem.utils.CryptoUtils;

import org.web3j.crypto.Credentials;

@RequiresApi(api = Build.VERSION_CODES.P)
public class VenderTokensActivity extends AppCompatActivity {

    private static final String TAG = "VenderTokens";

    private TextView tvBalanceETH;
    private TextView tvBalanceNPTX;
    private EditText etCantidadNPTX;
    private TextView tvETHARecibir;
    private Button btnVender;
    private TextView tvEstado;

    private WalletManager walletManager;
    private Web3Manager web3Manager;
    private String addressUsuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vender_tokens);

        initViews();
        initData();
        setupListeners();
        cargarBalances();
    }

    private void initViews() {
        tvBalanceETH = findViewById(R.id.tvBalanceETH);
        tvBalanceNPTX = findViewById(R.id.tvBalanceNPTX);
        etCantidadNPTX = findViewById(R.id.etCantidadNPTX);
        tvETHARecibir = findViewById(R.id.tvETHARecibir);
        btnVender = findViewById(R.id.btnVender);
        tvEstado = findViewById(R.id.tvEstado);
    }

    private void initData() {
        walletManager = new WalletManager(this);
        web3Manager = new Web3Manager(this);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        addressUsuario = prefs.getString(Constants.KEY_WALLET_ADDRESS, null);
    }

    private void setupListeners() {
        // Actualizar conversión en tiempo real
        etCantidadNPTX.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarConversion();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnVender.setOnClickListener(v -> ejecutarVenta());
    }

    private void actualizarConversion() {
        String input = etCantidadNPTX.getText().toString();

        if (input.isEmpty()) {
            tvETHARecibir.setText("0 ETH");
            return;
        }

        try {
            double nptxAmount = Double.parseDouble(input);

            // Con precio 1 NPTX = 1 ETH, la conversión es 1:1
            double ethARecibir = nptxAmount;

            tvETHARecibir.setText(String.format("%.4f ETH", ethARecibir));

        } catch (NumberFormatException e) {
            tvETHARecibir.setText("0 ETH");
        }
    }

    private void cargarBalances() {
        new Thread(() -> {
            try {
                // Obtener balance ETH
                long balanceETHWei = web3Manager.obtenerBalanceETH(addressUsuario);
                String balanceETHStr = CryptoUtils.convertirWeiAETH(balanceETHWei);

                // Obtener balance NPTX
                long balanceNPTXWei = web3Manager.obtenerBalanceNPTX(addressUsuario);
                String balanceNPTXStr = CryptoUtils.convertirWeiAETH(balanceNPTXWei);

                runOnUiThread(() -> {
                    tvBalanceETH.setText("ETH: " + balanceETHStr);
                    tvBalanceNPTX.setText("NPTX: " + balanceNPTXStr);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error al cargar balances", e);
                runOnUiThread(() -> {
                    tvBalanceETH.setText("ETH: Error");
                    tvBalanceNPTX.setText("NPTX: Error");
                });
            }
        }).start();
    }

    private void ejecutarVenta() {
        String input = etCantidadNPTX.getText().toString();

        if (input.isEmpty()) {
            Toast.makeText(this, "Ingresa una cantidad", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double nptxAmount = Double.parseDouble(input);

            if (nptxAmount <= 0) {
                Toast.makeText(this, "Cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }

            // Convertir NPTX a unidades con decimales (10 decimales)
            long nptxWei = (long) (nptxAmount * 1e10);

            btnVender.setEnabled(false);
            tvEstado.setVisibility(View.VISIBLE);
            tvEstado.setText("Obteniendo credentials...");

            // Obtener credentials con biometría
            walletManager.obtenerCredentials(new ObtenerCredentialsCallback() {
                @Override
                public void onCredentialsObtenidos(Credentials credentials) {
                    runOnUiThread(() -> {
                        tvEstado.setText("Vendiendo tokens...");
                    });

                    // Ejecutar venta en hilo separado
                    new Thread(() -> {
                        try {
                            String txHash = web3Manager.venderTokens(credentials, nptxWei);

                            runOnUiThread(() -> {
                                tvEstado.setText("Venta exitosa!\n\nTX: " +
                                        txHash.substring(0, 20) + "...");

                                Toast.makeText(VenderTokensActivity.this,
                                        "¡Tokens vendidos!", Toast.LENGTH_LONG).show();

                                // Recargar balances
                                cargarBalances();

                                // Limpiar input
                                etCantidadNPTX.setText("");
                                btnVender.setEnabled(true);
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Error al vender tokens", e);
                            runOnUiThread(() -> {
                                tvEstado.setText("Error: " + e.getMessage());
                                btnVender.setEnabled(true);
                            });
                        }
                    }).start();
                }

                @Override
                public void onError(String mensaje) {
                    runOnUiThread(() -> {
                        tvEstado.setText("Error: " + mensaje);
                        btnVender.setEnabled(true);
                    });
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Cantidad inválida", Toast.LENGTH_SHORT).show();
        }
    }
}