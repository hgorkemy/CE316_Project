package com.iae.dao;

import com.iae.model.Project;
import com.iae.model.ProjectStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO implements IProjectDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    @Override
    public void save(Project project) {
        String sql = """
            INSERT INTO projects
            (name, config_id, zip_directory, expected_output, run_args, status)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, project.getName());
            ps.setInt(2, project.getConfigId());
            ps.setString(3, project.getZipDirectory());
            ps.setString(4, project.getExpectedOutput());
            ps.setString(5, project.getRunArgs());
            ps.setString(6, project.getStatus().name());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) project.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save project: " + e.getMessage(), e);
        }
    }

    @Override
    public Project findById(int id) {
        String sql = """
            SELECT p.*, c.name AS config_name
            FROM projects p
            LEFT JOIN configurations c ON p.config_id = c.id
            WHERE p.id = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Project not found: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Project> findAll() {
        List<Project> list = new ArrayList<>();
        String sql = """
            SELECT p.*, c.name AS config_name
            FROM projects p
            LEFT JOIN configurations c ON p.config_id = c.id
            ORDER BY p.created_at DESC
        """;
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list projects: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void update(Project project) {
        String sql = """
            UPDATE projects SET
                name = ?, config_id = ?, zip_directory = ?,
                expected_output = ?, run_args = ?, status = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, project.getName());
            ps.setInt(2, project.getConfigId());
            ps.setString(3, project.getZipDirectory());
            ps.setString(4, project.getExpectedOutput());
            ps.setString(5, project.getRunArgs());
            ps.setString(6, project.getStatus().name());
            ps.setInt(7, project.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update project: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete project: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateStatus(int id, ProjectStatus status) {
        String sql = "UPDATE projects SET status = ?, last_run_at = datetime('now', 'localtime') WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update project status: " + e.getMessage(), e);
        }
    }

    private Project mapRow(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setConfigId(rs.getInt("config_id"));
        p.setConfigName(rs.getString("config_name"));
        p.setZipDirectory(rs.getString("zip_directory"));
        p.setExpectedOutput(rs.getString("expected_output"));
        p.setRunArgs(rs.getString("run_args"));
        p.setStatus(ProjectStatus.valueOf(rs.getString("status")));
        p.setCreatedAt(rs.getString("created_at"));
        p.setLastRunAt(rs.getString("last_run_at"));
        return p;
    }
}
