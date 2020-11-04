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

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.*;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.io.IOException;
import java.util.List;

public class ResourceManagementModule extends AzureRefreshableNode implements ResourceManagementModuleView {

    private static final String RESOURCE_MANAGEMENT_MODULE_ID = ResourceManagementModule.class.getName();
    private static final String ICON_PATH = "arm_resourcegroup.png";
    private static final String BASE_MODULE_NAME = "Resource Management";
    private final ResourceManagementModulePresenter<ResourceManagementModule> rmModulePresenter;
    public static final Object listenerObj = new Object();

    public ResourceManagementModule(Node parent) {
        super(RESOURCE_MANAGEMENT_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
        rmModulePresenter = new ResourceManagementModulePresenter<>();
        rmModulePresenter.onAttachView(ResourceManagementModule.this);
        createListener();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        try {
            rmModulePresenter.onModuleRefresh();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        } catch (final CanceledByUserException e) {
            DefaultLoader.getUIHelper().showWarningNotification("Refreshing cancelled", "You canceled refreshing resource groups.");
        }
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        EventUtil.executeWithLog(TelemetryConstants.ARM, TelemetryConstants.DELETE_RESOURCE_GROUP, (operation -> {
            rmModulePresenter.onDeleteResourceGroup(sid, id);
            removeDirectChildNode(node);
        }), (e) -> {
            DefaultLoader.getUIHelper()
                    .showException("An error occurred while attempting to delete the resource group",
                            e, "Azure Explorer - Error Deleting Resource Group", false, true);
        });
    }

    @Override
    public void renderChildren(List<ResourceEx<ResourceGroup>> resourceExes) {
        for (final ResourceEx<ResourceGroup> resourceEx : resourceExes) {
            final ResourceGroup rg = resourceEx.getResource();
            final ResourceManagementNode node = new ResourceManagementNode(this, resourceEx.getSubscriptionId(), rg);
            addChildNode(node);
        }
    }

    private void createListener() {
        String id = "ResourceManagementModule";
        AzureUIRefreshListener listener = new AzureUIRefreshListener() {
            @Override
            public void run() {
                if (event.opsType == AzureUIRefreshEvent.EventType.SIGNIN || event.opsType == AzureUIRefreshEvent
                        .EventType.SIGNOUT) {
                    removeAllChildNodes();
                } else if (event.object == listenerObj && (event.opsType == AzureUIRefreshEvent.EventType.UPDATE || event
                        .opsType == AzureUIRefreshEvent.EventType.REMOVE)) {
                    if (hasChildNodes()) {
                        load(true);
                    }
                } else if (event.object == listenerObj && event.opsType == AzureUIRefreshEvent.EventType.REFRESH) {
                    load(true);
                } else if (event.object instanceof String && event.opsType == AzureUIRefreshEvent.EventType.REFRESH) {
                    String rgName = (String) event.object;
                    ResourceManagementNode rgNode = findRgNode(rgName);
                    if (rgNode != null) {
                        rgNode.load(true);
                    }
                }
            }
        };
        AzureUIRefreshCore.addListener(id, listener);
    }

    private ResourceManagementNode findRgNode(String rgName) {
        try {
            ResourceManagementNode rgNode = findRgNodeLocal(rgName);
            if (rgNode != null) {
                return rgNode;
            }
            load(true).get();
            return findRgNodeLocal(rgName);
        } catch (Exception ignore) {
            return null;
        }
    }

    private ResourceManagementNode findRgNodeLocal(String rgName) {
        for (Node rgNode : getChildNodes()) {
            if (((ResourceManagementNode) rgNode).getRgName().equals(rgName)) {
                return (ResourceManagementNode) rgNode;
            }
        }
        return null;
    }
}
