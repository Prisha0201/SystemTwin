package com.systemtwin.model;

import java.util.*;

public class ComponentModel {

    private final String id;
    private String name;
    private String type;
    private ComponentState state;
    private double cpuUtilizationPercent;
    private double memoryUtilizationPercent;
    private double temperatureCelsius;
    private double failureProbability;
    private double recoveryProbability;
    private final Map<String, String> metadata = new HashMap<>();
    private final List<Sensor> sensors = new ArrayList<>();

    public ComponentModel(String id, String name, String type) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.type = requireText(type, "type");
        this.state = ComponentState.HEALTHY;
        this.cpuUtilizationPercent = 15.0;
        this.memoryUtilizationPercent = 25.0;
        this.temperatureCelsius = 45.0;
        this.failureProbability = 0.01;
        this.recoveryProbability = 0.05;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = requireText(name, "name");
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = requireText(type, "type");
    }

    public ComponentState getState() {
        return state;
    }

    public void setState(ComponentState state) {
        this.state = Objects.requireNonNull(state, "state must not be null");
    }

    public double getCpuUtilizationPercent() {
        return cpuUtilizationPercent;
    }

    public void setCpuUtilizationPercent(double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 100.0) {
            throw new IllegalArgumentException("CPU utilization must be between 0 and 100");
        }
        this.cpuUtilizationPercent = value;
    }

    public double getMemoryUtilizationPercent() {
        return memoryUtilizationPercent;
    }

    public void setMemoryUtilizationPercent(double memoryUtilizationPercent) {
        if (!Double.isFinite(memoryUtilizationPercent) || memoryUtilizationPercent < 0.0 || memoryUtilizationPercent > 100.0) {
            throw new IllegalArgumentException("Memory utilization must be between 0 and 100");
        }
        this.memoryUtilizationPercent = memoryUtilizationPercent;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public double getFailureProbability() {
        return failureProbability;
    }

    public void setFailureProbability(double failureProbability) {
        this.failureProbability = Math.max(0.0, Math.min(1.0, failureProbability));
    }

    public double getRecoveryProbability() {
        return recoveryProbability;
    }

    public void setRecoveryProbability(double recoveryProbability) {
        this.recoveryProbability = Math.max(0.0, Math.min(1.0, recoveryProbability));
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(String key, String value) {
        this.metadata.put(key, value);
    }

    public List<Sensor> getSensors() {
        return Collections.unmodifiableList(sensors);
    }

    public void addSensor(Sensor sensor) {
        if (sensor != null && !sensors.contains(sensor)) {
            sensors.add(sensor);
        }
    }

    public void removeSensor(String sensorId) {
        sensors.removeIf(s -> s.getId().equals(sensorId));
    }

    public boolean isOperational() {
        return state != null && state.isOperational();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentModel that = (ComponentModel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ComponentModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", state=" + state +
                ", cpu=" + cpuUtilizationPercent +
                "%}";
    }
}
