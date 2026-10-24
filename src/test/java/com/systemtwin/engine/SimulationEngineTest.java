package com.systemtwin.engine;

import com.systemtwin.events.EventBus;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SimulationEngineTest {

    private EventBus eventBus;
    private SensorEngine sensorEngine;
    private FailurePropagator failurePropagator;
    private FailureInjector failureInjector;
    private SimulationScheduler scheduler;
    private SimulationEngine engine;
    private SystemModel system;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        sensorEngine = new SensorEngine(12345L);
        failurePropagator = new FailurePropagator();
        failureInjector = new FailureInjector(eventBus, failurePropagator);
        scheduler = new SimulationScheduler();
        engine = new SimulationEngine(eventBus, sensorEngine, failureInjector, failurePropagator, scheduler, null, null);

        system = new SystemModel("sys-test", "Test Cluster", "Unit test cluster");
        ComponentModel srv1 = new ComponentModel("srv-1", "Server 1", "SERVER");
        srv1.addSensor(new Sensor("s1-cpu", "srv-1", "CPU", "%", 0.0, 100.0));
        ComponentModel srv2 = new ComponentModel("srv-2", "Server 2", "SERVER");
        srv2.addSensor(new Sensor("s2-cpu", "srv-2", "CPU", "%", 0.0, 100.0));

        system.addComponent(srv1);
        system.addComponent(srv2);
        system.addDependency("srv-1", "srv-2");
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            engine.stopSimulation();
        }
    }

    @Test
    @DisplayName("Verify simulation start and tick execution")
    void testSimulationLifecycle() {
        SimulationRun run = engine.startSimulation(system, "Unit-Test-Run", 12345L);
        assertNotNull(run);
        assertEquals("RUNNING", run.getStatus());
        assertTrue(engine.getScheduler().isRunning());

        // Perform manual ticks
        engine.executeTick();
        assertEquals(1, run.getTickCount());
        assertFalse(engine.getLatestReadings().isEmpty());

        engine.pauseSimulation();
        assertEquals("PAUSED", run.getStatus());
        assertTrue(engine.getScheduler().isPaused());

        engine.resumeSimulation();
        assertEquals("RUNNING", run.getStatus());
        assertFalse(engine.getScheduler().isPaused());

        engine.stopSimulation();
        assertEquals("COMPLETED", run.getStatus());
        assertFalse(engine.getScheduler().isRunning());
    }

    @Test
    @DisplayName("Verify deterministic sensor output with same random seed")
    void testDeterministicSeeding() {
        SensorEngine se1 = new SensorEngine(999L);
        SensorEngine se2 = new SensorEngine(999L);

        ComponentModel comp = new ComponentModel("c1", "TestComp", "SERVER");
        Sensor sensor = new Sensor("s1", "c1", "CPU", "%", 10.0, 80.0);

        SensorReading r1 = se1.generateReading("run-1", comp, sensor);
        SensorReading r2 = se2.generateReading("run-1", comp, sensor);

        assertEquals(r1.getValue(), r2.getValue(), 0.0001, "Same seed must produce identical sensor readings");
    }

    @Test
    @DisplayName("Verify event bus publishes telemetry asynchronously")
    void testEventBusDispatch() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger count = new AtomicInteger(0);

        eventBus.subscribe(SensorReading.class, reading -> {
            count.incrementAndGet();
            latch.countDown();
        });

        engine.startSimulation(system, "Async-Event-Run", 42L);
        engine.executeTick();

        boolean received = latch.await(2, TimeUnit.SECONDS);
        assertTrue(received, "Subscriber should receive sensor readings on tick");
        assertEquals(2, count.get());
    }

    @Test
    @DisplayName("Verify failure injection triggers state change and cascade")
    void testFailureInjectionInEngine() {
        engine.startSimulation(system, "Fault-Injection-Run", 42L);
        engine.injectFailure("srv-2", true);

        assertEquals(ComponentState.FAILED, system.getComponent("srv-2").getState());
        assertTrue(system.getComponent("srv-1").getState() == ComponentState.DEGRADED ||
                   system.getComponent("srv-1").getState() == ComponentState.FAILED);
    }
}
