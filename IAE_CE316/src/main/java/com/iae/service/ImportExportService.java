package com.iae.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    public Configuration importFromJson(String filePath) {
        try {
            ConfigDto dto = mapper.readValue(new File(filePath), ConfigDto.class);
            if (dto.name == null || dto.name.isBlank()) {
                throw new IllegalArgumentException("Imported file is missing 'name'.");
            }
            if (dto.language == null || dto.language.isBlank()) {
                throw new IllegalArgumentException("Imported file is missing 'language'.");
            }
            if (dto.runCommand == null || dto.runCommand.isBlank()) {
                throw new IllegalArgumentException("Imported file is missing 'runCommand'.");
            }
            if (dto.sourceFileName == null || dto.sourceFileName.isBlank()) {
                throw new IllegalArgumentException("Imported file is missing 'sourceFileName'.");
            }
            return fromDto(dto);
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON file: " + e.getMessage(), e);
        }
    }

    public List<Configuration> importAllFromJson(String filePath) {
        try {
            ConfigDto[] dtos = mapper.readValue(new File(filePath), ConfigDto[].class);
            List<Configuration> list = new ArrayList<>();
            for (ConfigDto dto : dtos) list.add(fromDto(dto));
            return list;
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON array file: " + e.getMessage(), e);
        }
    }

    private ConfigDto toDto(Configuration c) {
        ConfigDto dto = new ConfigDto();
        dto.name = c.getName();
        dto.language = c.getLanguage();
        dto.compileRequired = c.isCompileRequired();
        dto.compileCommand = c.getCompileCommand();
        dto.compileArgs = c.getCompileArgs();
        dto.runCommand = c.getRunCommand();
        dto.runArgs = c.getRunArgs();
        dto.sourceFileName = c.getSourceFileName();
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
        @JsonProperty public String name;
        @JsonProperty public String language;
        @JsonProperty public boolean compileRequired;
        @JsonProperty public String compileCommand;
        @JsonProperty public String compileArgs;
        @JsonProperty public String runCommand;
        @JsonProperty public String runArgs;
        @JsonProperty public String sourceFileName;
    }
}
