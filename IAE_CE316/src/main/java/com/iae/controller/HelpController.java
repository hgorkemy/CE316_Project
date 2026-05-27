package com.iae.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class HelpController implements Initializable {

    @FXML private WebView webView;
    @FXML private Button backButton;
    @FXML private Button forwardButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        URL helpUrl = getClass().getResource("/help/index.html");

        if (helpUrl != null) {
            webView.getEngine().load(helpUrl.toExternalForm());
        } else {
            webView.getEngine().loadContent("<h2>Help file not found.</h2>");
        }

        updateNavigationButtons();

        webView.getEngine().getLoadWorker().stateProperty().addListener(
                (observable, oldState, newState) -> updateNavigationButtons()
        );

        webView.getEngine().getHistory().currentIndexProperty().addListener(
                (observable, oldIndex, newIndex) -> updateNavigationButtons()
        );
    }

    @FXML
    private void onBack() {
        WebHistory history = webView.getEngine().getHistory();

        if (history.getCurrentIndex() > 0) {
            history.go(-1);
        }

        updateNavigationButtons();
    }

    @FXML
    private void onForward() {
        WebHistory history = webView.getEngine().getHistory();

        if (history.getCurrentIndex() < history.getEntries().size() - 1) {
            history.go(1);
        }

        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        WebEngine engine = webView.getEngine();
        WebHistory history = engine.getHistory();

        int currentIndex = history.getCurrentIndex();
        int historySize = history.getEntries().size();

        backButton.setDisable(currentIndex <= 0);
        forwardButton.setDisable(currentIndex >= historySize - 1);
    }
}