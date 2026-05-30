package com.iae.service;

import java.io.File;

public class ZipEntryResult {

    private final String studentId;
    private final File zipFile;
    private final File studentDirectory;
    private final boolean successful;
    private final String errorMessage;

    public ZipEntryResult(String studentId, File zipFile, File studentDirectory, boolean successful, String errorMessage) {
        this.studentId = studentId;
        this.zipFile = zipFile;
        this.studentDirectory = studentDirectory;
        this.successful = successful;
        this.errorMessage = errorMessage;
    }

    public String getStudentId() {
        return studentId;
    }

    public File getZipFile() {
        return zipFile;
    }

    public File getStudentDirectory() {
        return studentDirectory;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}