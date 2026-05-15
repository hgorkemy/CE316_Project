package com.iae;

import com.iae.dao.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.PrintWriter;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        DatabaseManager.getInstance();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);
        primaryStage.setTitle("IAE - Integrated Assignment Environment");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> writeCrashLog(e));
        try {
            launch(args);
        } catch (Throwable t) {
            writeCrashLog(t);
            throw t;
        }
    }

    private static void writeCrashLog(Throwable t) {
        try (PrintWriter pw = new PrintWriter("iae-error.log")) {
            t.printStackTrace(pw);
        } catch (Exception ignored) {}
        t.printStackTrace(System.err);
    }
}
