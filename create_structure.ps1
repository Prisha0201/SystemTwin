
$ErrorActionPreference = "Stop"

$directories = @(
    "docs",
    "examples/industrial-system",
    "examples/laboratory-system",
    "examples/data-center",
    "src/main/java/com/systemtwin/config",
    "src/main/java/com/systemtwin/model",
    "src/main/java/com/systemtwin/engine",
    "src/main/java/com/systemtwin/events",
    "src/main/java/com/systemtwin/graph",
    "src/main/java/com/systemtwin/analysis",
    "src/main/java/com/systemtwin/plugin",
    "src/main/java/com/systemtwin/service",
    "src/main/java/com/systemtwin/repository",
    "src/main/java/com/systemtwin/report",
    "src/main/java/com/systemtwin/benchmark",
    "src/main/java/com/systemtwin/ui",
    "src/main/kotlin/com/systemtwin/analysis",
    "src/main/resources/fxml",
    "src/main/resources/css",
    "src/main/resources/database",
    "src/test/java/com/systemtwin/engine",
    "src/test/java/com/systemtwin/graph",
    "src/test/java/com/systemtwin/analysis",
    "src/test/java/com/systemtwin/repository",
    "src/test/java/com/systemtwin/plugin",
    "src/test/kotlin/com/systemtwin/analysis"
)

$files = @(
    "pom.xml",
    "README.md",
    ".gitignore",
    "docs/architecture.md",
    "docs/algorithms.md",
    "docs/test-results.md",
    "src/main/java/com/systemtwin/config/SpringConfig.java",
    "src/main/java/com/systemtwin/model/SystemModel.java",
    "src/main/java/com/systemtwin/model/ComponentModel.java",
    "src/main/java/com/systemtwin/model/ComponentState.java",
    "src/main/java/com/systemtwin/model/Sensor.java",
    "src/main/java/com/systemtwin/model/SensorReading.java",
    "src/main/java/com/systemtwin/model/SimulationRun.java",
    "src/main/java/com/systemtwin/model/SimulationEvent.java",
    "src/main/java/com/systemtwin/model/AnomalyEvent.java",
    "src/main/java/com/systemtwin/engine/SimulationEngine.java",
    "src/main/java/com/systemtwin/engine/SimulationScheduler.java",
    "src/main/java/com/systemtwin/engine/SensorEngine.java",
    "src/main/java/com/systemtwin/engine/FailureInjector.java",
    "src/main/java/com/systemtwin/events/EventBus.java",
    "src/main/java/com/systemtwin/events/EventConsumer.java",
    "src/main/java/com/systemtwin/events/EventRecorder.java",
    "src/main/java/com/systemtwin/graph/DependencyGraph.java",
    "src/main/java/com/systemtwin/graph/FailurePropagator.java",
    "src/main/java/com/systemtwin/graph/CriticalityAnalyzer.java",
    "src/main/java/com/systemtwin/analysis/AnomalyDetector.java",
    "src/main/java/com/systemtwin/analysis/ThresholdDetector.java",
    "src/main/java/com/systemtwin/analysis/ZScoreDetector.java",
    "src/main/java/com/systemtwin/analysis/RootCauseAnalyzer.java",
    "src/main/java/com/systemtwin/analysis/PerformanceAnalyzer.java",
    "src/main/java/com/systemtwin/plugin/ComponentPlugin.java",
    "src/main/java/com/systemtwin/plugin/PluginRegistry.java",
    "src/main/java/com/systemtwin/service/SystemService.java",
    "src/main/java/com/systemtwin/service/SimulationService.java",
    "src/main/java/com/systemtwin/service/MonitoringService.java",
    "src/main/java/com/systemtwin/service/HistoryService.java",
    "src/main/java/com/systemtwin/service/ReplayService.java",
    "src/main/java/com/systemtwin/service/TemplateService.java",
    "src/main/java/com/systemtwin/service/ReportService.java",
    "src/main/java/com/systemtwin/repository/SystemRepository.java",
    "src/main/java/com/systemtwin/repository/ReadingRepository.java",
    "src/main/java/com/systemtwin/repository/SimulationRepository.java",
    "src/main/java/com/systemtwin/repository/EventRepository.java",
    "src/main/java/com/systemtwin/report/CsvReportExporter.java",
    "src/main/java/com/systemtwin/report/ReportGenerator.java",
    "src/main/java/com/systemtwin/benchmark/BenchmarkService.java",
    "src/main/java/com/systemtwin/ui/SystemTwinApplication.java",
    "src/main/java/com/systemtwin/ui/DashboardController.java",
    "src/main/java/com/systemtwin/ui/BuilderController.java",
    "src/main/java/com/systemtwin/ui/SimulationController.java",
    "src/main/java/com/systemtwin/ui/AnalysisController.java",
    "src/main/java/com/systemtwin/ui/HistoryController.java",
    "src/main/kotlin/com/systemtwin/analysis/ScenarioValidator.kt",
    "src/main/resources/fxml/dashboard.fxml",
    "src/main/resources/fxml/system-builder.fxml",
    "src/main/resources/fxml/simulation.fxml",
    "src/main/resources/fxml/history.fxml",
    "src/main/resources/css/application.css",
    "src/main/resources/database/schema.sql",
    "src/test/java/com/systemtwin/engine/SimulationEngineTest.java",
    "src/test/java/com/systemtwin/graph/DependencyGraphTest.java",
    "src/test/java/com/systemtwin/analysis/AnomalyDetectorTest.java",
    "src/test/java/com/systemtwin/repository/RepositoryTest.java",
    "src/test/java/com/systemtwin/plugin/PluginRegistryTest.java",
    "src/test/kotlin/com/systemtwin/analysis/ScenarioValidatorTest.kt"
)

foreach ($directory in $directories) {
    New-Item -ItemType Directory -Path $directory -Force |
        Out-Null
}

foreach ($file in $files) {
    if (-not (Test-Path $file)) {
        New-Item -ItemType File -Path $file |
            Out-Null
    }
}

Write-Host ""
Write-Host "SystemTwin project structure created successfully!" -ForegroundColor Green
Write-Host "Directories: $($directories.Count)"
Write-Host "Files: $($files.Count)"
Write-Host ""
Write-Host "All source files are empty placeholders."
Write-Host "Next: fill pom.xml and README.md, then implement each module."
