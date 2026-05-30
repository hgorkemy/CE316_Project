package com.iae.controller;

import com.iae.dao.IResultDAO;
import com.iae.dao.ResultDAO;
import com.iae.model.Project;
import com.iae.model.Result;
import com.iae.service.RunnerService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;
import java.util.ResourceBundle;

public class ProjectController implements Initializable {

    @FXML private Label lblProjectName;
    @FXML private Label lblConfiguration;
    @FXML private Label lblZipDirectory;
    @FXML private Label lblStatus;
    @FXML private Label lblProgress;
    @FXML private Button btnRun;
    @FXML private Button btnPreview;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea logArea;
    @FXML private TableView<Result> resultTable;
    @FXML private TableColumn<Result, String> colStudentId;
    @FXML private TableColumn<Result, String> colCompileStatus;
    @FXML private TableColumn<Result, String> colRunStatus;
    @FXML private TableColumn<Result, String> colComparisonStatus;
    @FXML private TableColumn<Result, Void> colDetail;

    private Project project;
    private final RunnerService runnerService = new RunnerService();
    private final IResultDAO resultDAO = new ResultDAO();
    private final ObservableList<Result> results = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colStudentId.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        colCompileStatus.setCellValueFactory(cell -> new SimpleStringProperty(valueOf(cell.getValue().getCompileStatus())));
        colRunStatus.setCellValueFactory(cell -> new SimpleStringProperty(valueOf(cell.getValue().getRunStatus())));
        colComparisonStatus.setCellValueFactory(cell -> new SimpleStringProperty(valueOf(cell.getValue().getComparisonStatus())));
        colDetail.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("View");
            {
                btn.setOnAction(e -> {
                    Result result = getTableView().getItems().get(getIndex());
                    openDetailWindow(result);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(btn));
            }
        });

        resultTable.setItems(results);
        progressBar.setProgress(0);
        lblProgress.setText("Ready");
    }

    public void setProject(Project project) {
        this.project = project;
        lblProjectName.setText(project.getName());
        lblConfiguration.setText(project.getConfigName() == null ? "Configuration ID: " + project.getConfigId() : project.getConfigName());
        lblZipDirectory.setText(project.getZipDirectory() == null ? "" : project.getZipDirectory());
        lblStatus.setText(project.getStatus() == null ? "" : project.getStatus().name());
        loadResults();
        previewZipFiles();
    }

    @FXML
    private void onBack() {
        ((javafx.stage.Stage) resultTable.getScene().getWindow()).close();
    }

    @FXML
    private void onRunProject() {
        if (project == null) {
            showError("Project was not loaded.");
            return;
        }

        List<File> zipFiles = runnerService.findZipFiles(project);

        if (zipFiles.isEmpty()) {
            showWarning("No ZIP files found in the selected directory.");
            return;
        }

        btnRun.setDisable(true);
        btnPreview.setDisable(true);
        progressBar.setProgress(0);
        logArea.clear();
        appendLog("Starting run for " + zipFiles.size() + " ZIP files.");

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                return runnerService.runProject(project, (message, done, total) -> Platform.runLater(() -> {
                    appendLog(message);
                    lblProgress.setText(message);

                    if (total > 0) {
                        progressBar.setProgress((double) done / total);
                    }
                }));
            }
        };

        task.setOnSucceeded(event -> {
            btnRun.setDisable(false);
            btnPreview.setDisable(false);
            progressBar.setProgress(1);
            lblStatus.setText("COMPLETED");
            loadResults();

            int processed = task.getValue();
            String message = "Run completed. " + processed + " students processed.";
            lblProgress.setText(message);
            appendLog(message);
            showInfo(message);
        });

        task.setOnFailed(event -> {
            btnRun.setDisable(false);
            btnPreview.setDisable(false);

            Throwable error = task.getException();
            String message = error == null ? "Run failed." : error.getMessage();
            lblProgress.setText("Run failed.");
            appendLog("Run failed: " + message);
            loadResults();
            showError("Run failed: " + message);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onPreviewZipFiles() {
        previewZipFiles();
    }

    private void openDetailWindow(Result result) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/result_detail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Result Detail - " + result.getStudentId());
            stage.setScene(new Scene(loader.load(), 1100, 700));
            ResultDetailController ctrl = loader.getController();
            String expectedOutput = "";
            try {
                expectedOutput = Files.readString(new File(project.getExpectedOutput()).toPath());
            } catch (IOException ignored) {}
            ctrl.setResult(result, expectedOutput);
            stage.showAndWait();
        } catch (Exception e) {
            showError("Failed to open detail: " + e.getMessage());
        }
    }

    private void loadResults() {
        if (project == null) {
            return;
        }

        results.setAll(resultDAO.findByProjectId(project.getId()));
    }

    private void previewZipFiles() {
        if (project == null) {
            return;
        }

        List<File> zipFiles = runnerService.findZipFiles(project);
        logArea.clear();

        if (zipFiles.isEmpty()) {
            appendLog("No ZIP files found in the selected directory.");
            lblProgress.setText("No ZIP files found.");
            progressBar.setProgress(0);
            return;
        }

        appendLog("Found " + zipFiles.size() + " ZIP files:");

        for (File zipFile : zipFiles) {
            appendLog("✓ " + zipFile.getName());
        }

        lblProgress.setText("Found " + zipFiles.size() + " ZIP files.");
        progressBar.setProgress(0);
    }

    private void appendLog(String message) {
        logArea.appendText(message + System.lineSeparator());
    }

    private String valueOf(Object value) {
        return value == null ? "" : value.toString();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    private void showWarning(String message) {
        new Alert(Alert.AlertType.WARNING, message, ButtonType.OK).showAndWait();
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }
}