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

import com.microsoft.azure.common.Utils;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

public class FunctionsNode extends RefreshableNode {

    private static final String ID = FunctionsNode.class.getName();
    private static final String NAME = "Functions";
    private static final String ICON_PATH = "azure-functions-small.png";

    private FunctionApp functionApp;
    private String subscriptionId;

    public FunctionsNode(final Node parent, FunctionApp functionApp) {
        super(ID, NAME, parent, ICON_PATH);
        this.functionApp = functionApp;
        this.subscriptionId = Utils.getSubscriptionId(functionApp.id());
    }

    public FunctionApp getFunctionApp() {
        return functionApp;
    }

    @Override
    @AzureOperation(value = "refresh functions in function app", type = AzureOperation.Type.ACTION)
    protected void refreshItems() throws AzureCmdException {
        AzureFunctionMvpModel.getInstance()
                .listFunctionEnvelopeInFunctionApp(subscriptionId, functionApp.id())
                .stream()
                .map(envelope -> new FunctionNode(envelope, this))
                .forEach(this::addChildNode);
    }
}
