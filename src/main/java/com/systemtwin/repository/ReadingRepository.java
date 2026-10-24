package com.systemtwin.repository;

import com.systemtwin.model.SensorReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * SQLite JDBC Repository for high-throughput sensor telemetry readings.
 */
public class ReadingRepository {

    private static final Logger log = LoggerFactory.getLogger(ReadingRepository.class);
    private final String dbUrl;

    public ReadingRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public void saveReading(SensorReading reading) throws SQLException {
        if (reading == null) return;
        saveReadingsBatch(Collections.singletonList(reading));
    }

    public void saveReadingsBatch(List<SensorReading> readings) throws SQLException {
        if (readings == null || readings.isEmpty()) return;

        String sql = "INSERT INTO sensor_readings (run_id, timestamp, sensor_id, component_id, metric_name, value, unit, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (SensorReading r : readings) {
                    ps.setString(1, r.getRunId());
                    ps.setLong(2, r.getTimestamp());
                    ps.setString(3, r.getSensorId());
                    ps.setString(4, r.getComponentId());
                    ps.setString(5, r.getMetricName());
                    ps.setDouble(6, r.getValue());
                    ps.setString(7, r.getUnit());
                    ps.setString(8, r.getStatus());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<SensorReading> findReadingsByRun(String runId) throws SQLException {
        List<SensorReading> list = new ArrayList<>();
        String sql = "SELECT id, run_id, timestamp, sensor_id, component_id, metric_name, value, unit, status FROM sensor_readings WHERE run_id = ? ORDER BY timestamp ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SensorReading r = new SensorReading(
                            rs.getLong("timestamp"),
                            rs.getString("run_id"),
                            rs.getString("sensor_id"),
                            rs.getString("component_id"),
                            rs.getString("metric_name"),
                            rs.getDouble("value"),
                            rs.getString("unit"),
                            rs.getString("status")
                    );
                    r.setId(rs.getLong("id"));
                    list.add(r);
                }
            }
        }
        return list;
    }

    public List<SensorReading> findRecentReadings(String runId, int limit) throws SQLException {
        List<SensorReading> list = new ArrayList<>();
        String sql = "SELECT id, run_id, timestamp, sensor_id, component_id, metric_name, value, unit, status FROM sensor_readings WHERE run_id = ? ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SensorReading r = new SensorReading(
                            rs.getLong("timestamp"),
                            rs.getString("run_id"),
                            rs.getString("sensor_id"),
                            rs.getString("component_id"),
                            rs.getString("metric_name"),
                            rs.getDouble("value"),
                            rs.getString("unit"),
                            rs.getString("status")
                    );
                    r.setId(rs.getLong("id"));
                    list.add(r);
                }
            }
        }
        Collections.reverse(list); // chronological
        return list;
    }

    public int countReadingsForRun(String runId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM sensor_readings WHERE run_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
