package com.systemtwin.service;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.Sensor;
import com.systemtwin.model.SystemModel;

import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides industrial, laboratory, and cloud data center digital twin templates.
 * Loads topology definitions directly from example JSON files when available.
 */
public class TemplateService {

    private final SystemService systemService;

    public TemplateService(SystemService systemService) {
        this.systemService = systemService;
    }

    public SystemModel loadFromJsonFile(String relativePath) {
        try {
            File f = new File(relativePath);
            if (!f.exists()) return null;
            String json = Files.readString(f.toPath());
            return parseSystemJson(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String extractField(String text, String field) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]+)\"").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static String extractFirstString(String text) {
        Matcher m = Pattern.compile("\"([^\"]+)\"").matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static double extractDouble(String text, String field, double defVal) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*([0-9.]+)").matcher(text);
        return m.find() ? Double.parseDouble(m.group(1)) : defVal;
    }

    public static SystemModel parseSystemJson(String json) {
        String sysId = extractField(json, "systemId");
        String name = extractField(json, "name");
        String desc = extractField(json, "description");
        SystemModel sys = new SystemModel(sysId != null ? sysId : "sys-" + System.currentTimeMillis(),
                name != null ? name : "Loaded System", desc != null ? desc : "");

        int compIdx = json.indexOf("\"components\":");
        int depIdx = json.indexOf("\"dependencies\":");
        if (compIdx != -1) {
            String compPart = depIdx != -1 ? json.substring(compIdx, depIdx) : json.substring(compIdx);
            Pattern compPattern = Pattern.compile(
                    "\\{\\s*\"id\":\\s*\"([^\"]+)\"\\s*,\\s*\"name\":\\s*\"([^\"]+)\"\\s*,\\s*\"type\":\\s*\"([^\"]+)\"\\s*,\\s*\"sensors\":\\s*\\[(.*?)\\]",
                    Pattern.DOTALL
            );
            Matcher compMatcher = compPattern.matcher(compPart);
            while (compMatcher.find()) {
                String id = compMatcher.group(1);
                String cName = compMatcher.group(2);
                String type = compMatcher.group(3);
                String sensorsPart = compMatcher.group(4);

                ComponentModel comp = new ComponentModel(id, cName, type);

                Pattern sensorPattern = Pattern.compile(
                        "\\{\\s*\"id\":\\s*\"([^\"]+)\"\\s*,\\s*\"metricName\":\\s*\"([^\"]+)\"\\s*,\\s*\"unit\":\\s*\"([^\"]+)\"\\s*,\\s*\"minNormal\":\\s*([0-9.]+)\\s*,\\s*\"maxNormal\":\\s*([0-9.]+)\\s*\\}"
                );
                Matcher sMatcher = sensorPattern.matcher(sensorsPart);
                while (sMatcher.find()) {
                    String sId = sMatcher.group(1);
                    String metric = sMatcher.group(2);
                    String unit = sMatcher.group(3);
                    double minN = Double.parseDouble(sMatcher.group(4));
                    double maxN = Double.parseDouble(sMatcher.group(5));
                    comp.addSensor(new Sensor(sId, id, metric, unit, minN, maxN));
                }
                sys.addComponent(comp);
            }
        }

        if (depIdx != -1) {
            String depPart = json.substring(depIdx);
            Matcher m = Pattern.compile("\\{\\s*\"source\":\\s*\"([^\"]+)\"\\s*,\\s*\"target\":\\s*\"([^\"]+)\"\\s*\\}").matcher(depPart);
            while (m.find()) {
                sys.addDependency(m.group(1), m.group(2));
            }
        }
        return sys;
    }

    public SystemModel createIndustrialManufacturingTemplate() {
        SystemModel loaded = loadFromJsonFile("examples/industrial-system/system.json");
        if (loaded != null) {
            if (systemService != null) systemService.setActiveSystem(loaded);
            return loaded;
        }

        SystemModel sys = new SystemModel(
                "sys-industrial",
                "Automated Assembly Line",
                "Industrial manufacturing twin with PLCs, conveyors, robotic assembly, and hydraulic pumps"
        );

        // Components
        ComponentModel plc = new ComponentModel("plc-main", "Master PLC Controller", "PLC");
        plc.addSensor(new Sensor("s-plc-cycle", "plc-main", "CYCLE_TIME", "ms", 5.0, 15.0));
        plc.addSensor(new Sensor("s-plc-volt", "plc-main", "VOLTAGE", "V", 23.8, 24.2));

        ComponentModel pump = new ComponentModel("pump-01", "Hydraulic Primary Pump", "PUMP");
        pump.addSensor(new Sensor("s-pump-press", "pump-01", "PRESSURE", "bar", 4.0, 8.0));
        pump.addSensor(new Sensor("s-pump-flow", "pump-01", "FLOW_RATE", "L/min", 40.0, 100.0));
        pump.addSensor(new Sensor("s-pump-vib", "pump-01", "VIBRATION", "mm/s", 0.5, 3.5));

        ComponentModel robot = new ComponentModel("robot-arm", "6-Axis Assembly Robot", "ROBOT");
        robot.addSensor(new Sensor("s-rob-temp", "robot-arm", "TEMPERATURE", "°C", 30.0, 65.0));
        robot.addSensor(new Sensor("s-rob-torque", "robot-arm", "TORQUE", "Nm", 20.0, 85.0));

        ComponentModel conveyor = new ComponentModel("conveyor-01", "Intake Conveyor Belt", "CONVEYOR");
        conveyor.addSensor(new Sensor("s-conv-speed", "conveyor-01", "SPEED", "m/s", 1.0, 2.5));

        ComponentModel qualitySensor = new ComponentModel("vision-qc", "Optical QC Scanner", "INSPECTION");
        qualitySensor.addSensor(new Sensor("s-qc-defect", "vision-qc", "ACCURACY", "%", 98.0, 100.0));

        sys.addComponent(plc);
        sys.addComponent(pump);
        sys.addComponent(robot);
        sys.addComponent(conveyor);
        sys.addComponent(qualitySensor);

        // Dependencies: Robot depends on Pump (hydraulic power) & PLC (control commands)
        sys.addDependency("robot-arm", "pump-01");
        sys.addDependency("robot-arm", "plc-main");
        sys.addDependency("conveyor-01", "plc-main");
        sys.addDependency("vision-qc", "conveyor-01");

        if (systemService != null) {
            systemService.setActiveSystem(sys);
        }
        return sys;
    }

