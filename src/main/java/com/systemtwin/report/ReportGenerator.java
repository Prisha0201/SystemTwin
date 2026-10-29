package com.systemtwin.report;

import com.systemtwin.graph.CriticalityAnalyzer;
import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;
import com.systemtwin.model.SimulationRun;
import com.systemtwin.model.SystemModel;

import java.time.Instant;
import java.util.*;

/**
 * Generates structured executive markdown and printable reports for simulation runs.
 */
public class ReportGenerator {

    public String generateExecutiveSummary(
            SimulationRun run,
            SystemModel system,
            List<SensorReading> readings,
            List<AnomalyEvent> anomalies,
            List<CriticalityAnalyzer.CriticalityReport> criticalityReports) {

        StringBuilder sb = new StringBuilder();
        sb.append("# SystemTwin Simulation Executive Summary\n\n");
        sb.append("**Generated on:** ").append(Instant.now()).append("\n\n");

        sb.append("## 1. Simulation Run Overview\n\n");
        if (run != null) {
            sb.append("| Metric | Value |\n");
            sb.append("| :--- | :--- |\n");
            sb.append("| **Run ID** | `").append(run.getId()).append("` |\n");
            sb.append("| **Run Name** | ").append(run.getName()).append(" |\n");
            sb.append("| **Status** | ").append(run.getStatus()).append(" |\n");
            sb.append("| **Ticks Evaluated** | ").append(run.getTickCount()).append(" |\n");
            sb.append("| **Total Telemetry Readings** | ").append(readings != null ? readings.size() : 0).append(" |\n");
            sb.append("| **Total Anomalies Detected** | ").append(anomalies != null ? anomalies.size() : 0).append(" |\n");
            sb.append("| **Random Seed** | ").append(run.getRandomSeed()).append(" |\n");
            sb.append("| **Duration (sec)** | ").append(run.getDurationSeconds()).append("s |\n\n");
        } else {
            sb.append("No active simulation run recorded.\n\n");
        }

        sb.append("## 2. System Topology and Health\n\n");
        if (system != null) {
            sb.append("| Component ID | Name | Type | State | CPU % |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- |\n");
            system.getComponents().values().forEach(comp -> {
                sb.append(String.format("| `%s` | %s | %s | %s | %.1f%% |\n",
                        comp.getId(), comp.getName(), comp.getType(), comp.getState(), comp.getCpuUtilizationPercent()));
            });
            sb.append("\n");
        }

        sb.append("## 3. Criticality & Single Point of Failure (SPOF) Analysis\n\n");
        if (criticalityReports != null && !criticalityReports.isEmpty()) {
            sb.append("| Component | Score | Blast Radius | Upstream Deps | SPOF? | Assessment |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- |\n");
            for (CriticalityAnalyzer.CriticalityReport cr : criticalityReports) {
                sb.append(String.format("| **%s** | %.1f | %d | %d | %s | %s |\n",
                        cr.getComponentName(), cr.getCriticalityScore(), cr.getBlastRadius(),
                        cr.getUpstreamDependencies(), cr.isSinglePointOfFailure() ? "⚠ **YES**" : "No", cr.getAssessment()));
            }
            sb.append("\n");
        }

        sb.append("## 4. Anomaly Breakdown\n\n");
        if (anomalies != null && !anomalies.isEmpty()) {
            long criticalCount = anomalies.stream().filter(a -> "CRITICAL".equalsIgnoreCase(a.getSeverity())).count();
            long highCount = anomalies.stream().filter(a -> "HIGH".equalsIgnoreCase(a.getSeverity())).count();
            long medCount = anomalies.stream().filter(a -> "MEDIUM".equalsIgnoreCase(a.getSeverity())).count();

            sb.append(String.format("- **CRITICAL severity:** %d alerts%n", criticalCount));
            sb.append(String.format("- **HIGH severity:** %d alerts%n", highCount));
            sb.append(String.format("- **MEDIUM severity:** %d alerts%n%n", medCount));

            sb.append("### Recent Notable Alerts\n\n");
            int count = 0;
            for (AnomalyEvent a : anomalies) {
                if (count++ >= 8) break;
                sb.append(String.format("- **[%s] %s** (`%s`): %s (value: %.2f vs baseline: %.2f)%n",
                        a.getSeverity(), a.getDetector(), a.getComponentId(), a.getMessage(), a.getMetricValue(), a.getBaselineValue()));
            }
        } else {
            sb.append("No anomalous events detected during this run.\n");
        }

        return sb.toString();
    }
}
