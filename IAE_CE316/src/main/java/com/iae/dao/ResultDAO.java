package com.iae.dao;

import com.iae.model.ComparisonStatus;
import com.iae.model.CompileStatus;
import com.iae.model.Result;
import com.iae.model.RunStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultDAO implements IResultDAO {

    private Connection conn() {
        return DatabaseManager.getInstance().getConnection();
    }

    @Override
    public void save(Result result) {
        String sql = """
            INSERT INTO results
            (project_id, student_id, compile_status, compile_error,
             run_status, run_error, actual_output, comparison_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(project_id, student_id) DO UPDATE SET
                compile_status    = excluded.compile_status,
                compile_error     = excluded.compile_error,
                run_status        = excluded.run_status,
                run_error         = excluded.run_error,
                actual_output     = excluded.actual_output,
                comparison_status = excluded.comparison_status,
                created_at        = datetime('now', 'localtime')
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, result.getProjectId());
            ps.setString(2, result.getStudentId());
            ps.setString(3, result.getCompileStatus() != null ? result.getCompileStatus().name() : null);
            ps.setString(4, result.getCompileError());
            ps.setString(5, result.getRunStatus() != null ? result.getRunStatus().name() : null);
            ps.setString(6, result.getRunError());
            ps.setString(7, result.getActualOutput());
            ps.setString(8, result.getComparisonStatus() != null ? result.getComparisonStatus().name() : null);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) result.setId(keys.getInt(1));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save result: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Result> findByProjectId(int projectId) {
        List<Result> list = new ArrayList<>();
        String sql = "SELECT * FROM results WHERE project_id = ? ORDER BY student_id";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list results: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public void deleteByProjectId(int projectId) {
        String sql = "DELETE FROM results WHERE project_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete results: " + e.getMessage(), e);
        }
    }

    private Result mapRow(ResultSet rs) throws SQLException {
        Result r = new Result();
        r.setId(rs.getInt("id"));
        r.setProjectId(rs.getInt("project_id"));
        r.setStudentId(rs.getString("student_id"));
        String cs = rs.getString("compile_status");
        if (cs != null) r.setCompileStatus(CompileStatus.valueOf(cs));
        r.setCompileError(rs.getString("compile_error"));
        String rs2 = rs.getString("run_status");
        if (rs2 != null) r.setRunStatus(RunStatus.valueOf(rs2));
        r.setRunError(rs.getString("run_error"));
        r.setActualOutput(rs.getString("actual_output"));
        String cmp = rs.getString("comparison_status");
        if (cmp != null) r.setComparisonStatus(ComparisonStatus.valueOf(cmp));
        r.setCreatedAt(rs.getString("created_at"));
        return r;
    }
}
