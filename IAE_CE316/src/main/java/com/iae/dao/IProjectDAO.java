package com.iae.dao;

import com.iae.model.Project;
import com.iae.model.ProjectStatus;
import java.util.List;

public interface IProjectDAO {
    void save(Project project);
    Project findById(int id);
    List<Project> findAll();
    void update(Project project);
    void delete(int id);
    void updateStatus(int id, ProjectStatus status);
}
