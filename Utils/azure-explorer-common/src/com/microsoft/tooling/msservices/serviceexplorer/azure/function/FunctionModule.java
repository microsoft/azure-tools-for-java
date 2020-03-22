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
package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.io.IOException;
import java.util.List;

public class FunctionModule extends AzureRefreshableNode implements FunctionModuleView {
    private static final String FUNCTION_SERVICE_MODULE_ID = FunctionModule.class.getName();
    private static final String ICON_PATH = "azure-functions-small.png";
    private static final String BASE_MODULE_NAME = "Function App(Preview)";
    private static final String FUNCTION_MODULE = "FunctionModule";
    private static final String FAILED_TO_DELETE_FUNCTION_APP = "Failed to delete Function App %s";
    private static final String ERROR_DELETING_FUNCTION_APP = "Azure Services Explorer - Error Deleting Function App";
    private final FunctionModulePresenter<FunctionModule> functionModulePresenter;

    public FunctionModule(Node parent) {
        super(FUNCTION_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
        functionModulePresenter = new FunctionModulePresenter<>();
        functionModulePresenter.onAttachView(FunctionModule.this);
        createListener();
    }

    @Override
    public void removeNode(String sid, String id, Node node) {
        try {
            functionModulePresenter.onDeleteFunctionApp(sid, id);
            removeDirectChildNode(node);
        } catch (IOException | CloudException e) {
            DefaultLoader.getUIHelper().showException(String.format(FAILED_TO_DELETE_FUNCTION_APP, node.getName()),
                    e, ERROR_DELETING_FUNCTION_APP, false, true);
            functionModulePresenter.onModuleRefresh();
        }
    }

    @Override
    public void renderChildren(@NotNull final List<ResourceEx<FunctionApp>> resourceExes) {
        for (final ResourceEx<FunctionApp> resourceEx : resourceExes) {
            final FunctionNode node = new FunctionNode(this, resourceEx.getSubscriptionId(), resourceEx.getResource());
            addChildNode(node);
        }
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        functionModulePresenter.onModuleRefresh();
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
        return eventObject == null || FunctionModule.class.getName().equals(eventObject);
    }
}
