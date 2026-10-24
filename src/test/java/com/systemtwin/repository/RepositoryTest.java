package com.systemtwin.repository;

import com.systemtwin.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTest {

    private String dbUrl;
    private SystemRepository systemRepo;
    private SimulationRepository simulationRepo;
    private ReadingRepository readingRepo;
    private EventRepository eventRepo;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        Path dbPath = tempDir.resolve("test-systemtwin.db");
        dbUrl = "jdbc:sqlite:" + dbPath.toAbsolutePath();

        systemRepo = new SystemRepository(dbUrl);
        simulationRepo = new SimulationRepository(dbUrl);
        readingRepo = new ReadingRepository(dbUrl);
        eventRepo = new EventRepository(dbUrl);

        // Initialize schema
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            InputStream is = getClass().getResourceAsStream("/database/schema.sql");
            assertNotNull(is, "schema.sql must exist");
            String schemaSql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : schemaSql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }
        }
    }

    @Test
    @DisplayName("Verify SystemRepository saves and retrieves complete system topology")
    void testSystemRepository() throws Exception {
        SystemModel sys = new SystemModel("sys-repo-test", "Factory Plant", "Automated plant");
        ComponentModel c1 = new ComponentModel("c1", "Controller", "PLC");
        ComponentModel c2 = new ComponentModel("c2", "Pump", "PUMP");
        sys.addComponent(c1);
        sys.addComponent(c2);
        sys.addDependency("c2", "c1");

        systemRepo.saveSystem(sys);

        Optional<SystemModel> loaded = systemRepo.findSystemById("sys-repo-test");
        assertTrue(loaded.isPresent());
        assertEquals("Factory Plant", loaded.get().getName());
        assertEquals(2, loaded.get().getComponents().size());
        assertTrue(loaded.get().getDependencyGraph().hasDependency("c2", "c1"));
    }

    @Test
    @DisplayName("Verify SimulationRepository CRUD operations")
    void testSimulationRepository() throws Exception {
        SimulationRun run = new SimulationRun("run-001", "sys-1", "Test-Run-1", 42L);
        run.setStatus("RUNNING");
        run.setTickCount(10);

        simulationRepo.saveRun(run);

        Optional<SimulationRun> loaded = simulationRepo.findRunById("run-001");
        assertTrue(loaded.isPresent());
        assertEquals("Test-Run-1", loaded.get().getName());
        assertEquals(10, loaded.get().getTickCount());

        run.setStatus("COMPLETED");
        run.setTickCount(20);
        simulationRepo.updateRun(run);

        Optional<SimulationRun> updated = simulationRepo.findRunById("run-001");
        assertTrue(updated.isPresent());
        assertEquals("COMPLETED", updated.get().getStatus());
        assertEquals(20, updated.get().getTickCount());
    }

    @Test
    @DisplayName("Verify ReadingRepository batch telemetry insertion")
    void testReadingRepositoryBatch() throws Exception {
        List<SensorReading> readings = List.of(
                new SensorReading("run-x", "s1", "c1", "CPU", 25.5, "%", "NORMAL"),
                new SensorReading("run-x", "s2", "c1", "TEMP", 48.0, "°C", "NORMAL"),
                new SensorReading("run-x", "s3", "c2", "PRESSURE", 6.2, "bar", "NORMAL")
        );

        readingRepo.saveReadingsBatch(readings);

        List<SensorReading> retrieved = readingRepo.findReadingsByRun("run-x");
        assertEquals(3, retrieved.size());
        assertEquals(3, readingRepo.countReadingsForRun("run-x"));
    }

    @Test
    @DisplayName("Verify EventRepository persists simulation and anomaly events")
    void testEventRepository() throws Exception {
        SimulationEvent ev = new SimulationEvent("run-y", 1, "FAILURE_INJECTED", "c1", null, "Manual fault", null);
        eventRepo.saveEvent(ev);

        List<SimulationEvent> events = eventRepo.findEventsByRun("run-y");
        assertEquals(1, events.size());
        assertEquals("FAILURE_INJECTED", events.get(0).getEventType());

        AnomalyEvent anom = new AnomalyEvent("run-y", "c1", "s1", "CPU", 99.0, 50.0, "CRITICAL", "THRESHOLD", "Overheat");
        eventRepo.saveAnomalyEvent(anom);

        List<AnomalyEvent> anomalies = eventRepo.findAnomaliesByRun("run-y");
        assertEquals(1, anomalies.size());
        assertEquals("CRITICAL", anomalies.get(0).getSeverity());
        assertEquals(1, eventRepo.countAnomaliesForRun("run-y"));
    }
}
