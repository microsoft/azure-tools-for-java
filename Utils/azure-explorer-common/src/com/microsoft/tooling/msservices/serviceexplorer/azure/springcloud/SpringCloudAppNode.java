/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceStatus;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpringCloudAppNode extends Node implements TelemetryProperties {

    private static final String ACTION_OPEN_IN_BROWSER = "Open In Browser";
    private static final DeploymentResourceStatus SERVER_UPDATING = DeploymentResourceStatus.fromString("Updating");
    private static final Map<DeploymentResourceStatus, AzureIconSymbol> STATUS_TO_ICON_MAP = new HashMap<>();

    @Getter
    private SpringCloudApp app;
    private DeploymentResourceStatus status;
    private SpringCloudDeployment deploy;

    private final Disposable rxSubscription;

    static {
        STATUS_TO_ICON_MAP.put(SERVER_UPDATING, AzureIconSymbol.SpringCloud.UPDATING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.UNKNOWN, AzureIconSymbol.SpringCloud.UNKNOWN);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.RUNNING, AzureIconSymbol.SpringCloud.RUNNING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.ALLOCATING, AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.COMPILING, AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.UPGRADING, AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.STOPPED, AzureIconSymbol.SpringCloud.STOPPED);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.FAILED, AzureIconSymbol.SpringCloud.FAILED);
    }

    public SpringCloudAppNode(SpringCloudApp app, SpringCloudNode parent) {
        super(app.entity().getId(), app.name(), parent, null, true);
        this.app = app;
        if (StringUtils.isNotBlank(app.getActiveDeploymentName())) {
            this.deploy = app.deployment(app.getActiveDeploymentName());
            this.status = DeploymentResourceStatus.fromString(deploy.entity().getStatus());
        } else {
            this.status = DeploymentResourceStatus.UNKNOWN;
        }
        fillData(app, deploy);
        rxSubscription = SpringCloudStateManager.INSTANCE.subscribeSpringAppEvent(event -> {
            if (event.isUpdate()) {
                fillData(event.getApp(), event.getDeployment());
            }
        }, this.app.entity().getId());
        AzureEventBus.after("springcloud|app.start", this::onAppStatusChanged);
        AzureEventBus.after("springcloud|app.stop", this::onAppStatusChanged);
        AzureEventBus.after("springcloud|app.restart", this::onAppStatusChanged);
    }

    public void onAppStatusChanged(SpringCloudApp app) {
        if (this.app.name().equals(app.name())) {
            this.refreshNode();
            this.fillData(this.app, this.app.activeDeployment());
        }
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return STATUS_TO_ICON_MAP.get(status);
    }

    @Override
    public List<NodeAction> getNodeActions() {
        syncActionState();
        return super.getNodeActions();
    }

    public void unsubscribe() {
        if (rxSubscription != null && !rxSubscription.isDisposed()) {
            rxSubscription.dispose();
        }
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(TelemetryConstants.SUBSCRIPTIONID, this.app.entity().getSubscriptionId());
        return properties;
    }

    @Override
    protected void loadActions() {
        addAction(initActionBuilder(this::start).withAction(AzureActionEnum.START).withBackgroudable(true).build());
        addAction(initActionBuilder(this::stop).withAction(AzureActionEnum.STOP).withBackgroudable(true).build());
        addAction(initActionBuilder(this::restart).withAction(AzureActionEnum.RESTART).withBackgroudable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::showProperties).withAction(AzureActionEnum.SHOW_PROPERTIES).build());
        addAction(initActionBuilder(this::openInBrowser).withAction(AzureActionEnum.OPEN_IN_BROWSER).withBackgroudable(true).build());
        super.loadActions();
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(SpringCloudModule.MODULE_NAME)
                .withInstanceName(name);
    }

    private static String getStatusDisplay(DeploymentResourceStatus status) {
        return status == null ? "Unknown" : status.toString();
    }

    private void syncActionState() {
        if (status != null) {
            final boolean stopped = DeploymentResourceStatus.STOPPED.equals(status);
            final boolean running = DeploymentResourceStatus.RUNNING.equals(status);
            final boolean unknown = DeploymentResourceStatus.UNKNOWN.equals(status);
            final boolean allocating = DeploymentResourceStatus.ALLOCATING.equals(status);
            final boolean hasURL = StringUtils.isNotEmpty(app.entity().getApplicationUrl()) && app.entity().getApplicationUrl().startsWith("http");
            getNodeActionByName(ACTION_OPEN_IN_BROWSER).setEnabled(hasURL && running);
            getNodeActionByName(AzureActionEnum.START.getName()).setEnabled(stopped);
            getNodeActionByName(AzureActionEnum.STOP.getName()).setEnabled(!stopped && !unknown && !allocating);
            getNodeActionByName(AzureActionEnum.RESTART.getName()).setEnabled(!stopped && !unknown && !allocating);
        } else {
            getNodeActionByName(ACTION_OPEN_IN_BROWSER).setEnabled(false);
            getNodeActionByName(AzureActionEnum.START.getName()).setEnabled(false);
            getNodeActionByName(AzureActionEnum.STOP.getName()).setEnabled(false);
            getNodeActionByName(AzureActionEnum.RESTART.getName()).setEnabled(false);
        }
    }

    private void fillData(SpringCloudApp newApp, SpringCloudDeployment deploy) {
        this.app = newApp;
        this.deploy = deploy;
        this.setName(String.format("%s - %s", app.name(), getStatusDisplay(status)));
        if (getNodeActionByName(AzureActionEnum.START.getName()) == null) {
            loadActions();
        }
        syncActionState();
    }

    private void refreshNode() {
        final SpringCloudDeployment deployment = this.app.refresh().activeDeployment();
        if (Objects.nonNull(deployment)) {
            this.status = DeploymentResourceStatus.fromString(deployment.entity().getStatus());
        }
    }

    @AzureOperation(name = ActionConstants.SpringCloud.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        status = SERVER_UPDATING;
        app.remove();
        SpringCloudMonitorUtil.awaitAndMonitoringStatus(app, null);
    }

    @AzureOperation(name = ActionConstants.SpringCloud.START, type = AzureOperation.Type.ACTION)
    private void start() {
        status = SERVER_UPDATING;
        deploy.start();
        SpringCloudMonitorUtil.awaitAndMonitoringStatus(app, status);
        this.refreshNode();
    }

    @AzureOperation(name = ActionConstants.SpringCloud.STOP, type = AzureOperation.Type.ACTION)
    private void stop() {
        status = SERVER_UPDATING;
        deploy.stop();
        SpringCloudMonitorUtil.awaitAndMonitoringStatus(app, status);
        this.refreshNode();
    }

    @AzureOperation(name = ActionConstants.SpringCloud.RESTART, type = AzureOperation.Type.ACTION)
    private void restart() {
        status = SERVER_UPDATING;
        deploy.restart();
        SpringCloudMonitorUtil.awaitAndMonitoringStatus(app, status);
        this.refreshNode();
    }

    @AzureOperation(name = ActionConstants.SpringCloud.OPEN_IN_PORTAL, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        openResourcesInPortal(app.entity().getSubscriptionId(), app.entity().getId());
    }

    @AzureOperation(name = ActionConstants.SpringCloud.SHOW_PROPERTIES, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        AzureTaskManager.getInstance().runLater(() -> DefaultLoader.getUIHelper().openSpringCloudAppPropertyView(SpringCloudAppNode.this));
    }

    @AzureOperation(name = ActionConstants.SpringCloud.OPEN_IN_BROWSER, type = AzureOperation.Type.ACTION)
    private void openInBrowser() {
        if (StringUtils.isNotEmpty(app.entity().getApplicationUrl())) {
            DefaultLoader.getUIHelper().openInBrowser(app.entity().getApplicationUrl());
        } else {
            DefaultLoader.getUIHelper().showInfo(this, "Public url is not available for app: " + app.name());
        }
    }

}
