package com.example.offlinepaymentsystem.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import com.example.offlinepaymentsystem.model.PagoPendiente;

import java.util.List;

@Dao
public interface PagoPendienteDao {

    @Insert
    void insertar(PagoPendiente pago);

    @Update
    void actualizar(PagoPendiente pago);

    @Query("SELECT * FROM pagos_pendientes WHERE estado = 'PREPARADO' ORDER BY timestampPreparacion DESC")
    List<PagoPendiente> obtenerPagosPreparados();

    @Query("SELECT * FROM pagos_pendientes WHERE estado = 'CONFIRMADO' ORDER BY timestampConfirmacion DESC")
    List<PagoPendiente> obtenerPagosConfirmados();

    @Query("SELECT * FROM pagos_pendientes ORDER BY timestampPreparacion DESC")
    List<PagoPendiente> obtenerTodosPagos();

    @Query("SELECT * FROM pagos_pendientes WHERE pagoId = :pagoId LIMIT 1")
    PagoPendiente obtenerPagoPorId(String pagoId);

    @Query("SELECT COUNT(*) FROM pagos_pendientes WHERE estado = 'PREPARADO'")
    int contarPagosPreparados();

    /**
     * Eliminar todos los pagos REVERTIDOS o FALLIDOS (limpieza)
     */
    @Query("DELETE FROM pagos_pendientes WHERE estado IN ('REVERTIDO', 'FALLIDO')")
    void limpiarPagosFallidos();
}