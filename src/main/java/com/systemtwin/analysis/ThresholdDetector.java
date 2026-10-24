package com.systemtwin.analysis;

import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anomaly detector that evaluates values against static warning and critical bounds.
 */
public class ThresholdDetector implements AnomalyDetector {

    public static class MetricThreshold {
        private final double normalMin;
        private final double normalMax;
        private final double criticalMax;

        public MetricThreshold(double normalMin, double normalMax, double criticalMax) {
            this.normalMin = normalMin;
            this.normalMax = normalMax;
            this.criticalMax = criticalMax;
        }

        public double getNormalMin() { return normalMin; }
        public double getNormalMax() { return normalMax; }
        public double getCriticalMax() { return criticalMax; }
    }

    private final Map<String, MetricThreshold> thresholdRules = new ConcurrentHashMap<>();

    public ThresholdDetector() {
        // Default industrial and server metric thresholds
        thresholdRules.put("CPU", new MetricThreshold(0.0, 80.0, 95.0));
        thresholdRules.put("TEMPERATURE", new MetricThreshold(15.0, 75.0, 90.0));
        thresholdRules.put("PRESSURE", new MetricThreshold(1.5, 9.0, 11.5));
        thresholdRules.put("QUERY_LATENCY", new MetricThreshold(0.0, 50.0, 120.0));
        thresholdRules.put("VIBRATION", new MetricThreshold(0.0, 4.5, 7.0));
    }

    public void setThreshold(String metricName, double min, double max, double criticalMax) {
        thresholdRules.put(metricName.toUpperCase(), new MetricThreshold(min, max, criticalMax));
    }

    @Override
    public String getDetectorName() {
        return "THRESHOLD";
    }

    @Override
    public Optional<AnomalyEvent> evaluate(SensorReading reading) {
        if (reading == null) return Optional.empty();

        String metric = reading.getMetricName().toUpperCase();
        MetricThreshold threshold = thresholdRules.get(metric);
        double val = reading.getValue();

        if (threshold != null) {
            if (val > threshold.getCriticalMax()) {
                return Optional.of(new AnomalyEvent(
                        reading.getRunId(),
                        reading.getComponentId(),
                        reading.getSensorId(),
                        reading.getMetricName(),
                        val,
                        threshold.getNormalMax(),
                        "CRITICAL",
                        getDetectorName(),
                        String.format("Critical threshold breach: %.2f %s exceeds critical limit %.2f",
                                val, reading.getUnit(), threshold.getCriticalMax())
                ));
            } else if (val > threshold.getNormalMax()) {
                return Optional.of(new AnomalyEvent(
                        reading.getRunId(),
                        reading.getComponentId(),
                        reading.getSensorId(),
                        reading.getMetricName(),
                        val,
                        threshold.getNormalMax(),
                        "HIGH",
                        getDetectorName(),
                        String.format("High threshold warning: %.2f %s exceeds upper limit %.2f",
                                val, reading.getUnit(), threshold.getNormalMax())
                ));
            } else if (val < threshold.getNormalMin()) {
                return Optional.of(new AnomalyEvent(
                        reading.getRunId(),
                        reading.getComponentId(),
                        reading.getSensorId(),
                        reading.getMetricName(),
                        val,
                        threshold.getNormalMin(),
                        "MEDIUM",
                        getDetectorName(),
                        String.format("Low threshold anomaly: %.2f %s is below normal lower bound %.2f",
                                val, reading.getUnit(), threshold.getNormalMin())
                ));
            }
        } else if ("CRITICAL".equalsIgnoreCase(reading.getStatus()) || "ANOMALY".equalsIgnoreCase(reading.getStatus())) {
            return Optional.of(new AnomalyEvent(
                    reading.getRunId(),
                    reading.getComponentId(),
                    reading.getSensorId(),
                    reading.getMetricName(),
                    val,
                    0.0,
                    "HIGH",
                    getDetectorName(),
                    "Telemetry flagged with " + reading.getStatus() + " status"
            ));
        }

        return Optional.empty();
    }

    @Override
    public void reset() {
        // Stateless evaluation
    }
}
