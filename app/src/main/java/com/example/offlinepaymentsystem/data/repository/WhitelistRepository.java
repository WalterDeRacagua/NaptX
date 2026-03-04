package com.example.offlinepaymentsystem.data.repository;

import android.content.Context;

import com.example.offlinepaymentsystem.data.AppDatabase;
import com.example.offlinepaymentsystem.data.local.WhitelistDao;
import com.example.offlinepaymentsystem.model.WhitelistItem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WhitelistRepository {

    private final WhitelistDao dao;
    private final ExecutorService executorService;

    public WhitelistRepository(Context context){
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.whitelistDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void insertar(WhitelistItem item, RepositoryCallback<Void> callback){
        executorService.execute(()->{
            try {
                this.dao.insertar(item);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void obtenerPorDireccion(String direccion, RepositoryCallback<WhitelistItem> callback){
        executorService.execute(()->{
            try {
                WhitelistItem item= this.dao.obtenerPorDireccion(direccion);
                callback.onSuccess(item);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void obtenerTodos(RepositoryCallback<List<WhitelistItem>> callback){
        executorService.execute(()->{
            try {
                List<WhitelistItem> items= this.dao.obtenerTodos();
                callback.onSuccess(items);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void existe(String direccion, RepositoryCallback<Boolean> callback){
        executorService.execute(()->{
            try {
                boolean existe = dao.existe(direccion) > 0;
                callback.onSuccess(existe);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

    public void eliminar(String direccion, RepositoryCallback<Void> callback) {
        executorService.execute(() -> {
            try {
                dao.eliminar(direccion);
                callback.onSuccess(null);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }

}
