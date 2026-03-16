package com.example.offlinepaymentsystem.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.model.Emisor;
import com.example.offlinepaymentsystem.ui.emisor.EmisorActivity;
import com.example.offlinepaymentsystem.ui.receptor.EscanearPagoActivity;
import com.example.offlinepaymentsystem.ui.receptor.ReceptorActivity;
import com.example.offlinepaymentsystem.ui.test.TestConnectionActivity;
import com.example.offlinepaymentsystem.ui.test.TestEstadoEmisorActivity;
import com.example.offlinepaymentsystem.ui.wallet.CrearWalletActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnPagador;
    private Button btnReceptor;
    private Button btnSincronizar;
    private Button btnTestWeb3;
    private Button btnTestEstadoEmisor;
    private Button btnCrearWallet;
    private TextView tvEstadoConexion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Inicializar las vistas
        btnPagador = findViewById(R.id.btnPagador);
        btnReceptor = findViewById(R.id.btnReceptor);
        btnSincronizar = findViewById(R.id.btnSincronizar);
        btnTestWeb3 = findViewById(R.id.btnTestWeb3);
        btnTestEstadoEmisor = findViewById(R.id.btnTestEstadoEmisor);
        btnCrearWallet = findViewById(R.id.btnCrearWallet);
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

        this.btnSincronizar.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                onSyncronizeClicked();
            }
        });

        this.btnTestWeb3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){onTestWeb3Clicked();}
        });

        this.btnTestEstadoEmisor.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){onTestEstadoEmisorClicked();}
        });



        this.btnCrearWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {onCrearWalletClicked();}
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

    private void onTestWeb3Clicked(){
        Intent intent = new Intent(this, TestConnectionActivity.class);
        startActivity(intent);
    }

    private void onTestEstadoEmisorClicked(){
        Intent intent = new Intent(this, TestEstadoEmisorActivity.class);
        startActivity(intent);
    }

    private void onCrearWalletClicked(){
        Intent intent = new Intent(this, CrearWalletActivity.class);
        startActivity(intent);
    }
}