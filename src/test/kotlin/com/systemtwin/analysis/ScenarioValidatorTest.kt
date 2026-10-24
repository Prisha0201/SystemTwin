package com.systemtwin.analysis

import com.systemtwin.model.ComponentModel
import com.systemtwin.model.ComponentState
import com.systemtwin.model.Sensor
import com.systemtwin.model.SystemModel
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ScenarioValidatorTest {

    private lateinit var validator: ScenarioValidator
    private lateinit var system: SystemModel

    @BeforeEach
    fun setUp() {
        validator = ScenarioValidator()
        system = SystemModel("sys-valid-test", "Valid System", "Test validation system")
    }

    @Test
    fun `test valid system returns successful validation result`() {
        val srv1 = ComponentModel("srv-1", "Server Alpha", "SERVER")
        srv1.addSensor(Sensor("s1", "srv-1", "CPU", "%", 0.0, 100.0))
        val srv2 = ComponentModel("srv-2", "Server Beta", "SERVER")
        srv2.addSensor(Sensor("s2", "srv-2", "CPU", "%", 0.0, 100.0))

        system.addComponent(srv1)
        system.addComponent(srv2)
        system.addDependency("srv-1", "srv-2")

        val result = validator.validateSystem(system)
        assertTrue(result.isValid)
        assertEquals(0, result.errorCount)
    }

    @Test
    fun `test invalid sensor range reports error`() {
        val srv = ComponentModel("srv-err", "Server Bad Sensor", "SERVER")
        srv.addSensor(Sensor("s-err", "srv-err", "CPU", "%", 90.0, 10.0)) // min > max

        system.addComponent(srv)
        val result = validator.validateSystem(system)

        assertFalse(result.isValid)
        assertTrue(result.errorCount > 0)
        assertTrue(result.issues.any { it.message.contains("invalid range") })
    }

    @Test
    fun `test cyclic dependency graph reports warning`() {
        val n1 = ComponentModel("n1", "Node 1", "SERVER")
        val n2 = ComponentModel("n2", "Node 2", "SERVER")

        system.addComponent(n1)
        system.addComponent(n2)
        system.addDependency("n1", "n2")
        system.addDependency("n2", "n1") // cycle

        val result = validator.validateSystem(system)
        assertTrue(result.warningCount > 0)
        assertTrue(result.issues.any { it.message.contains("cyclic") })
    }

    @Test
    fun `test failure injection validation on existing component`() {
        val comp = ComponentModel("target-1", "Target Node", "SERVER")
        system.addComponent(comp)

        val result = validator.validateFailureInjection(system, "target-1")
        assertTrue(result.isValid)

        comp.state = ComponentState.FAILED
        val failedResult = validator.validateFailureInjection(system, "target-1")
        assertTrue(failedResult.warningCount > 0)
    }
}
