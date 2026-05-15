package com.iae.service;

public class ExecutionResult {

    private boolean success;
    private int exitCode;
    private String stdout;
    private String stderr;
    private boolean timedOut;

    public ExecutionResult(boolean success, int exitCode, String stdout, String stderr, boolean timedOut) {
        this.success = success;
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.timedOut = timedOut;
    }

    public boolean isSuccess() { return success; }
    public int getExitCode() { return exitCode; }
    public String getStdout() { return stdout; }
    public String getStderr() { return stderr; }
    public boolean isTimedOut() { return timedOut; }
}
