package com.example.offlinepaymentsystem.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.example.offlinepaymentsystem.data.local.Converters;
import com.example.offlinepaymentsystem.data.local.PagoPendienteDao;
import com.example.offlinepaymentsystem.model.PagoPendiente;

@Database(
        entities = {PagoPendiente.class},
        version = 1,
        exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static  AppDatabase instance;
    public abstract PagoPendienteDao pagoPendienteDao();

    /**
     * Singleton
     * */

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null){
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "offline_payment_db"
            ).fallbackToDestructiveMigration().build();
        }

        return instance;
    }
}
