package com.systemtwin.service;

import com.systemtwin.events.EventBus;
import com.systemtwin.model.SimulationEvent;
import com.systemtwin.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * Executes deterministic replay of recorded simulation runs.
 * Dispatches historical events in sequence order preserving relative or scaled timing.
 */
public class ReplayService {

    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);

    private final EventBus eventBus;
    private final EventRepository eventRepository;
    private final ExecutorService replayExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "systemtwin-replay-worker");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean isReplaying = false;

    public ReplayService(EventBus eventBus, EventRepository eventRepository) {
        this.eventBus = eventBus;
        this.eventRepository = eventRepository;
    }

    public CompletableFuture<Integer> replayRun(String runId, double speedMultiplier) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        if (eventRepository == null) {
            future.complete(0);
            return future;
        }

        isReplaying = true;
        replayExecutor.submit(() -> {
            try {
                List<SimulationEvent> events = eventRepository.findEventsByRun(runId);
                events.sort(Comparator.comparingLong(SimulationEvent::getSequenceNumber));

                log.info("Starting deterministic replay for run {} with {} events", runId, events.size());

                long prevTimestamp = 0;
                int count = 0;

                for (SimulationEvent event : events) {
                    if (!isReplaying) break;

                    if (prevTimestamp > 0 && event.getTimestamp() > prevTimestamp) {
                        long diff = event.getTimestamp() - prevTimestamp;
                        long sleepMs = Math.min(2000, Math.max(10, (long) (diff / speedMultiplier)));
                        Thread.sleep(sleepMs);
                    }
                    prevTimestamp = event.getTimestamp();

                    if (eventBus != null) {
                        eventBus.publish(new SimulationEvent(
                                event.getId(),
                                event.getRunId(),
                                event.getTimestamp(),
                                event.getSequenceNumber(),
                                "REPLAY_" + event.getEventType(),
                                event.getSourceId(),
                                event.getTargetId(),
                                "[REPLAY] " + event.getDescription(),
                                event.getPayload()
                        ));
                    }
                    count++;
                }

                log.info("Replay finished. Dispatched {} events.", count);
                future.complete(count);
            } catch (Exception ex) {
                log.error("Replay encountered error: {}", ex.getMessage(), ex);
                future.completeExceptionally(ex);
            } finally {
                isReplaying = false;
            }
        });

        return future;
    }

    public void stopReplay() {
        this.isReplaying = false;
    }

    public boolean isReplaying() {
        return isReplaying;
    }
}
