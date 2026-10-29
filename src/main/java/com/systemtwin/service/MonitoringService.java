package com.systemtwin.service;

import com.systemtwin.analysis.ThresholdDetector;
import com.systemtwin.analysis.ZScoreDetector;
import com.systemtwin.events.EventBus;
import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;
import com.systemtwin.model.SimulationEvent;
import com.systemtwin.repository.EventRepository;
import com.systemtwin.repository.ReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Real-time monitoring service executing dual threshold and statistical anomaly checks
 * on active sensor telemetry streams.
 */
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final EventBus eventBus;
    private final ThresholdDetector thresholdDetector;
    private final ZScoreDetector zScoreDetector;
    private final ReadingRepository readingRepository;

    private final Queue<AnomalyEvent> activeAnomalies = new ConcurrentLinkedQueue<>();
    private final List<SensorReading> liveReadingBuffer = new CopyOnWriteArrayList<>();
    private static final int MAX_BUFFER_SIZE = 1000;

    private final EventRepository eventRepository;

    public MonitoringService(
            EventBus eventBus,
            ThresholdDetector thresholdDetector,
            ZScoreDetector zScoreDetector,
            ReadingRepository readingRepository) {
        this(eventBus, thresholdDetector, zScoreDetector, readingRepository, null);
    }

    public MonitoringService(
            EventBus eventBus,
            ThresholdDetector thresholdDetector,
            ZScoreDetector zScoreDetector,
            ReadingRepository readingRepository,
            EventRepository eventRepository) {
        this.eventBus = eventBus;
        this.thresholdDetector = thresholdDetector != null ? thresholdDetector : new ThresholdDetector();
        this.zScoreDetector = zScoreDetector != null ? zScoreDetector : new ZScoreDetector(30, 3.0);
        this.readingRepository = readingRepository;
        this.eventRepository = eventRepository;

        if (eventBus != null) {
            eventBus.subscribe(SensorReading.class, this::processReading);
            // A new run must not inherit anomalies (or detector history) from a previous run or template
            eventBus.subscribe(SimulationEvent.class, event -> {
                if (event != null && "SIMULATION_STARTED".equals(event.getEventType())) {
                    clear();
                }
            });
        }
    }

    public void processReading(SensorReading reading) {
        if (reading == null) return;

        liveReadingBuffer.add(reading);
        if (liveReadingBuffer.size() > MAX_BUFFER_SIZE) {
            liveReadingBuffer.remove(0);
        }

        // 1. Threshold check
        Optional<AnomalyEvent> thresholdAnomaly = thresholdDetector.evaluate(reading);
        thresholdAnomaly.ifPresent(this::handleAnomaly);

        // 2. Statistical Z-Score check
        Optional<AnomalyEvent> zScoreAnomaly = zScoreDetector.evaluate(reading);
        zScoreAnomaly.ifPresent(this::handleAnomaly);
    }

    private void handleAnomaly(AnomalyEvent anomaly) {
        activeAnomalies.add(anomaly);
        while (activeAnomalies.size() > 500) {
            activeAnomalies.poll();
        }

        if (eventRepository != null) {
            try {
                eventRepository.saveAnomalyEvent(anomaly);
            } catch (Exception ex) {
                log.warn("Failed persisting anomaly event {}: {}", anomaly.getId(), ex.getMessage());
            }
        }

        if (eventBus != null) {
            eventBus.publish(anomaly);
        }
    }

    public List<AnomalyEvent> getActiveAnomalies() {
        return new ArrayList<>(activeAnomalies);
    }

    public List<SensorReading> getLiveReadings() {
        return new ArrayList<>(liveReadingBuffer);
    }

    public void clear() {
        activeAnomalies.clear();
        liveReadingBuffer.clear();
        thresholdDetector.reset();
        zScoreDetector.reset();
    }

    public ThresholdDetector getThresholdDetector() {
        return thresholdDetector;
    }

    public ZScoreDetector getZScoreDetector() {
        return zScoreDetector;
    }
}
