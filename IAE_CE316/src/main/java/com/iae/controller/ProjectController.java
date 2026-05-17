package com.iae.controller;

import com.iae.dao.ConfigurationDAO;
import com.iae.dao.ProjectDAO;
import com.iae.dao.ResultDAO;
import com.iae.model.*;
import com.iae.service.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;

public class ProjectController {

    @FXML private Label projectNameLabel;
    @FXML private Label configurationNameLabel;
    @FXML private Label zipDirectoryLabel;
    @FXML private Label messageLabel;
    @FXML private Label summaryLabel;

    @FXML private Button runButton;
    @FXML private Button changeZipButton;
    @FXML private Button exportCsvButton;

    @FXML private ComboBox<String> filterComboBox;
    @FXML private ProgressBar progressBar;

    @FXML private TableView<Result> resultTable;
    @FXML private TableColumn<Result, String> studentIdColumn;
    @FXML private TableColumn<Result, String> compileColumn;
    @FXML private TableColumn<Result, String> runColumn;
    @FXML private TableColumn<Result, String> comparisonColumn;
    @FXML private TableColumn<Result, Void> detailColumn;

    private Project project;

    private final ProjectDAO projectDAO = new ProjectDAO();
    private final ConfigurationDAO configurationDAO = new ConfigurationDAO();
    private final ResultDAO resultDAO = new ResultDAO();

    private final ReportService reportService = new ReportService(resultDAO);

    private final ProjectService projectService =
            new ProjectService(projectDAO, configurationDAO);

    private final RunnerService runnerService =
            new RunnerService(
                    new ZipService(),
                    new ExecutionService(),
                    new ComparisonService(),
                    reportService,
                    projectService,
                    configurationDAO
            );

    private final ObservableList<Result> results = FXCollections.observableArrayList();
    private FilteredList<Result> filteredResults;

    @FXML
    private void initialize() {
        filterComboBox.setItems(FXCollections.observableArrayList(
                "All", "Pass", "Fail", "Compile Error", "Timeout"
        ));
        filterComboBox.getSelectionModel().select("All");

        studentIdColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStudentId()));

        compileColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getCompileStatus())));

        runColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getRunStatus())));

        comparisonColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getComparisonStatus())));

        setStatusCellStyle(compileColumn);
        setStatusCellStyle(runColumn);
        setStatusCellStyle(comparisonColumn);
        addDetailButtons();

        filteredResults = new FilteredList<>(results, r -> true);
        resultTable.setItems(filteredResults);

        resultTable.setPlaceholder(new Label("No results yet. Click Run to start."));

        filterComboBox.setOnAction(e -> applyFilter());

        progressBar.setVisible(false);
        messageLabel.setText("");
    }

    public void setProject(Project project) {
        this.project = project;

        projectNameLabel.setText(project.getName());
        configurationNameLabel.setText(project.getConfigName());
        zipDirectoryLabel.setText(project.getZipDirectory());

        refreshResults();
    }

    @FXML
    private void onRun() {
        if (project == null) return;

        runButton.setDisable(true);
        changeZipButton.setDisable(true);
        exportCsvButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);
        messageLabel.setText("Running project...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                runnerService.runProject(project, progress -> updateProgress(progress, 1.0));
                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            runButton.setDisable(false);
            changeZipButton.setDisable(false);
            exportCsvButton.setDisable(false);

            messageLabel.setText("Run completed.");
            refreshResults();
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setVisible(false);
            runButton.setDisable(false);
            changeZipButton.setDisable(false);
            exportCsvButton.setDisable(false);

            Throwable ex = task.getException();
            showError("Run failed: " + (ex == null ? "Unknown error" : ex.getMessage()));
            messageLabel.setText("Run failed.");
            refreshResults();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onChangeZipDirectory() {
        if (project == null) return;

        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select ZIP Directory");

        File current = new File(project.getZipDirectory());
        if (current.exists() && current.isDirectory()) {
            chooser.setInitialDirectory(current);
        }

        File selected = chooser.showDialog(currentWindow());
        if (selected == null) return;

        project.setZipDirectory(selected.getAbsolutePath());
        projectDAO.update(project);
        zipDirectoryLabel.setText(project.getZipDirectory());

        showInfo("ZIP directory updated.");
    }

    @FXML
    private void onExportCsv() {
        if (project == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Results as CSV");
        chooser.setInitialFileName(project.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + "_results.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));

        File file = chooser.showSaveDialog(currentWindow());
        if (file == null) return;

        try {
            reportService.exportCsv(project.getId(), file.getAbsolutePath());
            showInfo("CSV exported successfully.");
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void refreshResults() {
        if (project == null) return;

        results.setAll(reportService.getResults(project.getId()));
        applyFilter();

        if (results.isEmpty()) {
            summaryLabel.setText("Summary: No results yet.");
            messageLabel.setText("No results yet. Click Run to start.");
        } else {
            summaryLabel.setText(reportService.generateSummary(project.getId()));
            messageLabel.setText("");
        }
    }

    private void applyFilter() {
        String filter = filterComboBox.getSelectionModel().getSelectedItem();

        filteredResults.setPredicate(result -> {
            if (filter == null || filter.equals("All")) {
                return true;
            }

            return switch (filter) {
                case "Pass" ->
                        result.getCompileStatus() == CompileStatus.PASS
                                && result.getRunStatus() == RunStatus.PASS
                                && result.getComparisonStatus() == ComparisonStatus.PASS;

                case "Fail" ->
                        result.getComparisonStatus() == ComparisonStatus.FAIL;

                case "Compile Error" ->
                        result.getCompileStatus() == CompileStatus.FAIL;

                case "Timeout" ->
                        result.getRunStatus() == RunStatus.TIMEOUT;

                default -> true;
            };
        });
    }

    private void addDetailButtons() {
        detailColumn.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("View");

            {
                button.setOnAction(e -> {
                    Result result = getTableView().getItems().get(getIndex());
                    openDetailWindow(result);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(button));
            }
        });
    }

    private void openDetailWindow(Result result) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/result_detail.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Result Detail - " + result.getStudentId());
            stage.setScene(new Scene(loader.load(), 800, 600));

            ResultDetailController controller = loader.getController();
            String expectedOutput = Files.readString(new File(project.getExpectedOutput()).toPath());
            controller.setResult(result, expectedOutput);

            stage.showAndWait();
        } catch (Exception e) {
            showError("Failed to open result detail: " + e.getMessage());
        }
    }

    private void setStatusCellStyle(TableColumn<Result, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);

                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(status);

                switch (status) {
                    case "PASS" -> setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                    case "FAIL", "ERROR" -> setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    case "TIMEOUT" -> setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                    case "SKIPPED" -> setStyle("-fx-text-fill: gray; -fx-font-weight: bold;");
                    default -> setStyle("");
                }
            }
        });
    }

    private Stage currentWindow() {
        return (Stage) resultTable.getScene().getWindow();
    }

    private void showInfo(String message) {
        Platform.runLater(() ->
                new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait()
        );
    }

    private void showError(String message) {
        Platform.runLater(() ->
                new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait()
        );
    }
}