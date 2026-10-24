package com.systemtwin.engine;

import com.systemtwin.events.EventBus;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.model.*;
import com.systemtwin.repository.ReadingRepository;
import com.systemtwin.repository.SimulationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core simulation orchestrator managing digital twin lifecycle, telemetry generation,
 * fault injection, and state synchronization.
 */
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    private final EventBus eventBus;
    private final SensorEngine sensorEngine;
    private final FailureInjector failureInjector;
    private final FailurePropagator failurePropagator;
    private final SimulationScheduler scheduler;
    private final ReadingRepository readingRepository;
    private final SimulationRepository simulationRepository;

    private SystemModel currentSystem;
    private SimulationRun currentRun;
    private final List<SensorReading> latestReadings = new CopyOnWriteArrayList<>();

    public SimulationEngine(
            EventBus eventBus,
            SensorEngine sensorEngine,
            FailureInjector failureInjector,
            FailurePropagator failurePropagator,
            SimulationScheduler scheduler,
            ReadingRepository readingRepository,
            SimulationRepository simulationRepository) {
        this.eventBus = eventBus;
        this.sensorEngine = sensorEngine != null ? sensorEngine : new SensorEngine();
        this.failureInjector = failureInjector != null ? failureInjector : new FailureInjector(eventBus, failurePropagator);
        this.failurePropagator = failurePropagator != null ? failurePropagator : new FailurePropagator();
        this.scheduler = scheduler != null ? scheduler : new SimulationScheduler();
        this.readingRepository = readingRepository;
        this.simulationRepository = simulationRepository;
    }

    public synchronized SimulationRun startSimulation(SystemModel system, String name, long randomSeed) {
        if (scheduler.isRunning()) {
            stopSimulation();
        }

        this.currentSystem = system;
        this.sensorEngine.setSeed(randomSeed);
        this.currentRun = new SimulationRun(UUID.randomUUID().toString(), system != null ? system.getId() : "default", name, randomSeed);
        this.currentRun.setStatus("RUNNING");
        this.latestReadings.clear();

        if (simulationRepository != null) {
            try {
                simulationRepository.saveRun(currentRun);
            } catch (Exception ex) {
                log.warn("Failed saving simulation run initial state: {}", ex.getMessage());
            }
        }

        if (eventBus != null) {
            eventBus.publish(new SimulationEvent(
                    currentRun.getId(),
                    0,
                    "SIMULATION_STARTED",
                    null,
                    null,
                    "Started simulation run '" + currentRun.getName() + "' (seed=" + randomSeed + ")",
                    system != null ? "Components: " + system.getComponents().size() : ""
            ));
        }

        scheduler.start(this::executeTick, 1000L);
        log.info("Simulation {} started for system {}", currentRun.getId(), system != null ? system.getName() : "none");
        return currentRun;
    }

    public void executeTick() {
        if (currentSystem == null || currentRun == null) return;

        currentRun.incrementTicks();
        String runId = currentRun.getId();
        List<SensorReading> tickReadings = new ArrayList<>();

        for (ComponentModel comp : currentSystem.getComponents().values()) {
            sensorEngine.updateComponentMetrics(comp);

            for (Sensor sensor : comp.getSensors()) {
                SensorReading reading = sensorEngine.generateReading(runId, comp, sensor);
                if (reading != null) {
                    tickReadings.add(reading);
                    if (eventBus != null) {
                        eventBus.publish(reading);
                    }
                }
            }
        }

        latestReadings.clear();
        latestReadings.addAll(tickReadings);

        if (readingRepository != null && !tickReadings.isEmpty()) {
            try {
                readingRepository.saveReadingsBatch(tickReadings);
            } catch (Exception ex) {
                log.warn("Failed saving telemetry batch: {}", ex.getMessage());
            }
        }

        if (eventBus != null && currentRun.getTickCount() % 5 == 0) {
            eventBus.publish(new SimulationEvent(
                    runId,
                    0,
                    "SIMULATION_TICK",
                    null,
                    null,
                    "Tick #" + currentRun.getTickCount() + " evaluated (" + tickReadings.size() + " readings)",
                    null
            ));
        }

        if (simulationRepository != null && currentRun.getTickCount() % 10 == 0) {
            simulationRepository.updateRun(currentRun);
        }
    }

    public synchronized void pauseSimulation() {
        if (scheduler.isRunning()) {
            scheduler.pause();
            if (currentRun != null) {
                currentRun.setStatus("PAUSED");
                if (eventBus != null) {
                    eventBus.publish(new SimulationEvent(
                            currentRun.getId(), 0, "SIMULATION_PAUSED", null, null, "Simulation execution paused", null
                    ));
                }
            }
        }
    }

    public synchronized void resumeSimulation() {
        if (scheduler.isRunning()) {
            scheduler.resume();
            if (currentRun != null) {
                currentRun.setStatus("RUNNING");
                if (eventBus != null) {
                    eventBus.publish(new SimulationEvent(
                            currentRun.getId(), 0, "SIMULATION_RESUMED", null, null, "Simulation execution resumed", null
                    ));
                }
            }
        }
    }

    public synchronized void stepSimulation() {
        if (currentRun == null && currentSystem != null) {
            startSimulation(currentSystem, "Manual-Step-Run", 42L);
            pauseSimulation();
        }
        scheduler.step();
    }

    public synchronized void stopSimulation() {
        scheduler.stop();
        if (currentRun != null) {
            currentRun.setStatus("COMPLETED");
            currentRun.setEndTime(Instant.now());
            if (simulationRepository != null) {
                simulationRepository.updateRun(currentRun);
            }
            if (eventBus != null) {
                eventBus.publish(new SimulationEvent(
                        currentRun.getId(), 0, "SIMULATION_STOPPED", null, null,
                        "Simulation terminated at tick " + currentRun.getTickCount(), null
                ));
            }
            log.info("Simulation {} stopped", currentRun.getId());
        }
    }

    public List<FailurePropagator.CascadeStep> injectFailure(String componentId, boolean cascade) {
        if (currentSystem == null || currentRun == null) return List.of();
        return failureInjector.injectFailure(currentSystem, componentId, currentRun.getId(), cascade);
    }

    public void injectDegradation(String componentId) {
        if (currentSystem == null || currentRun == null) return;
        failureInjector.injectDegradation(currentSystem, componentId, currentRun.getId());
    }

    public List<FailurePropagator.CascadeStep> recoverComponent(String componentId) {
        if (currentSystem == null || currentRun == null) return List.of();
        return failureInjector.recoverComponent(currentSystem, componentId, currentRun.getId());
    }

    public SystemModel getCurrentSystem() {
        return currentSystem;
    }

    public void setCurrentSystem(SystemModel currentSystem) {
        this.currentSystem = currentSystem;
    }

    public SimulationRun getCurrentRun() {
        return currentRun;
    }

    public List<SensorReading> getLatestReadings() {
        return Collections.unmodifiableList(latestReadings);
    }

    public SimulationScheduler getScheduler() {
        return scheduler;
    }

    public FailurePropagator getFailurePropagator() {
        return failurePropagator;
    }

    public FailureInjector getFailureInjector() {
        return failureInjector;
    }
}
