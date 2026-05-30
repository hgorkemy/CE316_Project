package com.iae.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class HelpController implements Initializable {

    @FXML private WebView webView;
    @FXML private Button backButton;
    @FXML private Button forwardButton;
    @FXML private Button menuButton;

    private URL helpUrl;
    private int currentSectionIndex = 0;

    private final String[] sections = {
            "top",
            "getting-started",
            "configurations",
            "creating-project",
            "running-project",
            "viewing-results",
            "open-save",
            "examples"
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        helpUrl = getClass().getResource("/help/index.html");

        if (helpUrl != null) {
            loadSection(0);
        } else {
            webView.getEngine().loadContent("<h2>Help file not found.</h2>");
        }

        webView.getEngine().locationProperty().addListener((observable, oldLocation, newLocation) -> {
            updateCurrentSectionFromUrl(newLocation);
            updateNavigationButtons();
        });

        updateNavigationButtons();
    }

    @FXML
    private void onBack() {
        if (currentSectionIndex > 0) {
            loadSection(currentSectionIndex - 1);
        }
    }

    @FXML
    private void onForward() {
        if (currentSectionIndex < sections.length - 1) {
            loadSection(currentSectionIndex + 1);
        }
    }

    @FXML
    private void onBackToMenu() {
        loadSection(0);
    }

    private void loadSection(int index) {
        if (helpUrl == null) {
            return;
        }

        currentSectionIndex = index;
        String sectionUrl = helpUrl.toExternalForm() + "#" + sections[index];
        webView.getEngine().load(sectionUrl);
        updateNavigationButtons();
    }

    private void updateCurrentSectionFromUrl(String url) {
        if (url == null || !url.contains("#")) {
            currentSectionIndex = 0;
            return;
        }

        String fragment = url.substring(url.indexOf("#") + 1);

        for (int i = 0; i < sections.length; i++) {
            if (sections[i].equals(fragment)) {
                currentSectionIndex = i;
                return;
            }
        }
    }

    private void updateNavigationButtons() {
        backButton.setDisable(currentSectionIndex <= 0);
        forwardButton.setDisable(currentSectionIndex >= sections.length - 1);
        menuButton.setDisable(currentSectionIndex <= 0);
    }
}