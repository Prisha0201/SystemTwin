package com.systemtwin.analysis;

import com.systemtwin.graph.DependencyGraph;
import com.systemtwin.model.AnomalyEvent;

import java.util.*;

/**
 * Correlates multi-component anomaly bursts with topological dependency graph structure
 * to pinpoint the most probable primary root-cause component.
 */
public class RootCauseAnalyzer {

    public static class RootCauseDiagnosis {
        private final String rootComponentId;
        private final double confidenceScore; // 0.0 to 1.0
        private final List<String> propagationChain;
        private final String explanation;
        private final int anomalyCount;

        public RootCauseDiagnosis(String rootComponentId, double confidenceScore, List<String> propagationChain, String explanation, int anomalyCount) {
            this.rootComponentId = rootComponentId;
            this.confidenceScore = confidenceScore;
            this.propagationChain = propagationChain;
            this.explanation = explanation;
            this.anomalyCount = anomalyCount;
        }

        public String getRootComponentId() { return rootComponentId; }
        public double getConfidenceScore() { return confidenceScore; }
        public List<String> getPropagationChain() { return propagationChain; }
        public String getExplanation() { return explanation; }
        public int getAnomalyCount() { return anomalyCount; }

        @Override
        public String toString() {
            return String.format("Root Cause: %s (Confidence: %.0f%%) | Chain: %s | %s",
                    rootComponentId, confidenceScore * 100.0, String.join(" -> ", propagationChain), explanation);
        }
    }

    /**
     * Diagnose the root cause component from a set of anomaly events and the system graph.
     */
    public Optional<RootCauseDiagnosis> diagnose(List<AnomalyEvent> anomalies, DependencyGraph graph) {
        if (anomalies == null || anomalies.isEmpty() || graph == null) {
            return Optional.empty();
        }

        // Sort anomalies chronologically
        List<AnomalyEvent> sorted = new ArrayList<>(anomalies);
        sorted.sort(Comparator.comparingLong(AnomalyEvent::getTimestamp));

        // Group anomalies by component
        Map<String, List<AnomalyEvent>> compAnomalies = new LinkedHashMap<>();
        for (AnomalyEvent a : sorted) {
            compAnomalies.computeIfAbsent(a.getComponentId(), k -> new ArrayList<>()).add(a);
        }

        // Find candidate with earliest anomaly
        String earliestComp = sorted.get(0).getComponentId();

        // Check topological graph reachability:
        // A component that can reach all other affected components downstream has highest probability
        String bestCandidate = earliestComp;
        double highestScore = 0.0;
        List<String> bestChain = new ArrayList<>();

        for (String candidate : compAnomalies.keySet()) {
            List<String> downstream = graph.getDownstreamCascadeOrder(candidate);
            int covered = 0;
            for (String other : compAnomalies.keySet()) {
                if (other.equals(candidate) || downstream.contains(other)) {
                    covered++;
                }
            }

            double coverage = (double) covered / compAnomalies.size();
            boolean isEarliest = candidate.equals(earliestComp);
            double score = (coverage * 0.65) + (isEarliest ? 0.35 : 0.0);

            if (score > highestScore) {
                highestScore = score;
                bestCandidate = candidate;
                bestChain.clear();
                bestChain.add(candidate);
                for (String d : downstream) {
                    if (compAnomalies.containsKey(d) && !bestChain.contains(d)) {
                        bestChain.add(d);
                    }
                }
            }
        }

        String explanation = String.format(
                "Component '%s' identified as root cause: initiated earliest telemetry variance and is upstream to %d affected dependent nodes.",
                bestCandidate, Math.max(0, bestChain.size() - 1)
        );

        return Optional.of(new RootCauseDiagnosis(
                bestCandidate,
                Math.min(1.0, highestScore),
                bestChain,
                explanation,
                anomalies.size()
        ));
    }
}
