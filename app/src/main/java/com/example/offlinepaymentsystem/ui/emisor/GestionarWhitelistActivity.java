package com.example.offlinepaymentsystem.ui.emisor;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.repository.RepositoryCallback;
import com.example.offlinepaymentsystem.data.repository.WhitelistRepository;
import com.example.offlinepaymentsystem.model.WhitelistItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class GestionarWhitelistActivity extends AppCompatActivity {

    private static final String TAG = "GestionarWhitelist";

    //UI
    private ListView lvReceptores;
    private TextView tvVacio;
    private Button btnAnadirReceptor;

    //Datos
    private WhitelistRepository repository;
    private WhitelistAdapter adapter;
    private List<WhitelistItem> receptores;

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
    }

    private void initData(){
        this.repository = new WhitelistRepository(this);
        this.receptores = new ArrayList<>();
        this.adapter = new WhitelistAdapter(this, receptores);
        this.lvReceptores.setAdapter(this.adapter);
    }

    private void setupListeners(){
        btnAnadirReceptor.setOnClickListener(v -> mostrarDialogoAnadir());
        //TODO: Si algún día me da tiempo metemos el eliminar de la whitelist.
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
                    }else {
                        tvVacio.setVisibility(View.GONE);
                        lvReceptores.setVisibility((View.VISIBLE));
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

}
