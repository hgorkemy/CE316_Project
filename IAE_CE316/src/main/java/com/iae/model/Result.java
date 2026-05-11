package com.iae.model;

public class Result {

    private int id;
    private int projectId;
    private String studentId;
    private CompileStatus compileStatus;
    private String compileError;
    private RunStatus runStatus;
    private String runError;
    private String actualOutput;
    private ComparisonStatus comparisonStatus;
    private String createdAt;

    public Result() {}

    public Result(int projectId, String studentId) {
        this.projectId = projectId;
        this.studentId = studentId;
        this.compileStatus = CompileStatus.SKIPPED;
        this.runStatus = RunStatus.SKIPPED;
        this.comparisonStatus = ComparisonStatus.SKIPPED;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public CompileStatus getCompileStatus() { return compileStatus; }
    public void setCompileStatus(CompileStatus compileStatus) { this.compileStatus = compileStatus; }

    public String getCompileError() { return compileError; }
    public void setCompileError(String compileError) { this.compileError = compileError; }

    public RunStatus getRunStatus() { return runStatus; }
    public void setRunStatus(RunStatus runStatus) { this.runStatus = runStatus; }

    public String getRunError() { return runError; }
    public void setRunError(String runError) { this.runError = runError; }

    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }

    public ComparisonStatus getComparisonStatus() { return comparisonStatus; }
    public void setComparisonStatus(ComparisonStatus comparisonStatus) { this.comparisonStatus = comparisonStatus; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
