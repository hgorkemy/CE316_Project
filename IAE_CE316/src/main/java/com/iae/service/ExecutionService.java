package com.iae.service;

import com.iae.model.Configuration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ExecutionService {

    private static final int TIMEOUT_SECONDS = 30;

    public ExecutionResult compile(String workingDir, Configuration config) {
        if (!config.isCompileRequired()) {
            return new ExecutionResult(true, 0, "", "", false);
        }

        String sourceFile = config.getSourceFileName();
        String outputName = sourceFile.contains(".") ? sourceFile.substring(0, sourceFile.lastIndexOf('.')) : sourceFile;

        String args = config.getCompileArgs()
                .replace("{sourceFile}", sourceFile)
                .replace("{outputName}", outputName);

        List<String> command = new ArrayList<>();
        command.add(config.getCompileCommand());
        for (String arg : args.split("\\s+")) {
            if (!arg.isEmpty()) command.add(arg);
        }

        return runProcess(command, workingDir);
    }

    public ExecutionResult run(String workingDir, Configuration config, String runArgs) {
        String sourceFile = config.getSourceFileName();
        String outputName = sourceFile.contains(".") ? sourceFile.substring(0, sourceFile.lastIndexOf('.')) : sourceFile;

        String resolvedArgs = (runArgs == null ? "" : runArgs);
        String argTemplate = config.getRunArgs() == null ? "" : config.getRunArgs();
        String finalArgs = argTemplate
                .replace("{outputName}", outputName)
                .replace("{sourceFile}", sourceFile)
                .replace("{args}", resolvedArgs);

        String runCommand = config.getRunCommand().replace("{outputName}", outputName);

        List<String> command = new ArrayList<>();
        command.add(runCommand);
        for (String arg : finalArgs.split("\\s+")) {
            if (!arg.isEmpty()) command.add(arg);
        }

        return runProcess(command, workingDir);
    }

    private ExecutionResult runProcess(List<String> command, String workingDir) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workingDir));
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            return new ExecutionResult(false, -1, "", e.getMessage(), false);
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });

        Thread stderrReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });

        stdoutReader.start();
        stderrReader.start();

        boolean finished;
        try {
            finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroy();
            Thread.currentThread().interrupt();
            return new ExecutionResult(false, -1, "", "Process interrupted", false);
        }

        if (!finished) {
            process.destroy();
            return new ExecutionResult(false, -1, stdout.toString(), stderr.toString(), true);
        }

        try {
            stdoutReader.join(2000);
            stderrReader.join(2000);
        } catch (InterruptedException ignored) {}

        int exitCode = process.exitValue();
        return new ExecutionResult(exitCode == 0, exitCode, stdout.toString(), stderr.toString(), false);
    }
}
