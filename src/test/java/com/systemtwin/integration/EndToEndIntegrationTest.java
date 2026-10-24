package com.systemtwin.integration;

import com.systemtwin.analysis.*;
import com.systemtwin.benchmark.BenchmarkService;
import com.systemtwin.engine.*;
import com.systemtwin.events.EventBus;
import com.systemtwin.events.EventRecorder;
import com.systemtwin.graph.CriticalityAnalyzer;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.model.*;
import com.systemtwin.report.CsvReportExporter;
import com.systemtwin.report.ReportGenerator;
import com.systemtwin.repository.*;
import com.systemtwin.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Complete End-to-End integration test covering both development tracks:
 * Simulation Engine, Cascading Graph Propagation, Telemetry Streams,
 * Rolling Z-Score Anomaly Detection, Root Cause Localization, SQLite Persistence,
 * CSV Export, and Scenario Validation.
 */
class EndToEndIntegrationTest {

    private String dbUrl;
    private EventBus eventBus;
    private SensorEngine sensorEngine;
    private FailurePropagator failurePropagator;
    private FailureInjector failureInjector;
    private SimulationScheduler scheduler;
    private SimulationEngine simulationEngine;

    private SystemRepository systemRepo;
    private ReadingRepository readingRepo;
    private SimulationRepository simulationRepo;
    private EventRepository eventRepo;

    private ThresholdDetector thresholdDetector;
    private ZScoreDetector zScoreDetector;
    private RootCauseAnalyzer rootCauseAnalyzer;
    private BenchmarkService benchmarkService;

