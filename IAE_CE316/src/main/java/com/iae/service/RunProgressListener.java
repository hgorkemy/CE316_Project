package com.iae.service;

public interface RunProgressListener {
    void onProgress(String message, int done, int total);
}