package com.iae.service;

import com.iae.dao.ConfigurationDAO;
import com.iae.dao.IConfigurationDAO;
import com.iae.dao.IProjectDAO;
import com.iae.dao.IResultDAO;
import com.iae.dao.ProjectDAO;
import com.iae.dao.ResultDAO;
import com.iae.model.ComparisonStatus;
import com.iae.model.CompileStatus;
import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.ProjectStatus;
import com.iae.model.Result;
import com.iae.model.RunStatus;

import java.io.File;
import java.util.List;

public class RunnerService {

    private final ZipService zipService = new ZipService();
    private final ExecutionService executionService = new ExecutionService();
    private final ComparisonService comparisonService = new ComparisonService();
    private final IResultDAO resultDAO = new ResultDAO();
    private final IConfigurationDAO configurationDAO = new ConfigurationDAO();
    private final IProjectDAO projectDAO = new ProjectDAO();

    public List<File> findZipFiles(Project project) {
        return zipService.findZipFiles(project.getZipDirectory());
    }

    public int runProject(Project project, RunProgressListener listener) {
        List<File> zipFiles = zipService.findZipFiles(project.getZipDirectory());

        if (zipFiles.isEmpty()) {
            throw new NoZipFilesException("No ZIP files found in the selected directory.");
        }

        Configuration configuration = configurationDAO.findById(project.getConfigId());

        if (configuration == null) {
            throw new IllegalStateException("Project configuration was not found.");
        }

        projectDAO.updateStatus(project.getId(), ProjectStatus.RUNNING);

        File extractionRoot = new File(project.getZipDirectory(), ".iae_extracted_project_" + project.getId());
        extractionRoot.mkdirs();

        int total = zipFiles.size();
        int done = 0;

        for (File zipFile : zipFiles) {
            String studentId = zipService.getStudentIdFromZip(zipFile);
            notify(listener, "Processing " + studentId + "... (" + (done + 1) + "/" + total + ")", done, total);

            Result result = new Result(project.getId(), studentId);
            ZipEntryResult extraction = zipService.extractStudentZip(zipFile, extractionRoot.getAbsolutePath());

            if (!extraction.isSuccessful()) {
                result.setCompileStatus(CompileStatus.FAIL);
                result.setCompileError(extraction.getErrorMessage());
                result.setRunStatus(RunStatus.SKIPPED);
                result.setComparisonStatus(ComparisonStatus.SKIPPED);
                resultDAO.save(result);
                done++;
                notify(listener, "Processing " + studentId + " completed with ZIP error. (" + done + "/" + total + ")", done, total);
                continue;
            }

            File sourceFile = zipService.findSourceFile(extraction.getStudentDirectory().getAbsolutePath(), configuration.getSourceFileName());

            if (sourceFile == null) {
                result.setCompileStatus(CompileStatus.FAIL);
                result.setCompileError("Source file not found: " + configuration.getSourceFileName());
                result.setRunStatus(RunStatus.SKIPPED);
                result.setComparisonStatus(ComparisonStatus.SKIPPED);
                resultDAO.save(result);
                done++;
                notify(listener, "Processing " + studentId + " completed with missing source file. (" + done + "/" + total + ")", done, total);
                continue;
            }

            File workingDirectory = sourceFile.getParentFile();
            ExecutionResult compileResult = executionService.compile(workingDirectory.getAbsolutePath(), configuration);

            if (!compileResult.isSuccess()) {
                result.setCompileStatus(CompileStatus.FAIL);
                result.setCompileError(buildProcessError(compileResult));
                result.setRunStatus(RunStatus.SKIPPED);
                result.setComparisonStatus(ComparisonStatus.SKIPPED);
                resultDAO.save(result);
                done++;
                notify(listener, "Processing " + studentId + " completed with compile error. (" + done + "/" + total + ")", done, total);
                continue;
            }

            result.setCompileStatus(CompileStatus.PASS);

            ExecutionResult runResult = executionService.run(workingDirectory.getAbsolutePath(), configuration, project.getRunArgs());
            result.setActualOutput(runResult.getStdout());

            if (runResult.isTimedOut()) {
                result.setRunStatus(RunStatus.TIMEOUT);
                result.setRunError(buildProcessError(runResult));
                result.setComparisonStatus(ComparisonStatus.SKIPPED);
                resultDAO.save(result);
                done++;
                notify(listener, "Processing " + studentId + " completed with timeout. (" + done + "/" + total + ")", done, total);
                continue;
            }

            if (!runResult.isSuccess()) {
                result.setRunStatus(RunStatus.FAIL);
                result.setRunError(buildProcessError(runResult));
                result.setComparisonStatus(ComparisonStatus.SKIPPED);
                resultDAO.save(result);
                done++;
                notify(listener, "Processing " + studentId + " completed with run error. (" + done + "/" + total + ")", done, total);
                continue;
            }

            result.setRunStatus(RunStatus.PASS);

            boolean passed = comparisonService.compare(runResult.getStdout(), project.getExpectedOutput());
            result.setComparisonStatus(passed ? ComparisonStatus.PASS : ComparisonStatus.FAIL);

            if (!passed) {
                result.setRunError(comparisonService.generateDiff(runResult.getStdout(), project.getExpectedOutput()));
            }

            resultDAO.save(result);
            done++;
            notify(listener, "Processing " + studentId + " completed. (" + done + "/" + total + ")", done, total);
        }

        projectDAO.updateStatus(project.getId(), ProjectStatus.COMPLETED);
        notify(listener, "Run completed. " + done + " students processed.", done, total);

        return done;
    }

    private String buildProcessError(ExecutionResult result) {
        StringBuilder builder = new StringBuilder();

        if (result.isTimedOut()) {
            builder.append("Process timed out.");
        }

        if (result.getExitCode() != 0) {
            if (builder.length() > 0) {
                builder.append("\n");
            }

            builder.append("Exit code: ").append(result.getExitCode());
        }

        if (result.getStderr() != null && !result.getStderr().isBlank()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }

            builder.append(result.getStderr().trim());
        }

        if (builder.length() == 0 && result.getStdout() != null && !result.getStdout().isBlank()) {
            builder.append(result.getStdout().trim());
        }

        return builder.toString();
    }

    private void notify(RunProgressListener listener, String message, int done, int total) {
        if (listener != null) {
            listener.onProgress(message, done, total);
        }
    }

    public static class NoZipFilesException extends RuntimeException {
        public NoZipFilesException(String message) {
            super(message);
        }
    }
}