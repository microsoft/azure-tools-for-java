/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.google.common.hash.Hashing;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

public interface DeploymentSourceForm extends AzureFormJPanel<ContainerAppDraft.ImageConfig> {
    void setContainerApp(ContainerApp containerApp);

    ContainerApp getContainerApp();

    default String getDefaultFullImageName() {
        final ContainerApp app = this.getContainerApp();
        return generateDefaultFullImageNameFromApp(app);
    }

    static String generateDefaultFullImageNameFromApp(ContainerApp app) {
        final String originalString = app.getSubscriptionId() + "/" + app.getResourceGroupName() + "/" + Optional.ofNullable(app.getManagedEnvironment()).map(AbstractAzResource::getName).orElse(app.getName());
        final String registryName = "ca" + Hashing.sha256().hashString(originalString, StandardCharsets.UTF_8).toString().substring(0, 10) + "acr";  // ACR names must start + end in a letter
        final String tagNowSuffix = LocalDateTime.now().toString().replace(" ", "").replace("T", "").replace("-", "").replace(".", "").replace(":", "").toLowerCase();
        final String imageNameWithTag = app.getName() + ":aca-" + tagNowSuffix.substring(0, 17);
        return String.format("%s.azurecr.io/%s", registryName, imageNameWithTag);
    }
}
