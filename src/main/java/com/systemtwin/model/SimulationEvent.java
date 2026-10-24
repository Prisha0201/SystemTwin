package com.systemtwin.model;

import java.util.UUID;

public class SimulationEvent {

    private String id;
    private String runId;
    private long timestamp;
    private long sequenceNumber;
    private String eventType; // TICK, FAILURE_INJECTED, DEGRADATION_INJECTED, CASCADE_STEP, COMPONENT_RECOVERED, DEPENDENCY_SEVERED
    private String sourceId;
    private String targetId;
    private String description;
    private String payload;

    public SimulationEvent() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public SimulationEvent(String runId, long sequenceNumber, String eventType, String sourceId, String targetId, String description, String payload) {
        this.id = UUID.randomUUID().toString();
        this.runId = runId;
        this.timestamp = System.currentTimeMillis();
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.description = description;
        this.payload = payload;
    }

    public SimulationEvent(String id, String runId, long timestamp, long sequenceNumber, String eventType, String sourceId, String targetId, String description, String payload) {
        this.id = id;
        this.runId = runId;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        this.eventType = eventType;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.description = description;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "SimulationEvent{" +
                "#" + sequenceNumber +
                ", type='" + eventType + '\'' +
                ", desc='" + description + '\'' +
                '}';
    }
}
