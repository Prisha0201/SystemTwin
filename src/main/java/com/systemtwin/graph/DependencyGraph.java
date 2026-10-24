package com.systemtwin.graph;

import com.systemtwin.model.ComponentModel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Directed Dependency Graph for SystemTwin digital twin components.
 * An edge from A to B means "A depends on B" (A requires B to function).
 * Downstream: components that depend on B (if B fails, A is affected).
 * Upstream: components that A depends on (prerequisites for A).
 */
public class DependencyGraph {

    // Component storage
    private final Map<String, ComponentModel> nodeMap = new ConcurrentHashMap<>();

    // dependentId -> Set of components it depends on (outgoing dependencies)
    private final Map<String, Set<String>> outgoingDependencies = new ConcurrentHashMap<>();

    // componentId -> Set of components that depend on it (downstream dependents / reverse edges)
    private final Map<String, Set<String>> incomingDependents = new ConcurrentHashMap<>();

    public DependencyGraph() {}

    public void addComponent(ComponentModel component) {
        if (component == null) return;
        nodeMap.put(component.getId(), component);
        outgoingDependencies.computeIfAbsent(component.getId(), k -> new CopyOnWriteArraySet<>());
        incomingDependents.computeIfAbsent(component.getId(), k -> new CopyOnWriteArraySet<>());
    }

    public void removeComponent(String componentId) {
        if (componentId == null) return;
        nodeMap.remove(componentId);

        // Remove from outgoing
        Set<String> dependsOn = outgoingDependencies.remove(componentId);
        if (dependsOn != null) {
            for (String dep : dependsOn) {
                Set<String> in = incomingDependents.get(dep);
                if (in != null) in.remove(componentId);
            }
        }

        // Remove from incoming
        Set<String> dependents = incomingDependents.remove(componentId);
        if (dependents != null) {
            for (String d : dependents) {
                Set<String> out = outgoingDependencies.get(d);
                if (out != null) out.remove(componentId);
            }
        }
    }

    /**
     * Adds dependency: dependentId depends on targetId.
     */
    public void addDependency(String dependentId, String targetId) {
        if (dependentId == null || targetId == null || dependentId.equals(targetId)) return;
        outgoingDependencies.computeIfAbsent(dependentId, k -> new CopyOnWriteArraySet<>()).add(targetId);
        incomingDependents.computeIfAbsent(targetId, k -> new CopyOnWriteArraySet<>()).add(dependentId);
    }

    public void removeDependency(String dependentId, String targetId) {
        if (dependentId == null || targetId == null) return;
        Set<String> out = outgoingDependencies.get(dependentId);
        if (out != null) out.remove(targetId);
        Set<String> in = incomingDependents.get(targetId);
        if (in != null) in.remove(dependentId);
    }

    public boolean hasDependency(String dependentId, String targetId) {
        Set<String> out = outgoingDependencies.get(dependentId);
        return out != null && out.contains(targetId);
    }

    public Set<String> getDirectDependencies(String componentId) {
        Set<String> out = outgoingDependencies.get(componentId);
        return out != null ? Collections.unmodifiableSet(out) : Collections.emptySet();
    }

    /**
     * Returns components directly depending on componentId (blast radius hop 1).
     */
    public Set<String> getDirectDependents(String componentId) {
        Set<String> in = incomingDependents.get(componentId);
        return in != null ? Collections.unmodifiableSet(in) : Collections.emptySet();
    }

    /**
     * BFS traversal finding all downstream components affected if rootComponentId fails.
     */
    public List<String> getDownstreamCascadeOrder(String rootComponentId) {
        List<String> result = new ArrayList<>();
        if (!nodeMap.containsKey(rootComponentId)) return result;

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        visited.add(rootComponentId);
        queue.add(rootComponentId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            Set<String> dependents = incomingDependents.getOrDefault(current, Collections.emptySet());
            for (String dep : dependents) {
                if (visited.add(dep)) {
                    result.add(dep);
                    queue.add(dep);
                }
            }
        }
        return result;
    }

    /**
     * DFS traversal finding all upstream prerequisite dependencies.
     */
    public List<String> getUpstreamDependencies(String componentId) {
        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        dfsUpstream(componentId, visited, result);
        return result;
    }

    private void dfsUpstream(String current, Set<String> visited, List<String> result) {
        Set<String> deps = outgoingDependencies.getOrDefault(current, Collections.emptySet());
        for (String dep : deps) {
            if (visited.add(dep)) {
                result.add(dep);
                dfsUpstream(dep, visited, result);
            }
        }
    }

    /**
     * Cycle detection using 3-color DFS algorithm (0=WHITE, 1=GRAY, 2=BLACK).
     */
    public boolean hasCycle() {
        Map<String, Integer> color = new HashMap<>();
        for (String id : nodeMap.keySet()) {
            color.put(id, 0);
        }

        for (String id : nodeMap.keySet()) {
            if (color.get(id) == 0) {
                if (detectCycleDfs(id, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean detectCycleDfs(String node, Map<String, Integer> color) {
        color.put(node, 1); // GRAY
        Set<String> neighbors = outgoingDependencies.getOrDefault(node, Collections.emptySet());
        for (String neighbor : neighbors) {
            if (!nodeMap.containsKey(neighbor)) continue;
            Integer c = color.get(neighbor);
            if (c != null && c == 1) {
                return true; // back-edge found
            }
            if (c != null && c == 0 && detectCycleDfs(neighbor, color)) {
                return true;
            }
        }
        color.put(node, 2); // BLACK
        return false;
    }

    /**
     * Topological sort using Kahn's algorithm.
     * Returns empty list if graph has cycles.
     */
    public List<String> getTopologicalSort() {
        Map<String, Integer> inDegree = new HashMap<>();
        for (String id : nodeMap.keySet()) {
            inDegree.put(id, 0);
        }

        for (String id : nodeMap.keySet()) {
            Set<String> deps = outgoingDependencies.getOrDefault(id, Collections.emptySet());
            for (String dep : deps) {
                if (inDegree.containsKey(dep)) {
                    inDegree.put(dep, inDegree.get(dep) + 1);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String u = queue.poll();
            sorted.add(u);

            Set<String> deps = outgoingDependencies.getOrDefault(u, Collections.emptySet());
            for (String v : deps) {
                if (inDegree.containsKey(v)) {
                    int remaining = inDegree.get(v) - 1;
                    inDegree.put(v, remaining);
                    if (remaining == 0) {
                        queue.add(v);
                    }
                }
            }
        }

        return sorted.size() == nodeMap.size() ? sorted : Collections.emptyList();
    }

    public Map<String, ComponentModel> getNodes() {
        return Collections.unmodifiableMap(nodeMap);
    }

    public Set<String> getComponentIds() {
        return Collections.unmodifiableSet(nodeMap.keySet());
    }

    public int getNodeCount() {
        return nodeMap.size();
    }

    public int getEdgeCount() {
        int count = 0;
        for (Set<String> out : outgoingDependencies.values()) {
            count += out.size();
        }
        return count;
    }

    public Map<String, Set<String>> getAllDependencies() {
        return Collections.unmodifiableMap(outgoingDependencies);
    }
}
