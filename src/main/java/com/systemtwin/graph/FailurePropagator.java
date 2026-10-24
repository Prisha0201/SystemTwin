package com.systemtwin.graph;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;

import java.util.*;

/**
 * Executes graph-based cascading failure propagation.
 * Traverses downstream dependencies step-by-step and degrades or fails
 * dependent nodes according to propagation rules and cascade probabilities.
 */
public class FailurePropagator {

    public static class CascadeStep {
        private final int stepNumber;
        private final String componentId;
        private final String componentName;
        private final ComponentState previousState;
        private final ComponentState newState;
        private final String causeComponentId;
        private final String reason;

        public CascadeStep(int stepNumber, String componentId, String componentName, ComponentState previousState, ComponentState newState, String causeComponentId, String reason) {
            this.stepNumber = stepNumber;
            this.componentId = componentId;
            this.componentName = componentName;
            this.previousState = previousState;
            this.newState = newState;
            this.causeComponentId = causeComponentId;
            this.reason = reason;
        }

        public int getStepNumber() { return stepNumber; }
        public String getComponentId() { return componentId; }
        public String getComponentName() { return componentName; }
        public ComponentState getPreviousState() { return previousState; }
        public ComponentState getNewState() { return newState; }
        public String getCauseComponentId() { return causeComponentId; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("Step %d: %s (%s -> %s) due to %s [%s]",
                    stepNumber, componentName, previousState, newState, causeComponentId, reason);
        }
    }

    /**
     * Propagates failure originating at rootFailedComponentId.
     * Direct dependents are degraded or failed; second-order dependents are subsequently affected.
     */
    public List<CascadeStep> propagateFailure(DependencyGraph graph, String rootFailedComponentId, boolean immediateTotalFailure) {
        List<CascadeStep> cascadeSteps = new ArrayList<>();
        if (graph == null || rootFailedComponentId == null) return cascadeSteps;

        Map<String, ComponentModel> nodes = graph.getNodes();
        ComponentModel rootNode = nodes.get(rootFailedComponentId);
        if (rootNode == null) return cascadeSteps;

        // Step 0: Record root failure
        ComponentState prevRoot = rootNode.getState();
        rootNode.setState(ComponentState.FAILED);
        rootNode.setCpuUtilizationPercent(0.0);
        cascadeSteps.add(new CascadeStep(
                0,
                rootFailedComponentId,
                rootNode.getName(),
                prevRoot,
                ComponentState.FAILED,
                "MANUAL_INJECTION",
                "Primary injected fault"
        ));

        // BFS level-by-level cascade
        Queue<String> currentLevel = new LinkedList<>();
        currentLevel.add(rootFailedComponentId);
        Set<String> affectedNodes = new HashSet<>();
        affectedNodes.add(rootFailedComponentId);

        int step = 1;
        while (!currentLevel.isEmpty()) {
            int levelSize = currentLevel.size();
            List<String> nextLevel = new ArrayList<>();

            for (int i = 0; i < levelSize; i++) {
                String parentId = currentLevel.poll();
                Set<String> dependents = graph.getDirectDependents(parentId);

                for (String depId : dependents) {
                    if (affectedNodes.contains(depId)) continue;

                    ComponentModel depComp = nodes.get(depId);
                    if (depComp == null || depComp.getState() == ComponentState.FAILED) continue;

                    ComponentState before = depComp.getState();
                    ComponentState targetState;
                    String reason;

                    if (immediateTotalFailure || step >= 2) {
                        targetState = ComponentState.FAILED;
                        depComp.setCpuUtilizationPercent(0.0);
                        reason = "Cascading dependency failure: dependent service " + parentId + " unreachable";
                    } else {
                        targetState = ComponentState.DEGRADED;
                        depComp.setCpuUtilizationPercent(Math.min(99.0, depComp.getCpuUtilizationPercent() + 40.0));
                        reason = "Cascading upstream degradation: latency and error rate spiking";
                    }

                    depComp.setState(targetState);
                    affectedNodes.add(depId);
                    nextLevel.add(depId);

                    cascadeSteps.add(new CascadeStep(
                            step,
                            depId,
                            depComp.getName(),
                            before,
                            targetState,
                            parentId,
                            reason
                    ));
                }
            }

            for (String next : nextLevel) {
                currentLevel.add(next);
            }
            step++;
        }

        return cascadeSteps;
    }

    /**
     * Simulates recovery propagation: recovers a component and attempts to restore dependents.
     */
    public List<CascadeStep> propagateRecovery(DependencyGraph graph, String recoveredComponentId) {
        List<CascadeStep> steps = new ArrayList<>();
        if (graph == null || recoveredComponentId == null) return steps;

        Map<String, ComponentModel> nodes = graph.getNodes();
        ComponentModel root = nodes.get(recoveredComponentId);
        if (root == null) return steps;

        ComponentState prev = root.getState();
        root.setState(ComponentState.HEALTHY);
        root.setCpuUtilizationPercent(20.0);
        steps.add(new CascadeStep(
                0,
                recoveredComponentId,
                root.getName(),
                prev,
                ComponentState.HEALTHY,
                "OPERATOR_REPAIR",
                "Component restored and operational"
        ));

        // Check if direct dependents can recover (only if all their upstream dependencies are healthy)
        Set<String> dependents = graph.getDirectDependents(recoveredComponentId);
        int step = 1;
        for (String depId : dependents) {
            ComponentModel dep = nodes.get(depId);
            if (dep == null || dep.getState() == ComponentState.HEALTHY) continue;

            Set<String> upstream = graph.getDirectDependencies(depId);
            boolean allHealthy = true;
            for (String upId : upstream) {
                ComponentModel up = nodes.get(upId);
                if (up != null && !up.isOperational()) {
                    allHealthy = false;
                    break;
                }
            }

            if (allHealthy) {
                ComponentState depPrev = dep.getState();
                dep.setState(ComponentState.HEALTHY);
                dep.setCpuUtilizationPercent(18.0);
                steps.add(new CascadeStep(
                        step++,
                        depId,
                        dep.getName(),
                        depPrev,
                        ComponentState.HEALTHY,
                        recoveredComponentId,
                        "All upstream dependencies restored"
                ));
            }
        }
        return steps;
    }
}
