package com.systemtwin.ui;

import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;
import com.systemtwin.model.SimulationRun;
import com.systemtwin.model.SystemModel;
import com.systemtwin.service.MonitoringService;
import com.systemtwin.service.SimulationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * Controller for the main real-time dashboard view.
 */
public class DashboardController {

    @FXML private Label statusLabel;
    @FXML private Label componentsCountLabel;
    @FXML private Label anomaliesCountLabel;
    @FXML private Label simulationTimeLabel;

    @FXML private TableView<SensorReading> readingsTable;
    @FXML private TableColumn<SensorReading, String> compCol;
    @FXML private TableColumn<SensorReading, String> metricCol;
    @FXML private TableColumn<SensorReading, Double> valueCol;
    @FXML private TableColumn<SensorReading, String> unitCol;
    @FXML private TableColumn<SensorReading, String> statusCol;

    @FXML private ListView<String> anomalyListView;

    private SimulationService simulationService;
    private MonitoringService monitoringService;

    private final ObservableList<SensorReading> readingsData = FXCollections.observableArrayList();
    private final ObservableList<String> anomalyData = FXCollections.observableArrayList();

    public void initServices(SimulationService simService, MonitoringService monService) {
        this.simulationService = simService;
        this.monitoringService = monService;
    }

    @FXML
    public void initialize() {
        if (readingsTable != null) {
            compCol.setCellValueFactory(new PropertyValueFactory<>("componentId"));
            metricCol.setCellValueFactory(new PropertyValueFactory<>("metricName"));
            valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
            unitCol.setCellValueFactory(new PropertyValueFactory<>("unit"));
            statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            readingsTable.setItems(readingsData);
        }

        if (anomalyListView != null) {
            anomalyListView.setItems(anomalyData);
        }
    }

    public void updateDashboard(SystemModel system, SimulationRun run) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(run != null ? run.getStatus() : "IDLE");
            }
            if (componentsCountLabel != null) {
                int total = system != null ? system.getComponents().size() : 0;
                int healthy = system != null ? system.getHealthyCount() : 0;
                componentsCountLabel.setText(total + " (" + healthy + " healthy)");
            }
            if (anomaliesCountLabel != null) {
                int count = monitoringService != null ? monitoringService.getActiveAnomalies().size() : 0;
                anomaliesCountLabel.setText(String.valueOf(count));
            }
            if (simulationTimeLabel != null) {
                simulationTimeLabel.setText(run != null ? run.getTickCount() + " ticks" : "00:00");
            }

            if (simulationService != null) {
                readingsData.setAll(simulationService.getLatestReadings());
            }

            if (monitoringService != null) {
                anomalyData.clear();
                for (AnomalyEvent a : monitoringService.getActiveAnomalies()) {
                    anomalyData.add(String.format("[%s] %s (%s): %.2f - %s",
                            a.getSeverity(), a.getComponentId(), a.getMetricName(), a.getMetricValue(), a.getMessage()));
                }
            }
        });
    }
}
