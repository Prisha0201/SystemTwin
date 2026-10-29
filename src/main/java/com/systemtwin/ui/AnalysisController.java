package com.systemtwin.ui;

import com.systemtwin.analysis.RootCauseAnalyzer;
import com.systemtwin.graph.CriticalityAnalyzer;
import com.systemtwin.graph.DependencyGraph;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;
import com.systemtwin.model.SimulationRun;
import com.systemtwin.model.SystemModel;
import com.systemtwin.service.MonitoringService;
import com.systemtwin.service.SimulationService;
import com.systemtwin.service.SystemService;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for root-cause diagnosis and performance benchmarking.
 */
public class AnalysisController {

    @FXML private Label rootCauseCompLabel;
    @FXML private Label confidenceLabel;
    @FXML private TextArea propagationPathArea;
    @FXML private TextArea explanationArea;

    private MonitoringService monitoringService;
    private SystemService systemService;
    private SimulationService simulationService;
    private final RootCauseAnalyzer rootCauseAnalyzer = new RootCauseAnalyzer();

    public void initServices(MonitoringService monService, SystemService sysService, SimulationService simService) {
        this.monitoringService = monService;
        this.systemService = sysService;
        this.simulationService = simService;
    }

    public void initServices(MonitoringService monService, SystemService sysService) {
        initServices(monService, sysService, null);
    }

    @FXML
    public void initialize() {
        // Never leave the result boxes blank: show guidance until the first diagnosis runs
        setText(rootCauseCompLabel, "Not diagnosed yet");
        setText(confidenceLabel, "N/A");
        setText(propagationPathArea,
                "No diagnosis yet.\nPress '⚡ Simulate Fault & Localize' for a one-click demo, or inject a fault on the "
                        + "'Simulation & Faults' tab and press '🔍 Diagnose Active Anomalies'.");
        setText(explanationArea,
                "After a diagnosis, this box explains why a component was chosen as the root cause, "
                        + "which components were affected, and how the failure spread along the dependency edges.");
    }

    // ------------------------------------------------------------------
    // Root cause localization
    // ------------------------------------------------------------------

    @FXML
    public void handleRunRootCauseAnalysis() {
        runDiagnosisSafely(null);
    }

