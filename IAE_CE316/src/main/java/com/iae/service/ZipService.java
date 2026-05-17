package com.iae.service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public class ZipService {

    public List<String> extractAll(String zipFolderPath, String targetPath) {
        List<String> extractedIds = new ArrayList<>();
        File zipFolder = new File(zipFolderPath);
        File[] zipFiles = zipFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));

        if (zipFiles == null || zipFiles.length == 0) {
            System.out.println("No ZIP files found in: " + zipFolderPath);
            return extractedIds;
        }

        for (File zipFile : zipFiles) {
            String studentId = zipFile.getName().replace(".zip", "");
            File studentDir = new File(targetPath, studentId);
            studentDir.mkdirs();

            try {
                extractZip(zipFile, studentDir);
                extractedIds.add(studentId);
                System.out.println("Extracted: " + studentId);
            } catch (ZipException e) {
                System.err.println("Corrupt ZIP for student " + studentId + ": " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Failed to extract ZIP for student " + studentId + ": " + e.getMessage());
            }
        }

        return extractedIds;
    }

    private void extractZip(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        byte[] buffer = new byte[4096];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }

    public File findSourceFile(String studentDir, String sourceFileName) {
        return searchRecursively(new File(studentDir), sourceFileName);
    }

    private File searchRecursively(File dir, String sourceFileName) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (file.isFile() && file.getName().equals(sourceFileName)) {
                return file;
            }
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File found = searchRecursively(file, sourceFileName);
                if (found != null) return found;
            }
        }

        return null;
    }
}
