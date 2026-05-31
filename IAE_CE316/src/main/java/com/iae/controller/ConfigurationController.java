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
import java.util.ArrayList;
import java.util.List;
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
    @FXML private Button saveButton;

    private final ConfigurationService configurationService =
            new ConfigurationService(new ConfigurationDAO(), new ImportExportService());

    private final ObservableList<Configuration> items = FXCollections.observableArrayList();
    private Configuration editingConfig;
    private boolean editing;

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
                    if (newSel != null) {
                        loadIntoForm(newSel);
                        setEditing(false);
                    }
                    updateButtonStates();
                });

        compileRequiredCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            applyCompileFieldsState(newVal);
        });

        loadConfigurations();
        clearForm();
        setEditing(false);
        updateButtonStates();
    }

    @FXML
    private void onAdd() {
        configListView.getSelectionModel().clearSelection();
        clearForm();
        setEditing(true);
        formTitleLabel.setText("New Configuration");
        nameField.requestFocus();
    }

    @FXML
    private void onEdit() {
        Configuration selected = configListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a configuration to edit.");
            return;
        }
        loadIntoForm(selected);
        setEditing(true);
        formTitleLabel.setText("Edit Configuration");
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
            setEditing(false);
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
            warnIfUnixPaths(List.of(toSave));
            warnIfContentDuplicates(List.of(toSave));
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
        setEditing(false);
    }

    @FXML
    private void onImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Configuration(s)");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showOpenDialog(currentWindow());
        if (file == null) return;

        List<Configuration> parsed;
        try {
            parsed = configurationService.parseImportFile(file.getAbsolutePath());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        } catch (RuntimeException e) {
            showError("Failed to import configuration: " + e.getMessage());
            return;
        }

        warnIfUnixPaths(parsed);
        warnIfContentDuplicates(parsed);

        int imported = 0, overwritten = 0, skipped = 0;
        String lastName = null;
        for (Configuration config : parsed) {
            ImportResult result = importOne(config);
            switch (result) {
                case IMPORTED -> { imported++; lastName = config.getName(); }
                case OVERWRITTEN -> { overwritten++; lastName = config.getName(); }
                case SKIPPED -> skipped++;
            }
        }

        loadConfigurations();
        if (lastName != null) selectByName(lastName);
        showImportSummary(parsed.size(), imported, overwritten, skipped);
    }

    @FXML
    private void onExportAll() {
        if (items.isEmpty()) {
            showWarning("There are no configurations to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export All Configurations");
        chooser.setInitialFileName("configurations.json");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showSaveDialog(currentWindow());
        if (file == null) return;

        try {
            configurationService.exportAll(file.getAbsolutePath());
            showInfo("Exported " + items.size() + " configuration(s) to " + file.getName());
        } catch (RuntimeException e) {
            showError("Failed to export configurations: " + e.getMessage());
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

    private enum ImportResult { IMPORTED, OVERWRITTEN, SKIPPED }

    private ImportResult importOne(Configuration config) {
        try {
            configurationService.createConfiguration(config);
            return ImportResult.IMPORTED;
        } catch (NameConflictException conflict) {
            return resolveConflict(config);
        } catch (IllegalArgumentException e) {
            showError("Skipped '" + config.getName() + "': " + e.getMessage());
            return ImportResult.SKIPPED;
        }
    }

    private ImportResult resolveConflict(Configuration config) {
        ButtonType overwrite = new ButtonType("Overwrite", ButtonBar.ButtonData.OTHER);
        ButtonType rename = new ButtonType("Rename", ButtonBar.ButtonData.OTHER);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Name Conflict");
        dialog.setHeaderText(null);
        dialog.setContentText("Configuration '" + config.getName()
                + "' already exists. Overwrite or rename?");
        dialog.getButtonTypes().setAll(overwrite, rename, cancel);

        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() == cancel) return ImportResult.SKIPPED;

        if (choice.get() == overwrite) {
            try {
                configurationService.overwriteConfiguration(config);
                return ImportResult.OVERWRITTEN;
            } catch (RuntimeException ex) {
                showError("Failed to overwrite '" + config.getName() + "': " + ex.getMessage());
                return ImportResult.SKIPPED;
            }
        }
        return renameAndImport(config);
    }

    private ImportResult renameAndImport(Configuration config) {
        TextInputDialog dialog = new TextInputDialog(config.getName() + " (imported)");
        dialog.setTitle("Rename Configuration");
        dialog.setHeaderText("A configuration named '" + config.getName() + "' already exists.");
        dialog.setContentText("Enter a new name:");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return ImportResult.SKIPPED;

        String newName = result.get().trim();
        if (newName.isEmpty()) {
            showError("Name cannot be empty. '" + config.getName() + "' was skipped.");
            return ImportResult.SKIPPED;
        }
        config.setId(0);
        config.setName(newName);
        try {
            configurationService.createConfiguration(config);
            return ImportResult.IMPORTED;
        } catch (NameConflictException dup) {
            return resolveConflict(config);
        } catch (RuntimeException ex) {
            showError("Failed to import '" + newName + "': " + ex.getMessage());
            return ImportResult.SKIPPED;
        }
    }

    private void warnIfUnixPaths(List<Configuration> configs) {
        List<String> affected = new ArrayList<>();
        for (Configuration c : configs) {
            if (ImportExportService.hasUnixPaths(c)) affected.add(c.getName());
        }
        if (affected.isEmpty()) return;
        showWarning("This configuration contains Unix paths (e.g. /usr/bin/gcc). "
                + "It may not work on Windows. Please verify the command paths.\n\n"
                + "Affected: " + String.join(", ", affected));
    }

    private void warnIfContentDuplicates(List<Configuration> configs) {
        List<String> matches = new ArrayList<>();
        for (Configuration incoming : configs) {
            for (Configuration existing : items) {
                if (!existing.getName().equalsIgnoreCase(incoming.getName())
                        && sameContent(existing, incoming)) {
                    matches.add("'" + incoming.getName() + "' has the same settings as '"
                            + existing.getName() + "'");
                    break;
                }
            }
        }
        if (matches.isEmpty()) return;
        showWarning("Some imported configurations are identical to existing ones "
                + "(only the name differs):\n\n" + String.join("\n", matches)
                + "\n\nThey will still be imported.");
    }

    private static boolean sameContent(Configuration a, Configuration b) {
        return a.isCompileRequired() == b.isCompileRequired()
                && eq(a.getLanguage(), b.getLanguage())
                && eq(a.getCompileCommand(), b.getCompileCommand())
                && eq(a.getCompileArgs(), b.getCompileArgs())
                && eq(a.getRunCommand(), b.getRunCommand())
                && eq(a.getRunArgs(), b.getRunArgs())
                && eq(a.getSourceFileName(), b.getSourceFileName());
    }

    private static boolean eq(String x, String y) {
        return (x == null ? "" : x.trim()).equals(y == null ? "" : y.trim());
    }

    private void showImportSummary(int total, int imported, int overwritten, int skipped) {
        if (total == 1) {
            if (imported == 1) showInfo("Configuration imported.");
            else if (overwritten == 1) showInfo("Configuration overwritten.");
            return;
        }
        showInfo("Import finished.\n"
                + "Imported: " + imported + "\n"
                + "Overwritten: " + overwritten + "\n"
                + "Skipped: " + skipped);
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
                runCommandField.setText("python");
                runArgsField.setText("{sourceFile}");
                sourceFileNameField.setText("main.py");
            }
            case "C" -> {
                compileRequiredCheck.setSelected(true);
                compileCommandField.setText("gcc");
                compileArgsField.setText("{sourceFile} -o {outputName}");
                runCommandField.setText("{outputName}.exe");
                runArgsField.setText("{args}");
                sourceFileNameField.setText("main.c");
            }
            case "C++" -> {
                compileRequiredCheck.setSelected(true);
                compileCommandField.setText("g++");
                compileArgsField.setText("{sourceFile} -o {outputName}");
                runCommandField.setText("{outputName}.exe");
                runArgsField.setText("{args}");
                sourceFileNameField.setText("main.cpp");
            }
        }
        applyCompileFieldsState(compileRequiredCheck.isSelected());
    }

    private void setEditing(boolean on) {
        editing = on;
        nameField.setDisable(!on);
        languageCombo.setDisable(!on);
        compileRequiredCheck.setDisable(!on);
        runCommandField.setDisable(!on);
        runArgsField.setDisable(!on);
        sourceFileNameField.setDisable(!on);
        saveButton.setDisable(!on);
        applyCompileFieldsState(compileRequiredCheck.isSelected());
    }

    private void applyCompileFieldsState(boolean compileEnabled) {
        boolean usable = editing && compileEnabled;
        compileCommandField.setDisable(!usable);
        compileArgsField.setDisable(!usable);
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
