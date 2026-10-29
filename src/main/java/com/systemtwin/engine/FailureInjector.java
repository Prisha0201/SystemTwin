package com.systemtwin.engine;

import com.systemtwin.events.EventBus;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;
import com.systemtwin.model.Sensor;
import com.systemtwin.model.SimulationEvent;
import com.systemtwin.model.SystemModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles simulated fault injection (hard failures, performance degradation,
 * sensor anomalies) and triggers cascading graph propagation.
 */
public class FailureInjector {

    private static final Logger log = LoggerFactory.getLogger(FailureInjector.class);

    private final EventBus eventBus;
    private final FailurePropagator failurePropagator;

    public FailureInjector(EventBus eventBus, FailurePropagator failurePropagator) {
        this.eventBus = eventBus;
        this.failurePropagator = failurePropagator != null ? failurePropagator : new FailurePropagator();
    }

    /**
     * Injects a primary failure into componentId and optionally propagates cascades.
     */
    public List<FailurePropagator.CascadeStep> injectFailure(SystemModel system, String componentId, String runId, boolean cascade) {
        if (system == null || componentId == null) return List.of();

        ComponentModel comp = system.getComponent(componentId);
        if (comp == null) return List.of();

        log.info("Injecting failure into component {} (cascade={})", componentId, cascade);

        List<FailurePropagator.CascadeStep> steps;
        if (cascade) {
            steps = failurePropagator.propagateFailure(
                    system.getDependencyGraph(),
                    componentId,
                    false // allows progressive degradation
            );
        } else {
            comp.setState(ComponentState.FAILED);
            comp.setCpuUtilizationPercent(0.0);
            steps = List.of(new FailurePropagator.CascadeStep(
                    0,
                    componentId,
                    comp.getName(),
                    ComponentState.HEALTHY,
                    ComponentState.FAILED,
                    "INJECTED_FAULT",
                    "Direct isolated fault injection (cascading disabled)"
            ));
        }

        if (eventBus != null) {
            eventBus.publish(new SimulationEvent(
                    runId,
                    0,
                    "FAILURE_INJECTED",
                    componentId,
                    null,
                    "Injected hard fault on " + comp.getName(),
                    "Affected nodes: " + steps.size()
            ));

            for (FailurePropagator.CascadeStep step : steps) {
                if (step.getStepNumber() > 0) {
                    eventBus.publish(new SimulationEvent(
                            runId,
                            0,
                            "CASCADE_PROPAGATED",
                            step.getCauseComponentId(),
                            step.getComponentId(),
                            "Cascade step " + step.getStepNumber() + ": " + step.getReason(),
                            step.getNewState().name()
                    ));
                }
            }
        }

        return steps;
    }

    /**
     * Injects localized performance degradation without instant hard failure.
     */
    public void injectDegradation(SystemModel system, String componentId, String runId) {
        if (system == null || componentId == null) return;
        ComponentModel comp = system.getComponent(componentId);
        if (comp == null) return;

        comp.setState(ComponentState.DEGRADED);
        comp.setCpuUtilizationPercent(92.0);

        if (eventBus != null) {
            eventBus.publish(new SimulationEvent(
                    runId,
                    0,
                    "DEGRADATION_INJECTED",
                    componentId,
                    null,
                    "Injected degradation on " + comp.getName() + " (high load / thermal throttling)",
                    "CPU: 92%"
            ));
        }
    }

    /**
     * Restores a failed or degraded component to HEALTHY state.
     */
    public List<FailurePropagator.CascadeStep> recoverComponent(SystemModel system, String componentId, String runId) {
        if (system == null || componentId == null) return List.of();

        ComponentModel comp = system.getComponent(componentId);
        if (comp == null) return List.of();

        List<FailurePropagator.CascadeStep> steps = failurePropagator.propagateRecovery(
                system.getDependencyGraph(),
                componentId
        );

        if (eventBus != null) {
            eventBus.publish(new SimulationEvent(
                    runId,
                    0,
                    "COMPONENT_RECOVERED",
                    componentId,
                    null,
                    "Restored " + comp.getName() + " to healthy operational state",
                    "Recovered nodes count: " + steps.size()
            ));
        }

        return steps;
    }

    /**
     * Injects an artificial spike onto a specific sensor.
     */
    public void injectSensorSpike(SystemModel system, String sensorId, double multiplier) {
        if (system == null || sensorId == null) return;
        for (Sensor s : system.getAllSensors()) {
            if (s.getId().equals(sensorId)) {
                s.setCurrentValue(s.getCurrentValue() * multiplier);
                break;
            }
        }
    }
}
