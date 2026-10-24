package com.systemtwin.graph;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DependencyGraphTest {

    private DependencyGraph graph;
    private ComponentModel webServer;
    private ComponentModel appServer;
    private ComponentModel dbServer;
    private ComponentModel cacheServer;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
        webServer = new ComponentModel("web-01", "Web Server", "SERVER");
        appServer = new ComponentModel("app-01", "App Server", "SERVER");
        dbServer = new ComponentModel("db-01", "Database", "DATABASE");
        cacheServer = new ComponentModel("cache-01", "Redis Cache", "DATABASE");

        graph.addComponent(webServer);
        graph.addComponent(appServer);
        graph.addComponent(dbServer);
        graph.addComponent(cacheServer);

        // Web depends on App
        graph.addDependency("web-01", "app-01");
        // App depends on DB and Cache
        graph.addDependency("app-01", "db-01");
        graph.addDependency("app-01", "cache-01");
    }

    @Test
    @DisplayName("Verify component nodes and edges count")
    void testNodesAndEdges() {
        assertEquals(4, graph.getNodeCount());
        assertEquals(3, graph.getEdgeCount());
        assertTrue(graph.hasDependency("web-01", "app-01"));
        assertTrue(graph.hasDependency("app-01", "db-01"));
        assertFalse(graph.hasDependency("db-01", "web-01"));
    }

    @Test
    @DisplayName("Verify direct dependencies and dependents")
    void testDirectDependenciesAndDependents() {
        Set<String> appDeps = graph.getDirectDependencies("app-01");
        assertTrue(appDeps.contains("db-01"));
        assertTrue(appDeps.contains("cache-01"));

        Set<String> dbDependents = graph.getDirectDependents("db-01");
        assertTrue(dbDependents.contains("app-01"));
    }

    @Test
    @DisplayName("Verify BFS downstream cascade order when DB fails")
    void testDownstreamCascadeOrder() {
        // When db-01 fails, app-01 is affected, then web-01 is affected
        List<String> cascade = graph.getDownstreamCascadeOrder("db-01");
        assertEquals(2, cascade.size());
        assertEquals("app-01", cascade.get(0));
        assertEquals("web-01", cascade.get(1));
    }

    @Test
    @DisplayName("Verify DFS upstream prerequisites")
    void testUpstreamDependencies() {
        List<String> upstream = graph.getUpstreamDependencies("web-01");
        assertTrue(upstream.contains("app-01"));
        assertTrue(upstream.contains("db-01"));
        assertTrue(upstream.contains("cache-01"));
    }

    @Test
    @DisplayName("Verify cycle detection on acyclic and cyclic graphs")
    void testCycleDetection() {
        assertFalse(graph.hasCycle(), "Graph should initially be a DAG without cycles");

        // Introduce a cycle: db-01 -> web-01
        graph.addDependency("db-01", "web-01");
        assertTrue(graph.hasCycle(), "Graph should now detect a cycle");
    }

    @Test
    @DisplayName("Verify Kahn's topological sort on DAG")
    void testTopologicalSort() {
        List<String> order = graph.getTopologicalSort();
        assertFalse(order.isEmpty());
        assertEquals(4, order.size());

        // web-01 must come before app-01, and app-01 before db-01
        int webIdx = order.indexOf("web-01");
        int appIdx = order.indexOf("app-01");
        int dbIdx = order.indexOf("db-01");
        assertTrue(webIdx < appIdx);
        assertTrue(appIdx < dbIdx);
    }

    @Test
    @DisplayName("Verify FailurePropagator cascades through dependents")
    void testFailurePropagation() {
        FailurePropagator propagator = new FailurePropagator();
        List<FailurePropagator.CascadeStep> steps = propagator.propagateFailure(graph, "db-01", false);

        assertNotNull(steps);
        assertFalse(steps.isEmpty());
        assertEquals("db-01", steps.get(0).getComponentId());
        assertEquals(ComponentState.FAILED, dbServer.getState());

        // app-01 and web-01 should be degraded or failed
        assertTrue(appServer.getState() == ComponentState.DEGRADED || appServer.getState() == ComponentState.FAILED);
    }

    @Test
    @DisplayName("Verify CriticalityAnalyzer ranks database as high criticality")
    void testCriticalityAnalyzer() {
        CriticalityAnalyzer analyzer = new CriticalityAnalyzer();
        List<CriticalityAnalyzer.CriticalityReport> reports = analyzer.analyze(graph);

        assertNotNull(reports);
        assertEquals(4, reports.size());

        // db-01 or app-01 should have high blast radius
        CriticalityAnalyzer.CriticalityReport top = reports.get(0);
        assertTrue(top.getCriticalityScore() > 30.0);
    }
}
