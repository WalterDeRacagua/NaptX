package com.example.offlinepaymentsystem;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnPagador;
    private Button btnReceptor;
    private Button btnWallets;
    private Button btnSincronizar;
    private TextView tvEstadoConexion;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Inicializar las vistas
        btnPagador = findViewById(R.id.btnPagador);
        btnReceptor = findViewById(R.id.btnReceptor);
        btnWallets = findViewById(R.id.btnWallets);
        btnSincronizar = findViewById(R.id.btnSincronizar);
        tvEstadoConexion = findViewById(R.id.tvEstadoConexion);

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
        this.btnWallets.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onWalletsClicked();
            }
        });
        this.btnSincronizar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onSyncronizeClicked();
            }
        });
    }

    private void onPayerClicked() {
        //Toast para que aparezca un pequeño mensaje emergente en la pantalla
        Toast.makeText(this, "Modo pagador", Toast.LENGTH_SHORT).show();
    }
    private void onReceiverClicked() {
        //Toast para que aparezca un pequeño mensaje emergente en la pantalla
        Toast.makeText(this, "Modo receptor", Toast.LENGTH_SHORT).show();
    }

    private void onWalletsClicked() {
        //Toast para que aparezca un pequeño mensaje emergente en la pantalla
        Toast.makeText(this, "Ver mis wallets", Toast.LENGTH_SHORT).show();
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
}