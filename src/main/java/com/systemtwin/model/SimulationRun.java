package com.systemtwin.model;

import java.time.Instant;
import java.util.UUID;

public class SimulationRun {

    private String id;
    private String systemId;
    private String name;
    private Instant startTime;
    private Instant endTime;
    private String status; // INITIALIZED, RUNNING, PAUSED, COMPLETED, FAILED
    private int tickCount;
    private int eventCount;
    private int anomalyCount;
    private long randomSeed;

    public SimulationRun() {
        this.id = UUID.randomUUID().toString();
        this.startTime = Instant.now();
        this.status = "INITIALIZED";
        this.randomSeed = 42L;
    }

    public SimulationRun(String id, String systemId, String name, long randomSeed) {
        this.id = id != null ? id : UUID.randomUUID().toString();
        this.systemId = systemId;
        this.name = name != null ? name : "Simulation-" + this.id.substring(0, 8);
        this.startTime = Instant.now();
        this.status = "INITIALIZED";
        this.randomSeed = randomSeed;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTickCount() {
        return tickCount;
    }

    public void setTickCount(int tickCount) {
        this.tickCount = tickCount;
    }

    public void incrementTicks() {
        this.tickCount++;
    }

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }

    public void incrementEvents() {
        this.eventCount++;
    }

    public int getAnomalyCount() {
        return anomalyCount;
    }

    public void setAnomalyCount(int anomalyCount) {
        this.anomalyCount = anomalyCount;
    }

    public void incrementAnomalies() {
        this.anomalyCount++;
    }

    public long getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }

    public long getDurationSeconds() {
        if (startTime == null) return 0;
        Instant end = (endTime != null) ? endTime : Instant.now();
        return Math.max(0, end.getEpochSecond() - startTime.getEpochSecond());
    }

    @Override
    public String toString() {
        return "SimulationRun{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", ticks=" + tickCount +
                ", anomalies=" + anomalyCount +
                '}';
    }
}
