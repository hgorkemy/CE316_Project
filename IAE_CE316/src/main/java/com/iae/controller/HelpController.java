package com.iae.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebView;

import java.net.URL;
import java.util.ResourceBundle;

public class HelpController implements Initializable {

    @FXML private WebView webView;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        URL helpUrl = getClass().getResource("/help/index.html");
        if (helpUrl != null) {
            webView.getEngine().load(helpUrl.toExternalForm());
        } else {
            webView.getEngine().loadContent("<h2>Help file not found.</h2>");
        }
    }
}
