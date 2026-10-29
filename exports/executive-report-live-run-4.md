# SystemTwin Simulation Executive Summary

**Generated on:** 2026-10-29T07:21:08.080164500Z

## 1. Simulation Run Overview

| Metric | Value |
| :--- | :--- |
| **Run ID** | `c3447455-e534-44ea-afdc-2ac999202f45` |
| **Run Name** | Live-Run-4 |
| **Status** | COMPLETED |
| **Ticks Evaluated** | 114 |
| **Total Telemetry Readings** | 912 |
| **Total Anomalies Detected** | 157 |
| **Random Seed** | 42 |
| **Duration (sec)** | 126s |

## 2. System Topology and Health

| Component ID | Name | Type | State | CPU % |
| :--- | :--- | :--- | :--- | :--- |
| `plc-main` | Master PLC Controller | PLC | HEALTHY | 17.2% |
| `pump-01` | Hydraulic Primary Pump | PUMP | HEALTHY | 11.9% |
| `robot-arm` | 6-Axis Assembly Robot | ROBOT | FAILED | 0.0% |
| `conveyor-01` | Intake Conveyor Belt | CONVEYOR | HEALTHY | 9.7% |
| `vision-qc` | Optical QC Scanner | INSPECTION | HEALTHY | 10.4% |

## 3. Criticality & Single Point of Failure (SPOF) Analysis

| Component | Score | Blast Radius | Upstream Deps | SPOF? | Assessment |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Master PLC Controller** | 67.5 | 3 | 0 | ⚠️ **YES** | HIGH: Significant operational impact across multiple dependent subsystems. |
| **Intake Conveyor Belt** | 32.5 | 1 | 1 | No | MODERATE: Standard component with isolated or redundant downstream paths. |
| **Hydraulic Primary Pump** | 27.5 | 1 | 0 | No | MODERATE: Standard component with isolated or redundant downstream paths. |
| **6-Axis Assembly Robot** | 10.0 | 0 | 2 | No | LOW: Edge component with minimal cascading blast radius. |
| **Optical QC Scanner** | 10.0 | 0 | 2 | No | LOW: Edge component with minimal cascading blast radius. |

## 4. Anomaly Breakdown

- **CRITICAL severity:** 77 alerts
- **HIGH severity:** 74 alerts
- **MEDIUM severity:** 6 alerts

### Recent Notable Alerts

- **[HIGH] THRESHOLD** (`robot-arm`): Telemetry flagged with CRITICAL status (value: 138.32 vs baseline: 0.00)
- **[CRITICAL] THRESHOLD** (`robot-arm`): Critical threshold breach: 95.04 °C exceeds critical limit 90.00 (value: 95.04 vs baseline: 75.00)
- **[HIGH] THRESHOLD** (`robot-arm`): Telemetry flagged with CRITICAL status (value: 138.50 vs baseline: 0.00)
- **[CRITICAL] THRESHOLD** (`robot-arm`): Critical threshold breach: 97.33 °C exceeds critical limit 90.00 (value: 97.33 vs baseline: 75.00)
- **[HIGH] THRESHOLD** (`robot-arm`): Telemetry flagged with CRITICAL status (value: 139.89 vs baseline: 0.00)
- **[CRITICAL] THRESHOLD** (`robot-arm`): Critical threshold breach: 94.45 °C exceeds critical limit 90.00 (value: 94.45 vs baseline: 75.00)
- **[HIGH] THRESHOLD** (`robot-arm`): Telemetry flagged with CRITICAL status (value: 138.55 vs baseline: 0.00)
- **[CRITICAL] THRESHOLD** (`robot-arm`): Critical threshold breach: 96.03 °C exceeds critical limit 90.00 (value: 96.03 vs baseline: 75.00)
