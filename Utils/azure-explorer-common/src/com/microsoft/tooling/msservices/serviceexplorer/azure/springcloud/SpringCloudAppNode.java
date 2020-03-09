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
 *
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud;

import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentResource;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.AppResourceInner;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.WrappedTelemetryNodeActionListener;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class SpringCloudAppNode extends Node implements SpringCloudAppNodeView {
    private final AppResourceInner app;
    private final String clusterName;
    private static final String ACTION_START = "Start";
    private static final String ACTION_STOP = "Stop";
    private static final String ACTION_DELETE = "Delete";
    private static final String ACTION_RESTART = "Restart";
    private static final String ACTION_OPEN_IN_BROWSER = "Open In Browser";

    protected static final String ACTION_SHOW_PROPERTY = "Show Properties";
    private final SpringCloudAppNodePresenter springCloudAppNodePresenter;
    private DeploymentResource deploy;

    public SpringCloudAppNode(AppResourceInner app, DeploymentResource deploy, SpringCloudNode parent) {
        super(app.id(), String.format("%s - %s", app.name(), getStatus(deploy)), parent,
                String.format("azure-springcloud-app-%s.png", getStatusIcon(deploy)));
        this.app = app;
        this.deploy = deploy;
        this.clusterName = parent.getServiceName();
        springCloudAppNodePresenter = new SpringCloudAppNodePresenter();
        springCloudAppNodePresenter.onAttachView(this);

        loadActions();
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getAppName() {
        return app.name();
    }

    public String getAppId() {
        return app.id();
    }

    public String getResourceGroup() {
        return getResourceGroup(this.app.id());
    }

    public static String getResourceGroup(String serviceId) {
        final String[] attributes = serviceId.split("/");
        return attributes[ArrayUtils.indexOf(attributes, "resourceGroups") + 1];
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_START, new WrappedTelemetryNodeActionListener("AZURE_SPRING_CLOUD", "start-springcloud-app",
                createBackgroundActionListener("Starting", () -> startSpringCloudApp())));
        addAction(ACTION_STOP, new WrappedTelemetryNodeActionListener("AZURE_SPRING_CLOUD", "stop-springcloud-app",
                createBackgroundActionListener("Stopping", () -> stopSpringCloudApp())));

        addAction(ACTION_RESTART, new WrappedTelemetryNodeActionListener("AZURE_SPRING_CLOUD", "retstart-springcloud-app",
                createBackgroundActionListener("Restarting", () -> restartSpringCloudApp())));
        addAction(ACTION_DELETE, new WrappedTelemetryNodeActionListener("AZURE_SPRING_CLOUD", "delete-springcloud-app",
                createBackgroundActionListener("Deleting", () -> deleteSpringCloudApp())));
        addAction(ACTION_OPEN_IN_BROWSER, new WrappedTelemetryNodeActionListener("AZURE_SPRING_CLOUD", "open-inbrowser-springcloud",
                new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) {
                        if (StringUtils.isNotBlank(app.properties().url())) {
                            DefaultLoader.getUIHelper().openInBrowser(app.properties().url());
                        }
                    }
                }));
        addAction(ACTION_SHOW_PROPERTY, new WrappedTelemetryNodeActionListener("AZURE_SPRING_CLOUD", "showprop-springcloud-app",
                new NodeActionListener() {
                    @Override
                    protected void actionPerformed(NodeActionEvent e) {
                        DefaultLoader.getUIHelper().openSpringCloudAppPropertyView(SpringCloudAppNode.this);
                    }
                }));
        super.loadActions();
    }

    @Override
    public void updateStatus(String status) {
        if (status.endsWith("ing") && !StringUtils.equalsIgnoreCase(status, "running")) {
            status = "pending";
        }
        this.setIconPath(String.format("azure-springcloud-app-%s.png", status.toLowerCase()));
    }

    private static NodeActionListener createBackgroundActionListener(final String actionName, final Runnable runnable) {
        return new NodeActionListener() {
            @Override
            protected void actionPerformed(NodeActionEvent e) {
                DefaultLoader.getIdeHelper().runInBackground(null, actionName, false,
                        true, String.format("%s...", actionName), runnable);
            }
        };
    }

    private static String getStatus(DeploymentResource deploy) {
        return deploy == null ? "Stopped" : deploy.properties().status().toString();
    }

    private static String getStatusIcon(DeploymentResource deploy) {
        String status = getStatus(deploy);
        if (status.endsWith("ing") && !StringUtils.equalsIgnoreCase(status, "running")) {
            return "pending";
        }
        return status.toLowerCase();
    }

    private void startSpringCloudApp() {
        springCloudAppNodePresenter.onStartSpringCloudApp(this.app.id(), this.app.properties().activeDeploymentName());
    }

    private void restartSpringCloudApp() {
        springCloudAppNodePresenter.onStartSpringCloudApp(this.app.id(), this.app.properties().activeDeploymentName());
    }

    private void stopSpringCloudApp() {
        springCloudAppNodePresenter.onStopSpringCloudApp(this.app.id(), this.app.properties().activeDeploymentName());
    }

    private void deleteSpringCloudApp() {
        springCloudAppNodePresenter.onDeleteSpringCloudApp(this.app.id());
    }
}
