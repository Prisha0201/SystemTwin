package com.systemtwin.service;

import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;
import com.systemtwin.model.SimulationEvent;
import com.systemtwin.model.SimulationRun;
import com.systemtwin.repository.EventRepository;
import com.systemtwin.repository.ReadingRepository;
import com.systemtwin.repository.SimulationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;

/**
 * Service managing query of historical simulation runs and baseline vs failure comparisons.
 */
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);

    private final SimulationRepository simulationRepository;
    private final ReadingRepository readingRepository;
    private final EventRepository eventRepository;

    public static class RunComparison {
        private final String baselineRunId;
        private final String failureRunId;
        private final int baselineReadingsCount;
        private final int failureReadingsCount;
        private final int baselineAnomalies;
        private final int failureAnomalies;
        private final Map<String, Double> metricDeltaAverages;
        private final String summary;

        public RunComparison(String baselineRunId, String failureRunId, int baselineReadingsCount, int failureReadingsCount, int baselineAnomalies, int failureAnomalies, Map<String, Double> metricDeltaAverages, String summary) {
            this.baselineRunId = baselineRunId;
            this.failureRunId = failureRunId;
            this.baselineReadingsCount = baselineReadingsCount;
            this.failureReadingsCount = failureReadingsCount;
            this.baselineAnomalies = baselineAnomalies;
            this.failureAnomalies = failureAnomalies;
            this.metricDeltaAverages = metricDeltaAverages;
            this.summary = summary;
        }

        public String getBaselineRunId() { return baselineRunId; }
        public String getFailureRunId() { return failureRunId; }
        public int getBaselineReadingsCount() { return baselineReadingsCount; }
        public int getFailureReadingsCount() { return failureReadingsCount; }
        public int getBaselineAnomalies() { return baselineAnomalies; }
        public int getFailureAnomalies() { return failureAnomalies; }
        public Map<String, Double> getMetricDeltaAverages() { return metricDeltaAverages; }
        public String getSummary() { return summary; }
    }

    public HistoryService(SimulationRepository simulationRepository, ReadingRepository readingRepository, EventRepository eventRepository) {
        this.simulationRepository = simulationRepository;
        this.readingRepository = readingRepository;
        this.eventRepository = eventRepository;
    }

    public List<SimulationRun> getAllRuns() {
        if (simulationRepository == null) return Collections.emptyList();
        try {
            return simulationRepository.findAllRuns();
        } catch (SQLException e) {
            log.error("Failed retrieving all runs: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Optional<SimulationRun> getRunById(String runId) {
        if (simulationRepository == null || runId == null) return Optional.empty();
        try {
            return simulationRepository.findRunById(runId);
        } catch (SQLException e) {
            log.error("Failed retrieving run {}: {}", runId, e.getMessage());
            return Optional.empty();
        }
    }

    public List<SensorReading> getReadingsForRun(String runId) {
        if (readingRepository == null || runId == null) return Collections.emptyList();
        try {
            return readingRepository.findReadingsByRun(runId);
        } catch (SQLException e) {
            log.error("Failed retrieving readings for run {}: {}", runId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<SimulationEvent> getEventsForRun(String runId) {
        if (eventRepository == null || runId == null) return Collections.emptyList();
        try {
            return eventRepository.findEventsByRun(runId);
        } catch (SQLException e) {
            log.error("Failed retrieving events for run {}: {}", runId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<AnomalyEvent> getAnomaliesForRun(String runId) {
        if (eventRepository == null || runId == null) return Collections.emptyList();
        try {
            return eventRepository.findAnomaliesByRun(runId);
        } catch (SQLException e) {
            log.error("Failed retrieving anomalies for run {}: {}", runId, e.getMessage());
            return Collections.emptyList();
        }
    }

    public RunComparison compareRuns(String baselineRunId, String failureRunId) {
        List<SensorReading> baseReadings = getReadingsForRun(baselineRunId);
        List<SensorReading> failReadings = getReadingsForRun(failureRunId);

        List<AnomalyEvent> baseAnomalies = getAnomaliesForRun(baselineRunId);
        List<AnomalyEvent> failAnomalies = getAnomaliesForRun(failureRunId);

        Map<String, List<Double>> baseMetricVals = new HashMap<>();
        for (SensorReading r : baseReadings) {
            baseMetricVals.computeIfAbsent(r.getMetricName(), k -> new ArrayList<>()).add(r.getValue());
        }

        Map<String, List<Double>> failMetricVals = new HashMap<>();
        for (SensorReading r : failReadings) {
            failMetricVals.computeIfAbsent(r.getMetricName(), k -> new ArrayList<>()).add(r.getValue());
        }

        Map<String, Double> deltas = new LinkedHashMap<>();
        for (String metric : failMetricVals.keySet()) {
            double failAvg = failMetricVals.get(metric).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double baseAvg = baseMetricVals.containsKey(metric) ?
                    baseMetricVals.get(metric).stream().mapToDouble(Double::doubleValue).average().orElse(0.0) : 0.0;
            deltas.put(metric, failAvg - baseAvg);
        }

        String summary = String.format(
                "Comparison: Failure run had %d anomalies vs %d baseline. Observed %d metric delta variations.",
                failAnomalies.size(), baseAnomalies.size(), deltas.size()
        );

        return new RunComparison(
                baselineRunId,
                failureRunId,
                baseReadings.size(),
                failReadings.size(),
                baseAnomalies.size(),
                failAnomalies.size(),
                deltas,
                summary
        );
    }
}
