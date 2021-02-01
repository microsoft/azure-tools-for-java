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

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceLogFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceUserFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;

import java.util.HashMap;
import java.util.Map;

public class FunctionAppNode extends WebAppBaseNode implements FunctionAppNodeView {

    private static final String FUNCTION_LABEL = "Function";

    private FunctionApp functionApp;

    public FunctionAppNode(AzureRefreshableNode parent, String subscriptionId, FunctionApp functionApp) {
        super(functionApp.id(), functionApp.name(), FUNCTION_LABEL, parent, subscriptionId,
                functionApp.defaultHostName(), functionApp.operatingSystem().toString(), functionApp.state());
        this.functionApp = functionApp;
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean running = WebAppBaseState.RUNNING.equals(state);
        boolean updating = WebAppBaseState.UPDATING.equals(state);
        return running ? AzureIconSymbol.FunctionApp.RUNNING : updating ? AzureIconSymbol.FunctionApp.UPDATING : AzureIconSymbol.FunctionApp.STOPPED;
    }

    @Override
    protected void refreshItems() {
        this.renderSubModules();
    }

    @Override
    public void renderSubModules() {
        addChildNode(new FunctionsNode(this, this.functionApp));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, this.functionApp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, this.functionApp));
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).build());
        addAction(initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).build());
        addAction(initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).build());
        super.loadActions();
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(FunctionModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, getRegion());
        return properties;
    }

    public FunctionApp getFunctionApp() {
        return functionApp;
    }

    public String getFunctionAppId() {
        return this.functionApp.id();
    }

    public String getFunctionAppName() {
        return this.functionApp.name();
    }

    public String getRegion() {
        return this.functionApp.regionName();
    }

    @AzureOperation(name = ActionConstants.FunctionApp.START, type = AzureOperation.Type.ACTION)
    private void start() {
        AzureFunctionMvpModel.getInstance().startFunction(subscriptionId, this.getFunctionAppId());
        FunctionApp target = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, this.getFunctionAppId());
        this.renderNode(WebAppBaseState.fromString(target.state()));
    }

    @AzureOperation(name = ActionConstants.FunctionApp.STOP, type = AzureOperation.Type.ACTION)
    private void stop() {
        AzureFunctionMvpModel.getInstance().stopFunction(subscriptionId, this.getFunctionAppId());
        FunctionApp target = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, this.getFunctionAppId());
        this.renderNode(WebAppBaseState.fromString(target.state()));
    }

    @AzureOperation(name = ActionConstants.FunctionApp.RESTART, type = AzureOperation.Type.ACTION)
    private void restart() {
        AzureFunctionMvpModel.getInstance().restartFunction(subscriptionId, this.getFunctionAppId());
        FunctionApp target = AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, this.getFunctionAppId());
        this.renderNode(WebAppBaseState.fromString(target.state()));
    }

    @AzureOperation(name = ActionConstants.FunctionApp.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.getParent().removeNode(this.getSubscriptionId(), this.getFunctionAppId(), FunctionAppNode.this);
    }

    @AzureOperation(name = ActionConstants.FunctionApp.OPEN_IN_PORTAL, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        this.openResourcesInPortal(subscriptionId, this.getFunctionAppId());
    }

    @AzureOperation(name = ActionConstants.FunctionApp.SHOW_PROPERTIES, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openFunctionAppPropertyView(FunctionAppNode.this);
    }

}
