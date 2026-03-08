package com.example.offlinepaymentsystem.ui.emisor;

import com.example.offlinepaymentsystem.data.local.FirmarMensajeCallback;
import com.example.offlinepaymentsystem.data.repository.RepositoryCallback;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.offlinepaymentsystem.R;
import com.example.offlinepaymentsystem.data.local.DeviceIdManager;
import com.example.offlinepaymentsystem.data.local.WalletManager;
import com.example.offlinepaymentsystem.data.repository.PagoRepository;
import com.example.offlinepaymentsystem.data.repository.WhitelistRepository;
import com.example.offlinepaymentsystem.model.PagoPendiente;
import com.example.offlinepaymentsystem.model.WhitelistItem;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public class GenerarPagoActivity extends AppCompatActivity {

    private static final String TAG = "GenerarPagoActivity";
    private static final String PREFS_NAME = "WalletPrefs";
    private static final String KEY_WALLET_ADDRESS="WALLET_ADDRESS";

    //UI
    private TextView tvEmisorAddress;
    private Spinner spinnerReceptor;
    private EditText etMonto;
    private TextView tvMontoWei;
    private TextView tvEstado;
    private Button btnGenerarPago;
    private LinearLayout layoutQR;
    private ImageView ivQRCode;
    private Button btnCopiarDatos;

    //Datos
    private String addressEmisor;
    private byte[] deviceId;
    private WalletManager walletManager;
    private PagoRepository pagoRepository;
    private WhitelistRepository whitelistRepository;

    private List<WhitelistItem> receptoresWhitelist;
    private ReceptorSpinnerAdapter spinnerAdapter;

    //Pago actual
    private PagoPendiente pagoActual;
    private String datosQR;


    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generar_pago);

        initViews();
        initData();
        setupListeners();
    }

    private void initViews() {
        tvEmisorAddress = findViewById(R.id.tvEmisorAddress);
        spinnerReceptor = findViewById(R.id.spinnerReceptor);
        etMonto = findViewById(R.id.etMonto);
        tvMontoWei = findViewById(R.id.tvMontoWei);
        tvEstado = findViewById(R.id.tvEstado);
        btnGenerarPago = findViewById(R.id.btnGenerarPago);
        layoutQR = findViewById(R.id.layoutQR);
        ivQRCode = findViewById(R.id.ivQRCode);
        btnCopiarDatos = findViewById(R.id.btnCopiarDatos);
    }

    private void initData() {
        // Obtener address del emisor
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        addressEmisor = prefs.getString(KEY_WALLET_ADDRESS, null);

        if (addressEmisor == null) {
            btnGenerarPago.setEnabled(false);
            return;
        }

        tvEmisorAddress.setText(addressEmisor);

        deviceId = DeviceIdManager.obtenerDeviceId(this);
        walletManager = new WalletManager(this);
        pagoRepository = new PagoRepository(this);
        whitelistRepository = new WhitelistRepository(this);

        cargarReceptoresWhitelist();
    }

    private void cargarReceptoresWhitelist() {
        whitelistRepository.obtenerTodos(new RepositoryCallback<List<WhitelistItem>>() {
            @Override
            public void onSuccess(List<WhitelistItem> result) {
                runOnUiThread(() -> {
                    receptoresWhitelist = result;

                    if (receptoresWhitelist.isEmpty()) {
                        // No hay receptores en whitelist
                        tvEstado.setText("No hay receptores autorizados\nAñade receptores en Gestionar Whitelist");
                        btnGenerarPago.setEnabled(false);
                        spinnerReceptor.setEnabled(false);
                    } else {
                        // Poblar spinner
                        spinnerAdapter = new ReceptorSpinnerAdapter(
                                GenerarPagoActivity.this,
                                receptoresWhitelist
                        );
                        spinnerReceptor.setAdapter(spinnerAdapter);
                        btnGenerarPago.setEnabled(true);
                        spinnerReceptor.setEnabled(true);
                        tvEstado.setText("Introduce los datos del pago");
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    tvEstado.setText("Error al cargar whitelist: " + message);
                    btnGenerarPago.setEnabled(false);
                });
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void setupListeners() {
        // Listener para convertir ETH a wei en tiempo real
        etMonto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarMontoWei(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Botón generar pago
        btnGenerarPago.setOnClickListener(v -> prepararPago());

        // Botón copiar datos
        btnCopiarDatos.setOnClickListener(v -> copiarDatosQR());
    }

    private void actualizarMontoWei(String montoETH) {
        if (montoETH.isEmpty()) {
            tvMontoWei.setText("= 0 wei");
            return;
        }

        try {
            double eth = Double.parseDouble(montoETH);
            BigDecimal weiDecimal = new BigDecimal(eth)
                    .multiply(new BigDecimal("1000000000000000000"));

            tvMontoWei.setText("= " + weiDecimal.toPlainString() + " wei");
        } catch (NumberFormatException e) {
            tvMontoWei.setText("= formato inválido");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void prepararPago() {
        if (!validarInputs()){
            return;
        }

        WhitelistItem receptorSeleccionado = (WhitelistItem) spinnerReceptor.getSelectedItem();
        String receptor = receptorSeleccionado.getDireccion();
        String montoEthStr = etMonto.getText().toString().trim();

        long montoWei= convertirETHaWei(montoEthStr);

        if (montoWei > receptorSeleccionado.getLimite()) {
            Toast.makeText(this,
                    "Monto excede el límite permitido para este receptor",
                    Toast.LENGTH_LONG).show();
            return;
        }

        String pagoId = UUID.randomUUID().toString();
        long timestamp = System.currentTimeMillis() /1000;
        long nonce = generarNonce();
        byte[] hashUsado = obtenerHashUsado();

        if (hashUsado == null){
            return;
        }

        this.tvEstado.setText("Firmando pago con la biometría...");
        btnGenerarPago.setEnabled(false);

        walletManager.firmarPago(
                hashUsado,
                montoWei,
                receptor,
                timestamp,
                nonce,
                deviceId,
                new FirmarMensajeCallback() {
                    @Override
                    public void onMensajeFirmado(byte[] firma) {
                        runOnUiThread(() -> {
                            onPagoFirmado(pagoId, receptor, montoWei, hashUsado,
                                    timestamp, nonce, firma);
                        });
                    }

                    @Override
                    public void onError(String mensaje) {
                        runOnUiThread(() -> {
                            tvEstado.setText("Error al firmar: " + mensaje);
                            btnGenerarPago.setEnabled(true);
                        });
                    }
                }
        );
    }

    private boolean validarInputs() {
        if (receptoresWhitelist == null || receptoresWhitelist.isEmpty()) {
            Toast.makeText(this, "No hay receptores en la whitelist", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validar monto
        String montoStr = etMonto.getText().toString().trim();
        if (montoStr.isEmpty()) {
            etMonto.setError("Campo requerido");
            Toast.makeText(this, "Ingresa el monto", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            double monto = Double.parseDouble(montoStr);
            if (monto <= 0) {
                etMonto.setError("Debe ser mayor a 0");
                Toast.makeText(this, "El monto debe ser mayor a 0", Toast.LENGTH_SHORT).show();
                return false;
            }
        } catch (NumberFormatException e) {
            etMonto.setError("Formato inválido");
            Toast.makeText(this, "Formato de monto inválido", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
    private long convertirETHaWei(String montoETH) {
        double eth = Double.parseDouble(montoETH);
        BigDecimal weiDecimal = new BigDecimal(eth)
                .multiply(new BigDecimal("1000000000000000000"));
        return weiDecimal.longValue();
    }

    private long generarNonce() {
        SecureRandom random = new SecureRandom();
        return Math.abs(random.nextLong());
    }

    private byte[] obtenerHashUsado() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String hashActualHex = prefs.getString("HASH_ACTUAL", null);

        if (hashActualHex == null) {
            tvEstado.setText(" Error: No hay hash actual\n\nRegístrate primero en blockchain");
            btnGenerarPago.setEnabled(true);
            return null;
        }

        byte[] hashActual = Numeric.hexStringToByteArray(hashActualHex);
        return hashActual;
    }

    private void onPagoFirmado(String pagoId, String receptor, long montoWei,
                               byte[] hashUsado, long timestamp, long nonce, byte[] firma) {

        tvEstado.setText("Pago firmado\n Generando QR...");

        pagoActual = new PagoPendiente(
                pagoId,
                addressEmisor,
                receptor,
                montoWei,
                hashUsado,
                null,
                deviceId,
                firma
        );

        // Guardar en base de datos local
        pagoRepository.insertar(pagoActual, new RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    generarQR();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    tvEstado.setText("Error al guardar: " + message);
                    btnGenerarPago.setEnabled(true);
                });
            }
        });
    }

    private void generarQR(){
        try {
            JSONObject json = new JSONObject();
            json.put("hashUsado", Numeric.toHexString(pagoActual.getHashUsado()));
            json.put("amount", pagoActual.getAmount());
            json.put("receptor", pagoActual.getReceptor());
            json.put("timestamp", pagoActual.getTimestampPreparacion());
            json.put("nonce", System.currentTimeMillis()); // Nonce usado en la firma
            json.put("deviceId", Numeric.toHexString(pagoActual.getDeviceId()));
            json.put("firma", Numeric.toHexString(pagoActual.getFirma()));

            datosQR = json.toString();

            // Generar QR
            Bitmap qrBitmap = generarQRBitmap(datosQR, 512, 512);

            // Mostrar QR
            ivQRCode.setImageBitmap(qrBitmap);
            layoutQR.setVisibility(View.VISIBLE);

            tvEstado.setText("Pago preparado\n Muestra este QR al receptor");

        } catch (Exception e) {
            tvEstado.setText("Error al generar el QR"+ e.getMessage());
            btnGenerarPago.setEnabled(false);
        }
    }

    private Bitmap generarQRBitmap(String content, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return bitmap;
    }

    private void copiarDatosQR() {
        if (datosQR == null) {
            Toast.makeText(this, "No hay datos para copiar", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Datos del pago", datosQR);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "Datos copiados al portapapeles", Toast.LENGTH_SHORT).show();
    }
}
