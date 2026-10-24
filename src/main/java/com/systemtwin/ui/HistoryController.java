package com.systemtwin.ui;

import com.systemtwin.model.SimulationRun;
import com.systemtwin.service.HistoryService;
import com.systemtwin.service.ReportService;
import com.systemtwin.service.SystemService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Controller for historical simulation queries, run comparisons, and CSV reporting.
 */
public class HistoryController {

    @FXML private TableView<SimulationRun> runsTable;
    @FXML private TableColumn<SimulationRun, String> runIdCol;
    @FXML private TableColumn<SimulationRun, String> runNameCol;
    @FXML private TableColumn<SimulationRun, String> runStatusCol;
    @FXML private TableColumn<SimulationRun, Integer> runTicksCol;
    @FXML private TableColumn<SimulationRun, Integer> runAnomaliesCol;

    @FXML private ComboBox<String> baselineRunCombo;
    @FXML private ComboBox<String> failureRunCombo;
    @FXML private TextArea comparisonReportArea;

    private HistoryService historyService;
    private ReportService reportService;
    private SystemService systemService;

    private final ObservableList<SimulationRun> runsData = FXCollections.observableArrayList();

    public void initServices(HistoryService histService, ReportService repService, SystemService sysService) {
        this.historyService = histService;
        this.reportService = repService;
        this.systemService = sysService;
        refreshRuns();
    }

    @FXML
    public void initialize() {
        if (runsTable != null) {
            runIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
            runNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
            runStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
            runTicksCol.setCellValueFactory(new PropertyValueFactory<>("tickCount"));
            runAnomaliesCol.setCellValueFactory(new PropertyValueFactory<>("anomalyCount"));
            runsTable.setItems(runsData);
        }
    }

    public void refreshRuns() {
        if (historyService == null) return;
        List<SimulationRun> runs = historyService.getAllRuns();
        runsData.setAll(runs);

        if (baselineRunCombo != null && failureRunCombo != null) {
            List<String> ids = runs.stream().map(SimulationRun::getId).toList();
            baselineRunCombo.getItems().setAll(ids);
            failureRunCombo.getItems().setAll(ids);
        }
    }

    @FXML
    public void handleCompareRuns() {
        if (historyService == null) return;
        String baseId = baselineRunCombo != null ? baselineRunCombo.getValue() : null;
        String failId = failureRunCombo != null ? failureRunCombo.getValue() : null;

        if (baseId != null && failId != null) {
            HistoryService.RunComparison comp = historyService.compareRuns(baseId, failId);
            if (comparisonReportArea != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("=== RUN COMPARISON REPORT ===\n\n");
                sb.append("Baseline Run: ").append(comp.getBaselineRunId()).append("\n");
                sb.append("Failure Run: ").append(comp.getFailureRunId()).append("\n\n");
                sb.append("Baseline Telemetry Count: ").append(comp.getBaselineReadingsCount()).append("\n");
                sb.append("Failure Telemetry Count: ").append(comp.getFailureReadingsCount()).append("\n");
                sb.append("Baseline Anomalies: ").append(comp.getBaselineAnomalies()).append("\n");
                sb.append("Failure Anomalies: ").append(comp.getFailureAnomalies()).append("\n\n");
                sb.append("Metric Average Deltas:\n");
                comp.getMetricDeltaAverages().forEach((k, v) -> sb.append(String.format(" - %s: %+.2f\n", k, v)));
                sb.append("\nSummary: ").append(comp.getSummary()).append("\n");
                comparisonReportArea.setText(sb.toString());
            }
        }
    }

    @FXML
    public void handleExportTelemetryCsv() {
        if (reportService == null || runsTable == null) return;
        SimulationRun selected = runsTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Telemetry to CSV");
        chooser.setInitialFileName("telemetry-" + selected.getId().substring(0, 8) + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files (*.csv)", "*.csv"));

        File file = chooser.showSaveDialog(runsTable.getScene().getWindow());
        if (file != null) {
            try {
                reportService.exportRunTelemetryToCsv(selected.getId(), file);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
