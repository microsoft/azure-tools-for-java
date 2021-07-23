/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class FunctionModule extends AzureRefreshableNode {
    private static final String FUNCTION_SERVICE_MODULE_ID = FunctionModule.class.getName();
    private static final String ICON_PATH = "azure-functions-small.png";
    private static final String BASE_MODULE_NAME = "Function App";
    private static final String FUNCTION_MODULE = "FunctionModule";

    public static final String MODULE_NAME = "Function App";

    public FunctionModule(Node parent) {
        super(FUNCTION_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
        createListener();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.FunctionApp.MODULE;
    }

    @Override
    @AzureOperation(name = "function.delete", params = {"nameFromResourceId(id)", "sid"}, type = AzureOperation.Type.ACTION)
    public void removeNode(String sid, String id, Node node) {
        Azure.az(AzureAppService.class).functionApp(id).delete();
        removeDirectChildNode(node);
    }

    @Override
    @AzureOperation(name = "function.reload_all", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        Azure.az(AzureAppService.class).functionApps(true)
                .stream()
                .map(functionApp -> new FunctionAppNode(FunctionModule.this, functionApp))
                .forEach(this::addChildNode);
    }

    private void createListener() {
        AzureUIRefreshListener listener = new AzureUIRefreshListener() {
            @Override
            public void run() {
                if (event.opsType == null) {
                    return;
                }
                switch (event.opsType) {
                    case SIGNIN:
                    case SIGNOUT:
                        removeAllChildNodes();
                        break;
                    case REFRESH:
                        if (isFunctionModuleEvent(event.object)) {
                            load(true);
                        }
                        break;
                    default:
                        if (isFunctionModuleEvent(event.object) && hasChildNodes()) {
                            load(true);
                        }
                        break;
                }
            }
        };
        AzureUIRefreshCore.addListener(FUNCTION_MODULE, listener);
    }

    private static boolean isFunctionModuleEvent(Object eventObject) {
        return eventObject != null && eventObject instanceof IFunctionApp;
    }
}
