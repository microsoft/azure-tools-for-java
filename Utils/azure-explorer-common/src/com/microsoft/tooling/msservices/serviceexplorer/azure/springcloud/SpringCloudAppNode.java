/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceStatus;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import io.reactivex.rxjava3.disposables.Disposable;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpringCloudAppNode extends Node implements SpringCloudAppNodeView {

    private static final String ACTION_OPEN_IN_BROWSER = "Open In Browser";
    private static final Map<DeploymentResourceStatus, AzureIconSymbol> STATUS_TO_ICON_MAP = new HashMap<>();

    private AppResourceInner app;
    private DeploymentResourceStatus status;
    private DeploymentResourceInner deploy;

    private final String clusterName;
    private final String subscriptionId;
    private final String clusterId;
    private final Disposable rxSubscription;

    static {
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.UNKNOWN, AzureIconSymbol.SpringCloud.UNKNOWN);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.RUNNING, AzureIconSymbol.SpringCloud.RUNNING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.ALLOCATING, AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.COMPILING, AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.UPGRADING, AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.STOPPED, AzureIconSymbol.SpringCloud.STOPPED);
        STATUS_TO_ICON_MAP.put(DeploymentResourceStatus.FAILED, AzureIconSymbol.SpringCloud.FAILED);
    }

    public SpringCloudAppNode(AppResourceInner app, DeploymentResourceInner deploy, SpringCloudNode parent) {
        super(app.id(), app.name(), parent, getIconForStatus(deploy == null ? DeploymentResourceStatus.UNKNOWN : deploy.properties().status()), true);
        this.app = app;
        this.deploy = deploy;
        this.clusterName = SpringCloudIdHelper.getClusterName(app.id());
        this.clusterId = parent.getClusterId();
        this.subscriptionId = SpringCloudIdHelper.getSubscriptionId(app.id());
        fillData(app, deploy);
        rxSubscription = SpringCloudStateManager.INSTANCE.subscribeSpringAppEvent(event -> {
            if (event.isUpdate()) {
                fillData(event.getAppInner(), event.getDeploymentInner());
            }
        }, this.app.id());
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        if (Objects.isNull(deploy) || Objects.isNull(deploy.properties().status())) {
            return AzureIconSymbol.SpringCloud.UNKNOWN;
        }
        return STATUS_TO_ICON_MAP.get(deploy.properties().status());
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

    public String getClusterName() {
        return clusterName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getAppName() {
        return app.name();
    }

    public String getAppId() {
        return app.id();
    }

    public String getSubscriptionId() {
        return subscriptionId;
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

    private static String getIconForStatus(DeploymentResourceStatus statusEnum) {
        final String displayText = getStatusDisplay(statusEnum);
        String simpleStatus = displayText.endsWith("ing") && !StringUtils.equalsIgnoreCase(displayText, "running") ? "pending" : displayText.toLowerCase();
        return String.format("azure-springcloud-app-%s.png", simpleStatus);
    }

    private void syncActionState() {
        if (status != null) {
            boolean stopped = DeploymentResourceStatus.STOPPED.equals(status);
            boolean running = DeploymentResourceStatus.RUNNING.equals(status);
            boolean unknown = DeploymentResourceStatus.UNKNOWN.equals(status);
            boolean allocating = DeploymentResourceStatus.ALLOCATING.equals(status);
            boolean hasURL = StringUtils.isNotEmpty(app.properties().url()) && app.properties().url().startsWith("http");
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

    private void fillData(AppResourceInner newApp, DeploymentResourceInner deploy) {
        this.status = deploy == null ? DeploymentResourceStatus.UNKNOWN : deploy.properties().status();
        this.app = newApp;
        this.deploy = deploy;
        this.setIconPath(getIconForStatus(status));
        this.setName(String.format("%s - %s", app.name(), getStatusDisplay(status)));
        if (getNodeActionByName(AzureActionEnum.START.getName()) == null) {
            loadActions();
        }
        syncActionState();
    }

    @AzureOperation(name = ActionConstants.SpringCloud.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        AzureSpringCloudMvpModel.deleteApp(this.id).await();
        SpringCloudMonitorUtil.awaitAndMonitoringStatus(id, null);
    }

    @AzureOperation(name = ActionConstants.SpringCloud.START, type = AzureOperation.Type.ACTION)
    private void start() {
        AzureSpringCloudMvpModel.startApp(app.id(), app.properties().activeDeploymentName()).await();
        SpringCloudMonitorUtil.awaitAndMonitoringStatus(app.id(), status);
    }

    @AzureOperation(name = ActionConstants.SpringCloud.STOP, type = AzureOperation.Type.ACTION)
    private void stop() {
        AzureSpringCloudMvpModel.stopApp(app.id(), app.properties().activeDeploymentName()).await();
        SpringCloudMonitorUtil.awaitAndMonitoringStatus(app.id(), status);
    }

    @AzureOperation(name = ActionConstants.SpringCloud.RESTART, type = AzureOperation.Type.ACTION)
    private void restart() {
        AzureSpringCloudMvpModel.restartApp(app.id(), app.properties().activeDeploymentName()).await();
        SpringCloudMonitorUtil.awaitAndMonitoringStatus(app.id(), status);
    }

    @AzureOperation(name = ActionConstants.SpringCloud.OPEN_IN_PORTAL, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        openResourcesInPortal(getSubscriptionId(), getAppId());
    }

    @AzureOperation(name = ActionConstants.SpringCloud.SHOW_PROPERTIES, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        DefaultLoader.getUIHelper().openSpringCloudAppPropertyView(SpringCloudAppNode.this);
        // add this statement for false updating notice to update Property view
        // immediately
        SpringCloudStateManager.INSTANCE.notifySpringAppUpdate(clusterId,
                app, deploy);
    }

    @AzureOperation(name = ActionConstants.SpringCloud.OPEN_IN_BROWSER, type = AzureOperation.Type.ACTION)
    private void openInBrowser() {
        if (StringUtils.isNotEmpty(app.properties().url())) {
            DefaultLoader.getUIHelper().openInBrowser(app.properties().url());
        } else {
            DefaultLoader.getUIHelper().showInfo(this, "Public url is not available for app: " + app.name());
        }
    }

}
