package com.systemtwin.benchmark;

import com.systemtwin.analysis.ZScoreDetector;
import com.systemtwin.graph.DependencyGraph;
import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Executes performance benchmarks measuring event throughput, graph traversal latency,
 * and statistical detector processing speed.
 */
public class BenchmarkService {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkService.class);

    public static class BenchmarkResult {
        private final String benchmarkName;
        private final int operationsCount;
        private final long durationMillis;
        private final double throughputOpsPerSec;
        private final double memoryUsedMb;

        public BenchmarkResult(String benchmarkName, int operationsCount, long durationMillis, double throughputOpsPerSec, double memoryUsedMb) {
            this.benchmarkName = benchmarkName;
            this.operationsCount = operationsCount;
            this.durationMillis = durationMillis;
            this.throughputOpsPerSec = throughputOpsPerSec;
            this.memoryUsedMb = memoryUsedMb;
        }

        public String getBenchmarkName() { return benchmarkName; }
        public int getOperationsCount() { return operationsCount; }
        public long getDurationMillis() { return durationMillis; }
        public double getThroughputOpsPerSec() { return throughputOpsPerSec; }
        public double getMemoryUsedMb() { return memoryUsedMb; }

        @Override
        public String toString() {
            return String.format("[%s] %d ops in %d ms (%.1f ops/sec) | Heap: %.2f MB",
                    benchmarkName, operationsCount, durationMillis, throughputOpsPerSec, memoryUsedMb);
        }
    }

    public BenchmarkResult runZScoreThroughputBenchmark(int readingCount) {
        ZScoreDetector detector = new ZScoreDetector(30, 3.0);
        Random rand = new Random(42);

        System.gc();
        long memStart = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long start = System.currentTimeMillis();

        for (int i = 0; i < readingCount; i++) {
            double val = 50.0 + rand.nextGaussian() * 5.0;
            if (i % 250 == 0) val += 40.0; // spike
            SensorReading r = new SensorReading("bench-run", "s-" + (i % 10), "c-" + (i % 5), "CPU", val, "%", "NORMAL");
            detector.evaluate(r);
        }

        long duration = Math.max(1, System.currentTimeMillis() - start);
        long memEnd = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        double throughput = ((double) readingCount / duration) * 1000.0;
        double memUsedMb = Math.max(0.0, (memEnd - memStart) / (1024.0 * 1024.0));

        BenchmarkResult res = new BenchmarkResult("ZScoreDetector Throughput", readingCount, duration, throughput, memUsedMb);
        log.info("Benchmark finished: {}", res);
        return res;
    }

    public BenchmarkResult runGraphTraversalBenchmark(int nodeCount, int traversalIterations) {
        DependencyGraph graph = new DependencyGraph();
        for (int i = 0; i < nodeCount; i++) {
            graph.addComponent(new ComponentModel("node-" + i, "Node " + i, "SERVER"));
        }
        for (int i = 1; i < nodeCount; i++) {
            graph.addDependency("node-" + i, "node-" + (i / 2));
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < traversalIterations; i++) {
            graph.getDownstreamCascadeOrder("node-0");
        }
        long duration = Math.max(1, System.currentTimeMillis() - start);
        double throughput = ((double) traversalIterations / duration) * 1000.0;

        return new BenchmarkResult("Graph BFS Cascade Traversal", traversalIterations, duration, throughput, 0.0);
    }
}
