package com.systemtwin.events;

import com.systemtwin.model.SimulationEvent;
import com.systemtwin.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Captures, sequences, and records simulation events.
 * Provides in-memory event streaming and persistence to EventRepository.
 */
public class EventRecorder {

    private static final Logger log = LoggerFactory.getLogger(EventRecorder.class);

    private final EventBus eventBus;
    private final EventRepository eventRepository;
    private final AtomicLong sequenceCounter = new AtomicLong(1);
    private final Queue<SimulationEvent> eventHistory = new ConcurrentLinkedQueue<>();
    private static final int MAX_MEMORY_EVENTS = 5000;

    public EventRecorder(EventBus eventBus, EventRepository eventRepository) {
        this.eventBus = eventBus;
        this.eventRepository = eventRepository;

        if (eventBus != null) {
            eventBus.subscribe(SimulationEvent.class, this::recordEvent);
        }
    }

    public synchronized void recordEvent(SimulationEvent event) {
        if (event == null) return;
        if (event.getSequenceNumber() <= 0) {
            event.setSequenceNumber(sequenceCounter.getAndIncrement());
        }

        eventHistory.add(event);
        while (eventHistory.size() > MAX_MEMORY_EVENTS) {
            eventHistory.poll();
        }

        if (eventRepository != null) {
            try {
                eventRepository.saveEvent(event);
            } catch (Exception ex) {
                log.warn("Failed persisting event {}: {}", event.getId(), ex.getMessage());
            }
        }
    }

    public List<SimulationEvent> getRecentEvents(int limit) {
        List<SimulationEvent> all = new ArrayList<>(eventHistory);
        int from = Math.max(0, all.size() - limit);
        List<SimulationEvent> slice = new ArrayList<>(all.subList(from, all.size()));
        Collections.reverse(slice); // newest first
        return slice;
    }

    public List<SimulationEvent> getAllRecordedEvents() {
        return new ArrayList<>(eventHistory);
    }

    public void clear() {
        eventHistory.clear();
        sequenceCounter.set(1);
    }

    public long getCurrentSequence() {
        return sequenceCounter.get();
    }
}
