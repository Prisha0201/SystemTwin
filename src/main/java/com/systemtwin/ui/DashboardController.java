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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.*;

/**
 * Controller for the main real-time dashboard view with live telemetry charts.
 */
public class DashboardController {

    @FXML private Label statusLabel;
    @FXML private Label componentsCountLabel;
    @FXML private Label anomaliesCountLabel;
    @FXML private Label simulationTimeLabel;

    @FXML private LineChart<Number, Number> telemetryChart;
    @FXML private NumberAxis chartXAxis;
    @FXML private NumberAxis chartYAxis;

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
    private final Map<String, XYChart.Series<Number, Number>> chartSeriesMap = new LinkedHashMap<>();

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
                List<SensorReading> latest = simulationService.getLatestReadings();
                readingsData.setAll(latest);

                if (telemetryChart != null && run != null && !latest.isEmpty()) {
                    int tick = run.getTickCount();
                    for (SensorReading r : latest) {
                        String seriesKey = r.getComponentId() + ":" + r.getMetricName();
                        if (chartSeriesMap.size() < 4 || chartSeriesMap.containsKey(seriesKey)) {
                            XYChart.Series<Number, Number> s = chartSeriesMap.computeIfAbsent(seriesKey, k -> {
                                XYChart.Series<Number, Number> newSeries = new XYChart.Series<>();
                                newSeries.setName(k);
                                telemetryChart.getData().add(newSeries);
                                return newSeries;
                            });
                            s.getData().add(new XYChart.Data<>(tick, r.getValue()));
                            if (s.getData().size() > 25) {
                                s.getData().remove(0);
                            }
                        }
                    }
                }
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
