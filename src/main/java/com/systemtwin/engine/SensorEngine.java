package com.systemtwin.engine;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;
import com.systemtwin.model.Sensor;
import com.systemtwin.model.SensorReading;

import java.util.*;

/**
 * Generates realistic, stochastic sensor telemetry for components.
 * Accounts for component operating states (HEALTHY, DEGRADED, FAILED)
 * and supports deterministic PRNG seeding for replayable simulation.
 */
public class SensorEngine {

    private Random random;

    public SensorEngine() {
        this.random = new Random(42L);
    }

    public SensorEngine(long seed) {
        this.random = new Random(seed);
    }

    public void setSeed(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Generates a sensor reading based on the current state of its parent component.
     */
    public SensorReading generateReading(String runId, ComponentModel component, Sensor sensor) {
        if (component == null || sensor == null) return null;

        double value;
        String status = "NORMAL";

        double min = sensor.getMinNormal();
        double max = sensor.getMaxNormal();
        double center = (min + max) / 2.0;
        double range = Math.max(1.0, max - min);

        ComponentState state = component.getState();
        if (state == ComponentState.FAILED || state == ComponentState.OFFLINE) {
            // Failed: either zero, minimum, or extreme out of bounds
            if (sensor.getMetricName().equalsIgnoreCase("CPU") || sensor.getMetricName().equalsIgnoreCase("THROUGHPUT")) {
                value = 0.0;
            } else {
                value = max + range * 0.8 + (random.nextDouble() * 5.0);
            }
            status = "CRITICAL";
        } else if (state == ComponentState.DEGRADED) {
            // Degraded: elevated readings, high variance, spikes
            double offset = range * (0.6 + random.nextDouble() * 0.4);
            value = center + offset;
            status = value > max ? "ANOMALY" : "WARNING";
        } else {
            // Healthy: Gaussian noise centered in normal range
            double noise = random.nextGaussian() * (range * 0.12);
            value = center + noise;
            // Bound slightly within reasonable values
            value = Math.max(min * 0.9, Math.min(max * 1.05, value));
            if (value < min || value > max) {
                status = "WARNING";
            }
        }

        sensor.setCurrentValue(value);

        return new SensorReading(
                runId,
                sensor.getId(),
                component.getId(),
                sensor.getMetricName(),
                value,
                sensor.getUnit(),
                status
        );
    }

    /**
     * Updates component internal CPU, memory, and temperature based on state.
     */
    public void updateComponentMetrics(ComponentModel component) {
        if (component == null) return;
        ComponentState state = component.getState();

        if (state == ComponentState.FAILED || state == ComponentState.OFFLINE) {
            component.setCpuUtilizationPercent(0.0);
            component.setMemoryUtilizationPercent(0.0);
            component.setTemperatureCelsius(Math.max(20.0, component.getTemperatureCelsius() - 1.5));
        } else if (state == ComponentState.DEGRADED) {
            double targetCpu = 85.0 + random.nextDouble() * 14.0;
            component.setCpuUtilizationPercent(Math.min(99.0, targetCpu));
            component.setMemoryUtilizationPercent(Math.min(95.0, 75.0 + random.nextDouble() * 20.0));
            component.setTemperatureCelsius(Math.min(95.0, component.getTemperatureCelsius() + 1.2));
        } else {
            double normalCpu = 15.0 + random.nextGaussian() * 6.0;
            component.setCpuUtilizationPercent(Math.max(5.0, Math.min(45.0, normalCpu)));
            component.setMemoryUtilizationPercent(Math.max(10.0, Math.min(50.0, 30.0 + random.nextGaussian() * 5.0)));
            double normalTemp = 45.0 + random.nextGaussian() * 3.0;
            component.setTemperatureCelsius(Math.max(30.0, Math.min(60.0, normalTemp)));
        }
    }
}
