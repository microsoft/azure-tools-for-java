package com.microsoft.azure.toolkit.intellij.connector;

import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBoxSimple;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class ResourcesPanel<T extends Resource> implements AzureFormPanel<T> {
    private final Supplier<? extends List<ResourceDefinition<? extends Resource>>> definitionsSupplier;
    private final Project project;
    @Getter
    private JPanel contentPanel;
    @Getter
    private AzureComboBox<ResourceDefinition<? extends Resource>> typeSelector;
    private JPanel detailContainer;
    private AzureFormJPanel<T> detailPanel;

    public ResourcesPanel(Project project, Supplier<? extends List<ResourceDefinition<? extends Resource>>> typesSupplier) {
        this.project = project;
        this.definitionsSupplier = typesSupplier;
        this.init();
    }

    private void init() {
        this.contentPanel.setMaximumSize(new Dimension(800, 800));
        this.typeSelector.setMaximumSize(new Dimension(300, 50));
        this.typeSelector.addItemListener(this::onTypeChanged);
    }

    private void onTypeChanged(ItemEvent itemEvent) {
        final ResourceDefinition<? extends Resource> definition = this.typeSelector.getValue();
        final AzureFormJPanel<? extends Resource> panel = definition.getResourcesPanel(definition.getType(), this.project);
        if (Objects.nonNull(panel)) {
            this.detailPanel = (AzureFormJPanel<T>) panel;
            final GridConstraints constraints = new GridConstraints();
            constraints.setUseParentLayout(true);
            constraints.setFill(GridConstraints.FILL_HORIZONTAL);
            this.detailContainer.add(panel.getContentPanel(), constraints);
        }
    }

    @Override
    public void setData(T data) {
        this.detailPanel.setData(data);
    }

    @Override
    public T getData() {
        return this.detailPanel.getData();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return this.detailPanel.getInputs();
    }

    private void createUIComponents() {
        this.typeSelector = new AzureComboBoxSimple<>(this.definitionsSupplier);
    }
}
