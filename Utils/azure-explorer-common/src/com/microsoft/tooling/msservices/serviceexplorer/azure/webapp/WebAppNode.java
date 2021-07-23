/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceLogFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file.AppServiceUserFilesRootNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base.WebAppBaseState;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotModule;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebAppNode extends WebAppBaseNode {
    private static final String LABEL = "WebApp";
    public static final String SSH_INTO = "SSH into Web App (Preview)";
    public static final String PROFILE_FLIGHT_RECORDER = "Profile Flight Recorder";

    private final IWebApp webApp;

    public WebAppNode(WebAppModule parent, IWebApp webApp) {
        super(parent, LABEL, webApp);
        this.webApp = webApp;
    }

    public IWebApp getWebApp() {
        return webApp;
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        if (WebAppBaseState.UPDATING.equals(state)) {
            return AzureIconSymbol.WebApp.UPDATING;
        }
        boolean isLinux = webApp.getRuntime().getOperatingSystem() != OperatingSystem.WINDOWS;
        boolean running = WebAppBaseState.RUNNING.equals(state);
        if (isLinux) {
            return running ? AzureIconSymbol.WebApp.RUNNING_ON_LINUX : AzureIconSymbol.WebApp.STOPPED_ON_LINUX;
        } else {
            return running ? AzureIconSymbol.WebApp.RUNNING : AzureIconSymbol.WebApp.STOPPED;
        }
    }

    @Override
    @AzureOperation(name = "webapp.refresh", type = AzureOperation.Type.ACTION)
    protected void refreshItems() {
        super.refreshItems();
        this.renderSubModules();
    }

    public void renderSubModules() {
        addChildNode(new DeploymentSlotModule(this, this.subscriptionId, this.webApp));
        addChildNode(new AppServiceUserFilesRootNode(this, this.subscriptionId, this.webApp));
        addChildNode(new AppServiceLogFilesRootNode(this, this.subscriptionId, this.webApp));
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).build());
        addAction(initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).build());
        addAction(initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::openInBrowser).withAction(AzureActionEnum.OPEN_IN_BROWSER).withBackgroudable(true).build());
        addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).withBackgroudable(true).build());
        super.loadActions();
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
        properties.put(AppInsightsConstants.Region, this.webApp.entity().getRegion().getName());
        return properties;
    }

    public String getWebAppId() {
        return this.webApp.id();
    }

    public String getWebAppName() {
        return this.webApp.name();
    }

    public Runtime getWebAppRuntime() {
        return this.webApp.getRuntime();
    }

    @Override
    public List<NodeAction> getNodeActions() {
        boolean running = this.state == WebAppBaseState.RUNNING;
        getNodeActionByName(SSH_INTO).setEnabled(running);
        getNodeActionByName(PROFILE_FLIGHT_RECORDER).setEnabled(running);
        return super.getNodeActions();
    }

    @AzureOperation(name = "webapp.delete", params = {"this.webapp.name()"}, type = AzureOperation.Type.ACTION)
    private void delete() {
        this.getParent().removeNode(this.getSubscriptionId(), this.getId(), WebAppNode.this);
    }

    @AzureOperation(name = "webapp.start", params = {"this.webapp.name()"}, type = AzureOperation.Type.ACTION)
    private void start() {
        this.webApp.start();
        this.renderNode(WebAppBaseState.RUNNING);
    }

    @AzureOperation(name = "webapp.stop", params = {"this.webapp.name()"}, type = AzureOperation.Type.ACTION)
    private void stop() {
        this.webApp.stop();
        this.renderNode(WebAppBaseState.STOPPED);
    }

    @AzureOperation(name = "webapp.restart", params = {"this.webapp.name()"}, type = AzureOperation.Type.ACTION)
    private void restart() {
        this.webApp.restart();
        this.renderNode(WebAppBaseState.RUNNING);
    }

    @AzureOperation(name = "webapp.open_portal", params = {"this.webapp.name()"}, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        this.openResourcesInPortal(this.webApp.subscriptionId(), this.webApp.id());
    }

    @AzureOperation(name = "webapp.open_browser", params = {"this.webapp.name()"}, type = AzureOperation.Type.ACTION)
    private void openInBrowser() {
        DefaultLoader.getUIHelper().openInBrowser("http://" + this.webApp.hostName());
    }

    @AzureOperation(name = "webapp.show_properties", params = {"this.webapp.name()"}, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openWebAppPropertyView(WebAppNode.this);
    }

}
