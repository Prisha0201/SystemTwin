package com.systemtwin.analysis

import com.systemtwin.model.ComponentModel
import com.systemtwin.model.ComponentState
import com.systemtwin.model.SystemModel

enum class IssueLevel {
    INFO,
    WARNING,
    ERROR
}

data class ValidationIssue(
    val level: IssueLevel,
    val componentId: String?,
    val message: String
)

data class ValidationResult(
    val isValid: Boolean,
    val issues: List<ValidationIssue>
) {
    val errorCount: Int get() = issues.count { it.level == IssueLevel.ERROR }
    val warningCount: Int get() = issues.count { it.level == IssueLevel.WARNING }
}

/**
 * Kotlin-based ScenarioValidator evaluating system topology, sensor configurations,
 * and failure injection scenarios for structural integrity.
 */
class ScenarioValidator {

    fun validateSystem(system: SystemModel?): ValidationResult {
        if (system == null) {
            return ValidationResult(
                isValid = false,
                issues = listOf(ValidationIssue(IssueLevel.ERROR, null, "SystemModel cannot be null"))
            )
        }

        val issues = mutableListOf<ValidationIssue>()
        val components = system.components.values

        if (components.isEmpty()) {
            issues.add(ValidationIssue(IssueLevel.ERROR, null, "System contains no components"))
            return ValidationResult(isValid = false, issues = issues)
        }

        // 1. Check for cycles
        if (system.dependencyGraph.hasCycle()) {
            issues.add(
                ValidationIssue(
                    IssueLevel.WARNING,
                    null,
                    "Dependency graph contains cyclic references; cascading failures may loop"
                )
            )
        }

        // 2. Validate individual components
        for (comp in components) {
            if (comp.name.isBlank()) {
                issues.add(ValidationIssue(IssueLevel.ERROR, comp.id, "Component name is blank"))
            }

            // Sensor validations
            for (sensor in comp.sensors) {
                if (sensor.minNormal >= sensor.maxNormal) {
                    issues.add(
                        ValidationIssue(
                            IssueLevel.ERROR,
                            comp.id,
                            "Sensor '${sensor.metricName}' has invalid range: min (${sensor.minNormal}) >= max (${sensor.maxNormal})"
                        )
                    )
                }
            }

            // Check isolated nodes
            val inDeps = system.dependencyGraph.getDirectDependencies(comp.id)
            val outDeps = system.dependencyGraph.getDirectDependents(comp.id)
            if (inDeps.isEmpty() && outDeps.isEmpty() && components.size > 1) {
                issues.add(
                    ValidationIssue(
                        IssueLevel.INFO,
                        comp.id,
                        "Component '${comp.name}' is isolated with no incoming or outgoing dependencies"
                    )
                )
            }
        }

        val hasErrors = issues.any { it.level == IssueLevel.ERROR }
        return ValidationResult(isValid = !hasErrors, issues = issues)
    }

    fun validateFailureInjection(system: SystemModel?, targetComponentId: String?): ValidationResult {
        if (system == null || targetComponentId == null) {
            return ValidationResult(
                isValid = false,
                issues = listOf(ValidationIssue(IssueLevel.ERROR, null, "System or target component id is null"))
            )
        }

        val comp = system.getComponent(targetComponentId)
            ?: return ValidationResult(
                isValid = false,
                issues = listOf(ValidationIssue(IssueLevel.ERROR, targetComponentId, "Component does not exist in system"))
            )

        val issues = mutableListOf<ValidationIssue>()
        if (comp.state == ComponentState.FAILED || comp.state == ComponentState.OFFLINE) {
            issues.add(
                ValidationIssue(
                    IssueLevel.WARNING,
                    targetComponentId,
                    "Component '${comp.name}' is already failed or offline"
                )
            )
        }

        return ValidationResult(isValid = true, issues = issues)
    }
}
