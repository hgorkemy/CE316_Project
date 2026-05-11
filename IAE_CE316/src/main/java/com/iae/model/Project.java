package com.iae.model;

public class Project {

    private int id;
    private String name;
    private int configId;
    private String configName;
    private String zipDirectory;
    private String expectedOutput;
    private String runArgs;
    private ProjectStatus status;
    private String createdAt;
    private String lastRunAt;

    public Project() {}

    public Project(String name, int configId, String zipDirectory,
                   String expectedOutput, String runArgs) {
        this.name = name;
        this.configId = configId;
        this.zipDirectory = zipDirectory;
        this.expectedOutput = expectedOutput;
        this.runArgs = runArgs;
        this.status = ProjectStatus.NOT_RUN;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getConfigId() { return configId; }
    public void setConfigId(int configId) { this.configId = configId; }

    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }

    public String getZipDirectory() { return zipDirectory; }
    public void setZipDirectory(String zipDirectory) { this.zipDirectory = zipDirectory; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public String getRunArgs() { return runArgs; }
    public void setRunArgs(String runArgs) { this.runArgs = runArgs; }

    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(String lastRunAt) { this.lastRunAt = lastRunAt; }

    @Override
    public String toString() { return name; }
}
