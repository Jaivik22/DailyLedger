package com.example.dailyledger;

public interface UploadCallback {
    void onSuccess(String id);

    void onFailure(Exception e);
}
