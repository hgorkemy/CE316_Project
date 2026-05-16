package com.iae.service;

import com.iae.dao.IConfigurationDAO;
import com.iae.model.*;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;

public class RunnerService {

    private final ZipService zipService;
    private final ExecutionService executionService;
    private final ComparisonService comparisonService;
    private final ReportService reportService;
    private final ProjectService projectService;
    private final IConfigurationDAO configurationDAO;

    public RunnerService(ZipService zipService,
                         ExecutionService executionService,
                         ComparisonService comparisonService,
                         ReportService reportService,
                         ProjectService projectService,
                         IConfigurationDAO configurationDAO) {
        this.zipService = zipService;
        this.executionService = executionService;
        this.comparisonService = comparisonService;
        this.reportService = reportService;
        this.projectService = projectService;
        this.configurationDAO = configurationDAO;
    }

    public void runProject(Project project, Consumer<Double> progressCallback) {
        projectService.updateProjectStatus(project.getId(), ProjectStatus.RUNNING);

        try {
            Configuration config = configurationDAO.findById(project.getConfigId());

            if (config == null) {
                throw new IllegalStateException("Configuration could not be found.");
            }

            File studentsDir = new File(project.getZipDirectory(), "extracted_students");
            studentsDir.mkdirs();

            List<String> studentIds = zipService.extractAll(
                    project.getZipDirectory(),
                    studentsDir.getAbsolutePath()
            );

            String expectedOutput = Files.readString(new File(project.getExpectedOutput()).toPath());

            if (studentIds.isEmpty()) {
                projectService.updateProjectStatus(project.getId(), ProjectStatus.COMPLETED);
                if (progressCallback != null) progressCallback.accept(1.0);
                return;
            }

            for (int i = 0; i < studentIds.size(); i++) {
                String studentId = studentIds.get(i);
                File studentDir = new File(studentsDir, studentId);

                Result result = new Result(project.getId(), studentId);

                File sourceFile = zipService.findSourceFile(
                        studentDir.getAbsolutePath(),
                        config.getSourceFileName()
                );

                if (sourceFile == null) {
                    result.setCompileStatus(CompileStatus.FAIL);
                    result.setCompileError("Source file not found: " + config.getSourceFileName());
                    result.setRunStatus(RunStatus.SKIPPED);
                    result.setComparisonStatus(ComparisonStatus.SKIPPED);
                    reportService.saveResult(result);
                    updateProgress(progressCallback, i + 1, studentIds.size());
                    continue;
                }

                String workingDir = sourceFile.getParentFile().getAbsolutePath();

                ExecutionResult compileResult = executionService.compile(workingDir, config);

                if (!compileResult.isSuccess()) {
                    result.setCompileStatus(CompileStatus.FAIL);
                    result.setCompileError(compileResult.getStderr());
                    result.setRunStatus(RunStatus.SKIPPED);
                    result.setComparisonStatus(ComparisonStatus.SKIPPED);
                    reportService.saveResult(result);
                    updateProgress(progressCallback, i + 1, studentIds.size());
                    continue;
                }

                result.setCompileStatus(CompileStatus.PASS);

                ExecutionResult runResult = executionService.run(
                        workingDir,
                        config,
                        project.getRunArgs()
                );

                result.setActualOutput(runResult.getStdout());

                if (runResult.isTimedOut()) {
                    result.setRunStatus(RunStatus.TIMEOUT);
                    result.setRunError("Program timed out.");
                    result.setComparisonStatus(ComparisonStatus.SKIPPED);
                    reportService.saveResult(result);
                    updateProgress(progressCallback, i + 1, studentIds.size());
                    continue;
                }

                if (!runResult.isSuccess()) {
                    result.setRunStatus(RunStatus.ERROR);
                    result.setRunError(runResult.getStderr());
                    result.setComparisonStatus(ComparisonStatus.SKIPPED);
                    reportService.saveResult(result);
                    updateProgress(progressCallback, i + 1, studentIds.size());
                    continue;
                }

                result.setRunStatus(RunStatus.PASS);

                boolean match = comparisonService.compare(runResult.getStdout(), expectedOutput);
                result.setComparisonStatus(match ? ComparisonStatus.PASS : ComparisonStatus.FAIL);

                reportService.saveResult(result);
                updateProgress(progressCallback, i + 1, studentIds.size());
            }

            projectService.updateProjectStatus(project.getId(), ProjectStatus.COMPLETED);

        } catch (Exception e) {
            projectService.updateProjectStatus(project.getId(), ProjectStatus.ERROR);
            throw new RuntimeException("Project run failed: " + e.getMessage(), e);
        }
    }

    private void updateProgress(Consumer<Double> progressCallback, int done, int total) {
        if (progressCallback != null) {
            progressCallback.accept((double) done / total);
        }
    }
}