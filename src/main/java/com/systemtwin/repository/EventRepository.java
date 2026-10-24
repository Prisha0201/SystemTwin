package com.systemtwin.repository;

import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SimulationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * SQLite JDBC Repository for simulation lifecycle events and anomaly detections.
 */
public class EventRepository {

    private static final Logger log = LoggerFactory.getLogger(EventRepository.class);
    private final String dbUrl;

    public EventRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public void saveEvent(SimulationEvent event) throws SQLException {
        if (event == null) return;
        String sql = "INSERT OR REPLACE INTO simulation_events (id, run_id, timestamp, sequence_number, event_type, source_id, target_id, description, payload) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, event.getId());
            ps.setString(2, event.getRunId());
            ps.setLong(3, event.getTimestamp());
            ps.setLong(4, event.getSequenceNumber());
            ps.setString(5, event.getEventType());
            ps.setString(6, event.getSourceId());
            ps.setString(7, event.getTargetId());
            ps.setString(8, event.getDescription());
            ps.setString(9, event.getPayload());
            ps.executeUpdate();
        }
    }

    public void saveAnomalyEvent(AnomalyEvent anomaly) throws SQLException {
        if (anomaly == null) return;
        String sql = "INSERT OR REPLACE INTO anomaly_events (id, run_id, timestamp, component_id, sensor_id, metric_name, metric_value, baseline_value, severity, detector, message) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, anomaly.getId());
            ps.setString(2, anomaly.getRunId());
            ps.setLong(3, anomaly.getTimestamp());
            ps.setString(4, anomaly.getComponentId());
            ps.setString(5, anomaly.getSensorId());
            ps.setString(6, anomaly.getMetricName());
            ps.setDouble(7, anomaly.getMetricValue());
            ps.setDouble(8, anomaly.getBaselineValue());
            ps.setString(9, anomaly.getSeverity());
            ps.setString(10, anomaly.getDetector());
            ps.setString(11, anomaly.getMessage());
            ps.executeUpdate();
        }
    }

    public List<SimulationEvent> findEventsByRun(String runId) throws SQLException {
        List<SimulationEvent> list = new ArrayList<>();
        String sql = "SELECT id, run_id, timestamp, sequence_number, event_type, source_id, target_id, description, payload FROM simulation_events WHERE run_id = ? ORDER BY sequence_number ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SimulationEvent(
                            rs.getString("id"),
                            rs.getString("run_id"),
                            rs.getLong("timestamp"),
                            rs.getLong("sequence_number"),
                            rs.getString("event_type"),
                            rs.getString("source_id"),
                            rs.getString("target_id"),
                            rs.getString("description"),
                            rs.getString("payload")
                    ));
                }
            }
        }
        return list;
    }

    public List<AnomalyEvent> findAnomaliesByRun(String runId) throws SQLException {
        List<AnomalyEvent> list = new ArrayList<>();
        String sql = "SELECT id, run_id, timestamp, component_id, sensor_id, metric_name, metric_value, baseline_value, severity, detector, message FROM anomaly_events WHERE run_id = ? ORDER BY timestamp DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AnomalyEvent(
                            rs.getString("id"),
                            rs.getString("run_id"),
                            rs.getLong("timestamp"),
                            rs.getString("component_id"),
                            rs.getString("sensor_id"),
                            rs.getString("metric_name"),
                            rs.getDouble("metric_value"),
                            rs.getDouble("baseline_value"),
                            rs.getString("severity"),
                            rs.getString("detector"),
                            rs.getString("message")
                    ));
                }
            }
        }
        return list;
    }

    public int countAnomaliesForRun(String runId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM anomaly_events WHERE run_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
