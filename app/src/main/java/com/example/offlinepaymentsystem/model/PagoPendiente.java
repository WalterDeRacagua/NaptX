package com.example.offlinepaymentsystem.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.example.offlinepaymentsystem.data.local.Converters;

/***
 *Representa un pago en Proceso (sistema de 2 fases que hablamos con Jesús)
 *
 * FASE 1: (PREPARADO): Emisor crea un PagoOffline y genera un QR
 * FASE 2: (CONFIRMADO): Emisor confirma pago en blockchain
 */
@Entity(tableName = "pagos_pendientes")
@TypeConverters(Converters.class)
public class PagoPendiente {



    /**
     * Estados del pago:
     * PREPARADO: Fase 1: pago creado offline, QR generado
     * CONFIRMADO: Fase 2: Pago confirmado en blockchain
     * REVERTIDO: Timeout: pago cancelado
     * FALLIDO: Error en el procesamiento
     */

    public enum Estado {PREPARADO, CONFIRMADO, REVERTIDO, FALLIDO};

    // =================== CAMPOS DE LA BASE DATOS ===================

    @PrimaryKey
    @NonNull
    private String pagoId;

    private String emisor;
    private String receptor;
    private long amount;

    private byte[] hashUsado;
    private byte[] hashPreparado;
    private byte[] hashFinal;

    private long timestampPreparacion;
    private long timestampConfirmacion;
    private Estado estado;
    private byte[] txId;
    private byte[] deviceId;
    private byte[] firma;
    private long nonce;

    // =================== CONSTRUCTOR VACÍO EN ROOM ===================

    //Room necesita un constructor vacío.
    public PagoPendiente(){
        this.estado = Estado.PREPARADO;
    }

    public PagoPendiente(@NonNull String pagoId, String emisor, String receptor, long amount, byte[] hashUsado,
                         byte[] hashPreparado, byte[] deviceId, byte[] firma, long timestamp, long nonce){
        this.pagoId = pagoId;
        this.emisor = emisor;
        this.receptor=receptor;
        this.amount = amount;
        this.hashUsado = hashUsado;
        this.hashPreparado = hashPreparado;
        this.deviceId = deviceId;
        this.firma = firma;
        this.timestampPreparacion= timestamp;
        this.nonce = nonce;
        this.estado = Estado.PREPARADO;
        this.hashFinal =null;
        this.timestampConfirmacion=0; //Aun no lo hemos podido confirmar
        this.txId = null; //Aun no se ha creado la transacción
    }



    // =================== GETTERS Y SETTERS ===================


    @NonNull
    public String getPagoId() {
        return pagoId;
    }

    public void setPagoId(@NonNull String pagoId) {
        this.pagoId = pagoId;
    }

    public String getEmisor() {
        return emisor;
    }

    public void setEmisor(String emisor) {
        this.emisor = emisor;
    }

    public String getReceptor() {
        return receptor;
    }

    public void setReceptor(String receptor) {
        this.receptor = receptor;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public byte[] getHashUsado() {
        return hashUsado;
    }

    public void setHashUsado(byte[] hashUsado) {
        this.hashUsado = hashUsado;
    }

    public byte[] getHashPreparado() {
        return hashPreparado;
    }

    public void setHashPreparado(byte[] hashPreparado) {
        this.hashPreparado = hashPreparado;
    }

    public byte[] getHashFinal() {
        return hashFinal;
    }

    public void setHashFinal(byte[] hashFinal) {
        this.hashFinal = hashFinal;
    }

    public long getTimestampPreparacion() {
        return timestampPreparacion;
    }

    public void setTimestampPreparacion(long timestampPreparacion) {
        this.timestampPreparacion = timestampPreparacion;
    }

    public long getTimestampConfirmacion() {
        return timestampConfirmacion;
    }

    public void setTimestampConfirmacion(long timestampConfirmacion) {
        this.timestampConfirmacion = timestampConfirmacion;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public byte[] getTxId() {
        return txId;
    }

    public void setTxId(byte[] txId) {
        this.txId = txId;
    }

    public byte[] getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(byte[] deviceId) {
        this.deviceId = deviceId;
    }

    public byte[] getFirma() {
        return firma;
    }

    public void setFirma(byte[] firma) {
        this.firma = firma;
    }

    public long getNonce() {
        return nonce;
    }

    public void setNonce(long nonce) {
        this.nonce = nonce;
    }
}
