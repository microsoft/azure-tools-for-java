/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SpringCloudAppInstancesPanel extends JPanel {
    @Getter
    private JPanel contentPanel;
    private JBTable tableInstances;

    public SpringCloudAppInstancesPanel() {
        super();
        this.init();
    }

    private void init() {
        final DefaultTableModel model = new DefaultTableModel() {
            public boolean isCellEditable(int var1, int var2) {
                return false;
            }
        };
        model.addColumn("App Instances Name");
        model.addColumn("Status");
        model.addColumn("Discover Status");
        this.tableInstances.setModel(model);
        this.tableInstances.setRowSelectionAllowed(true);
        this.tableInstances.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.tableInstances.getEmptyText().setText("Loading instances");
    }

    public void setApp(@Nonnull SpringCloudApp app) {
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runOnPooledThread(() -> {
            final List<SpringCloudAppInstance> instances = Optional.ofNullable(app.getActiveDeployment())
                .or(() -> Optional.ofNullable(app.deployments().get("default", app.getResourceGroupName())))
                .map(SpringCloudDeployment::getInstances)
                .orElse(new ArrayList<>());
            manager.runLater(() -> {
                final DefaultTableModel model = (DefaultTableModel) this.tableInstances.getModel();
                model.setRowCount(0);
                instances.forEach(i -> model.addRow(new Object[]{i.getName(), i.getStatus(), i.getDiscoveryStatus()}));
                final int rows = model.getRowCount() < 5 ? 5 : instances.size();
                model.setRowCount(rows);
                this.tableInstances.setVisibleRowCount(rows);
            });
        });
    }

    public void setEnabled(boolean enable) {
        tableInstances.setEnabled(enable);
    }

    private void createUIComponents() {
    }
}
