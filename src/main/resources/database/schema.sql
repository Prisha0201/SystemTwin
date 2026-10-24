-- SQLite Database Schema for SystemTwin
PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS systems (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS components (
    id VARCHAR(64) PRIMARY KEY,
    system_id VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64) NOT NULL,
    state VARCHAR(32) NOT NULL DEFAULT 'HEALTHY',
    cpu_utilization REAL DEFAULT 0.0,
    metadata TEXT,
    FOREIGN KEY (system_id) REFERENCES systems(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS dependencies (
    source_id VARCHAR(64) NOT NULL,
    target_id VARCHAR(64) NOT NULL,
    system_id VARCHAR(64) NOT NULL,
    weight REAL DEFAULT 1.0,
    PRIMARY KEY (source_id, target_id, system_id),
    FOREIGN KEY (source_id) REFERENCES components(id) ON DELETE CASCADE,
    FOREIGN KEY (target_id) REFERENCES components(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS simulation_runs (
    id VARCHAR(64) PRIMARY KEY,
    system_id VARCHAR(64),
    name VARCHAR(128) NOT NULL,
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'INITIALIZED',
    tick_count INTEGER DEFAULT 0,
    event_count INTEGER DEFAULT 0,
    anomaly_count INTEGER DEFAULT 0,
    random_seed BIGINT DEFAULT 42
);

CREATE TABLE IF NOT EXISTS sensor_readings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    run_id VARCHAR(64),
    timestamp BIGINT NOT NULL,
    sensor_id VARCHAR(64) NOT NULL,
    component_id VARCHAR(64) NOT NULL,
    metric_name VARCHAR(64) NOT NULL,
    value REAL NOT NULL,
    unit VARCHAR(32),
    status VARCHAR(32) DEFAULT 'NORMAL',
    FOREIGN KEY (run_id) REFERENCES simulation_runs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS simulation_events (
    id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64),
    timestamp BIGINT NOT NULL,
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64),
    target_id VARCHAR(64),
    description TEXT,
    payload TEXT,
    FOREIGN KEY (run_id) REFERENCES simulation_runs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS anomaly_events (
    id VARCHAR(64) PRIMARY KEY,
    run_id VARCHAR(64),
    timestamp BIGINT NOT NULL,
    component_id VARCHAR(64) NOT NULL,
    sensor_id VARCHAR(64),
    metric_name VARCHAR(64),
    metric_value REAL,
    baseline_value REAL,
    severity VARCHAR(32) DEFAULT 'MEDIUM',
    detector VARCHAR(64),
    message TEXT,
    FOREIGN KEY (run_id) REFERENCES simulation_runs(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_readings_run_comp ON sensor_readings(run_id, component_id);
CREATE INDEX IF NOT EXISTS idx_readings_time ON sensor_readings(timestamp);
CREATE INDEX IF NOT EXISTS idx_events_run_seq ON simulation_events(run_id, sequence_number);
CREATE INDEX IF NOT EXISTS idx_anomalies_run ON anomaly_events(run_id, timestamp);
