package com.systemtwin.service;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.Sensor;
import com.systemtwin.model.SystemModel;

/**
 * Provides industrial, laboratory, and cloud data center digital twin templates.
 */
public class TemplateService {

    private final SystemService systemService;

    public TemplateService(SystemService systemService) {
        this.systemService = systemService;
    }

    public SystemModel createIndustrialManufacturingTemplate() {
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
