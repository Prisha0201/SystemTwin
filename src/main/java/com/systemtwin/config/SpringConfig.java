package com.systemtwin.config;

import com.systemtwin.analysis.ThresholdDetector;
import com.systemtwin.analysis.ZScoreDetector;
import com.systemtwin.engine.FailureInjector;
import com.systemtwin.engine.SensorEngine;
import com.systemtwin.engine.SimulationEngine;
import com.systemtwin.engine.SimulationScheduler;
import com.systemtwin.events.EventBus;
import com.systemtwin.events.EventRecorder;
import com.systemtwin.graph.CriticalityAnalyzer;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.plugin.PluginRegistry;
import com.systemtwin.repository.EventRepository;
import com.systemtwin.repository.ReadingRepository;
import com.systemtwin.repository.SimulationRepository;
import com.systemtwin.repository.SystemRepository;
import com.systemtwin.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class SpringConfig {

    private static final String DB_DIR = System.getProperty("user.home") + File.separator + ".systemtwin";
    private static final String DB_PATH = DB_DIR + File.separator + "systemtwin.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    public SpringConfig() {
        initDatabase();
    }

    public static String getDbUrl() {
        return DB_URL;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void initDatabase() {
        try {
            File dir = new File(DB_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                InputStream schemaStream = getClass().getResourceAsStream("/database/schema.sql");
                if (schemaStream != null) {
                    String schemaSql = new String(schemaStream.readAllBytes(), StandardCharsets.UTF_8);
                    for (String sqlStatement : schemaSql.split(";")) {
                        String trimmed = sqlStatement.trim();
                        if (!trimmed.isEmpty()) {
                            stmt.execute(trimmed);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Database initialization warning: " + e.getMessage());
        }
    }

    @Bean
    public EventBus eventBus() {
        return new EventBus();
    }

    @Bean
    public EventRecorder eventRecorder(EventBus eventBus, EventRepository eventRepository) {
        EventRecorder recorder = new EventRecorder(eventBus, eventRepository);
        return recorder;
    }

    @Bean
    public PluginRegistry pluginRegistry() {
        PluginRegistry registry = new PluginRegistry();
        registry.loadBuiltinPlugins();
        return registry;
    }

    @Bean
    public SensorEngine sensorEngine() {
        return new SensorEngine();
    }

    @Bean
    public FailurePropagator failurePropagator() {
        return new FailurePropagator();
    }

    @Bean
    public CriticalityAnalyzer criticalityAnalyzer() {
        return new CriticalityAnalyzer();
    }

    @Bean
    public FailureInjector failureInjector(EventBus eventBus, FailurePropagator failurePropagator) {
        return new FailureInjector(eventBus, failurePropagator);
    }

    @Bean
    public SimulationScheduler simulationScheduler() {
        return new SimulationScheduler();
    }

    @Bean
    public SystemRepository systemRepository() {
        return new SystemRepository(DB_URL);
    }

    @Bean
    public ReadingRepository readingRepository() {
        return new ReadingRepository(DB_URL);
    }

    @Bean
    public SimulationRepository simulationRepository() {
        return new SimulationRepository(DB_URL);
    }

    @Bean
    public EventRepository eventRepository() {
        return new EventRepository(DB_URL);
    }

    @Bean
    public ThresholdDetector thresholdDetector() {
        return new ThresholdDetector();
    }

    @Bean
    public ZScoreDetector zScoreDetector() {
        return new ZScoreDetector(30, 3.0);
    }

    @Bean
    public SimulationEngine simulationEngine(
            EventBus eventBus,
            SensorEngine sensorEngine,
            FailureInjector failureInjector,
            FailurePropagator failurePropagator,
            SimulationScheduler scheduler,
            ReadingRepository readingRepository,
            SimulationRepository simulationRepository) {
        return new SimulationEngine(
                eventBus,
                sensorEngine,
                failureInjector,
                failurePropagator,
                scheduler,
                readingRepository,
                simulationRepository
        );
    }

    @Bean
    public SystemService systemService(SystemRepository systemRepository, PluginRegistry pluginRegistry) {
        return new SystemService(systemRepository, pluginRegistry);
    }

    @Bean
    public SimulationService simulationService(
            SimulationEngine simulationEngine,
            SimulationRepository simulationRepository,
            EventRepository eventRepository) {
        return new SimulationService(simulationEngine, simulationRepository, eventRepository);
    }

    @Bean
    public ReplayService replayService(EventBus eventBus, EventRepository eventRepository) {
        return new ReplayService(eventBus, eventRepository);
    }

    @Bean
    public MonitoringService monitoringService(
            EventBus eventBus,
            ThresholdDetector thresholdDetector,
            ZScoreDetector zScoreDetector,
            ReadingRepository readingRepository) {
        return new MonitoringService(eventBus, thresholdDetector, zScoreDetector, readingRepository);
    }

    @Bean
    public HistoryService historyService(
            SimulationRepository simulationRepository,
            ReadingRepository readingRepository,
            EventRepository eventRepository) {
        return new HistoryService(simulationRepository, readingRepository, eventRepository);
    }

    @Bean
    public TemplateService templateService(SystemService systemService) {
        return new TemplateService(systemService);
    }

    @Bean
    public ReportService reportService(
            HistoryService historyService,
            ReadingRepository readingRepository,
            CriticalityAnalyzer criticalityAnalyzer) {
        return new ReportService(historyService, readingRepository, criticalityAnalyzer);
    }
}
