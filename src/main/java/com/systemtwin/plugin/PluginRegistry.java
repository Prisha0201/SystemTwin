package com.systemtwin.plugin;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;
import com.systemtwin.model.Sensor;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and discovery manager for component plugins.
 * Supplies default industrial and IT plugins and permits runtime registration.
 */
public class PluginRegistry {

    private final Map<String, ComponentPlugin> plugins = new ConcurrentHashMap<>();

    public PluginRegistry() {
        loadBuiltinPlugins();
    }

    public void register(ComponentPlugin plugin) {
        if (plugin != null) {
            plugins.put(plugin.getSupportedType().toUpperCase(), plugin);
        }
    }

    public ComponentPlugin getPlugin(String type) {
        if (type == null) return null;
        return plugins.get(type.toUpperCase());
    }

    public Collection<ComponentPlugin> getAllPlugins() {
        return Collections.unmodifiableCollection(plugins.values());
    }

    public Set<String> getSupportedTypes() {
        return Collections.unmodifiableSet(plugins.keySet());
    }

    public void loadBuiltinPlugins() {
        // 1. Server / Compute Node
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-compute-server"; }
            @Override public String getSupportedType() { return "SERVER"; }
            @Override public String getDescription() { return "High-density enterprise server host"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "SERVER");
                comp.addSensor(new Sensor(id + "-cpu", id, "CPU", "%", 5.0, 75.0));
                comp.addSensor(new Sensor(id + "-temp", id, "TEMPERATURE", "°C", 35.0, 70.0));
                comp.addSensor(new Sensor(id + "-net", id, "NETWORK_IO", "MB/s", 10.0, 500.0));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });

        // 2. Database Node
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-relational-db"; }
            @Override public String getSupportedType() { return "DATABASE"; }
            @Override public String getDescription() { return "Clustered SQL/NoSQL transactional database"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "DATABASE");
                comp.addSensor(new Sensor(id + "-iops", id, "IOPS", "iops", 100.0, 5000.0));
                comp.addSensor(new Sensor(id + "-latency", id, "QUERY_LATENCY", "ms", 1.0, 45.0));
                comp.addSensor(new Sensor(id + "-conn", id, "CONNECTIONS", "conn", 10.0, 800.0));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });

        // 3. Industrial Pump
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-industrial-pump"; }
            @Override public String getSupportedType() { return "PUMP"; }
            @Override public String getDescription() { return "Hydraulic variable-speed centrifugal pump"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "PUMP");
                comp.addSensor(new Sensor(id + "-pressure", id, "PRESSURE", "bar", 2.0, 8.5));
                comp.addSensor(new Sensor(id + "-flow", id, "FLOW_RATE", "L/min", 20.0, 150.0));
                comp.addSensor(new Sensor(id + "-vibration", id, "VIBRATION", "mm/s", 0.5, 4.2));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });

        // 4. Programmable Logic Controller (PLC)
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-industrial-plc"; }
            @Override public String getSupportedType() { return "PLC"; }
            @Override public String getDescription() { return "Real-time industrial automation PLC controller"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "PLC");
                comp.addSensor(new Sensor(id + "-cycle", id, "CYCLE_TIME", "ms", 2.0, 20.0));
                comp.addSensor(new Sensor(id + "-voltage", id, "VOLTAGE", "V", 23.5, 24.5));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });
    }
}
