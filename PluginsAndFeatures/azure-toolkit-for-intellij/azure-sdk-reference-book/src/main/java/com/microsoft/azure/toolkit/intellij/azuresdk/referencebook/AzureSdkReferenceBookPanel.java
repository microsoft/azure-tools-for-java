/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.ui.components.JBScrollPane;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;

import javax.annotation.Nonnull;
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

    public AzureSdkReferenceBookPanel() {
        this.contentPanel.setPreferredSize(new Dimension(840, 600));
        this.initListeners();
    }

    public void init(@Nullable final String featureName) {
        AzureTaskManager.getInstance().runInBackground(AzureString.fromString("loading Azure SDK data"), () -> this.servicesTreePanel.init(featureName));
    }

    private void initListeners() {
        this.servicesTreePanel.setOnSdkFeatureNodeSelected(feature -> this.featureDetailPanel.setData(feature));
    }
}
