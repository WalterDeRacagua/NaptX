package com.example.offlinepaymentsystem.utils;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeHelper {

    /**
     * Genera un Bitmap de código QR
     * @param content Contenido del QR (JSON, texto, etc.)
     * @param width Ancho en píxeles
     * @param height Alto en píxeles
     * @return Bitmap del QR generado
     * @throws WriterException Si hay error al generar el QR
     */
    public static Bitmap generarQRBitmap(String content, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y,
                        bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF
                );
            }
        }
        return bitmap;
    }

    public static Bitmap generarQRBitmap(String content) throws WriterException {
        return generarQRBitmap(content, 512, 512);
    }

    // Private constructor
    private QRCodeHelper() {
        throw new AssertionError("No se puede instanciar esta clase");
    }
}