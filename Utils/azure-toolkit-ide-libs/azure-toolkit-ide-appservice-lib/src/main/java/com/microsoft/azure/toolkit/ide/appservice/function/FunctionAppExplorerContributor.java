/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function;

import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileNode;
import com.microsoft.azure.toolkit.ide.appservice.function.node.FunctionsNode;
import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureFunction;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionAppExplorerContributor implements IExplorerContributor {
    private static final String NAME = "Function App";
    private static final String ICON = "/icons/functionapp.png";

    @Override
    public Node<?> getModuleNode() {
        final AzureFunction service = Azure.az(AzureFunction.class);
        return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(FunctionAppActionsContributor.SERVICE_ACTIONS)
                .addChildren(FunctionAppExplorerContributor::listFunctionApps, (webApp, webAppModule) -> new Node<>(webApp)
                        .view(new AzureResourceLabelView<>(webApp))
                        .actions(FunctionAppActionsContributor.FUNCTION_APP_ACTIONS)
                        .addChildren(Arrays::asList, (app, webAppNode) -> new FunctionsNode(app))
                        .addChildren(app -> Collections.singletonList(AppServiceFileNode.getRootFileNodeForAppService(app)),
                                (file, webAppNode) -> new AppServiceFileNode(file)) // Files
                        .addChildren(app -> Collections.singletonList(AppServiceFileNode.getRootLogNodeForAppService(app)),
                                (file, webAppNode) -> new AppServiceFileNode(file)) // Logs
                );
    }

    private static List<IFunctionApp> listFunctionApps(AzureFunction functionModule) {
        return functionModule.list().stream().sorted(Comparator.comparing(IFunctionApp::name)).collect(Collectors.toList());
    }
}
