# SystemTwin — Architecture & System Design

SystemTwin is a native Java desktop digital twin simulation and monitoring platform engineered with a clean, decoupled layer architecture, event-driven reactive messaging, graph-based topological dependency analysis, statistical anomaly detection, and embedded SQLite time-series telemetry persistence.

---

## 1. High-Level Architectural Layers

```
+---------------------------------------------------------------------------------+
|                       JavaFX Desktop UI Layer (ui/)                             |
|   DashboardController  |  BuilderController  | SimulationController | History   |
+---------------------------------------------------------------------------------+
         |                           |                                |
         v                           v                                v
+---------------------------------------------------------------------------------+
|                         Application Services (service/)                         |
|   SimulationService   |   SystemService   |  MonitoringService | HistoryService |
|   ReplayService       |   TemplateService |  ReportService                      |
+---------------------------------------------------------------------------------+
         |                           |                                |
         v                           v                                v
+----------------------------------+ +--------------------------------------------+
|  Simulation Engine (engine/)     | |  Analytics & Diagnostics (analysis/)       |
|  - SimulationEngine              | |  - ThresholdDetector                       |
|  - SimulationScheduler           | |  - ZScoreDetector (Rolling 3σ)             |
|  - SensorEngine                  | |  - RootCauseAnalyzer (Topological)         |
|  - FailureInjector               | |  - PerformanceAnalyzer                     |
|                                  | |  - ScenarioValidator (Kotlin Module)       |
+----------------------------------+ +--------------------------------------------+
         |                                                   |
         v                                                   v
+----------------------------------+ +--------------------------------------------+
|  Graph & Algorithms (graph/)     | |  Event Infrastructure (events/)            |
|  - DependencyGraph (BFS/DFS)     | |  - EventBus (Publish/Subscribe)            |
|  - FailurePropagator (Cascades)  | |  - EventRecorder (Ordered Log)             |
|  - CriticalityAnalyzer (SPOF)    | |  - EventConsumer (Functional SPI)          |
+----------------------------------+ +--------------------------------------------+
         |                                                   |
         +-------------------------+-------------------------+
                                   |
                                   v
+---------------------------------------------------------------------------------+
|                        Persistence Layer (repository/)                          |
|   SystemRepository   |  ReadingRepository  | SimulationRepository | EventRepo   |
|   SQLite Embedded Database (~/.systemtwin/systemtwin.db)                        |
+---------------------------------------------------------------------------------+
```

---

## 2. Component Ownership & Two-Track Architecture

| Development Track | Member | Packages & Responsibilities | Key Capabilities |
| :--- | :--- | :--- | :--- |
| **Track 1: Simulation & Systems Engineering** | **Member 1 (Pooja Balakrishnan)** | `model/`, `engine/`, `events/`, `graph/`, `plugin/`, `SimulationService`, `SystemService`, `ReplayService` | Graph BFS/DFS, cascading fault propagation, criticality & SPOF scoring, thread scheduler, deterministic replay with seeded PRNG, plugin SPI. |
| **Track 2: Desktop Application & Analytics** | **Member 2 (Prisha)** | `ui/`, `repository/`, `analysis/`, `report/`, `benchmark/`, `HistoryService`, `MonitoringService`, `TemplateService`, `ReportService`, `ScenarioValidator.kt` | JavaFX UI, SQLite JDBC batch persistence, rolling Z-score statistical outlier detection, root cause localization, benchmarks, CSV export, Kotlin validator. |

---

## 3. Concurrency & Threading Model

SystemTwin isolates heavy computation and I/O from the desktop user interface using dedicated execution contexts:

1. **JavaFX Application Thread (`Platform.runLater`)**:
   - Renders telemetry tables, charts, KPI cards, and event stream lists at 60 FPS without UI freezing.
   - Refreshes via a 500ms decoupled `Timeline` polling memory buffers.

2. **Simulation Scheduler (`ScheduledExecutorService`)**:
   - Single-threaded high-priority ticker executing at regulated intervals (e.g., 1000ms / speed factor).
   - Generates stochastic sensor telemetry, updates component states, and batches persistence writes.

3. **Event Bus Worker Pool (`Executors.newCachedThreadPool`)**:
   - Dispatches simulation and anomaly events asynchronously to decoupled listeners without blocking ticks.

4. **Deterministic Replay Worker (`Executors.newSingleThreadExecutor`)**:
   - Reads historical runs from SQLite and plays events in strict sequence preserving original or scaled time offsets.

---

## 4. SQLite Persistence & Schema Design

Stored locally at `~/.systemtwin/systemtwin.db`:

- `systems`: System metadata and descriptions.
- `components`: Nodes representing industrial/IT equipment (state, CPU, metadata).
- `dependencies`: Directed dependency edges (`source_id` depends on `target_id`).
- `simulation_runs`: Run headers (start, end, status, tick count, anomaly count, PRNG seed).
- `sensor_readings`: Time-series sensor points (batch-committed for high throughput).
- `simulation_events`: Sequenced lifecycle and cascade events.
- `anomaly_events`: Detected breaches with baseline values and severity levels.
