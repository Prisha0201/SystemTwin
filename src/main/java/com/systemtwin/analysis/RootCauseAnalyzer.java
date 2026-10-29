package com.systemtwin.analysis;

import com.systemtwin.graph.DependencyGraph;
import com.systemtwin.model.AnomalyEvent;

import java.util.*;

/**
 * Correlates multi-component anomaly bursts with topological dependency graph structure
 * to pinpoint the most probable primary root-cause component.
 *
 * Score(c) = 0.65 * Coverage(c) + 0.35 * RootPosition(c)
 *   Coverage(c)     = share of anomalous components that are c itself or downstream of c
 *   RootPosition(c) = 1 if c raised the earliest anomaly OR no anomalous component sits upstream of c
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

        // Candidate that raised the earliest anomaly
        String earliestComp = sorted.get(0).getComponentId();

        String bestCandidate = earliestComp;
        double highestScore = -1.0;
        int bestCovered = 0;
        boolean bestIsEarliest = false;
        boolean bestIsSource = false;
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
            boolean isSource = !hasAnomalousUpstream(candidate, compAnomalies.keySet(), graph);
            double score = (coverage * 0.65) + ((isEarliest || isSource) ? 0.35 : 0.0);

            if (score > highestScore) {
                highestScore = score;
                bestCandidate = candidate;
                bestCovered = covered;
                bestIsEarliest = isEarliest;
                bestIsSource = isSource;
                bestChain.clear();
                bestChain.add(candidate);
                for (String d : downstream) {
                    if (!bestChain.contains(d)) {
                        bestChain.add(d);
                    }
                }
            }
        }

        double confidence = Math.min(1.0, Math.max(0.0, highestScore));

        String chainStr = bestChain.size() > 1 ? String.join(" ➔ ", bestChain) : bestCandidate + " (Terminal Node)";

        String reason;
        if (bestIsSource && bestIsEarliest) {
            reason = "no anomalous component sits upstream of it and it raised the earliest anomaly";
        } else if (bestIsSource) {
            reason = "no anomalous component sits upstream of it, so nothing else can explain its fault";
        } else if (bestIsEarliest) {
            reason = "it raised the earliest anomaly";
        } else {
            reason = "it explains the largest share of the observed anomalies";
        }

        String explanation = String.format(
                "Component '%s' identified as primary root cause (confidence %.0f%%) because %s. "
                        + "It accounts for %d of %d anomalous component(s) and cascades downstream across %d dependent node(s):\nPathway: %s",
                bestCandidate, confidence * 100.0, reason,
                bestCovered, compAnomalies.size(), Math.max(0, bestChain.size() - 1), chainStr
        );

        return Optional.of(new RootCauseDiagnosis(
                bestCandidate,
                confidence,
                bestChain,
                explanation,
                anomalies.size()
        ));
    }

    /**
     * True when at least one other anomalous component is a (transitive) prerequisite of the candidate.
     */
    private boolean hasAnomalousUpstream(String candidate, Set<String> anomalous, DependencyGraph graph) {
        for (String upstream : graph.getUpstreamDependencies(candidate)) {
            if (!upstream.equals(candidate) && anomalous.contains(upstream)) {
                return true;
            }
        }
        return false;
    }
}
