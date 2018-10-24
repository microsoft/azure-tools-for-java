/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBasePropertyViewPresenter;

public class DeploymentSlotPropertyViewPresenter extends WebAppBasePropertyViewPresenter {
    @Override
    protected void updateAppSettings(@NotNull final String sid, @NotNull final String webAppId,
                                     @Nullable final String name, final Map toUpdate,
                                     final Set toRemove) throws Exception {
        AzureWebAppMvpModel.getInstance().updateDeploymentSlotAppSettings(sid, webAppId, name, toUpdate, toRemove);
    }

    @Override
    protected boolean getPublishingProfile(@NotNull final String subscriptionId, @NotNull final String webAppId,
                                           @Nullable final String name,
                                           @NotNull final String filePath) throws Exception {
        return AzureWebAppMvpModel.getInstance()
            .getSlotPublishingProfileXmlWithSecrets(subscriptionId, webAppId, name, filePath);
    }

    @Override
    protected WebAppBase getWebAppBase(@NotNull final String sid, @NotNull final String webAppId,
                                       @Nullable final String name) throws Exception {
        return AzureWebAppMvpModel.getInstance().getWebAppById(sid, webAppId).deploymentSlots().getByName(name);
    }
}
