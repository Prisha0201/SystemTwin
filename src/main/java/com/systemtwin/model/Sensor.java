package com.systemtwin.model;

import java.util.Objects;

public class Sensor {

    private final String id;
    private final String componentId;
    private String metricName;
    private String unit;
    private double minNormal;
    private double maxNormal;
    private double currentValue;
    private long sampleRateMs;

    public Sensor(String id, String componentId, String metricName, String unit, double minNormal, double maxNormal) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.componentId = Objects.requireNonNull(componentId, "componentId must not be null");
        this.metricName = Objects.requireNonNull(metricName, "metricName must not be null");
        this.unit = unit != null ? unit : "";
        this.minNormal = minNormal;
        this.maxNormal = maxNormal;
        this.currentValue = (minNormal + maxNormal) / 2.0;
        this.sampleRateMs = 1000L;
    }

    public String getId() {
        return id;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getMinNormal() {
        return minNormal;
    }

    public void setMinNormal(double minNormal) {
        this.minNormal = minNormal;
    }

    public double getMaxNormal() {
        return maxNormal;
    }

    public void setMaxNormal(double maxNormal) {
        this.maxNormal = maxNormal;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public long getSampleRateMs() {
        return sampleRateMs;
    }

    public void setSampleRateMs(long sampleRateMs) {
        this.sampleRateMs = sampleRateMs;
    }

    public boolean isWithinNormalRange(double value) {
        return value >= minNormal && value <= maxNormal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sensor sensor = (Sensor) o;
        return Objects.equals(id, sensor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "id='" + id + '\'' +
                ", componentId='" + componentId + '\'' +
                ", metricName='" + metricName + '\'' +
                ", value=" + currentValue +
                " " + unit +
                '}';
    }
}
