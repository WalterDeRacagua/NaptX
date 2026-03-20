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
}