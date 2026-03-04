package com.example.offlinepaymentsystem.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.offlinepaymentsystem.model.WhitelistItem;

import java.util.List;

@Dao
public interface WhitelistDao {

    /**
     * Si ya existe, reemplaza (actualiza límite)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertar(WhitelistItem item);

    @Update
    void actualizar(WhitelistItem item);

    @Query("SELECT * FROM whitelist WHERE direccion = :direccion LIMIT 1")
    WhitelistItem obtenerPorDireccion(String direccion);


    @Query("SELECT * FROM whitelist ORDER BY timestampAgregado DESC")
    List<WhitelistItem> obtenerTodos();

    @Query("SELECT COUNT(*) FROM whitelist WHERE direccion = :direccion")
    int existe(String direccion);

    @Query("DELETE FROM whitelist WHERE direccion = :direccion")
    void eliminar(String direccion);

    @Query("SELECT COUNT(*) FROM whitelist")
    int contar();

    @Query("DELETE FROM whitelist")
    void eliminarTodos();
}