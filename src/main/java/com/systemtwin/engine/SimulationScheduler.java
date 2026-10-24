package com.systemtwin.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages simulation execution timing, step execution, speed throttling, and pause/resume.
 */
public class SimulationScheduler {

    private static final Logger log = LoggerFactory.getLogger(SimulationScheduler.class);

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> scheduledFuture;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);

    private long baseIntervalMs = 1000L;
    private double speedMultiplier = 1.0;
    private Runnable currentTask;

    public SimulationScheduler() {}

    public synchronized void start(Runnable task, long intervalMs) {
        stop();
        this.currentTask = task;
        this.baseIntervalMs = intervalMs;
        this.isPaused.set(false);
        this.isRunning.set(true);

        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "systemtwin-simulation-ticker");
            t.setDaemon(true);
            return t;
        });

        scheduleNext();
        log.info("SimulationScheduler started (interval: {} ms, speed: {}x)", baseIntervalMs, speedMultiplier);
    }

    private synchronized void scheduleNext() {
        if (!isRunning.get() || executor == null || executor.isShutdown()) return;

        long effectiveInterval = Math.max(50, (long) (baseIntervalMs / speedMultiplier));
        scheduledFuture = executor.scheduleAtFixedRate(() -> {
            try {
                if (!isPaused.get() && currentTask != null) {
                    currentTask.run();
                }
            } catch (Exception ex) {
                log.error("Error executing simulation tick: {}", ex.getMessage(), ex);
            }
        }, effectiveInterval, effectiveInterval, TimeUnit.MILLISECONDS);
    }

    public synchronized void pause() {
        isPaused.set(true);
        log.info("SimulationScheduler paused");
    }

    public synchronized void resume() {
        isPaused.set(false);
        log.info("SimulationScheduler resumed");
    }

    public synchronized void step() {
        if (currentTask != null) {
            try {
                currentTask.run();
            } catch (Exception ex) {
                log.error("Error executing single step: {}", ex.getMessage(), ex);
            }
        }
    }

    public synchronized void setSpeed(double multiplier) {
        if (multiplier <= 0.0) return;
        this.speedMultiplier = multiplier;
        if (isRunning.get() && !isPaused.get()) {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
            scheduleNext();
        }
    }

    public synchronized void stop() {
        isRunning.set(false);
        isPaused.set(false);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        log.info("SimulationScheduler stopped");
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public boolean isPaused() {
        return isPaused.get();
    }

    public double getSpeedMultiplier() {
        return speedMultiplier;
    }
}
