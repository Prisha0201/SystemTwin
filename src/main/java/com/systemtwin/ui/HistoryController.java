package com.systemtwin.ui;

import com.systemtwin.model.SimulationRun;
import com.systemtwin.service.HistoryService;
import com.systemtwin.service.ReplayService;
import com.systemtwin.service.ReportService;
import com.systemtwin.service.SystemService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for historical simulation queries, run comparisons, exports and replay.
 *
 * Exports are written without a dialog into an "exports" folder (next to where the app was started).
 * If that folder is not writable the files go to ~/SystemTwin-exports instead.
 * Every button reports what it did in the results box and in the status line.
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
    @FXML private Label exportStatusLabel;

    private HistoryService historyService;
    private ReportService reportService;
    private SystemService systemService;
    private ReplayService replayService;

    private final ObservableList<SimulationRun> runsData = FXCollections.observableArrayList();

    /** Writes one export file; implemented by small lambdas below. */
    private interface ExportAction {
        void write(File target) throws Exception;
    }

    public void initServices(HistoryService histService, ReportService repService, SystemService sysService, ReplayService replay) {
        this.historyService = histService;
        this.reportService = repService;
        this.systemService = sysService;
        this.replayService = replay;
        refreshRuns();
    }

    public void initServices(HistoryService histService, ReportService repService, SystemService sysService) {
        initServices(histService, repService, sysService, null);
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
            runsTable.setPlaceholder(new Label("No simulation runs yet. Press ▶ Quick Start in the header, wait a few seconds, then press ⏹ Stop."));
        }
        show("Select a run in the table above, then use the buttons to compare, export or replay it.\n"
                + "Results and file locations are shown here.");
        setStatus("Exports are saved to: " + new File(System.getProperty("user.dir"), "exports").getAbsolutePath());
    }

    // ------------------------------------------------------------------
    // Runs table
    // ------------------------------------------------------------------

    @FXML
    public void refreshRuns() {
        if (historyService == null) {
            show("⚠ History service is not ready yet.");
            return;
        }
        try {
            SimulationRun previouslySelected = runsTable != null ? runsTable.getSelectionModel().getSelectedItem() : null;
            String keepId = previouslySelected != null ? previouslySelected.getId() : null;

            List<SimulationRun> runs = historyService.getAllRuns();
            runsData.setAll(runs);

            // Combo items look like "Live-Run-357  [<run id>]"; runs are sorted newest first
            List<String> items = new ArrayList<>();
            for (SimulationRun r : runs) {
                items.add(labelFor(r));
            }
            if (baselineRunCombo != null) {
                String oldValue = baselineRunCombo.getValue();
                baselineRunCombo.getItems().setAll(items);
                if (oldValue != null && items.contains(oldValue)) {
                    baselineRunCombo.setValue(oldValue);
                } else if (!items.isEmpty()) {
                    baselineRunCombo.getSelectionModel().select(Math.min(1, items.size() - 1)); // older run
                }
            }
            if (failureRunCombo != null) {
                String oldValue = failureRunCombo.getValue();
                failureRunCombo.getItems().setAll(items);
                if (oldValue != null && items.contains(oldValue)) {
                    failureRunCombo.setValue(oldValue);
                } else if (!items.isEmpty()) {
                    failureRunCombo.getSelectionModel().select(0); // newest run
                }
            }

            if (runsTable != null && !runs.isEmpty()) {
                int index = 0;
                if (keepId != null) {
                    for (int i = 0; i < runs.size(); i++) {
                        if (keepId.equals(runs.get(i).getId())) {
                            index = i;
                            break;
                        }
                    }
                }
                runsTable.getSelectionModel().select(index);
            }

            setStatus("Loaded " + runs.size() + " run(s) from the database.");
            if (runs.isEmpty()) {
                show(noRunsMessage());
            }
        } catch (Exception ex) {
            reportFailure("Refreshing runs", ex);
        }
    }

    private SimulationRun selectedRun() {
        SimulationRun selected = runsTable != null ? runsTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null && runsData.isEmpty()) {
            refreshRuns();
        }
        if (selected == null && !runsData.isEmpty()) {
            selected = runsData.get(0);
            if (runsTable != null) {
                runsTable.getSelectionModel().select(0);
            }
        }
        if (selected == null) {
            show(noRunsMessage());
            setStatus("No runs available to act on.");
        }
        return selected;
    }

    private static String noRunsMessage() {
        return "⚠ No simulation runs found in the database.\n\n"
                + "To create history:\n"
                + "1. Click '▶ Quick Start' in the top header\n"
                + "2. Let the simulation run for a few seconds\n"
                + "3. Click '⏹ Stop' in the header\n"
                + "4. Come back here (or press '🔄 Refresh Runs') to export, compare or replay the run.";
    }

    // ------------------------------------------------------------------
    // Compare
    // ------------------------------------------------------------------

    @FXML
    public void handleCompareRuns() {
        try {
            if (historyService == null) {
                show("❌ History service is not available.");
                return;
            }
            String baseId = idOf(baselineRunCombo != null ? baselineRunCombo.getValue() : null);
            String failId = idOf(failureRunCombo != null ? failureRunCombo.getValue() : null);

            if (baseId == null || failId == null) {
                refreshRuns();
                baseId = idOf(baselineRunCombo != null ? baselineRunCombo.getValue() : null);
                failId = idOf(failureRunCombo != null ? failureRunCombo.getValue() : null);
            }
            if (runsData.size() < 2 || baseId == null || failId == null) {
                show("⚠ At least two simulation runs are needed for a comparison.\n\n"
                        + "Run one simulation as a healthy baseline, stop it, then run another with a fault injected, "
                        + "stop that one too, and press 'Compare Runs'.");
                return;
            }
            if (baseId.equals(failId)) {
                show("⚠ Baseline and Failure are the same run. Pick two different runs in the drop-downs.");
                return;
            }

            HistoryService.RunComparison comp = historyService.compareRuns(baseId, failId);
            StringBuilder sb = new StringBuilder();
            sb.append("=== RUN COMPARISON REPORT ===\n\n");
            sb.append("Baseline Run: ").append(comp.getBaselineRunId()).append("\n");
            sb.append("Failure Run:  ").append(comp.getFailureRunId()).append("\n\n");
            sb.append("Baseline Telemetry Count: ").append(comp.getBaselineReadingsCount()).append("\n");
            sb.append("Failure Telemetry Count:  ").append(comp.getFailureReadingsCount()).append("\n");
            sb.append("Baseline Anomalies:       ").append(comp.getBaselineAnomalies()).append("\n");
            sb.append("Failure Anomalies:        ").append(comp.getFailureAnomalies()).append("\n\n");
            sb.append("Metric Average Deltas (failure minus baseline):\n");
            if (comp.getMetricDeltaAverages().isEmpty()) {
                sb.append(" (no telemetry to compare)\n");
            } else {
                comp.getMetricDeltaAverages().forEach((k, v) -> sb.append(String.format(" - %s: %+.2f%n", k, v)));
            }
            sb.append("\nSummary: ").append(comp.getSummary()).append("\n");
            show(sb.toString());
            setStatus("Comparison complete.");
        } catch (Exception ex) {
            reportFailure("Comparing runs", ex);
        }
    }

    // ------------------------------------------------------------------
    // Exports
    // ------------------------------------------------------------------

    @FXML
    public void handleExportTelemetryCsv() {
        try {
            if (reportService == null) {
                show("❌ Report service is not available.");
                return;
            }
            SimulationRun run = selectedRun();
            if (run == null) return;

            setStatus("Exporting telemetry…");
            File written = exportWithFallback("telemetry-" + fileStem(run) + ".csv",
                    target -> reportService.exportRunTelemetryToCsv(run.getId(), target));
            long rows = countDataRows(written);

            StringBuilder sb = new StringBuilder("✅ TELEMETRY EXPORT SUCCESSFUL\n\n");
            sb.append("Run:         ").append(run.getName()).append(" (").append(run.getId()).append(")\n");
            sb.append("File:        ").append(written.getAbsolutePath()).append("\n");
            sb.append("Size:        ").append(written.length()).append(" bytes\n");
            sb.append("Data rows:   ").append(rows >= 0 ? String.valueOf(rows) : "unknown").append("\n");
            sb.append("Ticks run:   ").append(run.getTickCount()).append("\n");
            if (rows == 0) {
                sb.append("\n⚠ The file only contains the header row: this run has no stored telemetry. ")
                        .append("Runs that were stopped immediately have no data. Let a run go for a few seconds before stopping it.");
            }
            show(sb.toString());
            setStatus("Telemetry saved: " + written.getAbsolutePath());
        } catch (Exception ex) {
            reportFailure("Telemetry export", ex);
        }
    }

    @FXML
    public void handleExportAnomaliesCsv() {
        try {
            if (reportService == null) {
                show("❌ Report service is not available.");
                return;
            }
            SimulationRun run = selectedRun();
            if (run == null) return;

            setStatus("Exporting anomalies…");
            File written = exportWithFallback("anomalies-" + fileStem(run) + ".csv",
                    target -> reportService.exportAnomaliesToCsv(run.getId(), target));
            long rows = countDataRows(written);

            StringBuilder sb = new StringBuilder("✅ ANOMALY LOG EXPORT SUCCESSFUL\n\n");
            sb.append("Run:         ").append(run.getName()).append(" (").append(run.getId()).append(")\n");
            sb.append("File:        ").append(written.getAbsolutePath()).append("\n");
            sb.append("Size:        ").append(written.length()).append(" bytes\n");
            sb.append("Data rows:   ").append(rows >= 0 ? String.valueOf(rows) : "unknown").append("\n");
            if (rows == 0) {
                sb.append("\nℹ No anomalies were recorded for this run. A healthy run can legitimately have none. ")
                        .append("Inject a failure during a run to produce anomalies.");
            }
            show(sb.toString());
            setStatus("Anomalies saved: " + written.getAbsolutePath());
        } catch (Exception ex) {
            reportFailure("Anomaly export", ex);
        }
    }

    @FXML
    public void handleGenerateExecutiveReport() {
        try {
            if (reportService == null) {
                show("❌ Report service is not available.");
                return;
            }
            SimulationRun run = selectedRun();
            if (run == null) return;

            setStatus("Generating executive report…");
            com.systemtwin.model.SystemModel system = systemService != null ? systemService.getActiveSystem() : null;
            String markdown = reportService.generateRunExecutiveSummary(run, system);

            File written = exportWithFallback("executive-report-" + fileStem(run) + ".md",
                    target -> Files.writeString(target.toPath(), markdown, StandardCharsets.UTF_8));

            show("✅ Executive report saved to:\n" + written.getAbsolutePath() + "\n\n" + markdown);
            setStatus("Report saved: " + written.getAbsolutePath());
        } catch (Exception ex) {
            reportFailure("Executive report", ex);
        }
    }

    @FXML
    public void handleOpenExportsFolder() {
        try {
            final File dir = ensureExportDir();
            final String path = dir.getAbsolutePath();
            setStatus("Exports folder: " + path);
            Thread opener = new Thread(() -> {
                try {
                    if (java.awt.Desktop.isDesktopSupported()
                            && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                        java.awt.Desktop.getDesktop().open(dir);
                    } else {
                        Platform.runLater(() -> show("Open this folder manually:\n" + path));
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> show("Could not open the folder automatically.\nOpen it manually:\n" + path));
                }
            }, "systemtwin-open-exports");
            opener.setDaemon(true);
            opener.start();
        } catch (Exception ex) {
            reportFailure("Opening exports folder", ex);
        }
    }

    // ------------------------------------------------------------------
    // Replay
    // ------------------------------------------------------------------

    @FXML
    public void handleReplayRun() {
        try {
            if (replayService == null) {
                show("❌ Replay service is not available.");
                return;
            }
            SimulationRun run = selectedRun();
            if (run == null) return;

            show("▶ Replaying run " + run.getName() + " (" + run.getId() + ") at 1.0x speed...\n\n"
                    + "Recorded lifecycle events (failure injections, cascades, ticks) are being re-dispatched onto the event bus "
                    + "with a REPLAY_ prefix. Open the '⚡ Simulation & Faults' tab to watch them appear in the event log.");
            setStatus("Replaying " + run.getName() + "…");

            replayService.replayRun(run.getId(), 1.0)
                    .thenAccept(count -> Platform.runLater(() -> {
                        if (count == 0) {
                            show("ℹ Replay finished, but this run has no recorded lifecycle events to replay.");
                        } else {
                            show("✅ Replay finished: " + count + " event(s) re-dispatched for run " + run.getName() + ".");
                        }
                        setStatus("Replay finished (" + count + " events).");
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> show("❌ Replay failed: " + ex.getMessage()));
                        return null;
                    });
        } catch (Exception ex) {
            reportFailure("Replay", ex);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Creates the exports folder (or the home-folder fallback) and returns it. */
    private File ensureExportDir() {
        File dir = new File(System.getProperty("user.dir"), "exports");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            dir = new File(System.getProperty("user.home"), "SystemTwin-exports");
            dir.mkdirs();
        }
        return dir;
    }

    /** Tries the exports folder first; if writing fails, retries in ~/SystemTwin-exports. */
    private File exportWithFallback(String fileName, ExportAction action) throws Exception {
        File primary = new File(ensureExportDir(), fileName);
        try {
            action.write(primary);
            return primary;
        } catch (Exception first) {
            File fallbackDir = new File(System.getProperty("user.home"), "SystemTwin-exports");
            if (!fallbackDir.isDirectory() && !fallbackDir.mkdirs()) {
                throw first;
            }
            File fallback = new File(fallbackDir, fileName);
            action.write(fallback);
            return fallback;
        }
    }

    private static String fileStem(SimulationRun run) {
        String name = run.getName() == null ? "run" : run.getName().toLowerCase().replaceAll("[^a-z0-9._-]+", "-");
        String id = run.getId() == null ? "" : run.getId();
        String shortId = id.length() > 8 ? id.substring(0, 8) : id;
        return shortId.isEmpty() ? name : name + "-" + shortId;
    }

    private static String labelFor(SimulationRun run) {
        return run.getName() + "  [" + run.getId() + "]";
    }

    /** Extracts the run id from a combo item such as "Live-Run-357  [uuid]". */
    private static String idOf(String comboItem) {
        if (comboItem == null) return null;
        int open = comboItem.lastIndexOf('[');
        int close = comboItem.lastIndexOf(']');
        if (open >= 0 && close > open) {
            return comboItem.substring(open + 1, close);
        }
        return comboItem;
    }

    /** Number of lines in the file minus the CSV header; -1 if the file cannot be read. */
    private static long countDataRows(File file) {
        try (java.util.stream.Stream<String> lines = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
            return Math.max(0L, lines.count() - 1L);
        } catch (Exception ex) {
            return -1L;
        }
    }

    private void reportFailure(String what, Exception ex) {
        String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        show("❌ " + what + " failed: " + detail
                + "\n\nIf this keeps happening, copy this message together with the console output.");
        setStatus(what + " failed: " + detail);
    }

    private void show(String text) {
        if (comparisonReportArea != null) {
            comparisonReportArea.setText(text);
        }
    }

    private void setStatus(String text) {
        if (exportStatusLabel != null) {
            exportStatusLabel.setText(text);
        }
    }
}
