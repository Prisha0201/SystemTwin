package com.systemtwin.service;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.SystemModel;
import com.systemtwin.plugin.ComponentPlugin;
import com.systemtwin.plugin.PluginRegistry;
import com.systemtwin.repository.SystemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service managing digital twin system models, component lifecycle, and topology.
 */
public class SystemService {

    private static final Logger log = LoggerFactory.getLogger(SystemService.class);

    private final SystemRepository systemRepository;
    private final PluginRegistry pluginRegistry;
    private SystemModel activeSystem;

    public SystemService(SystemRepository systemRepository, PluginRegistry pluginRegistry) {
        this.systemRepository = systemRepository;
        this.pluginRegistry = pluginRegistry != null ? pluginRegistry : new PluginRegistry();
    }

    public SystemModel createSystem(String id, String name, String description) {
        SystemModel system = new SystemModel(id, name, description);
        this.activeSystem = system;
        if (systemRepository != null) {
            try {
                systemRepository.saveSystem(system);
            } catch (Exception ex) {
                log.warn("Failed persisting system: {}", ex.getMessage());
            }
        }
        return system;
    }

    public ComponentModel addComponentFromPlugin(SystemModel system, String id, String name, String type) {
        if (system == null) return null;
        ComponentPlugin plugin = pluginRegistry.getPlugin(type);
        ComponentModel comp;
        if (plugin != null) {
            comp = plugin.createComponent(id, name);
        } else {
            comp = new ComponentModel(id, name, type);
        }
        system.addComponent(comp);

        if (systemRepository != null) {
            try {
                systemRepository.saveComponent(system.getId(), comp);
            } catch (Exception ex) {
                log.warn("Failed persisting component: {}", ex.getMessage());
            }
        }
        return comp;
    }

    public void addDependency(SystemModel system, String dependentId, String dependsOnId) {
        if (system == null) return;
        system.addDependency(dependentId, dependsOnId);
        if (systemRepository != null) {
            try {
                systemRepository.saveDependency(system.getId(), dependentId, dependsOnId);
            } catch (Exception ex) {
                log.warn("Failed persisting dependency: {}", ex.getMessage());
            }
        }
    }

    public SystemModel getActiveSystem() {
        return activeSystem;
    }

    public void setActiveSystem(SystemModel activeSystem) {
        this.activeSystem = activeSystem;
    }

    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }
}
