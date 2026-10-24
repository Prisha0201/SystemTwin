package com.systemtwin.model;

public enum ComponentState {
    HEALTHY,
    DEGRADED,
    FAILED,
    OFFLINE;

    public boolean isOperational() {
        return this == HEALTHY || this == DEGRADED;
    }

    public boolean isFailed() {
        return this == FAILED || this == OFFLINE;
    }
}
