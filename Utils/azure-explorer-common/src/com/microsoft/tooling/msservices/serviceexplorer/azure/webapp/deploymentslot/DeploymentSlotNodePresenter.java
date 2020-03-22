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

import java.io.IOException;

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;

public class DeploymentSlotNodePresenter<V extends DeploymentSlotNodeView> extends MvpPresenter<V> {
    public void onStartDeploymentSlot(final String subscriptionId, final String webAppId,
                                      final String slotName) throws IOException {
        AzureWebAppMvpModel.getInstance().startDeploymentSlot(subscriptionId, webAppId, slotName);
        final DeploymentSlotNodeView view = getMvpView();
        if (!isViewDetached()) {
            view.renderNode(WebAppBaseState.RUNNING);
        }
    }

    public void onStopDeploymentSlot(final String subscriptionId, final String webAppId,
                                     final String slotName) throws IOException {
        AzureWebAppMvpModel.getInstance().stopDeploymentSlot(subscriptionId, webAppId, slotName);
        final DeploymentSlotNodeView view = getMvpView();
        if (!isViewDetached()) {
            view.renderNode(WebAppBaseState.STOPPED);
        }
    }

    public void onRestartDeploymentSlot(final String subscriptionId, final String webAppId,
                                        final String slotName) throws IOException {
        AzureWebAppMvpModel.getInstance().restartDeploymentSlot(subscriptionId, webAppId, slotName);
        final DeploymentSlotNodeView view = getMvpView();
        if (!isViewDetached()) {
            view.renderNode(WebAppBaseState.RUNNING);
        }
    }

    public void onRefreshNode(final String subscriptionId, final String webAppId,
                              final String slotName) throws Exception {
        final WebApp app = AzureWebAppMvpModel.getInstance().getWebAppById(subscriptionId, webAppId);
        final DeploymentSlot slot = app.deploymentSlots().getByName(slotName);
        final DeploymentSlotNodeView view = getMvpView();
        if (!isViewDetached()) {
            view.renderNode(WebAppBaseState.fromString(slot.state()));
        }
    }

    public void onSwapWithProduction(final String subscriptionId, final String webAppId,
                                     final String slotName) throws IOException {
        AzureWebAppMvpModel.getInstance().swapSlotWithProduction(subscriptionId, webAppId, slotName);
    }
}
