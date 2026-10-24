package com.systemtwin.model;

import java.time.Instant;
import java.util.Objects;

public class SensorReading {

    private long id;
    private String runId;
    private long timestamp;
    private String sensorId;
    private String componentId;
    private String metricName;
    private double value;
    private String unit;
    private String status; // NORMAL, WARNING, ANOMALY, CRITICAL

    public SensorReading() {
        this.timestamp = System.currentTimeMillis();
        this.status = "NORMAL";
    }

    public SensorReading(String runId, String sensorId, String componentId, String metricName, double value, String unit, String status) {
        this.runId = runId;
        this.timestamp = System.currentTimeMillis();
        this.sensorId = Objects.requireNonNull(sensorId, "sensorId must not be null");
        this.componentId = Objects.requireNonNull(componentId, "componentId must not be null");
        this.metricName = Objects.requireNonNull(metricName, "metricName must not be null");
        this.value = value;
        this.unit = unit != null ? unit : "";
        this.status = status != null ? status : "NORMAL";
    }

    public SensorReading(long timestamp, String runId, String sensorId, String componentId, String metricName, double value, String unit, String status) {
        this.runId = runId;
        this.timestamp = timestamp;
        this.sensorId = sensorId;
        this.componentId = componentId;
        this.metricName = metricName;
        this.value = value;
        this.unit = unit;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getInstant() {
        return Instant.ofEpochMilli(timestamp);
    }

    @Override
    public String toString() {
        return "SensorReading{" +
                "comp='" + componentId + '\'' +
                ", metric='" + metricName + '\'' +
                ", val=" + String.format("%.2f", value) + " " + unit +
                ", status=" + status +
                '}';
    }
}
