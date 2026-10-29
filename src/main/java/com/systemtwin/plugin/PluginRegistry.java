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

        // 5. 6-Axis Industrial Robotic Arm
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-robotic-arm"; }
            @Override public String getSupportedType() { return "ROBOT"; }
            @Override public String getDescription() { return "6-Axis high-precision articulated robotic assembly arm"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "ROBOT");
                comp.addSensor(new Sensor(id + "-temp", id, "TEMPERATURE", "°C", 30.0, 65.0));
                comp.addSensor(new Sensor(id + "-torque", id, "TORQUE", "Nm", 20.0, 85.0));
                comp.addSensor(new Sensor(id + "-current", id, "MOTOR_CURRENT", "A", 2.0, 15.0));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });

        // 6. Edge / Cloud API Gateway
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-api-gateway"; }
            @Override public String getSupportedType() { return "GATEWAY"; }
            @Override public String getDescription() { return "High-throughput API load balancer and edge ingress proxy"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "GATEWAY");
                comp.addSensor(new Sensor(id + "-req", id, "REQUEST_RATE", "req/s", 500.0, 5000.0));
                comp.addSensor(new Sensor(id + "-cpu", id, "CPU", "%", 10.0, 60.0));
                comp.addSensor(new Sensor(id + "-latency", id, "LATENCY", "ms", 2.0, 25.0));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });

        // 7. HEPA Cleanroom HVAC
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-cleanroom-hvac"; }
            @Override public String getSupportedType() { return "HVAC"; }
            @Override public String getDescription() { return "Biosafety negative-pressure HVAC system with HEPA filtration"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "HVAC");
                comp.addSensor(new Sensor(id + "-flow", id, "AIR_FLOW", "CFM", 800.0, 1500.0));
                comp.addSensor(new Sensor(id + "-press", id, "PRESSURE_DELTA", "Pa", 25.0, 50.0));
                comp.addSensor(new Sensor(id + "-temp", id, "TEMPERATURE", "°C", 18.0, 24.0));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });

        // 8. Intake Conveyor Belt
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-conveyor-belt"; }
            @Override public String getSupportedType() { return "CONVEYOR"; }
            @Override public String getDescription() { return "Variable speed motorized conveyor belt system"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "CONVEYOR");
                comp.addSensor(new Sensor(id + "-speed", id, "SPEED", "m/s", 1.0, 2.5));
                comp.addSensor(new Sensor(id + "-load", id, "LOAD", "kg", 10.0, 120.0));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });

        // 9. Laboratory Cell Culture Incubator
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-incubator"; }
            @Override public String getSupportedType() { return "INCUBATOR"; }
            @Override public String getDescription() { return "Precision CO2 cell culture incubator"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "INCUBATOR");
                comp.addSensor(new Sensor(id + "-temp", id, "TEMPERATURE", "°C", 36.8, 37.2));
                comp.addSensor(new Sensor(id + "-co2", id, "CO2_LEVEL", "%", 4.9, 5.1));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });

        // 10. High-Speed Centrifuge
        register(new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-centrifuge"; }
            @Override public String getSupportedType() { return "CENTRIFUGE"; }
            @Override public String getDescription() { return "Automated refrigerated laboratory centrifuge"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                ComponentModel comp = new ComponentModel(id, name, "CENTRIFUGE");
                comp.addSensor(new Sensor(id + "-rpm", id, "RPM", "rpm", 5000.0, 15000.0));
                comp.addSensor(new Sensor(id + "-vib", id, "VIBRATION", "mm/s", 0.2, 2.0));
                return comp;
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        });
    }
}
