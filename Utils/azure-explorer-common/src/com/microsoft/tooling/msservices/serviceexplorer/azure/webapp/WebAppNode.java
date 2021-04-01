/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2019-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceLogFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceUserFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebAppNode extends WebAppBaseNode implements WebAppNodeView {
    private static final String DELETE_WEBAPP_PROMPT_MESSAGE = "This operation will delete the Web App: %s.\n"
        + "Are you sure you want to continue?";
    private static final String DELETE_WEBAPP_PROGRESS_MESSAGE = "Deleting Web App '%s'...";
    private static final String LABEL = "WebApp";
    public static final String SSH_INTO = "SSH into Web App (Preview)";
    public static final String PROFILE_FLIGHT_RECORDER = "Profile Flight Recorder";

    private final WebApp webapp;

    /**
     * Constructor.
     */
    public WebAppNode(WebAppModule parent, String subscriptionId, WebApp delegate) {
        super(delegate.id(), delegate.name(), LABEL, parent, subscriptionId, delegate.defaultHostName(),
                delegate.operatingSystem().toString(), delegate.state());
        this.webapp = delegate;
        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        boolean isLinux = OS_LINUX.equalsIgnoreCase(webapp.operatingSystem().toString());
        boolean running = WebAppBaseState.RUNNING.equals(state);
        boolean updating = WebAppBaseState.UPDATING.equals(state);
        if (isLinux) {
            return running ? AzureIconSymbol.WebApp.RUNNING_ON_LINUX :
                    updating ? AzureIconSymbol.WebApp.UPDATING_ON_LINUX : AzureIconSymbol.WebApp.STOPPED_ON_LINUX;
        } else {
            return running ? AzureIconSymbol.WebApp.RUNNING : updating ? AzureIconSymbol.WebApp.UPDATING : AzureIconSymbol.WebApp.STOPPED;
        }
    }

    @Override
    @AzureOperation(name = "webapp.refresh", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        this.renderSubModules();
    }

    @Override
    public void renderSubModules() {
        boolean isDeploymentSlotSupported = isDeploymentSlotSupported(this.subscriptionId, this.webapp);
        addChildNode(new DeploymentSlotModule(this, this.subscriptionId, this.webapp, isDeploymentSlotSupported));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, this.webapp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, this.webapp));
    }

    @Override
    protected NodeActionListener getStartActionListener() {
        return initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).build();
    }

    @Override
    protected NodeActionListener getRestartActionListener() {
        return initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build();
    }

    @Override
    protected NodeActionListener getStopActionListener() {
        return initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).build();
    }

    @Override
    protected NodeActionListener getShowPropertiesActionListener() {
        return initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).build();
    }

    @Override
    protected NodeActionListener getOpenInBrowserActionListener() {
        return initActionBuilder(this::openInBrowser).withAction(AzureActionEnum.OPEN_IN_BROWSER).withBackgroudable(true).build();
    }

    @Override
    protected NodeActionListener getDeleteActionListener() {
        return initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build();
    }

    protected final BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(WebAppModule.MODULE_NAME)
                .withInstanceName(name);
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.webapp.regionName());
        return properties;
    }

    public String getWebAppId() {
        return this.webapp.id();
    }

    public String getWebAppName() {
        return this.webapp.name();
    }

    public String getFxVersion() {
        return this.webapp.linuxFxVersion();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = this.state == WebAppBaseState.RUNNING;
        NodeAction sshNodeAction = getNodeActionByName(SSH_INTO);

        List<NodeAction> nodeActions = super.getNodeActions();
        if (sshNodeAction != null && this.webapp.operatingSystem() != OperatingSystem.LINUX)
            nodeActions.remove(sshNodeAction);

        if (sshNodeAction != null)
            sshNodeAction.setEnabled(running);

        NodeAction profileNodeAction = getNodeActionByName(PROFILE_FLIGHT_RECORDER);
        if (profileNodeAction != null)
            // TODO: Ignore the linuxFxVersion check since it is a heavy call that make an extra request
            //  and cause context menu show delay or failure (if no internet). We could replace it with constructor parameter
            //  that will be executed on module refresh, but it will slow down showing items in the Web App module node
            //  because of an extra call for each Web App node.
            profileNodeAction.setEnabled(running);// && !StringUtils.containsIgnoreCase(this.webapp.linuxFxVersion(), "DOCKER|"));

        return nodeActions;
    }

    public WebApp getWebapp() {
        return webapp;
    }

    @AzureOperation(name = ActionConstants.WebApp.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.getParent().removeNode(this.getSubscriptionId(), this.getId(), WebAppNode.this);
    }

    @AzureOperation(name = ActionConstants.WebApp.START, type = AzureOperation.Type.ACTION)
    private void start() {
        AzureWebAppMvpModel.getInstance().startWebApp(this.subscriptionId, this.webapp.id());
        this.renderNode(WebAppBaseState.RUNNING);
    }

    @AzureOperation(name = ActionConstants.WebApp.STOP, type = AzureOperation.Type.ACTION)
    private void stop() {
        AzureWebAppMvpModel.getInstance().stopWebApp(this.subscriptionId, this.webapp.id());
        this.renderNode(WebAppBaseState.STOPPED);
    }

    @AzureOperation(name = ActionConstants.WebApp.RESTART, type = AzureOperation.Type.ACTION)
    private void restart() {
        AzureWebAppMvpModel.getInstance().restartWebApp(this.subscriptionId, this.webapp.id());
        this.renderNode(WebAppBaseState.RUNNING);
    }

    @AzureOperation(name = ActionConstants.WebApp.OPEN_IN_BROWSER, type = AzureOperation.Type.ACTION)
    private void openInBrowser() {
        DefaultLoader.getUIHelper().openInBrowser("http://" + this.hostName);
    }

    @AzureOperation(name = ActionConstants.WebApp.SHOW_PROPERTIES, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openWebAppPropertyView(WebAppNode.this);
    }

}
