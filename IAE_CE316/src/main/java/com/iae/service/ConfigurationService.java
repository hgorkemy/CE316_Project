package com.iae.service;

import com.iae.dao.DatabaseManager;
import com.iae.dao.IConfigurationDAO;
import com.iae.model.Configuration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ConfigurationService {

    private final IConfigurationDAO configDAO;
    private final ImportExportService importExportService;

    public ConfigurationService(IConfigurationDAO configDAO, ImportExportService importExportService) {
        this.configDAO = configDAO;
        this.importExportService = importExportService;
    }

    public List<Configuration> getAllConfigurations() {
        return configDAO.findAll();
    }

    public Configuration getById(int id) {
        return configDAO.findById(id);
    }

    public void createConfiguration(Configuration config) {
        validate(config);
        if (configDAO.findByName(config.getName()) != null) {
            throw new NameConflictException(config.getName(), config);
        }
        configDAO.save(config);
    }

    public void updateConfiguration(Configuration config) {
        if (config.getId() <= 0) {
            throw new IllegalArgumentException("Configuration must have a valid id to update.");
        }
        validate(config);
        Configuration existing = configDAO.findByName(config.getName());
        if (existing != null && existing.getId() != config.getId()) {
            throw new NameConflictException(config.getName(), config);
        }
        configDAO.update(config);
    }

    public void deleteConfiguration(int id) {
        int usage = countProjectsUsing(id);
        if (usage > 0) {
            throw new ConfigurationInUseException(usage);
        }
        configDAO.delete(id);
    }

    public void exportConfiguration(Configuration config, String path) {
        importExportService.exportToJson(config, path);
    }

    public void exportAll(String path) {
        importExportService.exportAll(getAllConfigurations(), path);
    }

    /**
     * Parses a configuration file (single object or array) without saving anything.
     * The returned configurations always have id = 0 so they are treated as new.
     */
    public List<Configuration> parseImportFile(String path) {
        List<Configuration> parsed = importExportService.importAny(path);
        for (Configuration c : parsed) c.setId(0);
        return parsed;
    }

    /** Replaces the configuration that currently owns the given name with the imported one. */
    public void overwriteConfiguration(Configuration imported) {
        validate(imported);
        Configuration existing = configDAO.findByName(imported.getName());
        if (existing == null) {
            configDAO.save(imported);
            return;
        }
        imported.setId(existing.getId());
        configDAO.update(imported);
    }

    private void validate(Configuration config) {
        if (config == null) throw new IllegalArgumentException("Configuration is null.");
        requireNonBlank(config.getName(), "Name");
        requireNonBlank(config.getLanguage(), "Language");
        requireNonBlank(config.getRunCommand(), "Run command");
        requireNonBlank(config.getSourceFileName(), "Source file name");
        if (config.isCompileRequired()) {
            requireNonBlank(config.getCompileCommand(), "Compile command");
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
    }

    private int countProjectsUsing(int configId) {
        String sql = "SELECT COUNT(*) FROM projects WHERE config_id = ?";
        try (PreparedStatement ps = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, configId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check configuration usage: " + e.getMessage(), e);
        }
        return 0;
    }

    public static class NameConflictException extends RuntimeException {
        private final String conflictingName;
        private final Configuration parsedConfig;

        public NameConflictException(String name, Configuration parsedConfig) {
            super("A configuration named '" + name + "' already exists.");
            this.conflictingName = name;
            this.parsedConfig = parsedConfig;
        }

        public String getConflictingName() { return conflictingName; }
        public Configuration getParsedConfig() { return parsedConfig; }
    }

    public static class ConfigurationInUseException extends RuntimeException {
        private final int projectCount;

        public ConfigurationInUseException(int projectCount) {
            super("This configuration is used by " + projectCount + " project(s) and cannot be deleted.");
            this.projectCount = projectCount;
        }

        public int getProjectCount() { return projectCount; }
    }
}
