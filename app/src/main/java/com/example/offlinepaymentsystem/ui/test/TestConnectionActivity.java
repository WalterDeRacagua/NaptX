package com.example.offlinepaymentsystem.ui.test;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.blockchain.Web3Manager;

public class TestConnectionActivity extends AppCompatActivity {
    private static final String TAG = "TestConnection";

    private Web3Manager web3Manager;
    private TextView tvStatus;
    private Button btnTest;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_connection);

        tvStatus = findViewById(R.id.tvStatus);
        btnTest = findViewById(R.id.btnTest);

        // Inicializar Web3Manager
        tvStatus.setText("Inicializando Web3Manager...");
        web3Manager = new Web3Manager(this);
        tvStatus.append("\n✓ Web3Manager creado");

        // Botón de prueba
        btnTest.setOnClickListener(v -> probarConexion());
    }

    private void probarConexion(){
        tvStatus.setText("Probando conexión a Sepolia...\n");

        new Thread(()->{
            boolean conectado = web3Manager.verificarConexion();
            runOnUiThread(()-> {
                if (conectado){
                    tvStatus.append("Conexión exitosa a Sepolia");
                    tvStatus.append("El web3Manager esta funcionando correctamente");
                    Toast.makeText(this, "¡Conectado!", Toast.LENGTH_SHORT).show();
                } else {
                    tvStatus.append("Conexión a Sepolia fallida");
                    tvStatus.append("Verificar los logs");
                    Toast.makeText(this, "¡Error en la conexión!", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        if (web3Manager !=null){
            web3Manager.shutdown();
        }
    }
}
