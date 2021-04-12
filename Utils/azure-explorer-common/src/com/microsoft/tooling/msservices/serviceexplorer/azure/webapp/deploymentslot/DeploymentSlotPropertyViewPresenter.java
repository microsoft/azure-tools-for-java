/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

import java.util.Map;
import java.util.Set;

public class DeploymentSlotPropertyViewPresenter extends WebAppBasePropertyViewPresenter {
    @Override
    protected void updateAppSettings(@NotNull final String sid, @NotNull final String webAppId,
                                     @Nullable final String name, final Map toUpdate,
                                     final Set toRemove) {
        AzureWebAppMvpModel.getInstance().updateDeploymentSlotAppSettings(sid, webAppId, name, toUpdate, toRemove);
    }

    @Override
    protected boolean getPublishingProfile(@NotNull final String subscriptionId, @NotNull final String webAppId,
                                           @Nullable final String name,
                                           @NotNull final String filePath) {
        return AzureWebAppMvpModel.getInstance()
            .getSlotPublishingProfileXmlWithSecrets(subscriptionId, webAppId, name, filePath);
    }

    @Override
    protected WebAppBase getWebAppBase(@NotNull final String sid, @NotNull final String webAppId,
                                       @Nullable final String name) {
        return AzureWebAppMvpModel.getInstance().getWebAppById(sid, webAppId).deploymentSlots().getByName(name);
    }
}
