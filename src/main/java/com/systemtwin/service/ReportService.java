package com.systemtwin.service;

import com.systemtwin.graph.CriticalityAnalyzer;
import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;
import com.systemtwin.model.SimulationRun;
import com.systemtwin.model.SystemModel;
import com.systemtwin.report.CsvReportExporter;
import com.systemtwin.report.ReportGenerator;
import com.systemtwin.repository.ReadingRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Service managing telemetry reporting, CSV exports, and executive markdown summaries.
 */
public class ReportService {

    private final HistoryService historyService;
    private final ReadingRepository readingRepository;
    private final CriticalityAnalyzer criticalityAnalyzer;
    private final CsvReportExporter csvExporter;
    private final ReportGenerator reportGenerator;

    public ReportService(
            HistoryService historyService,
            ReadingRepository readingRepository,
            CriticalityAnalyzer criticalityAnalyzer) {
        this.historyService = historyService;
        this.readingRepository = readingRepository;
        this.criticalityAnalyzer = criticalityAnalyzer != null ? criticalityAnalyzer : new CriticalityAnalyzer();
        this.csvExporter = new CsvReportExporter();
        this.reportGenerator = new ReportGenerator();
    }

    public File exportRunTelemetryToCsv(String runId, File destinationFile) throws IOException {
        List<SensorReading> readings = historyService.getReadingsForRun(runId);
        csvExporter.exportReadingsToCsv(readings, destinationFile);
        return destinationFile;
    }

    public File exportAnomaliesToCsv(String runId, File destinationFile) throws IOException {
        List<AnomalyEvent> anomalies = historyService.getAnomaliesForRun(runId);
        csvExporter.exportAnomaliesToCsv(anomalies, destinationFile);
        return destinationFile;
    }

    public String generateRunExecutiveSummary(SimulationRun run, SystemModel system) {
        List<SensorReading> readings = run != null ? historyService.getReadingsForRun(run.getId()) : List.of();
        List<AnomalyEvent> anomalies = run != null ? historyService.getAnomaliesForRun(run.getId()) : List.of();
        List<CriticalityAnalyzer.CriticalityReport> criticality = system != null ?
                criticalityAnalyzer.analyze(system.getDependencyGraph()) : List.of();

        return reportGenerator.generateExecutiveSummary(run, system, readings, anomalies, criticality);
    }
}
