package com.systemtwin.ui;

import com.systemtwin.analysis.PerformanceAnalyzer;
import com.systemtwin.analysis.RootCauseAnalyzer;
import com.systemtwin.analysis.ZScoreDetector;
import com.systemtwin.benchmark.BenchmarkService;
import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SystemModel;
import com.systemtwin.service.MonitoringService;
import com.systemtwin.service.SystemService;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

/**
 * Controller for statistical analysis, root-cause diagnosis, and performance benchmarking.
 */
public class AnalysisController {

    @FXML private Label rootCauseCompLabel;
    @FXML private Label confidenceLabel;
    @FXML private TextArea propagationPathArea;
    @FXML private TextArea explanationArea;

    @FXML private Label benchmarkThroughputLabel;
    @FXML private Label benchmarkDurationLabel;
    @FXML private Label benchmarkHeapLabel;

    private MonitoringService monitoringService;
    private SystemService systemService;
    private final RootCauseAnalyzer rootCauseAnalyzer = new RootCauseAnalyzer();
    private final BenchmarkService benchmarkService = new BenchmarkService();

    public void initServices(MonitoringService monService, SystemService sysService) {
        this.monitoringService = monService;
        this.systemService = sysService;
    }

    @FXML
    public void handleRunRootCauseAnalysis() {
        if (monitoringService == null || systemService == null) return;
        SystemModel sys = systemService.getActiveSystem();
        List<AnomalyEvent> anomalies = monitoringService.getActiveAnomalies();

        if (sys == null || anomalies.isEmpty()) {
            if (explanationArea != null) {
                explanationArea.setText("No active anomalies or system topology available to analyze.");
            }
            return;
        }

        Optional<RootCauseAnalyzer.RootCauseDiagnosis> diagOpt = rootCauseAnalyzer.diagnose(anomalies, sys.getDependencyGraph());
        if (diagOpt.isPresent()) {
            RootCauseAnalyzer.RootCauseDiagnosis diag = diagOpt.get();
            if (rootCauseCompLabel != null) rootCauseCompLabel.setText(diag.getRootComponentId());
            if (confidenceLabel != null) confidenceLabel.setText(String.format("%.0f%%", diag.getConfidenceScore() * 100.0));
            if (propagationPathArea != null) propagationPathArea.setText(String.join(" ➔ ", diag.getPropagationChain()));
            if (explanationArea != null) explanationArea.setText(diag.getExplanation());
        }
    }

    @FXML
    public void handleRunBenchmark() {
        BenchmarkService.BenchmarkResult res = benchmarkService.runZScoreThroughputBenchmark(25000);
        if (benchmarkThroughputLabel != null) {
            benchmarkThroughputLabel.setText(String.format("%.1f ops/sec", res.getThroughputOpsPerSec()));
        }
        if (benchmarkDurationLabel != null) {
            benchmarkDurationLabel.setText(res.getDurationMillis() + " ms");
        }
        if (benchmarkHeapLabel != null) {
            benchmarkHeapLabel.setText(String.format("%.2f MB", res.getMemoryUsedMb()));
        }
    }
}
