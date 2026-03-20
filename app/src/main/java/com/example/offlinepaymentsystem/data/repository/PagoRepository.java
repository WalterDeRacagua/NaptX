package com.example.offlinepaymentsystem.data.repository;

import android.content.Context;

import com.example.offlinepaymentsystem.data.AppDatabase;
import com.example.offlinepaymentsystem.data.local.PagoPendienteDao;
import com.example.offlinepaymentsystem.model.PagoPendiente;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class PagoRepository {

    private final PagoPendienteDao dao;
    private final ExecutorService executorService;

    public PagoRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao= db.pagoPendienteDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertar(PagoPendiente pago, RepositoryCallback<Void> callback) {
        executorService.execute(()->{
            try {
                dao.insertar(pago);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void actualizar(PagoPendiente pago, RepositoryCallback<Void> callback) {
        executorService.execute(()->{
            try {
                dao.actualizar(pago);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void obtenerPagosPreparados(RepositoryCallback<List<PagoPendiente>>callback){
        executorService.execute(()->{
            try {
                List<PagoPendiente> pagos = dao.obtenerPagosPreparados();
                callback.onSuccess(pagos);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
}
