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

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentResourceStatus;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.azuretools.telemetry.TelemetryParameter;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionBackgroundable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionPromptable;
import com.microsoft.tooling.msservices.serviceexplorer.listener.ActionTelemetrable;
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
        addAction(new StartAction().asGenericListener(AzureActionEnum.START));
        addAction(new StopAction().asGenericListener(AzureActionEnum.STOP));
        addAction(new RestartAction().asGenericListener(AzureActionEnum.RESTART));
        addAction(new DeleteAction().asGenericListener(AzureActionEnum.DELETE));
        addAction(new OpenInPortalAction().asGenericListener(AzureActionEnum.OPEN_IN_PORTAL));
        addAction(ACTION_OPEN_IN_BROWSER, new OpenInBrowserAction().asGenericListener());
        addAction(new ShowPropertiesAction().asGenericListener(AzureActionEnum.SHOW_PROPERTIES));
        super.loadActions();
    }

    private static NodeActionListener createBackgroundActionListener(final String actionName, final Runnable runnable) {
        return new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                AzureTaskManager.getInstance().runInBackground(new AzureTask(null, String.format("%s...", actionName), false, runnable));
            }
        };
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

    // Delete action class
    private class DeleteAction extends NodeActionListener implements ActionBackgroundable, ActionPromptable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureSpringCloudMvpModel.deleteApp(SpringCloudAppNode.this.id).await();
            SpringCloudMonitorUtil.awaitAndMonitoringStatus(SpringCloudAppNode.this.id, null);
        }

        @Override
        public String getPromptMessage() {
            return Node.getPromptMessage(AzureActionEnum.DELETE.getName().toLowerCase(), SpringCloudModule.MODULE_NAME, SpringCloudAppNode.this.name);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.DELETE.getDoingName(), SpringCloudModule.MODULE_NAME, SpringCloudAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.SpringCloud.DELETE;
        }
    }

    // Start action class
    private class StartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureSpringCloudMvpModel.startApp(SpringCloudAppNode.this.app.id(), SpringCloudAppNode.this.app.properties().activeDeploymentName()).await();
            SpringCloudMonitorUtil.awaitAndMonitoringStatus(SpringCloudAppNode.this.app.id(), SpringCloudAppNode.this.status);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.START.getDoingName(), SpringCloudModule.MODULE_NAME, SpringCloudAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.SpringCloud.START;
        }
    }

    // Stop action class
    private class StopAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureSpringCloudMvpModel.stopApp(SpringCloudAppNode.this.app.id(), SpringCloudAppNode.this.app.properties().activeDeploymentName()).await();
            SpringCloudMonitorUtil.awaitAndMonitoringStatus(SpringCloudAppNode.this.app.id(), SpringCloudAppNode.this.status);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.STOP.getDoingName(), SpringCloudModule.MODULE_NAME, SpringCloudAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.SpringCloud.STOP;
        }
    }

    // Restart action class
    private class RestartAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            AzureSpringCloudMvpModel.restartApp(SpringCloudAppNode.this.app.id(), SpringCloudAppNode.this.app.properties().activeDeploymentName()).await();
            SpringCloudMonitorUtil.awaitAndMonitoringStatus(SpringCloudAppNode.this.app.id(), SpringCloudAppNode.this.status);
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.RESTART.getDoingName(), SpringCloudModule.MODULE_NAME, SpringCloudAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.SpringCloud.RESTART;
        }
    }

    // Open in portal action class
    private class OpenInPortalAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            SpringCloudAppNode.this.openResourcesInPortal(SpringCloudAppNode.this.getSubscriptionId(), SpringCloudAppNode.this.getAppId());
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage(AzureActionEnum.OPEN_IN_PORTAL.getDoingName(), SpringCloudModule.MODULE_NAME, SpringCloudAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.SpringCloud.OPEN_IN_PORTAL;
        }
    }

    // Show Properties
    private class ShowPropertiesAction extends NodeActionListener implements ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().openSpringCloudAppPropertyView(SpringCloudAppNode.this);
            // add this statement for false updating notice to update Property view
            // immediately
            SpringCloudStateManager.INSTANCE.notifySpringAppUpdate(SpringCloudAppNode.this.clusterId,
                    SpringCloudAppNode.this.app, SpringCloudAppNode.this.deploy);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.SpringCloud.SHOW_PROPERTIES;
        }
    }

    // Open in browser action class
    private class OpenInBrowserAction extends NodeActionListener implements ActionBackgroundable, ActionTelemetrable {

        @Override
        protected void actionPerformed(NodeActionEvent e) {
            if (StringUtils.isNotEmpty(SpringCloudAppNode.this.app.properties().url())) {
                DefaultLoader.getUIHelper().openInBrowser(SpringCloudAppNode.this.app.properties().url());
            } else {
                DefaultLoader.getUIHelper().showInfo(SpringCloudAppNode.this, "Public url is not available for app: " + app.name());
            }
        }

        @Override
        public String getProgressMessage() {
            return Node.getProgressMessage("Opening", SpringCloudModule.MODULE_NAME, SpringCloudAppNode.this.name);
        }

        @Override
        public TelemetryParameter getTelemetryParameter() {
            return TelemetryParameter.SpringCloud.OPEN_IN_BROWSER;
        }
    }
}
