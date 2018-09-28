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

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;

public class DeploymentSlotNode extends WebAppBaseNode implements DeploymentSlotNodeView {
    private static final String ACTION_SWAP_WITH_PRODUCTION = "Swap with production";
    private static final String LABEL = "Slot";
    private final DeploymentSlotNodePresenter presenter;
    protected final String webAppId;
    protected final String slotName;


    public DeploymentSlotNode(final String slotId, final String webAppId, final DeploymentSlotModule parent,
                              final String name, final String state, final String os,
                              final String subscriptionId, final String hostName) {
        super(slotId, name, LABEL, parent,subscriptionId, hostName, os, state);
        this.webAppId = webAppId;
        this.slotName = name;
        this.presenter = new DeploymentSlotNodePresenter();
        this.presenter.onAttachView(this);
    }

    @Override
    protected void loadActions() {
        // todo
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        // todo
    }
}
