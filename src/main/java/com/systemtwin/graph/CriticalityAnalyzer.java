package com.systemtwin.graph;

import com.systemtwin.model.ComponentModel;

import java.util.*;

/**
 * Calculates systemic criticality scores for components in a DependencyGraph.
 * Combines blast radius (downstream impact), dependency risk (upstream depth),
 * and degree centrality to identify Single Points of Failure (SPOFs).
 */
public class CriticalityAnalyzer {

    public static class CriticalityReport {
        private final String componentId;
        private final String componentName;
        private final double criticalityScore; // 0.0 to 100.0
        private final int blastRadius; // number of downstream components affected
        private final int upstreamDependencies; // number of dependencies
        private final boolean isSinglePointOfFailure;
        private final String assessment;

        public CriticalityReport(String componentId, String componentName, double criticalityScore, int blastRadius, int upstreamDependencies, boolean isSinglePointOfFailure, String assessment) {
            this.componentId = componentId;
            this.componentName = componentName;
            this.criticalityScore = criticalityScore;
            this.blastRadius = blastRadius;
            this.upstreamDependencies = upstreamDependencies;
            this.isSinglePointOfFailure = isSinglePointOfFailure;
            this.assessment = assessment;
        }

        public String getComponentId() { return componentId; }
        public String getComponentName() { return componentName; }
        public double getCriticalityScore() { return criticalityScore; }
        public int getBlastRadius() { return blastRadius; }
        public int getUpstreamDependencies() { return upstreamDependencies; }
        public boolean isSinglePointOfFailure() { return isSinglePointOfFailure; }
        public String getAssessment() { return assessment; }

        @Override
        public String toString() {
            return String.format("[%s] Score: %.1f | Blast Radius: %d | Upstream: %d | SPOF: %s",
                    componentName, criticalityScore, blastRadius, upstreamDependencies, isSinglePointOfFailure);
        }
    }

    public List<CriticalityReport> analyze(DependencyGraph graph) {
        List<CriticalityReport> reports = new ArrayList<>();
        if (graph == null || graph.getNodeCount() == 0) return reports;

        int totalNodes = graph.getNodeCount();
        Map<String, ComponentModel> nodes = graph.getNodes();

        for (String id : nodes.keySet()) {
            ComponentModel comp = nodes.get(id);
            List<String> downstream = graph.getDownstreamCascadeOrder(id);
            List<String> upstream = graph.getUpstreamDependencies(id);

            int blastRadius = downstream.size();
            int upstreamCount = upstream.size();
            int directDependents = graph.getDirectDependents(id).size();

            // Calculate normalized impact: blast radius percentage (0 - 50 pts)
            double blastFactor = totalNodes > 1 ? ((double) blastRadius / (totalNodes - 1)) * 50.0 : 0.0;

            // Direct dependents weighting (0 - 30 pts)
            double directFactor = totalNodes > 1 ? Math.min(30.0, ((double) directDependents / (totalNodes - 1)) * 30.0 * 2.0) : 0.0;

            // Upstream dependency risk (0 - 20 pts)
            double upstreamFactor = totalNodes > 1 ? Math.min(20.0, ((double) upstreamCount / (totalNodes - 1)) * 20.0) : 0.0;

            double rawScore = blastFactor + directFactor + upstreamFactor;
            double finalScore = Math.min(100.0, Math.max(5.0, rawScore));

            // Single Point of Failure criteria: affects > 35% of system or has highest blast radius
            boolean isSPOF = blastRadius >= Math.max(2, (int) Math.ceil(totalNodes * 0.35));

            String assessment;
            if (finalScore >= 75.0) {
                assessment = "CRITICAL: Mission-critical backbone. Failure will trigger severe cascading outage.";
            } else if (finalScore >= 50.0) {
                assessment = "HIGH: Significant operational impact across multiple dependent subsystems.";
            } else if (finalScore >= 25.0) {
                assessment = "MODERATE: Standard component with isolated or redundant downstream paths.";
            } else {
                assessment = "LOW: Edge component with minimal cascading blast radius.";
            }

            reports.add(new CriticalityReport(
                    id,
                    comp != null ? comp.getName() : id,
                    finalScore,
                    blastRadius,
                    upstreamCount,
                    isSPOF,
                    assessment
            ));
        }

        // Sort descending by criticality score
        reports.sort((a, b) -> Double.compare(b.getCriticalityScore(), a.getCriticalityScore()));
        return reports;
    }
}
