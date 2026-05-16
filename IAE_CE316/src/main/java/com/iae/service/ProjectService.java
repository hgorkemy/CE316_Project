package com.iae.service;

import com.iae.dao.IConfigurationDAO;
import com.iae.dao.IProjectDAO;
import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.ProjectStatus;

import java.io.File;
import java.util.List;

public class ProjectService {

    private final IProjectDAO projectDAO;
    private final IConfigurationDAO configurationDAO;

    public ProjectService(IProjectDAO projectDAO, IConfigurationDAO configurationDAO) {
        this.projectDAO = projectDAO;
        this.configurationDAO = configurationDAO;
    }

    public List<Project> getAllProjects() {
        return projectDAO.findAll();
    }

    public Project getById(int id) {
        return projectDAO.findById(id);
    }

    public void createProject(String name,
                              int configId,
                              String zipDirectory,
                              String expectedOutputPath,
                              String runArgs) {
        validateProjectData(name, configId, zipDirectory, expectedOutputPath);

        Project project = new Project();
        project.setName(name.trim());
        project.setConfigId(configId);
        project.setZipDirectory(zipDirectory.trim());
        project.setExpectedOutput(expectedOutputPath.trim());
        project.setRunArgs(runArgs == null ? "" : runArgs.trim());
        project.setStatus(ProjectStatus.NOT_RUN);

        projectDAO.save(project);
    }

    public void deleteProject(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid project id.");
        }

        projectDAO.delete(id);
    }

    public Project openProject(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid project id.");
        }

        Project project = projectDAO.findById(id);
        if (project == null) {
            throw new IllegalArgumentException("Project could not be found.");
        }

        return project;
    }

    public void updateProjectStatus(int id, ProjectStatus status) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid project id.");
        }

        if (status == null) {
            throw new IllegalArgumentException("Project status cannot be empty.");
        }

        projectDAO.updateStatus(id, status);
    }

    private void validateProjectData(String name,
                                     int configId,
                                     String zipDirectory,
                                     String expectedOutputPath) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name is required.");
        }

        if (configId <= 0) {
            throw new IllegalArgumentException("Please select a valid configuration.");
        }

        Configuration configuration = configurationDAO.findById(configId);
        if (configuration == null) {
            throw new IllegalArgumentException("Selected configuration does not exist.");
        }

        if (zipDirectory == null || zipDirectory.trim().isEmpty()) {
            throw new IllegalArgumentException("ZIP directory is required.");
        }

        File zipDir = new File(zipDirectory.trim());
        if (!zipDir.exists() || !zipDir.isDirectory()) {
            throw new IllegalArgumentException("ZIP directory does not exist.");
        }

        if (expectedOutputPath == null || expectedOutputPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Expected output file is required.");
        }

        File expectedOutput = new File(expectedOutputPath.trim());
        if (!expectedOutput.exists() || !expectedOutput.isFile()) {
            throw new IllegalArgumentException("Expected output file does not exist.");
        }
    }
}