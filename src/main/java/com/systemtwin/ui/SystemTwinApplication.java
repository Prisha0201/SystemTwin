package com.systemtwin.ui;

import com.systemtwin.analysis.PerformanceAnalyzer;
import com.systemtwin.analysis.RootCauseAnalyzer;
import com.systemtwin.analysis.ThresholdDetector;
import com.systemtwin.analysis.ZScoreDetector;
import com.systemtwin.benchmark.BenchmarkService;
import com.systemtwin.config.SpringConfig;
import com.systemtwin.engine.FailureInjector;
import com.systemtwin.engine.SensorEngine;
import com.systemtwin.engine.SimulationEngine;
import com.systemtwin.engine.SimulationScheduler;
import com.systemtwin.events.EventBus;
import com.systemtwin.events.EventRecorder;
import com.systemtwin.graph.CriticalityAnalyzer;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.SensorReading;
import com.systemtwin.model.SimulationRun;
import com.systemtwin.model.SystemModel;
import com.systemtwin.plugin.PluginRegistry;
import com.systemtwin.repository.EventRepository;
import com.systemtwin.repository.ReadingRepository;
import com.systemtwin.repository.SimulationRepository;
import com.systemtwin.repository.SystemRepository;
import com.systemtwin.service.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * Main JavaFX Desktop Application launcher for SystemTwin.
 */
public class SystemTwinApplication extends Application {

    private EventBus eventBus;
    private SensorEngine sensorEngine;
    private FailurePropagator failurePropagator;
    private FailureInjector failureInjector;
    private SimulationScheduler scheduler;
    private SimulationEngine simulationEngine;

    private SystemRepository systemRepository;
    private ReadingRepository readingRepository;
    private SimulationRepository simulationRepository;
    private EventRepository eventRepository;

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
    private ReplayService replayService;

    private DashboardController dashboardController;
    private BuilderController builderController;
    private SimulationController simulationController;
    private HistoryController historyController;
    private AnalysisController analysisController;

    private Timeline uiRefreshTimeline;
    private org.springframework.context.annotation.AnnotationConfigApplicationContext springContext;

