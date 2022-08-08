/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

public class AzureSdkReferenceBookPanel {
    @Getter
    private JPanel contentPanel;
    private AzureSdkTreePanel servicesTreePanel;
    private JBScrollPane rightPane;
    private AzureSdkFeatureDetailPanel featureDetailPanel;
    private JPanel leftPanel;

    private final Project project;

    public AzureSdkReferenceBookPanel(@Nullable Project project) {
        this.project = project;
        $$$setupUI$$$();
        this.contentPanel.setPreferredSize(new Dimension(960, 600));
        this.initListeners();
    }

    public void init(@Nullable final String featureName) {
        AzureTaskManager.getInstance().runInBackground(AzureString.fromString("loading Azure SDK data"), () -> this.servicesTreePanel.init(featureName));
    }

    private void initListeners() {
        this.servicesTreePanel.setOnSdkFeatureNodeSelected(feature -> this.featureDetailPanel.setData(feature));
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.featureDetailPanel = new AzureSdkFeatureDetailPanel(project);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
