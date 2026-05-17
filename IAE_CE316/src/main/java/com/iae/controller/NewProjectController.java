package com.iae.controller;

import com.iae.dao.ConfigurationDAO;
import com.iae.dao.ProjectDAO;
import com.iae.model.Configuration;
import com.iae.service.ConfigurationService;
import com.iae.service.ImportExportService;
import com.iae.service.ProjectService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class NewProjectController implements Initializable {

    @FXML private TextField projectNameField;
    @FXML private ComboBox<Configuration> configurationComboBox;
    @FXML private TextField zipDirectoryField;
    @FXML private TextField expectedOutputField;
    @FXML private TextField runArgsField;
    @FXML private Button createProjectButton;

    private Runnable onProjectCreated;

    private final ProjectService projectService =
            new ProjectService(new ProjectDAO(), new ConfigurationDAO());

    private final ConfigurationService configurationService =
            new ConfigurationService(new ConfigurationDAO(), new ImportExportService());

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupConfigurationComboBox();
        loadConfigurations();
    }

    public void setOnProjectCreated(Runnable callback) {
        this.onProjectCreated = callback;
    }

    @FXML
    private void onBrowseZipDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Student ZIP Directory");

        File selectedDirectory = chooser.showDialog(currentWindow());
        if (selectedDirectory != null) {
            zipDirectoryField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseExpectedOutput() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Expected Output File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = chooser.showOpenDialog(currentWindow());
        if (selectedFile != null) {
            expectedOutputField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void onCreateProject() {
        try {
            Configuration selectedConfig = configurationComboBox.getSelectionModel().getSelectedItem();

            if (selectedConfig == null) {
                showWarning("Please select a configuration.");
                return;
            }

            projectService.createProject(
                    projectNameField.getText(),
                    selectedConfig.getId(),
                    zipDirectoryField.getText(),
                    expectedOutputField.getText(),
                    runArgsField.getText()
            );

            showInfo("Project created successfully.");

            if (onProjectCreated != null) {
                onProjectCreated.run();
            }

            closeWindow();

        } catch (IllegalArgumentException e) {
            showWarning(e.getMessage());
        } catch (RuntimeException e) {
            showError("Failed to create project: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void setupConfigurationComboBox() {
        configurationComboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Configuration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        configurationComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Configuration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    private void loadConfigurations() {
        try {
            List<Configuration> configurations = configurationService.getAllConfigurations();
            configurationComboBox.setItems(FXCollections.observableArrayList(configurations));

            if (!configurations.isEmpty()) {
                configurationComboBox.getSelectionModel().selectFirst();
            } else {
                createProjectButton.setDisable(true);
                showWarning("No configuration found. Please create a configuration first.");
            }

        } catch (RuntimeException e) {
            createProjectButton.setDisable(true);
            showError("Failed to load configurations: " + e.getMessage());
        }
    }

    private Window currentWindow() {
        if (projectNameField.getScene() == null) {
            return null;
        }
        return projectNameField.getScene().getWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) projectNameField.getScene().getWindow();
        stage.close();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}