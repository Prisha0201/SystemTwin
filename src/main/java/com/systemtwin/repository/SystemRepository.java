package com.systemtwin.repository;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;
import com.systemtwin.model.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * SQLite JDBC Repository for systems, components, and dependency edges.
 */
public class SystemRepository {

    private static final Logger log = LoggerFactory.getLogger(SystemRepository.class);
    private final String dbUrl;

    public SystemRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    public void saveSystem(SystemModel system) throws SQLException {
        if (system == null) return;
        String sql = "INSERT OR REPLACE INTO systems (id, name, description) VALUES (?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, system.getId());
            ps.setString(2, system.getName());
            ps.setString(3, system.getDescription());
            ps.executeUpdate();
        }

        for (ComponentModel comp : system.getComponents().values()) {
            saveComponent(system.getId(), comp);
        }

        for (Map.Entry<String, Set<String>> entry : system.getDependencyGraph().getAllDependencies().entrySet()) {
            String dependent = entry.getKey();
            for (String target : entry.getValue()) {
                saveDependency(system.getId(), dependent, target);
            }
        }
    }

    public void saveComponent(String systemId, ComponentModel comp) throws SQLException {
        if (comp == null) return;
        String sql = "INSERT OR REPLACE INTO components (id, system_id, name, type, state, cpu_utilization, metadata) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, comp.getId());
            ps.setString(2, systemId);
            ps.setString(3, comp.getName());
            ps.setString(4, comp.getType());
            ps.setString(5, comp.getState().name());
            ps.setDouble(6, comp.getCpuUtilizationPercent());
            ps.setString(7, comp.getMetadata().toString());
            ps.executeUpdate();
        }
    }

    public void saveDependency(String systemId, String sourceId, String targetId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO dependencies (source_id, target_id, system_id, weight) VALUES (?, ?, ?, 1.0)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sourceId);
            ps.setString(2, targetId);
            ps.setString(3, systemId);
            ps.executeUpdate();
        }
    }

    public Optional<SystemModel> findSystemById(String id) throws SQLException {
        String sysSql = "SELECT id, name, description FROM systems WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sysSql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                SystemModel system = new SystemModel(rs.getString("id"), rs.getString("name"), rs.getString("description"));

                // Load components
                String compSql = "SELECT id, name, type, state, cpu_utilization FROM components WHERE system_id = ?";
                try (PreparedStatement compPs = conn.prepareStatement(compSql)) {
                    compPs.setString(1, id);
                    try (ResultSet compRs = compPs.executeQuery()) {
                        while (compRs.next()) {
                            ComponentModel comp = new ComponentModel(compRs.getString("id"), compRs.getString("name"), compRs.getString("type"));
                            comp.setState(ComponentState.valueOf(compRs.getString("state")));
                            comp.setCpuUtilizationPercent(compRs.getDouble("cpu_utilization"));
                            system.addComponent(comp);
                        }
                    }
                }

                // Load dependencies
                String depSql = "SELECT source_id, target_id FROM dependencies WHERE system_id = ?";
                try (PreparedStatement depPs = conn.prepareStatement(depSql)) {
                    depPs.setString(1, id);
                    try (ResultSet depRs = depPs.executeQuery()) {
                        while (depRs.next()) {
                            system.addDependency(depRs.getString("source_id"), depRs.getString("target_id"));
                        }
                    }
                }

                return Optional.of(system);
            }
        }
    }

    public List<SystemModel> findAllSystems() throws SQLException {
        List<SystemModel> list = new ArrayList<>();
        String sql = "SELECT id FROM systems";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                findSystemById(rs.getString("id")).ifPresent(list::add);
            }
        }
        return list;
    }

    public void deleteSystem(String id) throws SQLException {
        String sql = "DELETE FROM systems WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
    }
}
