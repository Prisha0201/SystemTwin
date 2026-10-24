package com.systemtwin.report;

import com.systemtwin.model.AnomalyEvent;
import com.systemtwin.model.SensorReading;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Exports telemetry data points and detected anomalies to standardized CSV formats.
 */
public class CsvReportExporter {

    public void exportReadingsToCsv(List<SensorReading> readings, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("Timestamp,Instant,RunId,ComponentId,SensorId,MetricName,Value,Unit,Status");
            if (readings != null) {
                for (SensorReading r : readings) {
                    writer.printf("%d,%s,\"%s\",\"%s\",\"%s\",\"%s\",%.3f,\"%s\",\"%s\"%n",
                            r.getTimestamp(),
                            Instant.ofEpochMilli(r.getTimestamp()).toString(),
                            escape(r.getRunId()),
                            escape(r.getComponentId()),
                            escape(r.getSensorId()),
                            escape(r.getMetricName()),
                            r.getValue(),
                            escape(r.getUnit()),
                            escape(r.getStatus())
                    );
                }
            }
        }
    }

    public void exportAnomaliesToCsv(List<AnomalyEvent> anomalies, File file) throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("Timestamp,Instant,RunId,ComponentId,SensorId,MetricName,Value,Baseline,Severity,Detector,Message");
            if (anomalies != null) {
                for (AnomalyEvent a : anomalies) {
                    writer.printf("%d,%s,\"%s\",\"%s\",\"%s\",\"%s\",%.3f,%.3f,\"%s\",\"%s\",\"%s\"%n",
                            a.getTimestamp(),
                            Instant.ofEpochMilli(a.getTimestamp()).toString(),
                            escape(a.getRunId()),
                            escape(a.getComponentId()),
                            escape(a.getSensorId()),
                            escape(a.getMetricName()),
                            a.getMetricValue(),
                            a.getBaselineValue(),
                            escape(a.getSeverity()),
                            escape(a.getDetector()),
                            escape(a.getMessage())
                    );
                }
            }
        }
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}
