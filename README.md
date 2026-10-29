# SystemTwin — Digital Twin Simulation & Anomaly Detection Platform

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-blue.svg)](https://kotlinlang.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-green.svg)](https://openjfx.io/)
[![SQLite](https://img.shields.io/badge/SQLite-JDBC-lightgrey.svg)](https://github.com/xerial/sqlite-jdbc)
[![Tests](https://img.shields.io/badge/Tests-28%20Passed-brightgreen.svg)]()

**SystemTwin** is a native Java desktop digital twin simulation and monitoring platform. It combines graph-based topological dependency modeling, real-time stochastic telemetry generation, cascading fault propagation, statistical anomaly detection (rolling 3σ Z-score and dual thresholds), topological root-cause analysis, and embedded SQLite time-series persistence.

---

## 👥 Equal Two-Person Development Team

| Team Member | Role & Track | GitHub Profile | Email | Primary Branch | Key Modules |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Member 1: Pooja Balakrishnan** | **Simulation & Systems Engineering** | [@Pooja-Balakrishnan](https://github.com/Pooja-Balakrishnan) | `poojakrishnan0204@gmail.com` | `feature/simulation-engine` | `model/`, `engine/`, `events/`, `graph/`, `plugin/`, `SimulationService`, `SystemService`, `ReplayService` |
| **Member 2: Prisha** | **Desktop Application & Analytics** | [@Prisha0201](https://github.com/Prisha0201) | `prishasathish0201@gmail.com` | `feature/desktop-analytics` | `ui/`, `repository/`, `analysis/`, `report/`, `benchmark/`, `HistoryService`, `MonitoringService`, `TemplateService`, `ReportService`, `ScenarioValidator.kt` |

---

## 🚀 Key Features

1. **Digital Twin Modeling & Component Plugins**:
   - Extensible SPI plugin system for industrial PLCs, hydraulic pumps, robotic arms, cloud servers, and relational databases.
   - Dynamic attached sensor telemetry streams with Gaussian noise and state-dependent degradation.

2. **Graph-Based Topological Dependency Engine**:
   - Directed acyclic and cyclic dependency tracking with 3-color DFS cycle detection and Kahn's topological sort.
   - Systemic criticality and blast radius scoring identifying Single Points of Failure (SPOFs).
   - Step-by-step cascading failure propagation (BFS) and automated dependency recovery.

3. **Statistical Anomaly Detection & Root-Cause Diagnosis**:
   - Real-time rolling window (W=30) sample mean and standard deviation Z-score outlier detection ($|Z| \ge 3.0$).
   - Static multi-tier threshold warning and critical alarms.
   - Topological root-cause analysis correlating temporal anomaly bursts with graph reachability.

4. **Deterministic Replay & Fault Injection**:
   - Reproducible simulation runs via seeded pseudo-random number generators (PRNG).
   - Sequenced event recording and historical deterministic replay.
   - Interactive hard fault injection, performance throttling, and dependency severed triggers.

5. **Embedded Persistence & Desktop UI**:
   - High-throughput batch writes to local SQLite database (`~/.systemtwin/systemtwin.db`).
   - Sleek dark-themed JavaFX desktop console featuring live KPI metric cards, real-time telemetry streaming, interactive system builder, and historical comparison reports.
   - CSV telemetry export and structured executive markdown reports.
   - Kotlin scenario validation module.

---

## 🛠️ How to Run the Application

### Prerequisites
- **JDK 21** or later (`java -version`)
- **Apache Maven 3.9+** (`mvn -version`)

### 1. Launch the Desktop Application
```bash
mvn clean javafx:run
```

### 2. Run All Unit Tests & Validation Suites
```bash
mvn clean test
```
*Executes all 27 unit tests across Graph, Engine, Plugins, Repositories, Anomaly Detectors, and Kotlin ScenarioValidator.*

---

## 🌿 Git Collaboration & Branching Strategy

This project follows a strict collaborative feature-branch workflow:
- `main` — Production-ready integrated application.
- `feature/simulation-engine` — Member 1 (Pooja Balakrishnan).
- `feature/desktop-analytics` — Member 2 (Prisha).

Changes are merged into `main` after running unit tests and coordinating shared interface contracts (`pom.xml`, `SpringConfig.java`, `schema.sql`, `model/`).

---

## 📚 Technical Documentation

- 🏛️ [Architecture & Threading Model](docs/architecture.md)
- 🧮 [Algorithms & Mathematical Formulations](docs/algorithms.md)
- 🧪 [Test Results & Verification Report](docs/test-results.md)
- 🏭 [Industrial Manufacturing Template](examples/industrial-system/system.json)
- ☁️ [Cloud Data Center Template](examples/data-center/system.json)
- 🔬 [Biosafety Laboratory Template](examples/laboratory-system/system.json)

---

## 💼 Internship Interview Guide

### Member 1 (Pooja Balakrishnan) — Talking Points
- **Graph Algorithms & Concurrency**: Explain how level-order BFS traverses downstream dependents for cascading failure propagation, and how Kahn's algorithm verifies topological ordering.
- **Simulation Scheduling**: Discuss the decoupling of the multi-threaded simulation ticker from the JavaFX UI rendering loop.
- **Deterministic Replay**: Explain how sequence numbers and seeded PRNG ensure 100% reproducible fault scenarios.

### Member 2 (Prisha) — Talking Points
- **Statistical Engineering**: Explain Welford's algorithm and rolling Z-score calculation ($Z = \frac{|x - \mu|}{\sigma}$), precision/recall/F1 metrics, and false positive mitigation.
- **Data Persistence**: Describe JDBC batch commit transactions achieving >10,000 sensor readings/sec in SQLite.
- **Root-Cause Analysis**: Detail how topological reachability coverage is blended with temporal anomaly ordering to pinpoint origin components.
