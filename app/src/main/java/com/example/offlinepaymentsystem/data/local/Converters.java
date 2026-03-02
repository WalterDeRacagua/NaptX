package com.example.offlinepaymentsystem.data.local;

import androidx.room.TypeConverter;

import com.example.offlinepaymentsystem.model.PagoPendiente;

/***
 * Converters es para tipos personalizados en Room, p.ej Enums
 */

public class Converters {
    //Estado --> String.
    @TypeConverter
    public static String fromEstado(PagoPendiente.Estado estado){
        return estado == null ? null: estado.name();
    }

    //String --> Estado
    @TypeConverter
    public static PagoPendiente.Estado toEstado(String estado){
        return estado == null ? null: PagoPendiente.Estado.valueOf(estado);
    }
}

