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

import java.math.BigDecimal;

@RequiresApi(api = Build.VERSION_CODES.P)
public class ComprarTokensActivity extends AppCompatActivity {

    private static final String TAG = "ComprarTokens";

    private TextView tvBalanceETH;
    private TextView tvBalanceNPTX;
    private EditText etCantidadETH;
    private TextView tvTokensARecibir;
    private Button btnComprar;
    private TextView tvEstado;

    private WalletManager walletManager;
    private Web3Manager web3Manager;
    private String addressEmisor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comprar_tokens);

        initViews();
        initData();
        setupListeners();
        cargarBalances();
    }

    private void initViews() {
        tvBalanceETH = findViewById(R.id.tvBalanceETH);
        tvBalanceNPTX = findViewById(R.id.tvBalanceNPTX);
        etCantidadETH = findViewById(R.id.etCantidadETH);
        tvTokensARecibir = findViewById(R.id.tvTokensARecibir);
        btnComprar = findViewById(R.id.btnComprar);
        tvEstado = findViewById(R.id.tvEstado);
    }

    private void initData() {
        walletManager = new WalletManager(this);
        web3Manager = new Web3Manager(this);

        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        addressEmisor = prefs.getString(Constants.KEY_WALLET_ADDRESS, null);
    }

    private void setupListeners() {
        // Actualizar conversión en tiempo real
        etCantidadETH.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarConversion();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnComprar.setOnClickListener(v -> ejecutarCompra());
    }

    private void actualizarConversion() {
        String input = etCantidadETH.getText().toString();

        if (input.isEmpty()) {
            tvTokensARecibir.setText("0 NPTX");
            return;
        }

        try {
            double ethAmount = Double.parseDouble(input);

            // Con precio 1 NPTX = 1 ETH, la conversión es 1:1
            double tokensARecibir = ethAmount;

            tvTokensARecibir.setText(String.format("%.4f NPTX", tokensARecibir));

        } catch (NumberFormatException e) {
            tvTokensARecibir.setText("0 NPTX");
        }
    }

    private void cargarBalances() {
        new Thread(() -> {
            try {
                // Obtener balance ETH
                long balanceETHWei = web3Manager.obtenerBalanceETH(addressEmisor);
                String balanceETHStr = CryptoUtils.convertirWeiAETH(balanceETHWei);

                // Obtener balance NPTX
                long balanceNPTXWei = web3Manager.obtenerBalanceNPTX(addressEmisor);
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

    private void ejecutarCompra() {
        String input = etCantidadETH.getText().toString();

        if (input.isEmpty()) {
            Toast.makeText(this, "Ingresa una cantidad", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            BigDecimal ethAmount = new BigDecimal(input);

            if (ethAmount.compareTo(BigDecimal.ZERO) <= 0) {
                Toast.makeText(this, "Cantidad debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return;
            }

            BigDecimal weiPerEth = new BigDecimal("1000000000000000000"); // 1e18
            BigDecimal ethWeiDecimal = ethAmount.multiply(weiPerEth);
            long ethWei = ethWeiDecimal.longValue();

            btnComprar.setEnabled(false);
            tvEstado.setVisibility(View.VISIBLE);
            tvEstado.setText("Obteniendo credentials...");

            walletManager.obtenerCredentials(new ObtenerCredentialsCallback() {
                @Override
                public void onCredentialsObtenidos(Credentials credentials) {
                    runOnUiThread(() -> {
                        tvEstado.setText("Comprando tokens...");
                    });

                    new Thread(() -> {
                        try {
                            String txHash = web3Manager.comprarTokens(credentials, ethWei);

                            runOnUiThread(() -> {
                                tvEstado.setText("Compra exitosa!\n\nTX: " +
                                        txHash.substring(0, 20) + "...");

                                Toast.makeText(ComprarTokensActivity.this,
                                        "¡Tokens comprados!", Toast.LENGTH_LONG).show();

                                // Recargar balances
                                cargarBalances();

                                // Limpiar input
                                etCantidadETH.setText("");
                                btnComprar.setEnabled(true);
                            });

                        } catch (Exception e) {
                            Log.e(TAG, "Error al comprar tokens", e);
                            runOnUiThread(() -> {
                                tvEstado.setText("Error: " + e.getMessage());
                                btnComprar.setEnabled(true);
                            });
                        }
                    }).start();
                }

                @Override
                public void onError(String mensaje) {
                    runOnUiThread(() -> {
                        tvEstado.setText("Error: " + mensaje);
                        btnComprar.setEnabled(true);
                    });
                }
            });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Cantidad inválida", Toast.LENGTH_SHORT).show();
        }
    }
}