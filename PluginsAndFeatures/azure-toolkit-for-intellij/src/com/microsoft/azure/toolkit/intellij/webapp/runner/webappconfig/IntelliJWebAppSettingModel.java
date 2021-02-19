/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.runner.webappconfig;

import com.microsoft.azuretools.core.mvp.model.webapp.WebAppSettingModel;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactType;

public class IntelliJWebAppSettingModel extends WebAppSettingModel {

    private AzureArtifactType azureArtifactType;
    private boolean openBrowserAfterDeployment = true;
    private boolean slotPanelVisible = false;
    private String artifactIdentifier;
    private String packaging;

    public String getArtifactIdentifier() {
        return artifactIdentifier;
    }

    public void setArtifactIdentifier(final String artifactIdentifier) {
        this.artifactIdentifier = artifactIdentifier;
    }

    public String getPackaging() {
        return packaging;
    }

    public void setPackaging(final String packaging) {
        this.packaging = packaging;
    }

    public boolean isOpenBrowserAfterDeployment() {
        return openBrowserAfterDeployment;
    }

    public void setOpenBrowserAfterDeployment(boolean openBrowserAfterDeployment) {
        this.openBrowserAfterDeployment = openBrowserAfterDeployment;
    }

    public boolean isSlotPanelVisible() {
        return slotPanelVisible;
    }

    public void setSlotPanelVisible(boolean slotPanelVisible) {
        this.slotPanelVisible = slotPanelVisible;
    }

    public AzureArtifactType getAzureArtifactType() {
        return azureArtifactType;
    }

    public void setAzureArtifactType(final AzureArtifactType azureArtifactType) {
        this.azureArtifactType = azureArtifactType;
    }
}
