package com.systemtwin.ui;

import com.systemtwin.graph.CriticalityAnalyzer;
import com.systemtwin.model.ComponentModel;
import com.systemtwin.model.SystemModel;
import com.systemtwin.service.SystemService;
import com.systemtwin.service.TemplateService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

/**
 * Controller for the interactive System Builder and graph topology designer.
 */
public class BuilderController {

    @FXML private TextField compIdField;
    @FXML private TextField compNameField;
    @FXML private ComboBox<String> compTypeCombo;

    @FXML private ComboBox<String> depSourceCombo;
    @FXML private ComboBox<String> depTargetCombo;

    @FXML private TableView<ComponentModel> componentsTable;
    @FXML private TableColumn<ComponentModel, String> colId;
    @FXML private TableColumn<ComponentModel, String> colName;
    @FXML private TableColumn<ComponentModel, String> colType;
    @FXML private TableColumn<ComponentModel, String> colState;

    @FXML private TableView<CriticalityAnalyzer.CriticalityReport> criticalityTable;
    @FXML private TableColumn<CriticalityAnalyzer.CriticalityReport, String> critNameCol;
    @FXML private TableColumn<CriticalityAnalyzer.CriticalityReport, Double> critScoreCol;
    @FXML private TableColumn<CriticalityAnalyzer.CriticalityReport, Integer> critBlastCol;
    @FXML private TableColumn<CriticalityAnalyzer.CriticalityReport, Boolean> critSpofCol;

    private SystemService systemService;
    private TemplateService templateService;
    private final CriticalityAnalyzer criticalityAnalyzer = new CriticalityAnalyzer();

    private final ObservableList<ComponentModel> componentsData = FXCollections.observableArrayList();
    private final ObservableList<CriticalityAnalyzer.CriticalityReport> criticalityData = FXCollections.observableArrayList();

    public void initServices(SystemService sysService, TemplateService tplService) {
        this.systemService = sysService;
        this.templateService = tplService;
        refreshView();
    }

    @FXML
    public void initialize() {
        if (compTypeCombo != null) {
            compTypeCombo.getItems().addAll("SERVER", "DATABASE", "PUMP", "PLC", "ROBOT", "GATEWAY", "HVAC");
            compTypeCombo.getSelectionModel().selectFirst();
        }

        if (componentsTable != null) {
            colId.setCellValueFactory(new PropertyValueFactory<>("id"));
            colName.setCellValueFactory(new PropertyValueFactory<>("name"));
            colType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colState.setCellValueFactory(new PropertyValueFactory<>("state"));
            componentsTable.setItems(componentsData);
        }

        if (criticalityTable != null) {
            critNameCol.setCellValueFactory(new PropertyValueFactory<>("componentName"));
            critScoreCol.setCellValueFactory(new PropertyValueFactory<>("criticalityScore"));
            critBlastCol.setCellValueFactory(new PropertyValueFactory<>("blastRadius"));
            critSpofCol.setCellValueFactory(new PropertyValueFactory<>("singlePointOfFailure"));
            criticalityTable.setItems(criticalityData);
        }
    }

    @FXML
    public void handleAddComponent() {
        if (systemService == null) return;
        SystemModel sys = systemService.getActiveSystem();
        if (sys == null) return;

        String id = compIdField != null ? compIdField.getText() : null;
        String name = compNameField != null ? compNameField.getText() : null;
        String type = compTypeCombo != null ? compTypeCombo.getValue() : "SERVER";

        if (id != null && !id.isBlank() && name != null && !name.isBlank()) {
            systemService.addComponentFromPlugin(sys, id.trim(), name.trim(), type);
            refreshView();
            if (compIdField != null) compIdField.clear();
            if (compNameField != null) compNameField.clear();
        }
    }

    @FXML
    public void handleAddDependency() {
        if (systemService == null) return;
        SystemModel sys = systemService.getActiveSystem();
        if (sys == null) return;

        String src = depSourceCombo != null ? depSourceCombo.getValue() : null;
        String tgt = depTargetCombo != null ? depTargetCombo.getValue() : null;

        if (src != null && tgt != null && !src.equals(tgt)) {
            systemService.addDependency(sys, src, tgt);
            refreshView();
        }
    }

    public void refreshView() {
        if (systemService == null) return;
        SystemModel sys = systemService.getActiveSystem();
        if (sys == null) return;

        componentsData.setAll(sys.getComponents().values());

        List<CriticalityAnalyzer.CriticalityReport> reports = criticalityAnalyzer.analyze(sys.getDependencyGraph());
        criticalityData.setAll(reports);

        if (depSourceCombo != null && depTargetCombo != null) {
            List<String> ids = sys.getComponents().keySet().stream().toList();
            depSourceCombo.getItems().setAll(ids);
            depTargetCombo.getItems().setAll(ids);
        }
    }
}
