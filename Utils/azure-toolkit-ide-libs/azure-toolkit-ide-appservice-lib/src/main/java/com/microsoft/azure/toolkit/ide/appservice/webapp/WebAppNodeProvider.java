/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.webapp;

import com.microsoft.azure.toolkit.ide.appservice.appsettings.AppSettingsNode;
import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileNode;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.*;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlotModule;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import org.apache.commons.lang3.StringUtils;

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
            return new AzServiceNode<>(Azure.az(AzureWebApp.class))
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(WebAppActionsContributor.SERVICE_ACTIONS)
                .addChildren(AzureWebApp::webApps, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof WebApp) {
            return new AzResourceNode<>((WebApp) data)
                .withIcon(WEBAPP_ICON_PROVIDER::getIcon)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addInlineAction(ResourceCommonActionsContributor.DEPLOY)
                .withActions(WebAppActionsContributor.WEBAPP_ACTIONS)
                .addChildren(WebApp::getSubModules, (module, webAppNode) -> createNode(module, webAppNode, manager))
                .addChild(AppServiceFileNode::getRootFileNodeForAppService, (d, p) -> this.createNode(d, p, manager)) // Files
                .addChild(AppServiceFileNode::getRootLogNodeForAppService, (d, p) -> this.createNode(d, p, manager))
                .addChild(app -> new AppSettingsNode(app.getValue()));
        } else if (data instanceof WebAppDeploymentSlotModule) {
            return new AzModuleNode<>((WebAppDeploymentSlotModule) data)
                .withIcon(AzureIcons.WebApp.DEPLOYMENT_SLOT)
                .withLabel("Deployment Slots")
                .withActions(WebAppActionsContributor.DEPLOYMENT_SLOTS_ACTIONS)
                .addChildren(WebAppDeploymentSlotModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof WebAppDeploymentSlot) {
            return new AzResourceNode<>((WebAppDeploymentSlot) data)
                .withActions(WebAppActionsContributor.DEPLOYMENT_SLOT_ACTIONS);
        } else if (data instanceof AppServiceFile) {
            return new AppServiceFileNode((AppServiceFile) data);
        } else if (data instanceof ServiceLinkerModule) {
            return new AzModuleNode<>((ServiceLinkerModule) data)
                .withIcon(AzureIcons.Connector.SERVICE_LINKER_MODULE)
                .withLabel("Service Connector")
                .withActions(ResourceCommonActionsContributor.SERVICE_LINKER_MODULE_ACTIONS)
                .addChildren(ServiceLinkerModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof ServiceLinker) {
            return new ServiceLinkerNode((ServiceLinker) data);
        }
        return null;
    }

    @Nullable
    public static AzureIcon.Modifier getOperatingSystemModifier(AppServiceAppBase<?, ?, ?> resource) {
        if (resource.getFormalStatus().isWaiting() || !resource.getFormalStatus().isConnected()) {
            return null;
        }
        if (resource instanceof FunctionApp app && StringUtils.isNoneBlank(app.getEnvironmentId())) {
            return AzureIcon.Modifier.CONTAINER;
        }
        final OperatingSystem os = Optional.ofNullable(resource.getRuntime())
                .map(Runtime::getOperatingSystem)
                .orElseGet(() -> Optional.ofNullable(resource.getAppServicePlan())
                        .map(AppServicePlan::getOperatingSystem).orElse(OperatingSystem.WINDOWS));
        return switch (os) {
            case LINUX -> AzureIcon.Modifier.LINUX;
            case DOCKER -> AzureIcon.Modifier.DOCKER;
            default -> null;
        };
    }
}
