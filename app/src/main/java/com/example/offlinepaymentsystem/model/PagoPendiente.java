package com.example.offlinepaymentsystem.model;

/***
 *Representa un pago en Proceso (sistema de 2 fases que hablamos con Jesús.
 */
public class PagoPendiente {

    /**
     * PREPARADO: Fase 1: fondos reservados
     * CONFIRMADO: Fase 2: pago completado
     * REVERTIDO: Timeout: pago cancelado
     * FALLIDO: Error en el procesamiento
     */
    public enum Estado {PREPARADO, CONFIRMADO, REVERTIDO, FALLIDO};
    private String idPago;
    private String emisor;
    private String receptor;
    private double amount;
    //Hash usado para generar el pago
    private String hashUsado;
    //Hash preparado temporal de la fase 1 del pago
    private String hashPreparado;
    //Hash final, una vez se ha confirmado el pago
    private String hashFinal;
    private long timestampPreparacion;
    private long timestampConfirmacion;
    private Estado estado;
    private String transaccionId;//Id de la transacción si se proceso.

    public PagoPendiente(){
        this.estado = Estado.PREPARADO;
    }

    public PagoPendiente(String idPago, String emisor, String receptor, double amount, String
            hashUsado, String hashPreparado){
        this.idPago = idPago;
        this.emisor= emisor;
        this.receptor = receptor;
        this.amount = amount;
        this.hashUsado = hashUsado;
        this.hashPreparado = hashPreparado;
        this.timestampPreparacion = System.currentTimeMillis();
        this.estado = Estado.PREPARADO;
    }


    public String getIdPago() {
        return idPago;
    }

    public void setIdPago(String idPago) {
        this.idPago = idPago;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getHashUsado() {
        return hashUsado;
    }

    public void setHashUsado(String hashUsado) {
        this.hashUsado = hashUsado;
    }

    public String getHashPreparado() {
        return hashPreparado;
    }

    public void setHashPreparado(String hashPreparado) {
        this.hashPreparado = hashPreparado;
    }

    public String getHashFinal() {
        return hashFinal;
    }

    public void setHashFinal(String hashFinal) {
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

    public String getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(String transaccionId) {
        this.transaccionId = transaccionId;
    }


}
