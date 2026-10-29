package com.systemtwin.benchmark;

import com.systemtwin.analysis.ZScoreDetector;
import com.systemtwin.graph.DependencyGraph;
import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

/**
 * Executes performance benchmarks measuring statistical detector throughput,
 * BFS cascade traversal speed and SQLite batch-insert throughput.
 *
 * All timings use System.nanoTime() so very fast loops are not distorted by
 * millisecond rounding.
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

    private static long usedHeapBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static BenchmarkResult buildResult(String name, int operations, long elapsedNanos, long memStart, long memEnd) {
        long safeNanos = Math.max(1L, elapsedNanos);
        long millis = Math.max(1L, safeNanos / 1_000_000L);
        double throughput = operations / (safeNanos / 1_000_000_000.0);
        double memMb = Math.max(0.0, (memEnd - memStart) / (1024.0 * 1024.0));
        BenchmarkResult res = new BenchmarkResult(name, operations, millis, throughput, memMb);
        log.info("Benchmark finished: {}", res);
        return res;
    }

    public BenchmarkResult runZScoreThroughputBenchmark(int readingCount) {
        ZScoreDetector detector = new ZScoreDetector(30, 3.0);
        Random rand = new Random(42);

        System.gc();
        long memStart = usedHeapBytes();
        long start = System.nanoTime();

        for (int i = 0; i < readingCount; i++) {
            double val = 50.0 + rand.nextGaussian() * 5.0;
            if (i % 250 == 0) val += 40.0; // spike
            SensorReading r = new SensorReading("bench-run", "s-" + (i % 10), "c-" + (i % 5), "CPU", val, "%", "NORMAL");
            detector.evaluate(r);
        }

        long elapsed = System.nanoTime() - start;
        long memEnd = usedHeapBytes();
        return buildResult("ZScoreDetector Throughput", readingCount, elapsed, memStart, memEnd);
    }

    /**
     * Benchmarks the BFS downstream cascade traversal on a synthetic binary-tree dependency graph.
     * Node i depends on node i/2, so a failure of node-0 cascades through every other node.
     */
    public BenchmarkResult runGraphTraversalBenchmark(int nodeCount, int traversalIterations) {
        int nodes = Math.max(2, nodeCount);
        DependencyGraph graph = new DependencyGraph();
        for (int i = 0; i < nodes; i++) {
            graph.addComponent(new ComponentModel("node-" + i, "Node " + i, "SERVER"));
        }
        for (int i = 1; i < nodes; i++) {
            graph.addDependency("node-" + i, "node-" + (i / 2));
        }

        // Warm-up so JIT compilation is not counted in the measurement
        long sink = 0;
        for (int i = 0; i < 1000; i++) {
            sink += graph.getDownstreamCascadeOrder("node-0").size();
        }

        System.gc();
        long memStart = usedHeapBytes();
        long start = System.nanoTime();
        for (int i = 0; i < traversalIterations; i++) {
            sink += graph.getDownstreamCascadeOrder("node-0").size();
        }
        long elapsed = System.nanoTime() - start;
        long memEnd = usedHeapBytes();

        log.debug("BFS benchmark visited {} nodes in total (checksum)", sink);
        return buildResult("Graph BFS Cascade Traversal", traversalIterations, elapsed, memStart, memEnd);
    }

    /**
     * Benchmarks batched SQLite inserts. The benchmark creates and drops its own scratch table,
     * so it works on a brand-new in-memory database and never touches real telemetry data.
     *
     * @throws IllegalStateException if the database cannot be written to
     */
    public BenchmarkResult runSqliteBatchBenchmark(String dbUrl, int readingCount) {
        String url = (dbUrl != null && !dbUrl.isBlank()) ? dbUrl : "jdbc:sqlite::memory:";
        String create = "CREATE TABLE IF NOT EXISTS bench_readings ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, run_id TEXT, timestamp INTEGER, sensor_id TEXT, "
                + "component_id TEXT, metric_name TEXT, value REAL, unit TEXT, status TEXT)";
        String insert = "INSERT INTO bench_readings (run_id, timestamp, sensor_id, component_id, metric_name, value, unit, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        System.gc();
        long memStart = usedHeapBytes();
        long elapsed;
        long memEnd;

        try (Connection conn = DriverManager.getConnection(url)) {
            try (Statement st = conn.createStatement()) {
                st.execute(create);
            }

            long start = System.nanoTime();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                for (int i = 0; i < readingCount; i++) {
                    ps.setString(1, "bench-run");
                    ps.setLong(2, System.currentTimeMillis());
                    ps.setString(3, "s-" + (i % 8));
                    ps.setString(4, "c-" + (i % 4));
                    ps.setString(5, "CPU");
                    ps.setDouble(6, 42.5);
                    ps.setString(7, "%");
                    ps.setString(8, "NORMAL");
                    ps.addBatch();
                    if ((i + 1) % 1000 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
            }
            conn.commit();
            elapsed = System.nanoTime() - start;
            memEnd = usedHeapBytes();

            try (Statement st = conn.createStatement()) {
                st.execute("DROP TABLE IF EXISTS bench_readings");
            }
        } catch (SQLException ex) {
            log.warn("SQLite benchmark failed: {}", ex.getMessage());
            throw new IllegalStateException("SQLite benchmark failed: " + ex.getMessage(), ex);
        }

        return buildResult("SQLite Batch Insert Throughput", readingCount, elapsed, memStart, memEnd);
    }
}