    /**
     * One-click demo: resets the system to healthy, makes sure a simulation is running,
     * injects a failure into the most critical component and localizes it.
     */
    @FXML
    public void handleSimulateFaultAndDiagnose() {
        try {
            SystemModel sys = systemService != null ? systemService.getActiveSystem() : null;
            if (sys == null || sys.getComponents().isEmpty() || simulationService == null) {
                runDiagnosisSafely(null);
                return;
            }

            // 1. Clean slate so the demo shows exactly one fault
            for (ComponentModel c : sys.getComponents().values()) {
                c.setState(ComponentState.HEALTHY);
            }

            // 2. Make sure a simulation is running on THIS system
            boolean needsStart = !simulationService.isRunning()
                    || simulationService.getCurrentRun() == null
                    || simulationService.getEngine().getCurrentSystem() != sys;
            if (needsStart) {
                simulationService.start(sys, "RCA-Demo-" + (System.currentTimeMillis() % 1000), 42L);
            }

            // 3. Inject a hard failure into the most critical component
            String target = pickFaultTarget(sys);
            List<FailurePropagator.CascadeStep> steps = simulationService.injectFailure(target, true);

            StringBuilder header = new StringBuilder();
            header.append("⚡ Injected a hard failure into '").append(target).append("'. Cascade steps:\n");
            if (steps.isEmpty()) {
                header.append(" • (no cascade steps were produced)\n");
            }
            for (FailurePropagator.CascadeStep step : steps) {
                header.append(" • ").append(step).append("\n");
            }
            final String headerText = header.toString();

            // 4. Diagnose immediately from component states, then refresh once telemetry has accumulated
            runDiagnosisSafely(headerText);
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> runDiagnosisSafely(headerText));
            pause.play();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private String pickFaultTarget(SystemModel sys) {
        List<CriticalityAnalyzer.CriticalityReport> reports = new CriticalityAnalyzer().analyze(sys.getDependencyGraph());
        CriticalityAnalyzer.CriticalityReport best = null;
        for (CriticalityAnalyzer.CriticalityReport r : reports) {
            if (sys.getComponent(r.getComponentId()) == null) continue;
            if (best == null || r.getCriticalityScore() > best.getCriticalityScore()) {
                best = r;
            }
        }
        if (best != null) {
            return best.getComponentId();
        }
        return sys.getComponents().keySet().iterator().next();
    }

    private void runDiagnosisSafely(String header) {
        try {
            runDiagnosis(header);
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        setText(rootCauseCompLabel, "Diagnosis error");
        setText(confidenceLabel, "N/A");
        setText(propagationPathArea, "The diagnosis could not be completed.");
        setText(explanationArea, "Error while diagnosing: " + ex
                + "\nCheck that a system is loaded (header template selector) and try again.");
    }

    private void runDiagnosis(String header) {
        SystemModel sys = systemService != null ? systemService.getActiveSystem() : null;
        if (sys == null || sys.getComponents().isEmpty()) {
            setText(rootCauseCompLabel, "No system loaded");
            setText(confidenceLabel, "N/A");
            setText(propagationPathArea, "No system is loaded, so there is nothing to trace.");
            setText(explanationArea, "Select a template in the header or build a system in the '🏗 System Builder' tab.");
            return;
        }

        DependencyGraph graph = sys.getDependencyGraph();
        SimulationRun run = simulationService != null ? simulationService.getCurrentRun() : null;
        String runId = run != null ? run.getId() : null;

        // 1. Gather all active anomalies for components belonging to the active system
        List<AnomalyEvent> activeAnomalies = new ArrayList<>();
        if (monitoringService != null) {
            List<AnomalyEvent> all = monitoringService.getActiveAnomalies();
            for (AnomalyEvent a : all) {
                if (a == null || a.getComponentId() == null) continue;
                if (sys.getComponent(a.getComponentId()) == null) continue;
                if (runId != null && a.getRunId() != null && !runId.equals(a.getRunId())) continue;
                activeAnomalies.add(a);
            }
            // If runId filter excluded everything but active anomalies exist for this system, fall back to all system anomalies
            if (activeAnomalies.isEmpty()) {
                for (AnomalyEvent a : all) {
                    if (a != null && a.getComponentId() != null && sys.getComponent(a.getComponentId()) != null) {
                        activeAnomalies.add(a);
                    }
                }
            }
        }

        // 2. Identify unhealthy components (FAILED, DEGRADED, OFFLINE)
        Map<String, ComponentModel> unhealthy = new LinkedHashMap<>();
        for (ComponentModel c : sys.getComponents().values()) {
            if (c.getState() != ComponentState.HEALTHY) {
                unhealthy.put(c.getId(), c);
            }
        }

        // 3. If there are neither active anomalies nor unhealthy components:
        if (activeAnomalies.isEmpty() && unhealthy.isEmpty()) {
            String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            setText(rootCauseCompLabel, "All Systems Healthy");
            setText(confidenceLabel, "100%");
            setText(propagationPathArea, "Nominal: No active anomalies or failure cascades detected in the dependency topology.\n"
                    + "All components are operating within normal parameters.");
            StringBuilder sb = new StringBuilder();
            if (header != null) sb.append(header).append("\n");
            sb.append("✓ Status check at ").append(timeStr).append(":\n");
            sb.append("All ").append(sys.getComponents().size()).append(" components are HEALTHY and 0 active anomalies were detected.\n\n");
            sb.append("💡 To test root-cause localization:\n")
                    .append("1. Press '⚡ Simulate Fault & Localize' (one-click demo), or\n")
                    .append("2. Open '⚡ Simulation & Faults', start the simulation and click '⚡ Inject Failure' or '⚠ High Load', ")
                    .append("then return here and press '🔍 Diagnose Active Anomalies'.");
            setText(explanationArea, sb.toString());
            return;
        }

        // 4. Evidence = active anomalies + synthesized state findings for unhealthy components not yet represented
        List<AnomalyEvent> evidence = new ArrayList<>(activeAnomalies);
        Set<String> covered = new HashSet<>();
        for (AnomalyEvent a : activeAnomalies) {
            covered.add(a.getComponentId());
        }
        int stateFindings = 0;
        for (ComponentModel c : unhealthy.values()) {
            if (covered.contains(c.getId())) continue;
            boolean down = c.getState() == ComponentState.FAILED || c.getState() == ComponentState.OFFLINE;
            evidence.add(new AnomalyEvent(
                    runId != null ? runId : "state-scan",
                    c.getId(),
                    "state",
                    "state",
                    down ? 0.0 : 50.0,
                    100.0,
                    down ? "CRITICAL" : "WARNING",
                    "STATE_SCANNER",
                    "Component state is " + c.getState()));
            stateFindings++;
            covered.add(c.getId());
        }

        Optional<RootCauseAnalyzer.RootCauseDiagnosis> diagOpt = rootCauseAnalyzer.diagnose(evidence, graph);
        if (diagOpt.isEmpty()) {
            setText(rootCauseCompLabel, "Inconclusive");
            setText(confidenceLabel, "N/A");
            setText(propagationPathArea, "Not enough evidence to correlate an anomaly propagation chain.");
            setText(explanationArea, "Active anomalies were recorded, but they could not be correlated with the graph topology.");
            return;
        }

        RootCauseAnalyzer.RootCauseDiagnosis diag = diagOpt.get();
        String rootId = diag.getRootComponentId();
        ComponentModel rootComp = sys.getComponent(rootId);
        String rootName = rootComp != null ? rootComp.getName() : rootId;

        setText(rootCauseCompLabel, rootName + "  (" + rootId + ")");
        setText(confidenceLabel, String.format("%.0f%%", diag.getConfidenceScore() * 100.0));

        // Propagation chain: arrow summary + one line per hop level
        LinkedHashMap<String, Integer> hops = computeHops(graph, rootId);
        int maxHop = 0;
        for (int h : hops.values()) {
            maxHop = Math.max(maxHop, h);
        }

        List<String> arrow = new ArrayList<>();
        for (String id : hops.keySet()) {
            arrow.add(describe(sys, id));
        }

        StringBuilder chain = new StringBuilder();
        chain.append(String.join("  ➔  ", arrow)).append("\n\n");
        for (int level = 0; level <= maxHop; level++) {
            List<String> atLevel = new ArrayList<>();
            for (Map.Entry<String, Integer> e : hops.entrySet()) {
                if (e.getValue() == level) {
                    atLevel.add(describe(sys, e.getKey()));
                }
            }
            chain.append(level == 0 ? "Origin : " : "Hop " + level + "  : ").append(String.join(", ", atLevel)).append("\n");
        }

        List<String> unrelated = new ArrayList<>();
        for (String id : covered) {
            if (!hops.containsKey(id)) {
                unrelated.add(describe(sys, id));
            }
        }
        if (!unrelated.isEmpty()) {
            chain.append("\nAlso anomalous but NOT downstream of the root cause (separate fault): ")
                    .append(String.join(", ", unrelated)).append("\n");
        }
        setText(propagationPathArea, chain.toString());

        // Diagnosis explanation
        String timeStr = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        StringBuilder sb = new StringBuilder();
        if (header != null) sb.append(header).append("\n");
        sb.append("[Diagnosis updated at ").append(timeStr).append("]\n");
        sb.append("ROOT CAUSE: ").append(rootName).append(" (").append(rootId).append(")")
                .append(String.format("  —  confidence %.0f%%%n%n", diag.getConfidenceScore() * 100.0));
        sb.append("Why this component:\n");
        String firstLine = diag.getExplanation().split("\n")[0];
        sb.append(" • ").append(firstLine).append("\n");
        sb.append(" • Evidence analyzed: ").append(activeAnomalies.size()).append(" active telemetry anomaly event(s) and ")
                .append(stateFindings).append(" state-based finding(s) across ")
                .append(covered.size()).append(" anomalous/impacted component(s).\n");

        sb.append("\nHow the failure spreads (an edge 'X depends on Y' means X needs Y to work):\n");
        if (hops.size() <= 1) {
            sb.append(" • ").append(rootId).append(" has no downstream dependents, so the fault did not cascade further.\n");
        } else {
            for (Map.Entry<String, Integer> e : hops.entrySet()) {
                if (e.getValue() == 0) continue;
                String id = e.getKey();
                List<String> causes = new ArrayList<>();
                for (String dep : graph.getDirectDependencies(id)) {
                    if (hops.containsKey(dep) && hops.get(dep) < e.getValue()) {
                        causes.add(dep);
                    }
                }
                sb.append(" • ").append(describe(sys, id)).append(" depends on ")
                        .append(causes.isEmpty() ? rootId : String.join(", ", causes))
                        .append(" (hop ").append(e.getValue()).append(").\n");
            }
        }
        sb.append("\n💡 Next step: Inspect and recover '").append(rootId)
                .append("' on the '⚡ Simulation & Faults' tab. Fixing the root cause restores the components that depend on it.");
        setText(explanationArea, sb.toString());
    }

    /** BFS over "dependents" edges: hop 0 is the root, hop 1 its direct dependents, and so on. */
    private static LinkedHashMap<String, Integer> computeHops(DependencyGraph graph, String root) {
        LinkedHashMap<String, Integer> hops = new LinkedHashMap<>();
        hops.put(root, 0);
        Deque<String> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            int level = hops.get(current);
            for (String dependent : graph.getDirectDependents(current)) {
                if (!hops.containsKey(dependent)) {
                    hops.put(dependent, level + 1);
                    queue.add(dependent);
                }
            }
        }
        return hops;
    }

    private static String describe(SystemModel sys, String id) {
        ComponentModel c = sys.getComponent(id);
        return c != null ? id + " [" + c.getState() + "]" : id;
    }


    // ------------------------------------------------------------------
    // Small null-safe helpers
    // ------------------------------------------------------------------

    private static void setText(Label label, String text) {
        if (label != null) label.setText(text);
    }

    private static void setText(TextArea area, String text) {
        if (area != null) area.setText(text);
    }
}
