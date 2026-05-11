package com.iae.controller;

// Oyku will implement this class
public class NewProjectController {
    private Runnable onProjectCreated;

    public void setOnProjectCreated(Runnable callback) {
        this.onProjectCreated = callback;
    }
}
