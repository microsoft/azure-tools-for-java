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

import com.microsoft.azure.management.appservice.DeploymentSlot;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;

import java.util.List;

public class DeploymentSlotModule extends AzureRefreshableNode implements DeploymentSlotModuleView {
    private static final String MODULE_ID = WebAppModule.class.getName();
    private static final String ICON_PATH = "Slot_16.png";
    private static final String MODULE_NAME = "Deployment Slots";

    private final DeploymentSlotModulePresenter presenter;
    protected final String subscriptionId;
    protected final WebApp webapp;

    public DeploymentSlotModule(final Node parent, final String subscriptionId, final WebApp webapp) {
        super(MODULE_ID, MODULE_NAME, parent, ICON_PATH);
        this.subscriptionId = subscriptionId;
        this.webapp = webapp;
        presenter = new DeploymentSlotModulePresenter<>();
        presenter.onAttachView(this);
    }

    @Override
    public void removeNode(final String sid, final String name, Node node) {
        try {
            presenter.onDeleteDeploymentSlot(sid, this.webapp.id(), name);
            removeDirectChildNode(node);
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete the Deployment Slot",
                                                      e, "Azure Services Explorer - Error Deleting Deployment Slot", false, true);
        }
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        try {
            presenter.onRefreshDeploymentSlotModule(this.subscriptionId, this.webapp.id());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void renderDeploymentSlots(@NotNull final List<DeploymentSlot> slots) {
        slots.forEach(slot -> addChildNode(
            new DeploymentSlotNode(slot.id(), slot.parent().id(), slot.parent().name(),
                                   this, slot.name(), slot.state(), slot.operatingSystem().toString(),
                                   this.subscriptionId, slot.defaultHostName())));
    }
}
