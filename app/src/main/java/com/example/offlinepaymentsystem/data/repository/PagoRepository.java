package com.example.offlinepaymentsystem.data.repository;

import android.content.Context;

import com.example.offlinepaymentsystem.data.AppDatabase;
import com.example.offlinepaymentsystem.data.local.PagoPendienteDao;
import com.example.offlinepaymentsystem.model.PagoPendiente;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Capa entre la Activity y el DAO
 * */
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

    public void obtenerPagosConfirmados(RepositoryCallback<List<PagoPendiente>>callback){
        executorService.execute(()->{
            try {
                List<PagoPendiente> pagos = dao.obtenerPagosConfirmados();
                callback.onSuccess(pagos);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void obtenerTodosPagos(RepositoryCallback<List<PagoPendiente>>callback){
        executorService.execute(()->{
            try {
                List<PagoPendiente> pagos = dao.obtenerTodosPagos();
                callback.onSuccess(pagos);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void obtenerPagoPorID(String pagoId, RepositoryCallback<PagoPendiente>callback){
        executorService.execute(()->{
            try {
                PagoPendiente pago = dao.obtenerPagoPorId(pagoId);
                callback.onSuccess(pago);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void contarPagosPreparados(RepositoryCallback<Integer>callback){
        executorService.execute(()->{
            try {
                int count = dao.contarPagosPreparados();
                callback.onSuccess(count);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }


    public void limpiarPagosFallidos(RepositoryCallback<Void>callback){
        executorService.execute(()->{
            try {
                dao.limpiarPagosFallidos();
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
}
