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



    public void decrementarLimite(String direccion, long amountPagado, RepositoryCallback<Void> callback){

            new Thread(()->{
                try {
                    WhitelistItem item = this.dao.obtenerPorDireccion(direccion);

                    if (item == null){
                        callback.onError("El receptor no se ha encontrado en la whitelist.");
                        return;
                    }

                    long nuevoLimite = item.getLimite() - amountPagado;

                    if (nuevoLimite < 0){
                        nuevoLimite =0;
                    }

                    item.setLimite(nuevoLimite);

                    this.dao.actualizar(item);

                    callback.onSuccess(null);
                } catch (Exception e) {
                    callback.onError(e.getMessage());
                }
            }).start();
    }

}
