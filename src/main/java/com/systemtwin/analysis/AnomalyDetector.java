package com.systemtwin.analysis;

import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;

import java.util.Optional;

/**
 * Strategy interface for anomaly detection algorithms.
 */
public interface AnomalyDetector {

    String getDetectorName();

    Optional<AnomalyEvent> evaluate(SensorReading reading);

    void reset();
}
