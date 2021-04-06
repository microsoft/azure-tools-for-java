/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2019-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.arm;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.azuretools.utils.CanceledByUserException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class ResourceManagementModule extends AzureRefreshableNode implements ResourceManagementModuleView {

    private static final String RESOURCE_MANAGEMENT_MODULE_ID = ResourceManagementModule.class.getName();
    private static final String ICON_PATH = "ResourceManagement/ResourceManagement.svg";
    public static final String MODULE_NAME = "Resource Management";
    private final ResourceManagementModulePresenter<ResourceManagementModule> rmModulePresenter;
    public static final Object listenerObj = new Object();

    public ResourceManagementModule(Node parent) {
        super(RESOURCE_MANAGEMENT_MODULE_ID, MODULE_NAME, parent, ICON_PATH);
        rmModulePresenter = new ResourceManagementModulePresenter<>();
        rmModulePresenter.onAttachView(ResourceManagementModule.this);
        createListener();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.ResourceManagement.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        try {
            rmModulePresenter.onModuleRefresh();
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
