/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.webapp;

import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileNode;
import com.microsoft.azure.toolkit.ide.appservice.webapp.node.WebAppDeploymentSlotsNode;
import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class WebAppExplorerContributor implements IExplorerContributor {
    public static final AzureIconProvider<IAppService<?>> APP_SERVICE_ICON_PROVIDER =
            new AzureIconProvider<>(AzureResourceLabelView::getAzureBaseResourceIconPath,
                    AzureResourceLabelView::getStatusModifier, WebAppExplorerContributor::getOperatingSystemModifier);

    private static final String NAME = "Web Apps";
    private static final String ICON = "/icons/webapp.svg";

    @Override
    public Node<?> getModuleNode() {
        final AzureWebApp service = Azure.az(AzureWebApp.class);
        return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(WebAppActionsContributor.SERVICE_ACTIONS)
                .addChildren(WebAppExplorerContributor::listWebApps, (webApp, webAppModule) -> new Node<>(webApp)
                        .view(new AzureResourceLabelView<>(webApp, WebApp::getStatus, APP_SERVICE_ICON_PROVIDER))
                        .actions(WebAppActionsContributor.WEBAPP_ACTIONS)
                        .addChildren(Arrays::asList, (app, webAppNode) -> new WebAppDeploymentSlotsNode(app))
                        .addChildren(app -> Collections.singletonList(AppServiceFileNode.getRootFileNodeForAppService(app)),
                                (file, webAppNode) -> new AppServiceFileNode(file)) // Files
                        .addChildren(app -> Collections.singletonList(AppServiceFileNode.getRootLogNodeForAppService(app)),
                                (file, webAppNode) -> new AppServiceFileNode(file)) // Logs
                );
    }

    public static AzureIcon.Modifier getOperatingSystemModifier(IAppService<?> resource) {
        // do not add os modifier in loading status as runtime request may have high cost
        if (StringUtils.equalsAnyIgnoreCase(resource.getStatus(), IAzureBaseResource.Status.LOADING, IAzureBaseResource.Status.PENDING)) {
            return null;
        }
        return resource.getRuntime().getOperatingSystem() == OperatingSystem.LINUX ?
                new AzureIcon.Modifier(OperatingSystem.LINUX.name().toLowerCase(), AzureIcon.ModifierLocation.BOTTOM_LEFT) : null;
    }

    private static List<WebApp> listWebApps(AzureWebApp webAppModule) {
        return webAppModule.list().stream().sorted(Comparator.comparing(WebApp::name)).collect(Collectors.toList());
    }
}
