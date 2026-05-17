package com.iae.service;

import com.iae.dao.IResultDAO;
import com.iae.model.ComparisonStatus;
import com.iae.model.CompileStatus;
import com.iae.model.Result;
import com.iae.model.RunStatus;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ReportService {

    private final IResultDAO resultDAO;

    public ReportService(IResultDAO resultDAO) {
        this.resultDAO = resultDAO;
    }

    public void saveResult(Result result) {
        resultDAO.save(result);
    }

    public List<Result> getResults(int projectId) {
        return resultDAO.findByProjectId(projectId);
    }

    public String generateSummary(int projectId) {
        List<Result> results = resultDAO.findByProjectId(projectId);

        long pass = results.stream()
                .filter(r -> r.getCompileStatus() == CompileStatus.PASS)
                .filter(r -> r.getRunStatus() == RunStatus.PASS)
                .filter(r -> r.getComparisonStatus() == ComparisonStatus.PASS)
                .count();

        long fail = results.stream()
                .filter(r -> r.getComparisonStatus() == ComparisonStatus.FAIL)
                .count();

        long compileError = results.stream()
                .filter(r -> r.getCompileStatus() == CompileStatus.FAIL)
                .count();

        long timeout = results.stream()
                .filter(r -> r.getRunStatus() == RunStatus.TIMEOUT)
                .count();

        return "Summary: " + pass + " PASS, " + fail + " FAIL, "
                + compileError + " Compile Error, " + timeout + " Timeout";
    }

    public void exportCsv(int projectId, String filePath) {
        List<Result> results = resultDAO.findByProjectId(projectId);

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("Student ID,Compile,Run,Output Match,Error\n");

            for (Result r : results) {
                writer.write(csv(r.getStudentId()) + ",");
                writer.write(csv(String.valueOf(r.getCompileStatus())) + ",");
                writer.write(csv(String.valueOf(r.getRunStatus())) + ",");
                writer.write(csv(String.valueOf(r.getComparisonStatus())) + ",");
                writer.write(csv(errorText(r)) + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to export CSV: " + e.getMessage(), e);
        }
    }

    private String errorText(Result r) {
        if (r.getCompileError() != null && !r.getCompileError().isBlank()) {
            return r.getCompileError();
        }

        if (r.getRunError() != null && !r.getRunError().isBlank()) {
            return r.getRunError();
        }

        return "";
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}