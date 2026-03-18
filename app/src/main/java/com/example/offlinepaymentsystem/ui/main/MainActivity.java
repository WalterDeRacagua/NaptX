package com.example.offlinepaymentsystem.ui.main;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.blockchain.Web3Manager;
import com.example.offlinepaymentsystem.ui.emisor.EmisorActivity;
import com.example.offlinepaymentsystem.ui.receptor.ReceptorActivity;
import com.example.offlinepaymentsystem.ui.wallet.CrearWalletActivity;
import com.example.offlinepaymentsystem.utils.CryptoUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MAIN";
    private Button btnPagador;
    private Button btnReceptor;
    private Button btnCrearWallet;
    private Button btnVerEstado;
    private TextView tvEstadoConexion;

    private Web3Manager web3Manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.web3Manager = new Web3Manager(this);

        //Inicializar las vistas
        this.btnPagador = findViewById(R.id.btnPagador);
        this.btnReceptor = findViewById(R.id.btnReceptor);
        this.btnCrearWallet = findViewById(R.id.btnCrearWallet);
        this.btnVerEstado = findViewById(R.id.btnVerEstado);
        this.tvEstadoConexion = findViewById(R.id.tvEstadoConexion);

        this.btnPagador.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onPayerClicked();
            }
        });
        this.btnReceptor.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onReceiverClicked();
            }
        });

        this.btnCrearWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {onCrearWalletClicked();}
        });

        this.btnVerEstado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {onConsultarEstado();}
        });
    }

    private void onPayerClicked() {
        Intent intent = new Intent(this, EmisorActivity.class);
        startActivity(intent);
    }
    private void onReceiverClicked() {
        Intent intent = new Intent(this, ReceptorActivity.class);
        startActivity(intent);
    }

    private void onSyncronizeClicked(){
        Toast.makeText(this, "Sincronizando...", Toast.LENGTH_SHORT).show();

        tvEstadoConexion.setText("Sincronizando");
        tvEstadoConexion.setTextColor(ContextCompat.getColor(this, R.color.status_connecting));

        //Simulamos una sincronización (4 segundos)
        tvEstadoConexion.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvEstadoConexion.setText("Conectado a Sepolia");
                tvEstadoConexion.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.status_connected));
            }
        }, 4000);
    }

    private void onCrearWalletClicked(){
        Intent intent = new Intent(this, CrearWalletActivity.class);
        startActivity(intent);
    }


    private void onConsultarEstado() {
        Log.d(TAG, "=== CONSULTAR ESTADO INICIADO ===");

        // Mostrar dialog de carga
        AlertDialog dialogCarga = new AlertDialog.Builder(this)
                .setMessage("Consultando estado...")
                .setCancelable(false)
                .create();
        dialogCarga.show();

        Log.d(TAG, "Dialog de carga mostrado");

        // Consultar en background
        new Thread(() -> {
            try {
                Log.d(TAG, "Thread iniciado");

                // Obtener address
                SharedPreferences prefs = getSharedPreferences("WalletPrefs", MODE_PRIVATE);
                String address = prefs.getString("WALLET_ADDRESS", null);
                String hashActual = prefs.getString("HASH_ACTUAL", "No disponible");

                Log.d(TAG, "Address: " + address);
                Log.d(TAG, "HashActual: " + hashActual);

                if (address == null) {
                    Log.e(TAG, "Address es null");
                    runOnUiThread(() -> {
                        dialogCarga.dismiss();
                        Toast.makeText(this, "No hay wallet creada", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Log.d(TAG, "Consultando balance NPTX...");
                // Consultar balances
                long balanceNPTX = web3Manager.obtenerBalanceNPTX(address);
                Log.d(TAG, "Balance NPTX obtenido: " + balanceNPTX);

                Log.d(TAG, "Consultando balance ETH...");
                long balanceETH = web3Manager.obtenerBalanceETH(address);
                Log.d(TAG, "Balance ETH obtenido: " + balanceETH);

                // Convertir a formato legible
                String balanceNPTXStr = CryptoUtils.convertirWeiAETH(balanceNPTX);
                String balanceETHStr = CryptoUtils.convertirWeiAETH(balanceETH);

                Log.d(TAG, "Balances convertidos. Mostrando dialog...");

                // Mostrar en UI
                runOnUiThread(() -> {
                    dialogCarga.dismiss();
                    mostrarDialogEstado(address, balanceNPTXStr, balanceETHStr, hashActual);
                });

            } catch (Exception e) {
                Log.e(TAG, "ERROR en consultarEstado: " + e.getMessage(), e);
                e.printStackTrace();
                runOnUiThread(() -> {
                    dialogCarga.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Mostrar dialog con el estado
     */
    private void mostrarDialogEstado(String address, String balanceNPTX,
                                     String balanceETH, String hashActual) {

        // Manejar hashActual null o corto
        String hashMostrar;
        if (hashActual == null || hashActual.equals("No disponible")) {
            hashMostrar = "No disponible";
        } else if (hashActual.length() > 20) {
            hashMostrar = hashActual.substring(0, 20) + "...";
        } else {
            hashMostrar = hashActual;
        }

        String mensaje = "Wallet:\n" + address + "\n\n" +
                "Balance NPTX:\n" + balanceNPTX + " NPTX\n\n" +
                "Balance SepoliaETH:\n" + balanceETH + " ETH\n\n" +
                "HashActual:\n" + hashMostrar;

        new AlertDialog.Builder(this)
                .setTitle("Estado del Emisor")
                .setMessage(mensaje)
                .setPositiveButton("OK", null)
                .setNeutralButton("Ver en Etherscan", (dialog, which) -> {
                    abrirEtherscan(address);
                })
                .show();
    }

    private void abrirEtherscan(String address) {
        String url = "https://sepolia.etherscan.io/address/" + address;
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}