package com.example.offlinepaymentsystem.data.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(String message);
}
