# SystemTwin — Test Results & Verification Report

Comprehensive test run executed on Windows 11 with JDK 21, Kotlin 2.1.21, JUnit 5.12.2, and SQLite JDBC 3.49.1.0.

---

## 1. Test Execution Summary

| Metric | Result |
| :--- | :--- |
| **Total Test Suites** | 6 |
| **Total Test Cases** | **27** |
| **Failures** | **0** |
| **Errors** | **0** |
| **Skipped** | **0** |
| **Build Status** | **BUILD SUCCESS** |
| **Total Test Execution Time** | ~1.5 seconds |

---

## 2. Test Suite Breakdown

### Track 1: Member 1 (Pooja Balakrishnan) — Simulation & Systems Engineering

| Test Suite | Tests Run | Result | Key Verified Behaviors |
| :--- | :--- | :--- | :--- |
| `DependencyGraphTest.java` | 8 | PASSED | - Node addition and removal<br>- Direct incoming/outgoing edge lookups<br>- BFS downstream cascade reachability<br>- DFS upstream prerequisite resolution<br>- 3-color DFS cycle detection<br>- Kahn's topological sort on DAGs<br>- Cascading failure propagation<br>- Criticality score & SPOF identification |
| `SimulationEngineTest.java` | 4 | PASSED | - Simulation lifecycle (start, pause, resume, step, stop)<br>- Deterministic seeding (reproducible telemetry)<br>- Asynchronous event bus dispatch<br>- Failure injection and cascading state update |
| `PluginRegistryTest.java` | 3 | PASSED | - Built-in plugin discovery (SERVER, DATABASE, PUMP, PLC)<br>- Dynamic sensor configuration<br>- Custom third-party plugin registration |

### Track 2: Member 2 (Prisha) — Desktop Application & Analytics

| Test Suite | Tests Run | Result | Key Verified Behaviors |
| :--- | :--- | :--- | :--- |
| `AnomalyDetectorTest.java` | 4 | PASSED | - ThresholdDetector upper bound & critical alarms<br>- ZScoreDetector rolling 3σ outlier identification<br>- RootCauseAnalyzer topological candidate localization<br>- PerformanceAnalyzer precision, recall, and F1 scoring |
| `RepositoryTest.java` | 4 | PASSED | - SQLite schema initialization<br>- SystemModel & dependency topology persistence<br>- SimulationRun lifecycle update queries<br>- High-throughput sensor reading batch insertion<br>- SimulationEvent and AnomalyEvent persistence |
| `ScenarioValidatorTest.kt` | 4 | PASSED | - Valid system structure verification<br>- Invalid sensor range detection (min >= max)<br>- Cyclic dependency warning detection<br>- Failure injection validation on pre-failed components |

---

## 3. Performance Benchmark Results

- **ZScoreDetector Statistical Outlier Evaluation**:
  - Operations: 25,000 synthetic sensor readings
  - Elapsed Time: ~35 ms
  - Throughput: **~714,000 operations/sec**
  - Heap Impact: < 2.0 MB

- **Graph BFS Cascade Traversal**:
  - Operations: 10,000 traversals on multi-tier tree
  - Elapsed Time: ~18 ms
  - Throughput: **~555,000 traversals/sec**

- **SQLite Batch Telemetry Insertion**:
  - Telemetry points per batch: 1,000
  - Ingestion rate: **~12,500 readings/sec** on NVMe SSD
