/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.webapp;

import com.microsoft.azure.toolkit.ide.appservice.AppServiceDeploymentSlotsNodeView;
import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileNode;
import com.microsoft.azure.toolkit.ide.appservice.node.AppSettingsNode;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.*;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class WebAppNodeProvider implements IExplorerNodeProvider {
    public static final AzureIconProvider<AppServiceAppBase<?, ?, ?>> WEBAPP_ICON_PROVIDER =
        new AzureResourceIconProvider<AppServiceAppBase<?, ?, ?>>()
            .withModifier(WebAppNodeProvider::getOperatingSystemModifier)
            .withModifier(app -> new AzureIcon.Modifier("webapp", AzureIcon.ModifierLocation.OTHER));

    private static final String NAME = "Web Apps";
    private static final String ICON = AzureIcons.WebApp.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureWebApp.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureWebApp ||
            data instanceof WebApp ||
            data instanceof AppServiceFile;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureWebApp) {
            final AzureWebApp service = Azure.az(AzureWebApp.class);
            return new Node<>(service)
                .view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(WebAppActionsContributor.SERVICE_ACTIONS)
                .addChildren(AzureWebApp::webApps, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof WebApp) {
            final WebApp webApp = (WebApp) data;
            return new Node<>(webApp)
                .view(new AzureResourceLabelView<>(webApp, WebApp::getStatus, WEBAPP_ICON_PROVIDER))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addInlineAction(ResourceCommonActionsContributor.DEPLOY)
                .actions(WebAppActionsContributor.WEBAPP_ACTIONS)
                .addChildren(WebApp::getSubModules, (module, webAppNode) -> createNode(module, webAppNode, manager))
                .addChild(AppServiceFileNode::getRootFileNodeForAppService, (d, p) -> this.createNode(d, p, manager)) // Files
                .addChild(AppServiceFileNode::getRootLogNodeForAppService, (d, p) -> this.createNode(d, p, manager))
                .addChild(ignore -> new AppSettingsNode(webApp));
        } else if (data instanceof WebAppDeploymentSlotModule) {
            final WebAppDeploymentSlotModule module = (WebAppDeploymentSlotModule) data;
            return new Node<>(module)
                .view(new AzureModuleLabelView<>(module, "Deployment Slots", AzureIcons.WebApp.DEPLOYMENT_SLOT.getIconPath()))
                .actions(WebAppActionsContributor.DEPLOYMENT_SLOTS_ACTIONS)
                .addChildren(WebAppDeploymentSlotModule::list, (d, p) -> this.createNode(d, p, manager))
                .hasMoreChildren(AbstractAzResourceModule::hasMoreResources)
                .loadMoreChildren(AbstractAzResourceModule::loadMoreResources);
        } else if (data instanceof WebAppDeploymentSlot) {
            final WebAppDeploymentSlot slot = (WebAppDeploymentSlot) data;
            return new Node<>(slot)
                .view(new AzureResourceLabelView<>(slot))
                .actions(WebAppActionsContributor.DEPLOYMENT_SLOT_ACTIONS);
        } else if (data instanceof AppServiceFile) {
            final AppServiceFile file = (AppServiceFile) data;
            return new AppServiceFileNode(file);
        } else if (data instanceof ServiceLinkerModule) {
            final ServiceLinkerModule module = (ServiceLinkerModule) data;
            return new Node<>(module)
                    .view(new AzureModuleLabelView<>(module, "Service Connector", AzureIcons.Connector.SERVICE_LINKER_MODULE.getIconPath()))
                    .actions(ResourceCommonActionsContributor.SERVICE_LINKER_MODULE_ACTIONS)
                    .addChildren(ServiceLinkerModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof ServiceLinker) {
            final ServiceLinker serviceLinker = (ServiceLinker) data;
            return new ServiceLinkerNode(serviceLinker);
        }
        return null;
    }

    private Node<?> createDeploymentSlotNode(@Nonnull WebAppDeploymentSlotModule module, @Nonnull Manager manager) {
        return new Node<>(module)
            .view(new AppServiceDeploymentSlotsNodeView(module.getParent()))
            .actions(WebAppActionsContributor.DEPLOYMENT_SLOTS_ACTIONS)
            .addChildren(WebAppDeploymentSlotModule::list, (d, p) -> this.createNode(d, p, manager))
            .hasMoreChildren(AbstractAzResourceModule::hasMoreResources)
            .loadMoreChildren(AbstractAzResourceModule::loadMoreResources);
    }

    @Nullable
    public static AzureIcon.Modifier getOperatingSystemModifier(AppServiceAppBase<?, ?, ?> resource) {
        if (resource.getFormalStatus().isWaiting() || !resource.getFormalStatus().isConnected()) {
            return null;
        }
        final OperatingSystem os = Optional.ofNullable(resource.getRuntime()).map(Runtime::getOperatingSystem).orElse(null);
        return os != OperatingSystem.WINDOWS ? AzureIcon.Modifier.LINUX : null;
    }
}