    public SystemModel createDataCenterTemplate() {
        SystemModel loaded = loadFromJsonFile("examples/data-center/system.json");
        if (loaded != null) {
            if (systemService != null) systemService.setActiveSystem(loaded);
            return loaded;
        }

        SystemModel sys = new SystemModel(
                "sys-datacenter",
                "Cloud Microservices Cluster",
                "Enterprise infrastructure with Load Balancer, Web nodes, Microservices, Redis, and Database"
        );

        ComponentModel lb = new ComponentModel("edge-lb", "Edge Load Balancer", "GATEWAY");
        lb.addSensor(new Sensor("s-lb-req", "edge-lb", "REQUEST_RATE", "req/s", 500.0, 5000.0));
        lb.addSensor(new Sensor("s-lb-cpu", "edge-lb", "CPU", "%", 10.0, 60.0));

        ComponentModel web1 = new ComponentModel("web-srv-1", "Web Frontend Node 1", "SERVER");
        web1.addSensor(new Sensor("s-w1-cpu", "web-srv-1", "CPU", "%", 15.0, 75.0));
        web1.addSensor(new Sensor("s-w1-temp", "web-srv-1", "TEMPERATURE", "°C", 35.0, 68.0));

        ComponentModel web2 = new ComponentModel("web-srv-2", "Web Frontend Node 2", "SERVER");
        web2.addSensor(new Sensor("s-w2-cpu", "web-srv-2", "CPU", "%", 15.0, 75.0));

        ComponentModel apiService = new ComponentModel("api-gateway", "Core API Service", "SERVER");
        apiService.addSensor(new Sensor("s-api-latency", "api-gateway", "LATENCY", "ms", 5.0, 45.0));

        ComponentModel redis = new ComponentModel("redis-cache", "Redis Distributed Cache", "DATABASE");
        redis.addSensor(new Sensor("s-red-hit", "redis-cache", "HIT_RATE", "%", 85.0, 99.5));

        ComponentModel database = new ComponentModel("pg-db-cluster", "PostgreSQL Primary", "DATABASE");
        database.addSensor(new Sensor("s-db-iops", "pg-db-cluster", "IOPS", "iops", 200.0, 4000.0));
        database.addSensor(new Sensor("s-db-lat", "pg-db-cluster", "QUERY_LATENCY", "ms", 2.0, 35.0));

        sys.addComponent(lb);
        sys.addComponent(web1);
        sys.addComponent(web2);
        sys.addComponent(apiService);
        sys.addComponent(redis);
        sys.addComponent(database);

        sys.addDependency("edge-lb", "web-srv-1");
        sys.addDependency("edge-lb", "web-srv-2");
        sys.addDependency("web-srv-1", "api-gateway");
        sys.addDependency("web-srv-2", "api-gateway");
        sys.addDependency("api-gateway", "redis-cache");
        sys.addDependency("api-gateway", "pg-db-cluster");

        if (systemService != null) {
            systemService.setActiveSystem(sys);
        }
        return sys;
    }

    public SystemModel createLaboratorySystemTemplate() {
        SystemModel loaded = loadFromJsonFile("examples/laboratory-system/system.json");
        if (loaded != null) {
            if (systemService != null) systemService.setActiveSystem(loaded);
            return loaded;
        }

        SystemModel sys = new SystemModel(
                "sys-laboratory",
                "Automated Biosafety Laboratory",
                "Environmental and bioprocess monitoring twin for clinical incubators, centrifuges, and HVAC"
        );

        ComponentModel hvac = new ComponentModel("hvac-main", "HEPA Cleanroom HVAC", "HVAC");
        hvac.addSensor(new Sensor("s-hvac-flow", "hvac-main", "AIR_FLOW", "CFM", 800.0, 1500.0));
        hvac.addSensor(new Sensor("s-hvac-press", "hvac-main", "PRESSURE_DELTA", "Pa", 25.0, 50.0));

        ComponentModel incubator = new ComponentModel("incubator-01", "Cell Culture Incubator", "INCUBATOR");
        incubator.addSensor(new Sensor("s-inc-temp", "incubator-01", "TEMPERATURE", "°C", 36.8, 37.2));
        incubator.addSensor(new Sensor("s-inc-co2", "incubator-01", "CO2_LEVEL", "%", 4.9, 5.1));

        ComponentModel centrifuge = new ComponentModel("centrifuge-01", "High-Speed Centrifuge", "CENTRIFUGE");
        centrifuge.addSensor(new Sensor("s-cent-rpm", "centrifuge-01", "RPM", "rpm", 5000.0, 15000.0));
        centrifuge.addSensor(new Sensor("s-cent-vib", "centrifuge-01", "VIBRATION", "mm/s", 0.2, 2.0));

        sys.addComponent(hvac);
        sys.addComponent(incubator);
        sys.addComponent(centrifuge);

        sys.addDependency("incubator-01", "hvac-main");

        if (systemService != null) {
            systemService.setActiveSystem(sys);
        }
        return sys;
    }
}
