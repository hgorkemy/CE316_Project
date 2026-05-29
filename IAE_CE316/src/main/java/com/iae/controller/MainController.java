package com.iae.controller;

import com.iae.dao.ProjectDAO;
import com.iae.dao.IProjectDAO;
import com.iae.model.Project;
import com.iae.model.ProjectStatus;
import com.iae.service.RecentProjectsService;
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
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private TableView<Project> projectTable;
    @FXML private TableColumn<Project, String> colName;
    @FXML private TableColumn<Project, String> colConfig;
    @FXML private TableColumn<Project, ProjectStatus> colStatus;
    @FXML private TableColumn<Project, String> colDate;
    @FXML private Menu recentProjectsMenu;

    private final IProjectDAO projectDAO = new ProjectDAO();
    private final RecentProjectsService recentService = new RecentProjectsService();
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
        refreshRecentMenu();
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
    private void onSaveProject() {
        Project selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a project to save.");
            return;
        }
        projectDAO.update(selected);
        recentService.addRecent(selected.getId(), selected.getName());
        refreshRecentMenu();
        Alert info = new Alert(Alert.AlertType.INFORMATION,
                "Project '" + selected.getName() + "' has been saved.", ButtonType.OK);
        info.setTitle("Save Project");
        info.showAndWait();
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
            recentService.remove(selected.getId());
            refreshRecentMenu();
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
            stage.setTitle("IAE User Manual");
            stage.setScene(new Scene(loader.load(), 800, 600));
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            stage.show();
        } catch (Exception e) {
            showError("Failed to open Help: " + e.getMessage());
        }
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About IAE");
        alert.setHeaderText("Integrated Assignment Environment v1.0");
        alert.setContentText(
                "CE 316 — Software Engineering\n" +
                        "Izmir University of Economics\n\n" +
                        "Team:\n" +
                        "  Ant Efe Kaynarcali\n" +
                        "  Efe Cengiz\n" +
                        "  Egemen Timbil\n" +
                        "  Halil Gorkem Yigit\n" +
                        "  Oyku Doga Dabak"
        );
        alert.showAndWait();
    }

    private void openSelectedProject() {
        Project selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a project to open.");
            return;
        }
        openProject(selected);
    }

    private void openProject(Project project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/project.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Project: " + project.getName());
            stage.setScene(new Scene(loader.load(), 900, 600));
            ProjectController ctrl = loader.getController();
            ctrl.setProject(project);
            recentService.addRecent(project.getId(), project.getName());
            refreshRecentMenu();
            stage.setOnHidden(e -> loadProjects());
            stage.show();
        } catch (Exception e) {
            showError("Failed to open project: " + e.getMessage());
        }
    }

    private void openProjectById(int projectId) {
        Project project = projectDAO.findById(projectId);
        if (project == null) {
            showWarning("This project no longer exists. It may have been deleted.");
            recentService.remove(projectId);
            refreshRecentMenu();
            return;
        }
        openProject(project);
    }

    private void refreshRecentMenu() {
        if (recentProjectsMenu == null) return;
        recentProjectsMenu.getItems().clear();
        List<RecentProjectsService.RecentEntry> entries = recentService.getRecent();
        if (entries.isEmpty()) {
            MenuItem empty = new MenuItem("(No recent projects)");
            empty.setDisable(true);
            recentProjectsMenu.getItems().add(empty);
        } else {
            for (RecentProjectsService.RecentEntry entry : entries) {
                MenuItem item = new MenuItem(entry.name);
                final int id = entry.id;
                item.setOnAction(e -> openProjectById(id));
                recentProjectsMenu.getItems().add(item);
            }
            recentProjectsMenu.getItems().add(new SeparatorMenuItem());
            MenuItem clearItem = new MenuItem("Clear Recent Projects");
            clearItem.setOnAction(e -> {
                entries.forEach(en -> recentService.remove(en.id));
                refreshRecentMenu();
            });
            recentProjectsMenu.getItems().add(clearItem);
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