    @Override
    public void init() {
        // Bootstraps Spring ApplicationContext and wires all beans
        springContext = new org.springframework.context.annotation.AnnotationConfigApplicationContext(SpringConfig.class);

        eventBus = springContext.getBean(EventBus.class);
        sensorEngine = springContext.getBean(SensorEngine.class);
        failurePropagator = springContext.getBean(FailurePropagator.class);
        failureInjector = springContext.getBean(FailureInjector.class);
        scheduler = springContext.getBean(SimulationScheduler.class);
        systemRepository = springContext.getBean(SystemRepository.class);
        readingRepository = springContext.getBean(ReadingRepository.class);
        simulationRepository = springContext.getBean(SimulationRepository.class);
        eventRepository = springContext.getBean(EventRepository.class);
        thresholdDetector = springContext.getBean(ThresholdDetector.class);
        zScoreDetector = springContext.getBean(ZScoreDetector.class);
        rootCauseAnalyzer = springContext.getBean(RootCauseAnalyzer.class);
        benchmarkService = springContext.getBean(BenchmarkService.class);
        simulationEngine = springContext.getBean(SimulationEngine.class);
        systemService = springContext.getBean(SystemService.class);
        simulationService = springContext.getBean(SimulationService.class);
        monitoringService = springContext.getBean(MonitoringService.class);
        historyService = springContext.getBean(HistoryService.class);
        templateService = springContext.getBean(TemplateService.class);
        reportService = springContext.getBean(ReportService.class);
        replayService = springContext.getBean(ReplayService.class);

        // Load default template from JSON
        templateService.createIndustrialManufacturingTemplate();
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top Header
        HBox header = createHeader();
        root.setTop(header);

        // Center Multi-Tab View
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // 1. Dashboard Tab
        Tab dashTab = new Tab("📊 Dashboard");
        dashTab.setContent(loadView("/fxml/dashboard.fxml", (DashboardController c) -> {
            this.dashboardController = c;
            c.initServices(simulationService, monitoringService);
        }));

        // 2. System Builder Tab
        Tab builderTab = new Tab("🏗 System Builder");
        builderTab.setContent(loadView("/fxml/system-builder.fxml", (BuilderController c) -> {
            this.builderController = c;
            c.initServices(systemService, templateService);
        }));

        // 3. Simulation & Fault Injection Tab
        Tab simTab = new Tab("⚡ Simulation & Faults");
        simTab.setContent(loadView("/fxml/simulation.fxml", (SimulationController c) -> {
            this.simulationController = c;
            c.initServices(simulationService, systemService, eventBus);
        }));

        // 4. Analytics & Diagnostics Tab
        Tab diagTab = new Tab("🔬 Root Cause Analysis");
        diagTab.setContent(loadView("/fxml/analysis.fxml", (AnalysisController c) -> {
            this.analysisController = c;
            c.initServices(monitoringService, systemService, simulationService);
        }));
        diagTab.setOnSelectionChanged(e -> {
            if (diagTab.isSelected() && analysisController != null) {
                analysisController.handleRunRootCauseAnalysis();
            }
        });

        // 5. History & Reports Tab
        Tab histTab = new Tab("📜 History & Export");
        histTab.setContent(loadView("/fxml/history.fxml", (HistoryController c) -> {
            this.historyController = c;
            c.initServices(historyService, reportService, systemService, replayService);
        }));
        histTab.setOnSelectionChanged(e -> {
            if (histTab.isSelected() && historyController != null) {
                historyController.refreshRuns();
            }
        });

        tabPane.getTabs().addAll(dashTab, builderTab, simTab, diagTab, histTab);
        root.setCenter(tabPane);

        // Scene setup
        Scene scene = new Scene(root, 1260, 800);
        try {
            scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
        } catch (Exception ex) {
            System.err.println("CSS warning: " + ex.getMessage());
        }

        stage.setTitle("SystemTwin — Digital Twin Simulation & Anomaly Detection Platform");
        stage.setMinWidth(1050);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();

        // UI Refresh Timer (every 500ms)
        uiRefreshTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            if (dashboardController != null && systemService != null && simulationService != null) {
                dashboardController.updateDashboard(systemService.getActiveSystem(), simulationService.getCurrentRun());
            }
        }));
        uiRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        uiRefreshTimeline.play();
    }

    private HBox createHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-header");

        VBox titleBox = new VBox(2);
        Label title = new Label("SystemTwin");
        title.getStyleClass().add("title-text");
        Label subtitle = new Label("Digital Twin Simulation & Real-Time Monitoring");
        subtitle.getStyleClass().add("subtitle-text");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Template Selector
        Label tplLabel = new Label("Template:");
        tplLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        ComboBox<String> templateCombo = new ComboBox<>();
        templateCombo.getItems().addAll("Automated Assembly Line", "Cloud Microservices", "Biosafety Laboratory");
        templateCombo.getSelectionModel().select(0);
        templateCombo.setPrefWidth(240);
        templateCombo.setMinWidth(220);
        templateCombo.setOnAction(e -> {
            String selected = templateCombo.getValue();
            if ("Automated Assembly Line".equals(selected)) {
                templateService.createIndustrialManufacturingTemplate();
            } else if ("Cloud Microservices".equals(selected)) {
                templateService.createDataCenterTemplate();
            } else if ("Biosafety Laboratory".equals(selected)) {
                templateService.createLaboratorySystemTemplate();
            }
            if (builderController != null) builderController.refreshView();
            if (simulationController != null) simulationController.refreshComponents();
            if (monitoringService != null) monitoringService.clear();
        });

        // Quick simulation action buttons
        Button quickStartBtn = new Button("▶ Quick Start");
        quickStartBtn.getStyleClass().add("btn-primary");
        quickStartBtn.setOnAction(e -> {
            if (monitoringService != null) monitoringService.clear();
            SystemModel sys = systemService.getActiveSystem();
            if (sys != null) {
                simulationService.start(sys, "Live-Run-" + System.currentTimeMillis() % 1000, 42L);
            }
        });

        Button quickStopBtn = new Button("⏹ Stop");
        quickStopBtn.getStyleClass().add("btn-danger");
        quickStopBtn.setOnAction(e -> {
            simulationService.stop();
            if (historyController != null) {
                historyController.refreshRuns();
            }
        });

        header.getChildren().addAll(titleBox, spacer, tplLabel, templateCombo, quickStartBtn, quickStopBtn);
        return header;
    }

    private NodeWrapper createDiagnosticsTabContent() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(16, 20, 20, 20));
        root.setStyle("-fx-background-color: #0f172a;");

        // Top Diagnostic Card
        VBox rcaCard = new VBox(12);
        rcaCard.getStyleClass().add("metric-card");

        Label rcaTitle = new Label("TOPOLOGICAL ROOT CAUSE ANALYSIS");
        rcaTitle.getStyleClass().add("metric-card-title");

        Label rcaCompLabel = new Label("No diagnosis run yet");
        rcaCompLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        TextArea rcaExplanation = new TextArea();
        rcaExplanation.setEditable(false);
        rcaExplanation.setPrefRowCount(3);
        rcaExplanation.setPromptText("Click 'Run Root Cause Diagnosis' to correlate active anomalies with graph structure.");

        Button runRcaBtn = new Button("🔍 Run Root Cause Diagnosis");
        runRcaBtn.getStyleClass().add("btn-primary");
        runRcaBtn.setOnAction(e -> {
            SystemModel sys = systemService.getActiveSystem();
            List<AnomalyEvent> anomalies = monitoringService.getActiveAnomalies();
            if (sys != null && !anomalies.isEmpty()) {
                Optional<RootCauseAnalyzer.RootCauseDiagnosis> diag = rootCauseAnalyzer.diagnose(anomalies, sys.getDependencyGraph());
                if (diag.isPresent()) {
                    rcaCompLabel.setText("Root Cause Component: " + diag.get().getRootComponentId() +
                            String.format(" (Confidence: %.0f%%)", diag.get().getConfidenceScore() * 100.0));
                    rcaExplanation.setText(diag.get().getExplanation() + "\nPropagation Chain: " +
                            String.join(" ➔ ", diag.get().getPropagationChain()));
                }
            } else {
                rcaExplanation.setText("No active anomalies detected yet. Try injecting a fault on the 'Simulation & Faults' tab first!");
            }
        });

        rcaCard.getChildren().addAll(rcaTitle, rcaCompLabel, rcaExplanation, runRcaBtn);

        // Bottom Benchmark Card
        VBox benchCard = new VBox(12);
        benchCard.getStyleClass().add("metric-card");
        VBox.setVgrow(benchCard, Priority.ALWAYS);

        Label benchTitle = new Label("ENGINE PERFORMANCE & STATISTICAL DETECTOR BENCHMARKS");
        benchTitle.getStyleClass().add("metric-card-title");

        Label benchResultLabel = new Label("Press 'Execute Stress Benchmark' to run 25,000 synthetic operations.");
        benchResultLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");

        Button runBenchBtn = new Button("⚡ Execute Stress Benchmark");
        runBenchBtn.getStyleClass().add("btn-secondary");
        runBenchBtn.setOnAction(e -> {
            BenchmarkService.BenchmarkResult res = benchmarkService.runZScoreThroughputBenchmark(25000);
            benchResultLabel.setText(String.format("✓ Processed %d operations in %d ms (Throughput: %,.1f ops/sec) | Memory Heap: %.2f MB",
                    res.getOperationsCount(), res.getDurationMillis(), res.getThroughputOpsPerSec(), res.getMemoryUsedMb()));
        });

        benchCard.getChildren().addAll(benchTitle, benchResultLabel, runBenchBtn);
        root.getChildren().addAll(rcaCard, benchCard);
        return new NodeWrapper(root);
    }

    private static class NodeWrapper extends ScrollPane {
        public NodeWrapper(Parent content) {
            setContent(content);
            setFitToWidth(true);
            setFitToHeight(true);
            setStyle("-fx-background-color: #0f172a; -fx-border-color: transparent;");
        }
    }

    private <T> Parent loadView(String fxmlPath, ControllerInitializer<T> initializer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            T controller = loader.getController();
            if (initializer != null && controller != null) {
                initializer.init(controller);
            }
            return view;
        } catch (Exception ex) {
            System.err.println("Failed to load FXML " + fxmlPath + ": " + ex.getMessage());
            Label errorLabel = new Label("View load error: " + fxmlPath + "\n" + ex.getMessage());
            errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-padding: 20px;");
            return errorLabel;
        }
    }

    @FunctionalInterface
    private interface ControllerInitializer<T> {
        void init(T controller);
    }

    @Override
    public void stop() {
        if (uiRefreshTimeline != null) {
            uiRefreshTimeline.stop();
        }
        if (simulationService != null) {
            simulationService.stop();
        }
        if (springContext != null) {
            springContext.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
