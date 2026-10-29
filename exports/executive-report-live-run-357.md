# SystemTwin Simulation Executive Summary

**Generated on:** 2026-10-29T07:15:53.712660700Z

## 1. Simulation Run Overview

| Metric | Value |
| :--- | :--- |
| **Run ID** | `b5319405-d2f9-48e4-aaaa-1e66aec529dd` |
| **Run Name** | Live-Run-357 |
| **Status** | COMPLETED |
| **Ticks Evaluated** | 122 |
| **Total Telemetry Readings** | 976 |
| **Total Anomalies Detected** | 99 |
| **Random Seed** | 42 |
| **Duration (sec)** | 185s |

## 2. System Topology and Health

## 3. Criticality & Single Point of Failure (SPOF) Analysis

## 4. Anomaly Breakdown

- **CRITICAL severity:** 26 alerts
- **HIGH severity:** 67 alerts
- **MEDIUM severity:** 6 alerts

### Recent Notable Alerts

- **[MEDIUM] Z_SCORE** (`plc-main`): Statistical outlier: Z=3.20 (val: 23.65, μ: 24.01, σ: 0.11) (value: 23.65 vs baseline: 24.01)
- **[MEDIUM] Z_SCORE** (`conveyor-01`): Statistical outlier: Z=3.31 (val: 1.37, μ: 1.79, σ: 0.12) (value: 1.37 vs baseline: 1.79)
- **[MEDIUM] Z_SCORE** (`vision-qc`): Statistical outlier: Z=3.38 (val: 98.31, μ: 99.10, σ: 0.23) (value: 98.31 vs baseline: 99.10)
- **[MEDIUM] Z_SCORE** (`plc-main`): Statistical outlier: Z=3.27 (val: 23.66, μ: 24.02, σ: 0.11) (value: 23.66 vs baseline: 24.02)
- **[HIGH] THRESHOLD** (`robot-arm`): Telemetry flagged with ANOMALY status (value: 106.26 vs baseline: 0.00)
- **[HIGH] THRESHOLD** (`robot-arm`): High threshold warning: 80.69 °C exceeds upper limit 75.00 (value: 80.69 vs baseline: 75.00)
- **[HIGH] THRESHOLD** (`pump-01`): Telemetry flagged with CRITICAL status (value: 151.68 vs baseline: 0.00)
- **[CRITICAL] THRESHOLD** (`pump-01`): Critical threshold breach: 14.32 bar exceeds critical limit 11.50 (value: 14.32 vs baseline: 9.00)
