package com.iae.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.iae.model.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ImportExportService {

    private final ObjectMapper mapper;

    public ImportExportService() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void exportToJson(Configuration config, String filePath) {
        try {
            mapper.writeValue(new File(filePath), toDto(config));
        } catch (IOException e) {
            throw new RuntimeException("Failed to export configuration: " + e.getMessage(), e);
        }
    }

    public void exportAll(List<Configuration> configs, String filePath) {
        try {
            List<ConfigDto> dtos = new ArrayList<>();
            for (Configuration c : configs) dtos.add(toDto(c));
            mapper.writeValue(new File(filePath), dtos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export configurations: " + e.getMessage(), e);
        }
    }

    /**
     * Reads a configuration file that holds either a single object {...} or an
     * array [{...}, {...}] and returns every configuration it contains.
     */
    public List<Configuration> importAny(String filePath) {
        JsonNode root;
        try {
            root = mapper.readTree(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON file: " + e.getMessage(), e);
        }
        if (root == null || root.isNull()) {
            throw new IllegalArgumentException("The selected file is empty or not valid JSON.");
        }

        List<Configuration> list = new ArrayList<>();
        try {
            if (root.isArray()) {
                int index = 0;
                for (JsonNode node : root) {
                    index++;
                    ConfigDto dto = mapper.treeToValue(node, ConfigDto.class);
                    try {
                        validateDto(dto);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Entry #" + index + ": " + e.getMessage());
                    }
                    list.add(fromDto(dto));
                }
                if (list.isEmpty()) {
                    throw new IllegalArgumentException("The file contains an empty configuration list.");
                }
            } else {
                ConfigDto dto = mapper.treeToValue(root, ConfigDto.class);
                validateDto(dto);
                list.add(fromDto(dto));
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Invalid JSON file: " + e.getMessage(), e);
        }
        return list;
    }

    /** True when a compile or run command points at a Unix absolute path (e.g. /usr/bin/gcc). */
    public static boolean hasUnixPaths(Configuration c) {
        return isUnixPath(c.getCompileCommand()) || isUnixPath(c.getRunCommand());
    }

    private static boolean isUnixPath(String command) {
        if (command == null) return false;
        return command.trim().startsWith("/");
    }

    private void validateDto(ConfigDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Imported file does not contain a configuration.");
        }
        if (dto.name == null || dto.name.isBlank()) {
            throw new IllegalArgumentException("Imported configuration is missing 'name'.");
        }
        if (dto.language == null || dto.language.isBlank()) {
            throw new IllegalArgumentException("Imported configuration is missing 'language'.");
        }
        if (dto.runCommand == null || dto.runCommand.isBlank()) {
            throw new IllegalArgumentException("Imported configuration is missing 'runCommand'.");
        }
        if (dto.sourceFileName == null || dto.sourceFileName.isBlank()) {
            throw new IllegalArgumentException("Imported configuration is missing 'sourceFileName'.");
        }
    }

    private ConfigDto toDto(Configuration c) {
        ConfigDto dto = new ConfigDto();
        dto.id = c.getId();
        dto.name = c.getName();
        dto.language = c.getLanguage();
        dto.compileRequired = c.isCompileRequired();
        dto.compileCommand = c.getCompileCommand();
        dto.compileArgs = c.getCompileArgs();
        dto.runCommand = c.getRunCommand();
        dto.runArgs = c.getRunArgs();
        dto.sourceFileName = c.getSourceFileName();
        dto.createdAt = c.getCreatedAt();
        return dto;
    }

    private Configuration fromDto(ConfigDto dto) {
        Configuration c = new Configuration();
        c.setName(dto.name);
        c.setLanguage(dto.language);
        c.setCompileRequired(dto.compileRequired);
        c.setCompileCommand(dto.compileCommand);
        c.setCompileArgs(dto.compileArgs);
        c.setRunCommand(dto.runCommand);
        c.setRunArgs(dto.runArgs);
        c.setSourceFileName(dto.sourceFileName);
        return c;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ConfigDto {
        @JsonProperty public int id;
        @JsonProperty public String name;
        @JsonProperty public String language;
        @JsonProperty public boolean compileRequired;
        @JsonProperty public String compileCommand;
        @JsonProperty public String compileArgs;
        @JsonProperty public String runCommand;
        @JsonProperty public String runArgs;
        @JsonProperty public String sourceFileName;
        @JsonProperty public String createdAt;
    }
}
