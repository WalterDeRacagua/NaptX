package com.example.offlinepaymentsystem.ui.emisor;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.blockchain.Web3Manager;
import com.example.offlinepaymentsystem.data.local.FirmarMensajeCallback;
import com.example.offlinepaymentsystem.data.local.ObtenerCredentialsCallback;
import com.example.offlinepaymentsystem.data.local.WalletManager;
import com.example.offlinepaymentsystem.data.repository.RepositoryCallback;
import com.example.offlinepaymentsystem.data.repository.WhitelistRepository;
import com.example.offlinepaymentsystem.model.WhitelistItem;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class GestionarWhitelistActivity extends AppCompatActivity {

    private static final String TAG = "GestionarWhitelist";
    private static final String PREFS_NAME = "WalletPrefs";
    private static final String KEY_WALLET_ADDRESS = "WALLET_ADDRESS";

    //UI
    private ListView lvReceptores;
    private TextView tvVacio;
    private Button btnAnadirReceptor;
    private Button btnSincronizarBlockchain;

    //Datos
    private WhitelistRepository repository;
    private WhitelistAdapter adapter;
    private List<WhitelistItem> receptores;

    //Blockchain
    private WalletManager walletManager;
    private Web3Manager web3Manager;
    private String addressEmisor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestionar_whitelist);

        initViews();
        initData();
        setupListeners();
        cargarReceptores();
    }

    private void initViews(){
        lvReceptores = findViewById(R.id.lvReceptores);
        tvVacio = findViewById(R.id.tvVacio);
        btnAnadirReceptor = findViewById(R.id.btnAnadirReceptor);
        btnSincronizarBlockchain = findViewById(R.id.btnSincronizarBlockchain);
    }

    private void initData(){
        this.repository = new WhitelistRepository(this);
        this.receptores = new ArrayList<>();
        this.adapter = new WhitelistAdapter(this, receptores);
        this.lvReceptores.setAdapter(this.adapter);

        this.walletManager = new WalletManager(this);
        this.web3Manager = new Web3Manager(this);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        this.addressEmisor = prefs.getString(KEY_WALLET_ADDRESS, null);
    }

    private void setupListeners(){
        this.btnAnadirReceptor.setOnClickListener(v -> mostrarDialogoAnadir());
        this.btnSincronizarBlockchain.setOnClickListener(v -> sincronizarConBlockchain());
    }


    private void cargarReceptores(){
        repository.obtenerTodos(new RepositoryCallback<List<WhitelistItem>>() {
            @Override
            public void onSuccess(List<WhitelistItem> result) {
                runOnUiThread(()->{
                    receptores.clear();
                    receptores.addAll(result);
                    adapter.actualizarDatos(receptores);

                    if (receptores.isEmpty()){
                        tvVacio.setVisibility(View.VISIBLE);
                        lvReceptores.setVisibility(View.GONE);
                        btnSincronizarBlockchain.setEnabled(false);
                    }else {
                        tvVacio.setVisibility(View.GONE);
                        lvReceptores.setVisibility((View.VISIBLE));
                        btnSincronizarBlockchain.setEnabled(true);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(GestionarWhitelistActivity.this,
                            "Error al cargar: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void mostrarDialogoAnadir(){
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_anadir_receptor, null);

        EditText etNombre = dialogView.findViewById(R.id.etNombre);
        EditText etDireccion = dialogView.findViewById(R.id.etDireccion);
        EditText etLimite = dialogView.findViewById(R.id.etLimite);

        AlertDialog dialog = new AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create();
        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(v-> dialog.dismiss());

        Button btnGuardar = dialogView.findViewById(R.id.btnGuardar);
        btnGuardar.setOnClickListener(v->{
            String nombre = etNombre.getText().toString().trim();
            String direccion = etDireccion.getText().toString().trim();
            String limiteStr = etLimite.getText().toString().trim();

            if (!validarDatosReceptor(direccion, limiteStr)){
                return;
            }

            double limiteETH = Double.parseDouble(limiteStr);
            BigDecimal limiteWeiDecimal = new BigDecimal(limiteETH).multiply(new BigDecimal("1000000000000000000"));
            long limiteWEI = limiteWeiDecimal.longValue();

            WhitelistItem item = new WhitelistItem(direccion,nombre,limiteWEI);

            repository.insertar(item, new RepositoryCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        Toast.makeText(GestionarWhitelistActivity.this,
                                "Receptor añadido", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        cargarReceptores();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        Toast.makeText(GestionarWhitelistActivity.this,
                                "Error " + message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });
        dialog.show();
    }

    private boolean validarDatosReceptor(String direccion, String limiteStr) {
        if (direccion.isEmpty()) {
            Toast.makeText(this, "Ingresa la dirección del receptor", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!direccion.startsWith("0x") || direccion.length() != 42) {
            Toast.makeText(this, "Dirección inválida (debe ser 0x... con 42 caracteres)",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (limiteStr.isEmpty()) {
            Toast.makeText(this, "Ingresa el límite", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double limite = Double.parseDouble(limiteStr);
            if (limite <= 0) {
                Toast.makeText(this, "El límite debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Límite inválido", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void sincronizarConBlockchain(){
        if (receptores.isEmpty()){
            Toast.makeText(this, "No hay receptores para sincronizar", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] receptoresArray = new String[receptores.size()];
        long[] limitesArray = new long[receptores.size()];

        for (int i = 0; i < receptores.size(); i++) {
            receptoresArray[i] = receptores.get(i).getDireccion();
            limitesArray[i] = receptores.get(i).getLimite();
        }

        long timestamp = System.currentTimeMillis()/1000;
        long nonce = generarNonce();

        btnSincronizarBlockchain.setEnabled(false);

        walletManager.firmarConfiguracionWhitelist(receptoresArray, limitesArray, timestamp, nonce,
                new FirmarMensajeCallback() {
                    @Override
                    public void onMensajeFirmado(byte[] firma) {
                        runOnUiThread(()->{
                            new Handler().postDelayed(() -> {
                                obtenerCredentialsYEnviar(receptoresArray, limitesArray, timestamp, nonce, firma);
                            }, 500);
                        });
                    }

                    @Override
                    public void onError(String mensaje) {
                        runOnUiThread(()->{
                            btnSincronizarBlockchain.setEnabled(true);
                        });
                    }
                });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void obtenerCredentialsYEnviar(String[] receptores, long[] limites,
                                           long timestamp, long nonce, byte[] firma) {

        walletManager.obtenerCredentials(new ObtenerCredentialsCallback() {
            @Override
            public void onCredentialsObtenidos(org.web3j.crypto.Credentials credentials) {
                new Thread(() -> {
                    enviarTransaccion(credentials, receptores, limites, timestamp, nonce, firma);
                }).start();
            }

            @Override
            public void onError(String mensaje) {
                runOnUiThread(() -> {
                    btnSincronizarBlockchain.setEnabled(true);
                });
            }
        });
    }

    private void enviarTransaccion(org.web3j.crypto.Credentials credentials,
                                   String[] receptores, long[] limites,
                                   long timestamp, long nonce, byte[] firma) {
        try {

            String txHash = web3Manager.configurarWhitelist(
                    credentials,
                    receptores,
                    limites,
                    timestamp,
                    nonce,
                    firma
            );

            runOnUiThread(() -> {
                Toast.makeText(this,
                        "Whitelist sincronizada con blockchain",
                        Toast.LENGTH_LONG).show();
                btnSincronizarBlockchain.setEnabled(true);
            });

        } catch (Exception e) {
            runOnUiThread(() -> {
                btnSincronizarBlockchain.setEnabled(true);
            });
        }
    }
    private long generarNonce() {
        SecureRandom random = new SecureRandom();
        return Math.abs(random.nextLong());
    }
}
