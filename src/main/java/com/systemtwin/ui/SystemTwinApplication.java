
package com.systemtwin.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SystemTwinApplication extends Application {

    @Override
    public void start(Stage stage) {
        Label title = new Label("SystemTwin");
        title.setStyle(
            "-fx-font-size: 30px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #e2e8f0;"
        );

        Label subtitle = new Label(
            "Digital Twin Simulation and Monitoring"
        );
        subtitle.setStyle(
            "-fx-font-size: 14px;" +
            "-fx-text-fill: #94a3b8;"
        );

        VBox header = new VBox(8, title, subtitle);
        header.setPadding(new Insets(25));

        GridPane dashboard = new GridPane();
        dashboard.setHgap(16);
        dashboard.setVgap(16);
        dashboard.setPadding(new Insets(25));

        addCard(dashboard, "SYSTEM STATUS", "Not started", 0, 0);
        addCard(dashboard, "COMPONENTS", "0", 1, 0);
        addCard(dashboard, "ACTIVE ANOMALIES", "0", 0, 1);
        addCard(dashboard, "SIMULATION TIME", "00:00", 1, 1);

        Label message = new Label(
            "Welcome! Your SystemTwin desktop foundation is ready."
        );
        message.setStyle(
            "-fx-text-fill: #94a3b8;" +
            "-fx-font-size: 13px;"
        );
        message.setPadding(new Insets(20, 25, 25, 25));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(dashboard);
        root.setBottom(message);
        root.setStyle("-fx-background-color: #0f172a;");

        Scene scene = new Scene(root, 1000, 650);

        stage.setTitle("SystemTwin - Digital Twin Platform");
        stage.setMinWidth(750);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
    }

    private void addCard(
            GridPane grid,
            String heading,
            String value,
            int column,
            int row) {

        Label headingLabel = new Label(heading);
        headingLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #94a3b8;"
        );

        Label valueLabel = new Label(value);
        valueLabel.setStyle(
            "-fx-font-size: 24px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #e2e8f0;"
        );

        VBox card = new VBox(14, headingLabel, valueLabel);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(22));
        card.setMinSize(250, 130);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
            "-fx-background-color: #1e293b;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: #334155;" +
            "-fx-border-radius: 12px;"
        );

        grid.add(card, column, row);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
