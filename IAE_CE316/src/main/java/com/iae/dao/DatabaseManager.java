package com.iae.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    private static final String APP_DIR =
        System.getProperty("os.name").toLowerCase().contains("win")
            ? System.getenv("APPDATA") + File.separator + "IAE"
            : System.getProperty("user.home") + File.separator + ".iae";

    private static final String DB_PATH = APP_DIR + File.separator + "iae.db";

    private DatabaseManager() {
        try {
            new File(APP_DIR).mkdirs();
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initializeSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database: " + e.getMessage(), e);
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private void initializeSchema() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS configurations (
                id               INTEGER PRIMARY KEY AUTOINCREMENT,
                name             TEXT NOT NULL UNIQUE,
                language         TEXT NOT NULL,
                compile_required INTEGER NOT NULL DEFAULT 1,
                compile_command  TEXT,
                compile_args     TEXT,
                run_command      TEXT NOT NULL,
                run_args         TEXT,
                source_filename  TEXT NOT NULL,
                created_at       TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS projects (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                name            TEXT NOT NULL UNIQUE,
                config_id       INTEGER NOT NULL,
                zip_directory   TEXT,
                expected_output TEXT,
                run_args        TEXT,
                status          TEXT NOT NULL DEFAULT 'NOT_RUN',
                created_at      TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                last_run_at     TEXT,
                FOREIGN KEY (config_id) REFERENCES configurations(id)
            )
        """);

        stmt.executeUpdate("""
            CREATE TABLE IF NOT EXISTS results (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                project_id        INTEGER NOT NULL,
                student_id        TEXT NOT NULL,
                compile_status    TEXT,
                compile_error     TEXT,
                run_status        TEXT,
                run_error         TEXT,
                actual_output     TEXT,
                comparison_status TEXT,
                created_at        TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
                FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
                UNIQUE(project_id, student_id)
            )
        """);

        stmt.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_results_project_id ON results(project_id)");
        stmt.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_results_student ON results(project_id, student_id)");
        stmt.executeUpdate(
            "CREATE INDEX IF NOT EXISTS idx_projects_config_id ON projects(config_id)");

        stmt.close();
    }
}
