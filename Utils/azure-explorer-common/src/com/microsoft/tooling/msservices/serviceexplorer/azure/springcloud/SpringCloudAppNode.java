/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.model.SpringCloudDeploymentStatus;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SpringCloudAppNode extends Node implements TelemetryProperties {

    private static final String ACTION_OPEN_IN_BROWSER = "Open In Browser";
    private static final Map<String, AzureIconSymbol> STATUS_TO_ICON_MAP = new HashMap<>();
    @Getter
    private final SpringCloudApp app;
    private String status;

    public static final String STATUS_UPDATING = "Updating";

    static {
        STATUS_TO_ICON_MAP.put(STATUS_UPDATING, AzureIconSymbol.SpringCloud.UPDATING);
        STATUS_TO_ICON_MAP.put(SpringCloudDeploymentStatus.UNKNOWN.getLabel(), AzureIconSymbol.SpringCloud.UNKNOWN);
        STATUS_TO_ICON_MAP.put(SpringCloudDeploymentStatus.RUNNING.getLabel(), AzureIconSymbol.SpringCloud.RUNNING);
        STATUS_TO_ICON_MAP.put(SpringCloudDeploymentStatus.ALLOCATING.getLabel(), AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(SpringCloudDeploymentStatus.COMPILING.getLabel(), AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(SpringCloudDeploymentStatus.UPGRADING.getLabel(), AzureIconSymbol.SpringCloud.PENDING);
        STATUS_TO_ICON_MAP.put(SpringCloudDeploymentStatus.STOPPED.getLabel(), AzureIconSymbol.SpringCloud.STOPPED);
        STATUS_TO_ICON_MAP.put(SpringCloudDeploymentStatus.FAILED.getLabel(), AzureIconSymbol.SpringCloud.FAILED);
    }

    public SpringCloudAppNode(@Nonnull SpringCloudApp app, SpringCloudNode parent) {
        super(app.entity().getId(), app.name(), parent, null, true);
        this.app = app;
        AzureEventBus.after("springcloud|app.start", this::onAppStatusChanged);
        AzureEventBus.after("springcloud|app.stop", this::onAppStatusChanged);
        AzureEventBus.after("springcloud|app.restart", this::onAppStatusChanged);
        AzureEventBus.before("springcloud|app.start", this::onAppStatusChanging);
        AzureEventBus.before("springcloud|app.stop", this::onAppStatusChanging);
        AzureEventBus.before("springcloud|app.restart", this::onAppStatusChanging);
        AzureEventBus.before("springcloud|app.remove", this::onAppStatusChanging);
        this.status = this.refreshStatus();
        this.loadActions();
        this.rerender();
    }

    public void onAppStatusChanged(SpringCloudApp app) {
        if (this.app.name().equals(app.name())) {
            this.status = this.refreshStatus();
            this.rerender();
        }
    }

    public void onAppStatusChanging(SpringCloudApp app) {
        if (this.app.name().equals(app.name())) {
            this.status = STATUS_UPDATING;
        }
    }

    private String refreshStatus() {
        return Optional.ofNullable(this.app.refresh().activeDeployment())
                .map(d -> d.entity().getStatus())
                .orElse(SpringCloudDeploymentStatus.UNKNOWN).getLabel();
    }

    private void rerender() {
        this.setName(String.format("%s - %s", this.app.name(), status));
        final boolean stopped = SpringCloudDeploymentStatus.STOPPED.getLabel().equals(status);
        final boolean running = SpringCloudDeploymentStatus.RUNNING.getLabel().equals(status);
        final boolean unknown = SpringCloudDeploymentStatus.UNKNOWN.getLabel().equals(status);
        final boolean allocating = SpringCloudDeploymentStatus.ALLOCATING.getLabel().equals(status);
        final boolean hasURL = Optional.ofNullable(app.entity().getApplicationUrl()).filter(u -> u.startsWith("http")).isPresent();
        getNodeActionByName(ACTION_OPEN_IN_BROWSER).setEnabled(hasURL && running);
        getNodeActionByName(AzureActionEnum.START.getName()).setEnabled(stopped);
        getNodeActionByName(AzureActionEnum.STOP.getName()).setEnabled(!stopped && !unknown && !allocating);
        getNodeActionByName(AzureActionEnum.RESTART.getName()).setEnabled(!stopped && !unknown && !allocating);
    }

    @AzureOperation(name = "springcloud|app.delete", params = {"this.app.name()"}, type = AzureOperation.Type.ACTION)
    private void delete() {
        app.remove();
    }

    @AzureOperation(name = "springcloud|app.start", params = {"this.app.name()"}, type = AzureOperation.Type.ACTION)
    private void start() {
        app.start();
    }

    @AzureOperation(name = "springcloud|app.stop", params = {"this.app.name()"}, type = AzureOperation.Type.ACTION)
    private void stop() {
        app.stop();
    }

    @AzureOperation(name = "springcloud|app.restart", params = {"this.app.name()"}, type = AzureOperation.Type.ACTION)
    private void restart() {
        app.restart();
    }

    @AzureOperation(name = "springcloud|app.open_portal", params = {"this.app.name()"}, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        openResourcesInPortal(app.entity().getSubscriptionId(), app.entity().getId());
    }

    @AzureOperation(name = "springcloud|app.show_properties", params = {"this.app.name()"}, type = AzureOperation.Type.ACTION)
    private void showProperties() {
        AzureTaskManager.getInstance().runLater(() -> DefaultLoader.getUIHelper().openSpringCloudAppPropertyView(SpringCloudAppNode.this));
    }

    @AzureOperation(name = "springcloud|app.open_browser", params = {"this.app.name()"}, type = AzureOperation.Type.ACTION)
    private void openInBrowser() {
        if (StringUtils.isNotEmpty(app.entity().getApplicationUrl())) {
            DefaultLoader.getUIHelper().openInBrowser(app.entity().getApplicationUrl());
        } else {
            DefaultLoader.getUIHelper().showInfo(this, "Public url is not available for app: " + app.name());
        }
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return STATUS_TO_ICON_MAP.get(status);
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

    public void unsubscribe() {
        // TODO: remove app event listeners
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(TelemetryConstants.SUBSCRIPTIONID, this.app.entity().getSubscriptionId());
        return properties;
    }
}
