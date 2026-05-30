package com.iae.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

public class ZipService {

    public List<File> findZipFiles(String zipFolderPath) {
        File zipFolder = new File(zipFolderPath == null ? "" : zipFolderPath);
        File[] zipFiles = zipFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));

        if (zipFiles == null || zipFiles.length == 0) {
            return new ArrayList<>();
        }

        Arrays.sort(zipFiles, Comparator.comparing(File::getName));
        return new ArrayList<>(Arrays.asList(zipFiles));
    }

    public List<String> extractAll(String zipFolderPath, String targetPath) {
        List<String> extractedIds = new ArrayList<>();
        List<File> zipFiles = findZipFiles(zipFolderPath);

        for (File zipFile : zipFiles) {
            ZipEntryResult result = extractStudentZip(zipFile, targetPath);

            if (result.isSuccessful()) {
                extractedIds.add(result.getStudentId());
            }
        }

        return extractedIds;
    }

    public ZipEntryResult extractStudentZip(File zipFile, String targetPath) {
        String studentId = getStudentIdFromZip(zipFile);
        File studentDir = new File(targetPath, studentId);
        studentDir.mkdirs();

        try {
            extractZip(zipFile, studentDir);
            return new ZipEntryResult(studentId, zipFile, studentDir, true, null);
        } catch (ZipException e) {
            return new ZipEntryResult(studentId, zipFile, studentDir, false, "ZIP extraction failed: " + e.getMessage());
        } catch (IOException e) {
            return new ZipEntryResult(studentId, zipFile, studentDir, false, "ZIP extraction failed: " + e.getMessage());
        }
    }

    public String getStudentIdFromZip(File zipFile) {
        String name = zipFile.getName();
        int index = name.toLowerCase().lastIndexOf(".zip");

        if (index > 0) {
            return name.substring(0, index);
        }

        return name;
    }

    public boolean isValidZip(File zipFile) {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            boolean hasEntry = false;

            while (zis.getNextEntry() != null) {
                hasEntry = true;
                zis.closeEntry();
            }

            return hasEntry;
        } catch (IOException e) {
            return false;
        }
    }

    private void extractZip(File zipFile, File targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(targetDir, entry.getName());
                String targetPath = targetDir.getCanonicalPath() + File.separator;
                String outputPath = outFile.getCanonicalPath();

                if (!outputPath.startsWith(targetPath)) {
                    throw new ZipException("Invalid ZIP entry path: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parent = outFile.getParentFile();

                    if (parent != null) {
                        parent.mkdirs();
                    }

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

        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().equals(sourceFileName)) {
                return file;
            }
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File found = searchRecursively(file, sourceFileName);

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }
}