package com.iae.model;

public class Configuration {

    private int id;
    private String name;
    private String language;
    private boolean compileRequired;
    private String compileCommand;
    private String compileArgs;
    private String runCommand;
    private String runArgs;
    private String sourceFileName;
    private String createdAt;

    public Configuration() {}

    public Configuration(String name, String language, boolean compileRequired,
                         String compileCommand, String compileArgs,
                         String runCommand, String runArgs, String sourceFileName) {
        this.name = name;
        this.language = language;
        this.compileRequired = compileRequired;
        this.compileCommand = compileCommand;
        this.compileArgs = compileArgs;
        this.runCommand = runCommand;
        this.runArgs = runArgs;
        this.sourceFileName = sourceFileName;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isCompileRequired() { return compileRequired; }
    public void setCompileRequired(boolean compileRequired) { this.compileRequired = compileRequired; }

    public String getCompileCommand() { return compileCommand; }
    public void setCompileCommand(String compileCommand) { this.compileCommand = compileCommand; }

    public String getCompileArgs() { return compileArgs; }
    public void setCompileArgs(String compileArgs) { this.compileArgs = compileArgs; }

    public String getRunCommand() { return runCommand; }
    public void setRunCommand(String runCommand) { this.runCommand = runCommand; }

    public String getRunArgs() { return runArgs; }
    public void setRunArgs(String runArgs) { this.runArgs = runArgs; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}
