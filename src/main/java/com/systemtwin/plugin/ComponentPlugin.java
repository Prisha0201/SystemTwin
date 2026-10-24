package com.systemtwin.plugin;

import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.ComponentState;

/**
 * Service Provider Interface (SPI) for extensible component types in SystemTwin.
 */
public interface ComponentPlugin {

    String getPluginId();

    String getSupportedType();

    String getDescription();

    ComponentModel createComponent(String id, String name);

    void onTick(ComponentModel component, double deltaTime);

    void onStateChange(ComponentModel component, ComponentState oldState, ComponentState newState);
}
