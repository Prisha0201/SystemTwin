package com.systemtwin.analysis;

import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Statistical rolling-window Z-Score anomaly detector.
 * Calculates rolling sample mean and standard deviation:
 * Z = (X - μ) / σ
 * Triggers anomaly alerts when |Z| exceeds standard deviation threshold (default 3.0σ).
 */
public class ZScoreDetector implements AnomalyDetector {

    private final int windowSize;
    private final double zThreshold;

    private static class RollingWindow {
        private final int capacity;
        private final Deque<Double> values = new ArrayDeque<>();
        private double sum = 0.0;
        private double sumSquares = 0.0;

        public RollingWindow(int capacity) {
            this.capacity = capacity;
        }

        public synchronized void add(double value) {
            if (values.size() >= capacity) {
                double removed = values.removeFirst();
                sum -= removed;
                sumSquares -= (removed * removed);
            }
            values.addLast(value);
            sum += value;
            sumSquares += (value * value);
        }

        public synchronized int size() {
            return values.size();
        }

        public synchronized double getMean() {
            return values.isEmpty() ? 0.0 : sum / values.size();
        }

        public synchronized double getStandardDeviation() {
            int n = values.size();
            if (n < 2) return 0.0;
            double mean = sum / n;
            double variance = Math.max(0.0, (sumSquares / n) - (mean * mean));
            return Math.sqrt(variance);
        }
    }

    private final Map<String, RollingWindow> windows = new ConcurrentHashMap<>();

    public ZScoreDetector() {
        this(30, 3.0);
    }

    public ZScoreDetector(int windowSize, double zThreshold) {
        this.windowSize = Math.max(5, windowSize);
        this.zThreshold = Math.max(1.0, zThreshold);
    }

    @Override
    public String getDetectorName() {
        return "Z_SCORE";
    }

    @Override
    public Optional<AnomalyEvent> evaluate(SensorReading reading) {
        if (reading == null) return Optional.empty();

        String key = reading.getComponentId() + ":" + reading.getMetricName();
        RollingWindow window = windows.computeIfAbsent(key, k -> new RollingWindow(windowSize));

        double value = reading.getValue();

        // Require minimum observations for statistical significance
        if (window.size() >= 8) {
            double mean = window.getMean();
            double stdDev = window.getStandardDeviation();

            // Only score if stdDev is non-trivial to prevent division by zero
            if (stdDev > 0.001) {
                double z = Math.abs(value - mean) / stdDev;
                if (z >= zThreshold) {
                    String severity = z >= 4.5 ? "CRITICAL" : (z >= 3.5 ? "HIGH" : "MEDIUM");
                    window.add(value); // record observation

                    return Optional.of(new AnomalyEvent(
                            reading.getRunId(),
                            reading.getComponentId(),
                            reading.getSensorId(),
                            reading.getMetricName(),
                            value,
                            mean,
                            severity,
                            getDetectorName(),
                            String.format("Statistical outlier: Z=%.2f (val: %.2f, μ: %.2f, σ: %.2f)",
                                    z, value, mean, stdDev)
                    ));
                }
            }
        }

        window.add(value);
        return Optional.empty();
    }

    @Override
    public void reset() {
        windows.clear();
    }

    public int getWindowSize() {
        return windowSize;
    }

    public double getZThreshold() {
        return zThreshold;
    }
}
