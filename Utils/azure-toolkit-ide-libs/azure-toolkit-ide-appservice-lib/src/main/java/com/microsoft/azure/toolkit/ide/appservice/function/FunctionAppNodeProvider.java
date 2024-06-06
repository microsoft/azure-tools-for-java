/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function;

import com.microsoft.azure.toolkit.ide.appservice.appsettings.AppSettingsNode;
import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileNode;
import com.microsoft.azure.toolkit.ide.appservice.function.node.FunctionsNode;
import com.microsoft.azure.toolkit.ide.appservice.webapp.WebAppNodeProvider;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.*;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotModule;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class FunctionAppNodeProvider implements IExplorerNodeProvider {
    public static final AzureIconProvider<AppServiceAppBase<?, ?, ?>> FUNCTIONAPP_ICON_PROVIDER =
        new AzureResourceIconProvider<AppServiceAppBase<?, ?, ?>>()
            .withModifier(WebAppNodeProvider::getOperatingSystemModifier)
            .withModifier(app -> new AzureIcon.Modifier("functionapp", AzureIcon.ModifierLocation.OTHER));

    private static final String NAME = "Function App";
    private static final String ICON = AzureIcons.FunctionApp.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureFunctions.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureFunctions ||
            data instanceof FunctionApp ||
            data instanceof AppServiceFile;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureFunctions) {
            return new AzServiceNode<>(Azure.az(AzureFunctions.class))
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(FunctionAppActionsContributor.SERVICE_ACTIONS)
                .addChildren(AzureFunctions::functionApps, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof FunctionApp function) {
            final Node<FunctionApp> appNode = new AzResourceNode<>(function)
                .withIcon(FUNCTIONAPP_ICON_PROVIDER::getIcon)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .addInlineAction(ResourceCommonActionsContributor.DEPLOY)
                .withActions(FunctionAppActionsContributor.FUNCTION_APP_ACTIONS)
                .addChildren(Arrays::asList, (app, webAppNode) -> new FunctionsNode(app));
            if (!function.isContainerHostingFunctionApp()) {
                appNode.addChild(AppServiceFileNode::getRootFileNodeForAppService, (d, p) -> this.createNode(d, p, manager));
                if(!function.isFlexConsumptionApp()){
                    appNode.addChild(AppServiceFileNode::getRootLogNodeForAppService, (d, p) -> this.createNode(d, p, manager))
                            .addChild(FunctionApp::getDeploymentModule, (module, functionAppNode) -> createNode(module, functionAppNode, manager));
                }
            }
            return appNode.addChild(app -> new AppSettingsNode(app.getValue()));
        } else if (data instanceof FunctionAppDeploymentSlotModule) {
            return new AzModuleNode<>((FunctionAppDeploymentSlotModule) data)
                .withIcon(AzureIcons.WebApp.DEPLOYMENT_SLOT)
                .withLabel("Deployment Slots")
                .withActions(FunctionAppActionsContributor.DEPLOYMENT_SLOTS_ACTIONS)
                .addChildren(FunctionAppDeploymentSlotModule::list, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof FunctionAppDeploymentSlot) {
            return new AzResourceNode<>((FunctionAppDeploymentSlot) data)
                .withActions(FunctionAppActionsContributor.DEPLOYMENT_SLOT_ACTIONS);
        } else if (data instanceof AppServiceFile) {
            return new AppServiceFileNode((AppServiceFile) data);
        }
        return null;
    }
}
