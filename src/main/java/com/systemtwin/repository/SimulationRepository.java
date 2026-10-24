package com.systemtwin.repository;

import com.systemtwin.model.SimulationRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * SQLite JDBC Repository for simulation runs and metadata.
 */
public class SimulationRepository {

    private static final Logger log = LoggerFactory.getLogger(SimulationRepository.class);
    private final String dbUrl;

    public SimulationRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public void saveRun(SimulationRun run) throws SQLException {
        if (run == null) return;
        String sql = "INSERT OR REPLACE INTO simulation_runs (id, system_id, name, start_time, end_time, status, tick_count, event_count, anomaly_count, random_seed) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, run.getId());
            ps.setString(2, run.getSystemId());
            ps.setString(3, run.getName());
            ps.setTimestamp(4, run.getStartTime() != null ? Timestamp.from(run.getStartTime()) : Timestamp.from(Instant.now()));
            ps.setTimestamp(5, run.getEndTime() != null ? Timestamp.from(run.getEndTime()) : null);
            ps.setString(6, run.getStatus());
            ps.setInt(7, run.getTickCount());
            ps.setInt(8, run.getEventCount());
            ps.setInt(9, run.getAnomalyCount());
            ps.setLong(10, run.getRandomSeed());
            ps.executeUpdate();
        }
    }

    public void updateRun(SimulationRun run) {
        if (run == null) return;
        String sql = "UPDATE simulation_runs SET end_time = ?, status = ?, tick_count = ?, event_count = ?, anomaly_count = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, run.getEndTime() != null ? Timestamp.from(run.getEndTime()) : null);
            ps.setString(2, run.getStatus());
            ps.setInt(3, run.getTickCount());
            ps.setInt(4, run.getEventCount());
            ps.setInt(5, run.getAnomalyCount());
            ps.setString(6, run.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.warn("Failed updating run {}: {}", run.getId(), e.getMessage());
        }
    }

    public Optional<SimulationRun> findRunById(String id) throws SQLException {
        String sql = "SELECT id, system_id, name, start_time, end_time, status, tick_count, event_count, anomaly_count, random_seed FROM simulation_runs WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                SimulationRun run = mapRow(rs);
                return Optional.of(run);
            }
        }
    }

    public List<SimulationRun> findAllRuns() throws SQLException {
        List<SimulationRun> list = new ArrayList<>();
        String sql = "SELECT id, system_id, name, start_time, end_time, status, tick_count, event_count, anomaly_count, random_seed FROM simulation_runs ORDER BY start_time DESC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public void deleteRun(String id) throws SQLException {
        String sql = "DELETE FROM simulation_runs WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }

    private SimulationRun mapRow(ResultSet rs) throws SQLException {
        SimulationRun run = new SimulationRun();
        run.setId(rs.getString("id"));
        run.setSystemId(rs.getString("system_id"));
        run.setName(rs.getString("name"));
        Timestamp start = rs.getTimestamp("start_time");
        if (start != null) run.setStartTime(start.toInstant());
        Timestamp end = rs.getTimestamp("end_time");
        if (end != null) run.setEndTime(end.toInstant());
        run.setStatus(rs.getString("status"));
        run.setTickCount(rs.getInt("tick_count"));
        run.setEventCount(rs.getInt("event_count"));
        run.setAnomalyCount(rs.getInt("anomaly_count"));
        run.setRandomSeed(rs.getLong("random_seed"));
        return run;
    }
}
