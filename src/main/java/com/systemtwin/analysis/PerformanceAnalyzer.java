package com.systemtwin.analysis;

import java.util.*;

/**
 * Computes telemetry throughput, latency percentiles, and anomaly detection quality metrics
 * (Precision, Recall, F1-Score, False Positive Rate).
 */
public class PerformanceAnalyzer {

    public static class DetectionMetrics {
        private final int truePositives;
        private final int falsePositives;
        private final int trueNegatives;
        private final int falseNegatives;

        public DetectionMetrics(int tp, int fp, int tn, int fn) {
            this.truePositives = tp;
            this.falsePositives = fp;
            this.trueNegatives = tn;
            this.falseNegatives = fn;
        }

        public double getPrecision() {
            int denom = truePositives + falsePositives;
            return denom > 0 ? (double) truePositives / denom : 1.0;
        }

        public double getRecall() {
            int denom = truePositives + falseNegatives;
            return denom > 0 ? (double) truePositives / denom : 1.0;
        }

        public double getF1Score() {
            double p = getPrecision();
            double r = getRecall();
            return (p + r) > 0 ? 2.0 * (p * r) / (p + r) : 0.0;
        }

        public double getAccuracy() {
            int total = truePositives + falsePositives + trueNegatives + falseNegatives;
            return total > 0 ? (double) (truePositives + trueNegatives) / total : 1.0;
        }

        public int getTruePositives() { return truePositives; }
        public int getFalsePositives() { return falsePositives; }
        public int getTrueNegatives() { return trueNegatives; }
        public int getFalseNegatives() { return falseNegatives; }

        @Override
        public String toString() {
            return String.format("Precision: %.2f | Recall: %.2f | F1-Score: %.2f | Accuracy: %.2f",
                    getPrecision(), getRecall(), getF1Score(), getAccuracy());
        }
    }

    public static class LatencyProfile {
        private final double p50Ms;
        private final double p95Ms;
        private final double p99Ms;
        private final double meanMs;
        private final double throughputPerSec;

        public LatencyProfile(double p50Ms, double p95Ms, double p99Ms, double meanMs, double throughputPerSec) {
            this.p50Ms = p50Ms;
            this.p95Ms = p95Ms;
            this.p99Ms = p99Ms;
            this.meanMs = meanMs;
            this.throughputPerSec = throughputPerSec;
        }

        public double getP50Ms() { return p50Ms; }
        public double getP95Ms() { return p95Ms; }
        public double getP99Ms() { return p99Ms; }
        public double getMeanMs() { return meanMs; }
        public double getThroughputPerSec() { return throughputPerSec; }

        @Override
        public String toString() {
            return String.format("Throughput: %.1f ops/s | p50: %.2fms | p95: %.2fms | p99: %.2fms",
                    throughputPerSec, p50Ms, p95Ms, p99Ms);
        }
    }

    public LatencyProfile calculateLatencyProfile(List<Long> latenciesNanos, long totalDurationMs) {
        if (latenciesNanos == null || latenciesNanos.isEmpty()) {
            return new LatencyProfile(0.0, 0.0, 0.0, 0.0, 0.0);
        }

        List<Long> sorted = new ArrayList<>(latenciesNanos);
        Collections.sort(sorted);
        int n = sorted.size();

        double p50 = sorted.get((int) (n * 0.50)) / 1_000_000.0;
        double p95 = sorted.get((int) (n * 0.95)) / 1_000_000.0;
        double p99 = sorted.get(Math.min(n - 1, (int) (n * 0.99))) / 1_000_000.0;

        double sum = 0.0;
        for (long l : sorted) sum += (l / 1_000_000.0);
        double mean = sum / n;

        double durationSec = Math.max(0.001, totalDurationMs / 1000.0);
        double throughput = n / durationSec;

        return new LatencyProfile(p50, p95, p99, mean, throughput);
    }

    public DetectionMetrics evaluateDetectionQuality(int tp, int fp, int tn, int fn) {
        return new DetectionMetrics(tp, fp, tn, fn);
    }
}
