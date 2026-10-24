package com.systemtwin.analysis;

import com.systemtwin.graph.DependencyGraph;
import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.SensorReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AnomalyDetectorTest {

    private ThresholdDetector thresholdDetector;
    private ZScoreDetector zScoreDetector;

    @BeforeEach
    void setUp() {
        thresholdDetector = new ThresholdDetector();
        zScoreDetector = new ZScoreDetector(25, 3.0);
    }

    @Test
    @DisplayName("Verify ThresholdDetector triggers on upper and critical bounds")
    void testThresholdDetector() {
        SensorReading normal = new SensorReading("run-1", "s1", "c1", "CPU", 45.0, "%", "NORMAL");
        Optional<AnomalyEvent> normalRes = thresholdDetector.evaluate(normal);
        assertFalse(normalRes.isPresent(), "Normal 45% CPU should not trigger anomaly");

        SensorReading warning = new SensorReading("run-1", "s1", "c1", "CPU", 85.0, "%", "NORMAL");
        Optional<AnomalyEvent> warnRes = thresholdDetector.evaluate(warning);
        assertTrue(warnRes.isPresent(), "85% CPU should trigger HIGH threshold warning");
        assertEquals("HIGH", warnRes.get().getSeverity());

        SensorReading critical = new SensorReading("run-1", "s1", "c1", "CPU", 98.0, "%", "NORMAL");
        Optional<AnomalyEvent> critRes = thresholdDetector.evaluate(critical);
        assertTrue(critRes.isPresent(), "98% CPU should trigger CRITICAL threshold alert");
        assertEquals("CRITICAL", critRes.get().getSeverity());
    }

    @Test
    @DisplayName("Verify ZScoreDetector identifies statistical 4-sigma outlier")
    void testZScoreDetector() {
        Random rand = new Random(42);

        // Feed 15 stable observations: mean around 50.0, stddev around 1.0
        for (int i = 0; i < 15; i++) {
            double val = 50.0 + rand.nextGaussian() * 0.5;
            SensorReading r = new SensorReading("run-1", "s-temp", "comp-1", "TEMPERATURE", val, "°C", "NORMAL");
            Optional<AnomalyEvent> res = zScoreDetector.evaluate(r);
            assertFalse(res.isPresent(), "Baseline readings should not trigger anomaly");
        }

        // Feed sudden extreme outlier: 70.0 (over 20 units above mean, ~40 sigma)
        SensorReading outlier = new SensorReading("run-1", "s-temp", "comp-1", "TEMPERATURE", 70.0, "°C", "NORMAL");
        Optional<AnomalyEvent> outlierRes = zScoreDetector.evaluate(outlier);

        assertTrue(outlierRes.isPresent(), "Statistical outlier should be detected by ZScoreDetector");
        assertEquals("Z_SCORE", outlierRes.get().getDetector());
    }

    @Test
    @DisplayName("Verify RootCauseAnalyzer isolates earliest upstream component")
    void testRootCauseAnalyzer() {
        DependencyGraph graph = new DependencyGraph();
        ComponentModel db = new ComponentModel("db-main", "Database", "DATABASE");
        ComponentModel app = new ComponentModel("app-api", "API Server", "SERVER");
        graph.addComponent(db);
        graph.addComponent(app);
        graph.addDependency("app-api", "db-main"); // app depends on db

        long t0 = System.currentTimeMillis();
        List<AnomalyEvent> anomalies = List.of(
                new AnomalyEvent("run-1", "db-main", "s-db", "IOPS", 10.0, 500.0, "CRITICAL", "THRESHOLD", "IOPS drop"),
                new AnomalyEvent("run-1", "app-api", "s-app", "LATENCY", 450.0, 20.0, "HIGH", "THRESHOLD", "Spiking latency")
        );
        anomalies.get(0).setTimestamp(t0);
        anomalies.get(1).setTimestamp(t0 + 2000);

        RootCauseAnalyzer rca = new RootCauseAnalyzer();
        Optional<RootCauseAnalyzer.RootCauseDiagnosis> diag = rca.diagnose(anomalies, graph);

        assertTrue(diag.isPresent());
        assertEquals("db-main", diag.get().getRootComponentId(), "db-main should be identified as root cause");
        assertTrue(diag.get().getConfidenceScore() >= 0.7);
    }

    @Test
    @DisplayName("Verify PerformanceAnalyzer detection metrics (precision, recall, F1)")
    void testPerformanceAnalyzerMetrics() {
        PerformanceAnalyzer pa = new PerformanceAnalyzer();
        PerformanceAnalyzer.DetectionMetrics metrics = pa.evaluateDetectionQuality(90, 10, 80, 20);

        assertEquals(0.90, metrics.getPrecision(), 0.01);
        assertEquals(0.818, metrics.getRecall(), 0.01);
        assertTrue(metrics.getF1Score() > 0.85);
    }
}
