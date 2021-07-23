/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.function;

import com.microsoft.azure.toolkit.lib.appservice.service.IFunctionApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
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

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class FunctionAppNode extends WebAppBaseNode {

    private static final String FUNCTION_LABEL = "Function";

    private final IFunctionApp functionApp;

    public FunctionAppNode(@Nonnull AzureRefreshableNode parent, @Nonnull IFunctionApp functionApp) {
        super(parent, FUNCTION_LABEL, functionApp);
        this.functionApp = functionApp;
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean running = WebAppBaseState.RUNNING.equals(state);
        boolean updating = WebAppBaseState.UPDATING.equals(state);
        return running ? AzureIconSymbol.FunctionApp.RUNNING : updating ? AzureIconSymbol.FunctionApp.UPDATING : AzureIconSymbol.FunctionApp.STOPPED;
    }

    @Override
    protected void refreshItems() {
        super.refreshItems();
        this.renderSubModules();
    }

    public void renderSubModules() {
        // todo: implement with app service library
        addChildNode(new FunctionsNode(this, functionApp));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, functionApp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, functionApp));
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).build());
        addAction(initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).build());
        addAction(initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).withBackgroudable(true).build());
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
        properties.put(AppInsightsConstants.Region, this.functionApp.entity().getRegion().getName());
        return properties;
    }

    public String getFunctionAppId() {
        return this.functionApp.id();
    }

    public String getFunctionAppName() {
        return this.functionApp.name();
    }

    public String getRegion() {
        return this.functionApp.entity().getRegion().getName();
    }

    @AzureOperation(name = "function.start", params = {"this.functionApp.name()"}, type = AzureOperation.Type.ACTION)
    private void start() {
        functionApp.start();
        refreshStatus();
    }

    @AzureOperation(name = "function.stop", params = {"this.functionApp.name()"}, type = AzureOperation.Type.ACTION)
    private void stop() {
        functionApp.stop();
        refreshStatus();
    }

    @AzureOperation(name = "function.restart", params = {"this.functionApp.name()"}, type = AzureOperation.Type.ACTION)
    private void restart() {
        functionApp.restart();
        refreshStatus();
    }

    @AzureOperation(name = "function.delete", params = {"this.functionApp.name()"}, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.getParent().removeNode(this.getSubscriptionId(), this.getFunctionAppId(), FunctionAppNode.this);
    }

    @AzureOperation(name = "function.open_portal", params = {"this.functionApp.name()"}, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        this.openResourcesInPortal(subscriptionId, this.getFunctionAppId());
    }

    @AzureOperation(name = "function.show_properties", params = {"this.functionApp.name()"}, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openFunctionAppPropertyView(FunctionAppNode.this);
    }

    // todo: replace with Azure Event Bus
    private void refreshStatus() {
        functionApp.refresh();
        this.renderNode(WebAppBaseState.fromString(functionApp.state()));
    }
}
