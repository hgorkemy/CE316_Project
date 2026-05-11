package com.iae.dao;

import com.iae.model.Configuration;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigurationDAO implements IConfigurationDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    @Override
    public void save(Configuration config) {
        String sql = """
            INSERT INTO configurations
            (name, language, compile_required, compile_command,
             compile_args, run_command, run_args, source_filename)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, config.getName());
            ps.setString(2, config.getLanguage());
            ps.setInt(3, config.isCompileRequired() ? 1 : 0);
            ps.setString(4, config.getCompileCommand());
            ps.setString(5, config.getCompileArgs());
            ps.setString(6, config.getRunCommand());
            ps.setString(7, config.getRunArgs());
            ps.setString(8, config.getSourceFileName());
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) config.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save configuration: " + e.getMessage(), e);
        }
    }

    @Override
    public Configuration findById(int id) {
        String sql = "SELECT * FROM configurations WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Configuration not found: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Configuration findByName(String name) {
        String sql = "SELECT * FROM configurations WHERE name = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Configuration not found: " + e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Configuration> findAll() {
        List<Configuration> list = new ArrayList<>();
        String sql = "SELECT * FROM configurations ORDER BY name";
        try (Statement stmt = conn().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list configurations: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void update(Configuration config) {
        String sql = """
            UPDATE configurations SET
                name = ?, language = ?, compile_required = ?,
                compile_command = ?, compile_args = ?,
                run_command = ?, run_args = ?, source_filename = ?
            WHERE id = ?
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, config.getName());
            ps.setString(2, config.getLanguage());
            ps.setInt(3, config.isCompileRequired() ? 1 : 0);
            ps.setString(4, config.getCompileCommand());
            ps.setString(5, config.getCompileArgs());
            ps.setString(6, config.getRunCommand());
            ps.setString(7, config.getRunArgs());
            ps.setString(8, config.getSourceFileName());
            ps.setInt(9, config.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update configuration: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM configurations WHERE id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete configuration: " + e.getMessage(), e);
        }
    }

    private Configuration mapRow(ResultSet rs) throws SQLException {
        Configuration c = new Configuration();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setLanguage(rs.getString("language"));
        c.setCompileRequired(rs.getInt("compile_required") == 1);
        c.setCompileCommand(rs.getString("compile_command"));
        c.setCompileArgs(rs.getString("compile_args"));
        c.setRunCommand(rs.getString("run_command"));
        c.setRunArgs(rs.getString("run_args"));
        c.setSourceFileName(rs.getString("source_filename"));
        c.setCreatedAt(rs.getString("created_at"));
        return c;
    }
}
