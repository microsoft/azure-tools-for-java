/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2020 JetBrains s.r.o.
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

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodeBase;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodePresenterBase;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.slot.DeploymentSlotNodeView;
import org.jetbrains.annotations.NotNull;

public class DeploymentSlotNode extends DeploymentSlotNodeBase<DeploymentSlot> implements DeploymentSlotNodeView {

    private final DeploymentSlotNodePresenterBase<DeploymentSlot, DeploymentSlotNodeView> myPresenter;

    public DeploymentSlotNode(final String slotId, final String webAppId, final String webAppName,
                              final DeploymentSlotModule parent, final String name, final String state, final String os,
                              final String subscriptionId, final String hostName) {
        super(webAppId, webAppName, parent, slotId, name, state, os, subscriptionId, hostName);
        this.myPresenter = new DeploymentSlotNodePresenter<>();
        this.myPresenter.onAttachView(this);
    }

    @NotNull
    @Override
    public DeploymentSlotNodePresenterBase<DeploymentSlot, DeploymentSlotNodeView> getPresenter() {
        return myPresenter;
    }

    @Override
    public void openDeploymentSlotPropertyAction() {
        DefaultLoader.getUIHelper().openDeploymentSlotPropertyView(this);
    }
}
