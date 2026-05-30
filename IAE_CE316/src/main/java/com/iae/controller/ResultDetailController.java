package com.iae.controller;

import com.iae.model.Result;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ResultDetailController {

    @FXML private Label studentIdLabel;
    @FXML private Label compileStatusLabel;
    @FXML private Label runStatusLabel;
    @FXML private Label comparisonStatusLabel;

    @FXML private TextArea compileErrorArea;
    @FXML private TextArea runErrorArea;
    @FXML private TextArea expectedOutputArea;
    @FXML private TextArea actualOutputArea;

    public void setResult(Result result, String expectedOutput) {
        studentIdLabel.setText(result.getStudentId());

        compileStatusLabel.setText(String.valueOf(result.getCompileStatus()));
        runStatusLabel.setText(String.valueOf(result.getRunStatus()));
        comparisonStatusLabel.setText(String.valueOf(result.getComparisonStatus()));

        setErrorArea(compileErrorArea, result.getCompileError());
        setErrorArea(runErrorArea, result.getRunError());
        expectedOutputArea.setText(emptyIfNull(expectedOutput));
        actualOutputArea.setText(emptyIfNull(result.getActualOutput()));
    }

    private void setErrorArea(TextArea area, String error) {
        if (error == null || error.isBlank()) {
            area.setText("No error.");
            area.setStyle("-fx-text-fill: #2ecc71; -fx-font-family: monospace;");
        } else {
            area.setText(error.trim());
            area.setStyle("-fx-font-family: monospace;");
        }
    }

    @FXML
    private void onClose() {
        Stage stage = (Stage) studentIdLabel.getScene().getWindow();
        stage.close();
    }

    private String emptyIfNull(String text) {
        return text == null ? "" : text;
    }
}