package com.systemtwin.model;

import com.systemtwin.graph.DependencyGraph;

import java.util.*;

public class SystemModel {

    private final String id;
    private String name;
    private String description;
    private final Map<String, ComponentModel> components = new LinkedHashMap<>();
    private final DependencyGraph dependencyGraph = new DependencyGraph();

    public SystemModel(String id, String name, String description) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description != null ? description : "";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, ComponentModel> getComponents() {
        return Collections.unmodifiableMap(components);
    }

    public ComponentModel getComponent(String id) {
        return components.get(id);
    }

    public void addComponent(ComponentModel component) {
        if (component == null) return;
        components.put(component.getId(), component);
        dependencyGraph.addComponent(component);
    }

    public void removeComponent(String componentId) {
        components.remove(componentId);
        dependencyGraph.removeComponent(componentId);
    }

    public void addDependency(String dependentId, String dependsOnId) {
        dependencyGraph.addDependency(dependentId, dependsOnId);
    }

    public void removeDependency(String dependentId, String dependsOnId) {
        dependencyGraph.removeDependency(dependentId, dependsOnId);
    }

    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }

    public List<Sensor> getAllSensors() {
        List<Sensor> all = new ArrayList<>();
        for (ComponentModel comp : components.values()) {
            all.addAll(comp.getSensors());
        }
        return all;
    }

    public int getHealthyCount() {
        return (int) components.values().stream().filter(c -> c.getState() == ComponentState.HEALTHY).count();
    }

    public int getDegradedCount() {
        return (int) components.values().stream().filter(c -> c.getState() == ComponentState.DEGRADED).count();
    }

    public int getFailedCount() {
        return (int) components.values().stream().filter(c -> c.getState() == ComponentState.FAILED || c.getState() == ComponentState.OFFLINE).count();
    }

    @Override
    public String toString() {
        return "SystemModel{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", components=" + components.size() +
                '}';
    }
}
