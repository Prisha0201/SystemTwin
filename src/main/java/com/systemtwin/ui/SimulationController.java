package com.systemtwin.ui;

import com.systemtwin.engine.SimulationScheduler;
import com.systemtwin.events.EventBus;
import com.systemtwin.graph.FailurePropagator;
import com.systemtwin.model.SimulationEvent;
import com.systemtwin.model.SystemModel;
import com.systemtwin.service.SimulationService;
import com.systemtwin.service.SystemService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

/**
 * Controller for simulation lifecycle execution, speed control, and fault injection.
 */
public class SimulationController {

    @FXML private Button startBtn;
    @FXML private Button pauseBtn;
    @FXML private Button resumeBtn;
    @FXML private Button stepBtn;
    @FXML private Button stopBtn;

    @FXML private Slider speedSlider;
    @FXML private Label speedLabel;

    @FXML private ComboBox<String> componentFaultCombo;
    @FXML private Button injectFailureBtn;
    @FXML private Button injectDegradeBtn;
    @FXML private Button recoverBtn;

    @FXML private ListView<String> eventLogView;

    private SimulationService simulationService;
    private SystemService systemService;
    private EventBus eventBus;

    private final ObservableList<String> eventLogData = FXCollections.observableArrayList();

    public void initServices(SimulationService simService, SystemService sysService, EventBus eventBus) {
        this.simulationService = simService;
        this.systemService = sysService;
        this.eventBus = eventBus;

        if (eventBus != null) {
            eventBus.subscribe(SimulationEvent.class, event -> {
                Platform.runLater(() -> {
                    eventLogData.add(0, String.format("[%s] %s", event.getEventType(), event.getDescription()));
                    if (eventLogData.size() > 200) {
                        eventLogData.remove(eventLogData.size() - 1);
                    }
                });
            });
        }
        refreshComponents();
    }

    @FXML
    public void initialize() {
        if (eventLogView != null) {
            eventLogView.setItems(eventLogData);
        }

        if (speedSlider != null) {
            speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                double speed = Math.round(newVal.doubleValue() * 10.0) / 10.0;
                if (speedLabel != null) speedLabel.setText(speed + "x");
                if (simulationService != null) simulationService.setSpeed(speed);
            });
        }
    }

    @FXML
    public void handleStart() {
        if (simulationService == null || systemService == null) return;
        SystemModel sys = systemService.getActiveSystem();
        if (sys != null) {
            simulationService.start(sys, "Live-Run-" + System.currentTimeMillis() % 1000, 42L);
        }
    }

    @FXML
    public void handlePause() {
        if (simulationService != null) simulationService.pause();
    }

    @FXML
    public void handleResume() {
        if (simulationService != null) simulationService.resume();
    }

    @FXML
    public void handleStep() {
        if (simulationService != null) simulationService.step();
    }

    @FXML
    public void handleStop() {
        if (simulationService != null) simulationService.stop();
    }

    @FXML
    public void handleInjectFailure() {
        if (simulationService == null || componentFaultCombo == null) return;
        String compId = componentFaultCombo.getValue();
        if (compId != null) {
            List<FailurePropagator.CascadeStep> steps = simulationService.injectFailure(compId, true);
            for (FailurePropagator.CascadeStep step : steps) {
                eventLogData.add(0, "🚨 " + step.toString());
            }
        }
    }

    @FXML
    public void handleInjectDegradation() {
        if (simulationService == null || componentFaultCombo == null) return;
        String compId = componentFaultCombo.getValue();
        if (compId != null) {
            simulationService.injectDegradation(compId);
            eventLogData.add(0, "⚠ Degradation applied to " + compId);
        }
    }

    @FXML
    public void handleRecover() {
        if (simulationService == null || componentFaultCombo == null) return;
        String compId = componentFaultCombo.getValue();
        if (compId != null) {
            simulationService.recoverComponent(compId);
            eventLogData.add(0, "✅ Recovery initiated for " + compId);
        }
    }

    public void refreshComponents() {
        if (systemService != null && componentFaultCombo != null) {
            SystemModel sys = systemService.getActiveSystem();
            if (sys != null) {
                componentFaultCombo.getItems().setAll(sys.getComponents().keySet());
                if (!componentFaultCombo.getItems().isEmpty()) {
                    componentFaultCombo.getSelectionModel().selectFirst();
                }
            }
        }
    }
}