    private SystemService systemService;
    private SimulationService simulationService;
    private MonitoringService monitoringService;
    private HistoryService historyService;
    private TemplateService templateService;
    private ReportService reportService;
    private ScenarioValidator scenarioValidator;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        Path dbPath = tempDir.resolve("e2e-systemtwin.db");
        dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        // 1. Initialize SQLite Database Schema
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            InputStream is = getClass().getResourceAsStream("/database/schema.sql");
            assertNotNull(is, "schema.sql must be loaded");
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }
        }

        // 2. Wire core components
        eventBus = new EventBus();
        sensorEngine = new SensorEngine(42L);
        failurePropagator = new FailurePropagator();
        failureInjector = new FailureInjector(eventBus, failurePropagator);
        scheduler = new SimulationScheduler();

        systemRepo = new SystemRepository(dbUrl);
        readingRepo = new ReadingRepository(dbUrl);
        simulationRepo = new SimulationRepository(dbUrl);
        eventRepo = new EventRepository(dbUrl);

        new EventRecorder(eventBus, eventRepo);

        thresholdDetector = new ThresholdDetector();
        zScoreDetector = new ZScoreDetector(20, 3.0);
        rootCauseAnalyzer = new RootCauseAnalyzer();
        benchmarkService = new BenchmarkService();
        scenarioValidator = new ScenarioValidator();

        simulationEngine = new SimulationEngine(
                eventBus, sensorEngine, failureInjector, failurePropagator, scheduler,
                readingRepo, simulationRepo
        );

        systemService = new SystemService(systemRepo, null);
        simulationService = new SimulationService(simulationEngine, simulationRepo, eventRepo);
        monitoringService = new MonitoringService(eventBus, thresholdDetector, zScoreDetector, readingRepo);
        historyService = new HistoryService(simulationRepo, readingRepo, eventRepo);
        templateService = new TemplateService(systemService);
        reportService = new ReportService(historyService, readingRepo, new CriticalityAnalyzer());
    }

    @Test
    @DisplayName("Complete E2E workflow: template -> validation -> simulation -> failure cascade -> anomaly detection -> root cause -> export")
    void testCompleteSystemTwinLifecycle(@TempDir Path tempDir) throws Exception {
        // Step 1: Create Industrial Template Twin
        SystemModel system = templateService.createIndustrialManufacturingTemplate();
        assertNotNull(system);
        assertEquals(5, system.getComponents().size());
        assertTrue(system.getDependencyGraph().getNodeCount() >= 5);

        // Step 2: Validate System with Kotlin ScenarioValidator
        ValidationResult validation = scenarioValidator.validateSystem(system);
        assertTrue(validation.isValid(), "Industrial template must pass validation");
        assertEquals(0, validation.getErrorCount());

        // Step 3: Run Criticality Analysis & Identify SPOFs
        CriticalityAnalyzer analyzer = new CriticalityAnalyzer();
        List<CriticalityAnalyzer.CriticalityReport> criticalityReports = analyzer.analyze(system.getDependencyGraph());
        assertFalse(criticalityReports.isEmpty());
        // Master PLC or Pump should have high criticality score
        assertTrue(criticalityReports.get(0).getCriticalityScore() >= 40.0);

        // Step 4: Start Simulation Run
        SimulationRun run = simulationService.start(system, "E2E-Integration-Run", 101L);
        assertNotNull(run);
        assertEquals("RUNNING", run.getStatus());

        // Step 5: Execute 10 healthy ticks and verify telemetry ingestion
        for (int i = 0; i < 10; i++) {
            simulationEngine.executeTick();
        }
        assertEquals(10, run.getTickCount());
        assertFalse(simulationService.getLatestReadings().isEmpty());

        // Verify SQLite batch persistence
        List<SensorReading> persistedReadings = readingRepo.findReadingsByRun(run.getId());
        assertFalse(persistedReadings.isEmpty(), "Sensor readings must be persisted in SQLite");
        assertTrue(persistedReadings.size() >= 50);

        // Step 6: Clear baseline warmup anomalies and inject Hard Failure into 'pump-01'
        monitoringService.clear();
        List<FailurePropagator.CascadeStep> cascade = simulationService.injectFailure("pump-01", true);
        assertNotNull(cascade);
        assertFalse(cascade.isEmpty());
        assertEquals("pump-01", cascade.get(0).getComponentId());
        assertEquals(ComponentState.FAILED, system.getComponent("pump-01").getState());

        // 'robot-arm' depends on 'pump-01' -> must have degraded or failed
        ComponentModel robot = system.getComponent("robot-arm");
        assertTrue(robot.getState() == ComponentState.DEGRADED || robot.getState() == ComponentState.FAILED,
                "Cascading failure must propagate to robot-arm");

        // Step 7: Tick simulation in failed state to trigger telemetry anomalies
        AtomicInteger anomaliesReceived = new AtomicInteger(0);
        eventBus.subscribe(AnomalyEvent.class, a -> anomaliesReceived.incrementAndGet());

        for (int i = 0; i < 5; i++) {
            simulationEngine.executeTick();
        }

        // Monitoring service should have captured anomalies
        List<AnomalyEvent> activeAnomalies = monitoringService.getActiveAnomalies();
        assertFalse(activeAnomalies.isEmpty(), "Anomalies must be flagged for degraded/failed components");

        // Step 8: Topological Root Cause Analysis
        Optional<RootCauseAnalyzer.RootCauseDiagnosis> diagnosis = rootCauseAnalyzer.diagnose(activeAnomalies, system.getDependencyGraph());
        assertTrue(diagnosis.isPresent(), "Root cause analyzer must produce diagnosis");
        // 'pump-01' is the upstream origin of the cascade
        assertEquals("pump-01", diagnosis.get().getRootComponentId(), "Root cause must correctly pinpoint pump-01");
        assertTrue(diagnosis.get().getConfidenceScore() >= 0.6);

        // Step 9: Recover Component
        List<FailurePropagator.CascadeStep> recoverySteps = simulationService.recoverComponent("pump-01");
        assertFalse(recoverySteps.isEmpty());
        assertEquals(ComponentState.HEALTHY, system.getComponent("pump-01").getState());

        // Step 10: Stop Simulation
        simulationService.stop();
        assertEquals("COMPLETED", run.getStatus());

        // Step 11: Export Telemetry & Anomalies to CSV
        File csvTelemetryFile = tempDir.resolve("e2e-telemetry.csv").toFile();
        File csvAnomalyFile = tempDir.resolve("e2e-anomalies.csv").toFile();

        CsvReportExporter exporter = new CsvReportExporter();
        exporter.exportReadingsToCsv(persistedReadings, csvTelemetryFile);
        exporter.exportAnomaliesToCsv(activeAnomalies, csvAnomalyFile);

        assertTrue(csvTelemetryFile.exists() && csvTelemetryFile.length() > 0, "Telemetry CSV must be generated");
        assertTrue(csvAnomalyFile.exists() && csvAnomalyFile.length() > 0, "Anomalies CSV must be generated");

        // Step 12: Executive Markdown Report Generation
        ReportGenerator reportGenerator = new ReportGenerator();
        String summary = reportGenerator.generateExecutiveSummary(run, system, persistedReadings, activeAnomalies, criticalityReports);
        assertNotNull(summary);
        assertTrue(summary.contains("SystemTwin Simulation Executive Summary"));
        assertTrue(summary.contains("pump-01"));

        // Step 13: Stress Benchmark Check
        BenchmarkService.BenchmarkResult bench = benchmarkService.runZScoreThroughputBenchmark(5000);
        assertTrue(bench.getThroughputOpsPerSec() > 50000.0, "Throughput should exceed 50k ops/sec");
    }
}
