package com.systemtwin.plugin;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginRegistryTest {

    private PluginRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PluginRegistry();
    }

    @Test
    @DisplayName("Verify builtin plugins are loaded")
    void testBuiltinPlugins() {
        assertNotNull(registry.getPlugin("SERVER"));
        assertNotNull(registry.getPlugin("DATABASE"));
        assertNotNull(registry.getPlugin("PUMP"));
        assertNotNull(registry.getPlugin("PLC"));
        assertTrue(registry.getSupportedTypes().contains("SERVER"));
    }

    @Test
    @DisplayName("Verify component creation via plugin configures sensors")
    void testPluginComponentCreation() {
        ComponentPlugin serverPlugin = registry.getPlugin("SERVER");
        ComponentModel comp = serverPlugin.createComponent("srv-101", "Edge Server");

        assertEquals("srv-101", comp.getId());
        assertEquals("SERVER", comp.getType());
        assertFalse(comp.getSensors().isEmpty());
        assertTrue(comp.getSensors().stream().anyMatch(s -> s.getMetricName().equalsIgnoreCase("CPU")));
    }

    @Test
    @DisplayName("Verify custom plugin registration")
    void testCustomPluginRegistration() {
        ComponentPlugin customPlugin = new ComponentPlugin() {
            @Override public String getPluginId() { return "plugin-custom-robot"; }
            @Override public String getSupportedType() { return "ROBOT"; }
            @Override public String getDescription() { return "Autonomous assembly robot"; }
            @Override
            public ComponentModel createComponent(String id, String name) {
                return new ComponentModel(id, name, "ROBOT");
            }
            @Override public void onTick(ComponentModel comp, double dt) {}
            @Override public void onStateChange(ComponentModel comp, ComponentState old, ComponentState next) {}
        };

        registry.register(customPlugin);
        assertNotNull(registry.getPlugin("ROBOT"));
        assertEquals("plugin-custom-robot", registry.getPlugin("ROBOT").getPluginId());
    }
}
