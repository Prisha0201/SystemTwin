package com.systemtwin.service;

import com.systemtwin.engine.SimulationEngine;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.model.SensorReading;
import com.systemtwin.model.SimulationRun;
import com.systemtwin.model.SystemModel;
import com.systemtwin.repository.EventRepository;
import com.systemtwin.repository.SimulationRepository;

import java.util.List;

/**
 * Service facade providing simulation controls, fault injection, and telemetry access.
 */
public class SimulationService {

    private final SimulationEngine simulationEngine;
    private final SimulationRepository simulationRepository;
    private final EventRepository eventRepository;

    public SimulationService(
            SimulationEngine simulationEngine,
            SimulationRepository simulationRepository,
            EventRepository eventRepository) {
        this.simulationEngine = simulationEngine;
        this.simulationRepository = simulationRepository;
        this.eventRepository = eventRepository;
    }

    public SimulationRun start(SystemModel system, String name, long randomSeed) {
        return simulationEngine.startSimulation(system, name, randomSeed);
    }

    public void pause() {
        simulationEngine.pauseSimulation();
    }

    public void resume() {
        simulationEngine.resumeSimulation();
    }

    public void step() {
        simulationEngine.stepSimulation();
    }

    public void stop() {
        simulationEngine.stopSimulation();
    }

    public void setSpeed(double multiplier) {
        simulationEngine.getScheduler().setSpeed(multiplier);
    }

    public List<FailurePropagator.CascadeStep> injectFailure(String componentId, boolean cascade) {
        return simulationEngine.injectFailure(componentId, cascade);
    }

    public void injectDegradation(String componentId) {
        simulationEngine.injectDegradation(componentId);
    }

    public List<FailurePropagator.CascadeStep> recoverComponent(String componentId) {
        return simulationEngine.recoverComponent(componentId);
    }

    public SimulationRun getCurrentRun() {
        return simulationEngine.getCurrentRun();
    }

    public List<SensorReading> getLatestReadings() {
        return simulationEngine.getLatestReadings();
    }

    public boolean isRunning() {
        return simulationEngine.getScheduler().isRunning();
    }

    public boolean isPaused() {
        return simulationEngine.getScheduler().isPaused();
    }

    public SimulationEngine getEngine() {
        return simulationEngine;
    }
}
