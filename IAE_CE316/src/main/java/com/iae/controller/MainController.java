package com.iae.controller;

import com.iae.dao.ProjectDAO;
import com.iae.dao.IProjectDAO;
import com.iae.model.Project;
import com.iae.model.ProjectStatus;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TableView<Project> projectTable;
    @FXML private TableColumn<Project, String> colName;
    @FXML private TableColumn<Project, String> colConfig;
    @FXML private TableColumn<Project, ProjectStatus> colStatus;
    @FXML private TableColumn<Project, String> colDate;

    private final IProjectDAO projectDAO = new ProjectDAO();
    private final ObservableList<Project> projects = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colConfig.setCellValueFactory(new PropertyValueFactory<>("configName"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        projectTable.setItems(projects);
        projectTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openSelectedProject();
        });

        loadProjects();
    }

    @FXML
    private void onNewProject() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/new_project.fxml"));
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("New Project");
            dialog.setScene(new Scene(loader.load(), 620, 520));
            dialog.setMinWidth(620);
            dialog.setMinHeight(520);
            NewProjectController ctrl = loader.getController();
            ctrl.setOnProjectCreated(this::loadProjects);
            dialog.showAndWait();
        } catch (Exception e) {
            showError("Failed to open New Project: " + e.getMessage());
        }
    }

    @FXML
    private void onOpenProject() {
        openSelectedProject();
    }

    @FXML
    private void onDeleteProject() {
        Project selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a project to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete '" + selected.getName() + "'? This action cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Project");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.YES) {
            projectDAO.delete(selected.getId());
            loadProjects();
        }
    }

    @FXML
    private void onManageConfigurations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/configuration.fxml"));
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Configurations");
            stage.setScene(new Scene(loader.load(), 800, 500));
            stage.showAndWait();
        } catch (Exception e) {
            showError("Failed to open Configurations: " + e.getMessage());
        }
    }

    @FXML
    private void onHelp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/help.fxml"));
            Stage stage = new Stage();
            stage.setTitle("User Manual");
            stage.setScene(new Scene(loader.load(), 800, 600));
            stage.show();
        } catch (Exception e) {
            showError("Failed to open Help: " + e.getMessage());
        }
    }

    private void openSelectedProject() {
        Project selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a project to open.");
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/project.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Project: " + selected.getName());
            stage.setScene(new Scene(loader.load(), 900, 600));
            ProjectController ctrl = loader.getController();
            ctrl.setProject(selected);
            stage.show();
        } catch (Exception e) {
            showError("Failed to open project: " + e.getMessage());
        }
    }

    @FXML
    private void onExit() {
        javafx.application.Platform.exit();
    }

    public void loadProjects() {
        projects.setAll(projectDAO.findAll());
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    private void showWarning(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }
}
