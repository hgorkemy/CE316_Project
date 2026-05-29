package com.iae.controller;

import com.iae.dao.ConfigurationDAO;
import com.iae.model.Configuration;
import com.iae.service.ConfigurationService;
import com.iae.service.ConfigurationService.ConfigurationInUseException;
import com.iae.service.ConfigurationService.NameConflictException;
import com.iae.service.ImportExportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class ConfigurationController implements Initializable {

    @FXML private ListView<Configuration> configListView;

    @FXML private Label formTitleLabel;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> languageCombo;
    @FXML private CheckBox compileRequiredCheck;
    @FXML private TextField compileCommandField;
    @FXML private TextField compileArgsField;
    @FXML private TextField runCommandField;
    @FXML private TextField runArgsField;
    @FXML private TextField sourceFileNameField;

    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button exportButton;

    private final ConfigurationService configurationService =
            new ConfigurationService(new ConfigurationDAO(), new ImportExportService());

    private final ObservableList<Configuration> items = FXCollections.observableArrayList();
    private Configuration editingConfig;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configListView.setItems(items);
        configListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Configuration item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });

        languageCombo.getItems().addAll("Java", "Python", "C", "C++");
        languageCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) applyPreset(newVal);
        });

        configListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldSel, newSel) -> {
                    if (newSel != null) loadIntoForm(newSel);
                    updateButtonStates();
                });

        compileRequiredCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            applyCompileFieldsState(newVal);
        });

        loadConfigurations();
        clearForm();
        updateButtonStates();
    }

    @FXML
    private void onAdd() {
        configListView.getSelectionModel().clearSelection();
        clearForm();
        nameField.requestFocus();
        formTitleLabel.setText("New Configuration");
    }

    @FXML
    private void onEdit() {
        Configuration selected = configListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a configuration to edit.");
            return;
        }
        loadIntoForm(selected);
        nameField.requestFocus();
    }

    @FXML
    private void onDelete() {
        Configuration selected = configListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a configuration to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete configuration '" + selected.getName() + "'?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Configuration");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        try {
            configurationService.deleteConfiguration(selected.getId());
            loadConfigurations();
            clearForm();
        } catch (ConfigurationInUseException e) {
            showError(e.getMessage());
        } catch (RuntimeException e) {
            showError("Failed to delete configuration: " + e.getMessage());
        }
    }

    @FXML
    private void onSave() {
        try {
            Configuration toSave = readForm();
            if (editingConfig != null && editingConfig.getId() > 0) {
                toSave.setId(editingConfig.getId());
                configurationService.updateConfiguration(toSave);
            } else {
                configurationService.createConfiguration(toSave);
            }
            loadConfigurations();
            selectByName(toSave.getName());
            showInfo("Configuration saved.");
        } catch (NameConflictException e) {
            showError(e.getMessage());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (RuntimeException e) {
            showError("Failed to save configuration: " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        configListView.getSelectionModel().clearSelection();
        clearForm();
    }

    @FXML
    private void onImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Configuration");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showOpenDialog(currentWindow());
        if (file == null) return;

        try {
            Configuration imported = configurationService.importConfiguration(file.getAbsolutePath());
            loadConfigurations();
            selectByName(imported.getName());
            showInfo("Configuration '" + imported.getName() + "' imported.");
        } catch (NameConflictException e) {
            handleImportConflict(e);
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (RuntimeException e) {
            showError("Failed to import configuration: " + e.getMessage());
        }
    }

    @FXML
    private void onExport() {
        Configuration selected = configListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a configuration to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Configuration");
        chooser.setInitialFileName(safeFileName(selected.getName()) + ".json");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showSaveDialog(currentWindow());
        if (file == null) return;

        try {
            configurationService.exportConfiguration(selected, file.getAbsolutePath());
            showInfo("Configuration exported to " + file.getName());
        } catch (RuntimeException e) {
            showError("Failed to export configuration: " + e.getMessage());
        }
    }

    private void handleImportConflict(NameConflictException e) {
        TextInputDialog dialog = new TextInputDialog(e.getConflictingName() + " (imported)");
        dialog.setTitle("Name Conflict");
        dialog.setHeaderText("A configuration named '" + e.getConflictingName() + "' already exists.");
        dialog.setContentText("Enter a new name (or Cancel to abort):");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;
        String newName = result.get().trim();
        if (newName.isEmpty()) {
            showError("Name cannot be empty.");
            return;
        }
        try {
            Configuration parsed = e.getParsedConfig();
            parsed.setId(0);
            parsed.setName(newName);
            configurationService.createConfiguration(parsed);
            loadConfigurations();
            selectByName(newName);
            showInfo("Configuration imported as '" + newName + "'.");
        } catch (NameConflictException dup) {
            showError(dup.getMessage());
        } catch (RuntimeException ex) {
            showError("Failed to import configuration: " + ex.getMessage());
        }
    }

    private void loadConfigurations() {
        items.setAll(configurationService.getAllConfigurations());
    }

    private void loadIntoForm(Configuration c) {
        editingConfig = c;
        formTitleLabel.setText("Edit Configuration");
        nameField.setText(nullToEmpty(c.getName()));
        languageCombo.setValue(nullToEmpty(c.getLanguage()));
        compileRequiredCheck.setSelected(c.isCompileRequired());
        compileCommandField.setText(nullToEmpty(c.getCompileCommand()));
        compileArgsField.setText(nullToEmpty(c.getCompileArgs()));
        runCommandField.setText(nullToEmpty(c.getRunCommand()));
        runArgsField.setText(nullToEmpty(c.getRunArgs()));
        sourceFileNameField.setText(nullToEmpty(c.getSourceFileName()));
        applyCompileFieldsState(c.isCompileRequired());
    }

    private void clearForm() {
        editingConfig = null;
        formTitleLabel.setText("Configuration Details");
        nameField.clear();
        languageCombo.setValue(null);
        languageCombo.getEditor().clear();
        compileRequiredCheck.setSelected(true);
        compileCommandField.clear();
        compileArgsField.clear();
        runCommandField.clear();
        runArgsField.clear();
        sourceFileNameField.clear();
        applyCompileFieldsState(true);
    }

    private Configuration readForm() {
        Configuration c = new Configuration();
        c.setName(trim(nameField.getText()));
        String lang = languageCombo.getEditor().getText();
        if (lang == null || lang.isBlank()) lang = languageCombo.getValue();
        c.setLanguage(trim(lang));
        c.setCompileRequired(compileRequiredCheck.isSelected());
        c.setCompileCommand(trim(compileCommandField.getText()));
        c.setCompileArgs(trim(compileArgsField.getText()));
        c.setRunCommand(trim(runCommandField.getText()));
        c.setRunArgs(trim(runArgsField.getText()));
        c.setSourceFileName(trim(sourceFileNameField.getText()));
        return c;
    }

    private void applyPreset(String language) {
        switch (language) {
            case "Java" -> {
                compileRequiredCheck.setSelected(true);
                compileCommandField.setText("javac");
                compileArgsField.setText("{sourceFile}");
                runCommandField.setText("java");
                runArgsField.setText("{outputName}");
                sourceFileNameField.setText("Main.java");
            }
            case "Python" -> {
                compileRequiredCheck.setSelected(false);
                compileCommandField.clear();
                compileArgsField.clear();
                runCommandField.setText("python3");
                runArgsField.setText("{sourceFile}");
                sourceFileNameField.setText("main.py");
            }
            case "C" -> {
                compileRequiredCheck.setSelected(true);
                compileCommandField.setText("gcc");
                compileArgsField.setText("{sourceFile} -o {outputName}");
                runCommandField.setText("./{outputName}");
                runArgsField.setText("{args}");
                sourceFileNameField.setText("main.c");
            }
            case "C++" -> {
                compileRequiredCheck.setSelected(true);
                compileCommandField.setText("g++");
                compileArgsField.setText("{sourceFile} -o {outputName}");
                runCommandField.setText("./{outputName}");
                runArgsField.setText("{args}");
                sourceFileNameField.setText("main.cpp");
            }
        }
        applyCompileFieldsState(compileRequiredCheck.isSelected());
    }

    private boolean isFormEmpty() {
        return compileCommandField.getText().isBlank()
                && runCommandField.getText().isBlank()
                && sourceFileNameField.getText().isBlank();
    }

    private void applyCompileFieldsState(boolean enabled) {
        compileCommandField.setDisable(!enabled);
        compileArgsField.setDisable(!enabled);
    }

    private void updateButtonStates() {
        boolean hasSelection = configListView.getSelectionModel().getSelectedItem() != null;
        editButton.setDisable(!hasSelection);
        deleteButton.setDisable(!hasSelection);
        exportButton.setDisable(!hasSelection);
    }

    private void selectByName(String name) {
        for (Configuration c : items) {
            if (c.getName().equals(name)) {
                configListView.getSelectionModel().select(c);
                configListView.scrollTo(c);
                return;
            }
        }
    }

    private Window currentWindow() {
        return configListView.getScene() != null ? configListView.getScene().getWindow() : null;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static String trim(String s) { return s == null ? "" : s.trim(); }

    private static String safeFileName(String name) {
        return name == null ? "configuration" : name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showWarning(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
