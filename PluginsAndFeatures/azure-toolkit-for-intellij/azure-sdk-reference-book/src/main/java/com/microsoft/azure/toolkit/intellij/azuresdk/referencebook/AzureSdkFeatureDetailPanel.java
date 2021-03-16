/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTabbedPane;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkFeatureEntity;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import javax.swing.*;
import java.util.List;
import java.util.Optional;

public class AzureSdkFeatureDetailPanel {
    private JBLabel titleLabel;
    private JPanel descPanel;
    @Getter
    private JPanel contentPanel;
    private JBTabbedPane tabPane;
    private HyperlinkLabel featureDocLink;
    private JBLabel descLabel;

    public void setData(final AzureSdkFeatureEntity feature) {
        AzureTaskManager.getInstance().runLater(() -> {
            this.titleLabel.setText(feature.getName());
            SwingUtils.setTextAndEnableAutoWrap(this.descLabel, feature.getDescription());

            Optional.ofNullable(feature.getMsdocs()).ifPresent(link -> {
                this.featureDocLink.setHyperlinkText("Product documentation");
                this.featureDocLink.setHyperlinkTarget(link);
            });

            this.buildTabs(feature);
        });
    }

    private void buildTabs(AzureSdkFeatureEntity feature) {
        final List<AzureSdkArtifactEntity> clientArtifacts = feature.getClientArtifacts();
        final List<AzureSdkArtifactEntity> springArtifacts = feature.getSpringArtifacts();
        final List<AzureSdkArtifactEntity> managementArtifacts = feature.getManagementArtifacts();
        this.tabPane.removeAll();
        if (CollectionUtils.isNotEmpty(clientArtifacts)) {
            final AzureSdkArtifactGroupPanel clientPanel = new AzureSdkArtifactGroupPanel();
            this.tabPane.insertTab("Client SDK", null, clientPanel.getContentPanel(), "", this.tabPane.getTabCount());
            clientPanel.setData(clientArtifacts);
        }
        if (CollectionUtils.isNotEmpty(springArtifacts)) {
            final AzureSdkArtifactGroupPanel springPanel = new AzureSdkArtifactGroupPanel();
            this.tabPane.insertTab("Spring SDK", null, springPanel.getContentPanel(), "", this.tabPane.getTabCount());
            springPanel.setData(springArtifacts);
        }
        if (CollectionUtils.isNotEmpty(managementArtifacts)) {
            final AzureSdkArtifactGroupPanel managementSdkPanel = new AzureSdkArtifactGroupPanel();
            this.tabPane.insertTab("Management SDK", null, managementSdkPanel.getContentPanel(), "", this.tabPane.getTabCount());
            managementSdkPanel.setData(managementArtifacts);
        }
    }
}
