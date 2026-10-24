package com.systemtwin.model;

import java.util.UUID;

public class AnomalyEvent {

    private String id;
    private String runId;
    private long timestamp;
    private String componentId;
    private String sensorId;
    private String metricName;
    private double metricValue;
    private double baselineValue;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String detector; // THRESHOLD, Z_SCORE, ROLLING_WINDOW
    private String message;

    public AnomalyEvent() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.severity = "MEDIUM";
    }

    public AnomalyEvent(String runId, String componentId, String sensorId, String metricName, double metricValue, double baselineValue, String severity, String detector, String message) {
        this.id = UUID.randomUUID().toString();
        this.runId = runId;
        this.timestamp = System.currentTimeMillis();
        this.componentId = componentId;
        this.sensorId = sensorId;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.baselineValue = baselineValue;
        this.severity = severity != null ? severity : "MEDIUM";
        this.detector = detector != null ? detector : "THRESHOLD";
        this.message = message;
    }

    public AnomalyEvent(String id, String runId, long timestamp, String componentId, String sensorId, String metricName, double metricValue, double baselineValue, String severity, String detector, String message) {
        this.id = id;
        this.runId = runId;
        this.timestamp = timestamp;
        this.componentId = componentId;
        this.sensorId = sensorId;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.baselineValue = baselineValue;
        this.severity = severity;
        this.detector = detector;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(double metricValue) {
        this.metricValue = metricValue;
    }

    public double getBaselineValue() {
        return baselineValue;
    }

    public void setBaselineValue(double baselineValue) {
        this.baselineValue = baselineValue;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getDetector() {
        return detector;
    }

    public void setDetector(String detector) {
        this.detector = detector;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "AnomalyEvent{" +
                "comp='" + componentId + '\'' +
                ", metric='" + metricName + '\'' +
                ", val=" + metricValue +
                ", sev='" + severity + '\'' +
                ", detector='" + detector + '\'' +
                '}';
    }
}
