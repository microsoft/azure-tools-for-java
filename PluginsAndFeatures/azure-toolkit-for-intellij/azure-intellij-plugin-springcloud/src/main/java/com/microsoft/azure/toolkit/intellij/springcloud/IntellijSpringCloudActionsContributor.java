/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActionsContributor;
import com.microsoft.azure.toolkit.intellij.springcloud.creation.CreateSpringCloudAppAction;
import com.microsoft.azure.toolkit.intellij.springcloud.deplolyment.DeploySpringCloudAppAction;
import com.microsoft.azure.toolkit.intellij.springcloud.remotedebug.SpringCloudAppEnableDebuggingAction;
import com.microsoft.azure.toolkit.intellij.springcloud.remotedebug.SpringCloudAppInstanceDebuggingAction;
import com.microsoft.azure.toolkit.intellij.springcloud.streaminglog.SpringCloudStreamingLogAction;
import com.microsoft.azure.toolkit.intellij.springcloud.streaminglog.SpringCloudStreamingLogManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijSpringCloudActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        this.registerCreateServiceActionHandler(am);
        this.registerCreateAppActionHandler(am);
        this.registerDeployAppActionHandler(am);
        this.registerStreamLogActionHandler(am);
        this.registerStreamLogInstanceActionHandler(am);
        this.registerEnableRemoteDebuggingHandler(am);
        this.registerDisableRemoteDebuggingHandler(am);
        this.registerStartDebuggingHandler(am);
        this.registerStartDebuggingAppHandler(am);
    }

    private void registerCreateServiceActionHandler(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureSpringCloud;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> {
            final IAccount account = Azure.az(IAzureAccount.class).account();
            final String url = String.format("%s/#create/Microsoft.AppPlatform", account.getPortalUrl());
            am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url, null);
        };
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);
        am.registerHandler(SpringCloudActionsContributor.GROUP_CREATE_CLUSTER, (r, e) -> true, handler);
    }

    private void registerCreateAppActionHandler(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof SpringCloudCluster;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) -> CreateSpringCloudAppAction.createApp((SpringCloudCluster) c, e.getProject());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);
    }

    private void registerDeployAppActionHandler(AzureActionManager am) {
        final BiPredicate<AzResource<?, ?, ?>, AnActionEvent> condition = (r, e) -> r instanceof SpringCloudApp && Objects.nonNull(e.getProject());
        final BiConsumer<AzResource<?, ?, ?>, AnActionEvent> handler = (c, e) -> {
            final Project project = Objects.requireNonNull(e.getProject());
            DeploySpringCloudAppAction.deploy((SpringCloudApp) c, project);
        };
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, condition, handler);
    }

    private void registerStreamLogActionHandler(AzureActionManager am) {
        final BiPredicate<SpringCloudApp, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<SpringCloudApp, AnActionEvent> handler = (c, e) -> SpringCloudStreamingLogAction.startLogStreaming(c, e.getProject());
        am.registerHandler(SpringCloudActionsContributor.STREAM_LOG, condition, handler);
    }

    private void registerStreamLogInstanceActionHandler(AzureActionManager am) {
        final BiPredicate<SpringCloudAppInstance, AnActionEvent> condition = (r, e) -> r.getFormalStatus().isRunning();
        final BiConsumer<SpringCloudAppInstance, AnActionEvent> handler = (c, e) -> SpringCloudStreamingLogManager.getInstance().showStreamingLog(e.getProject(), c.getParent().getParent(), c.getName());
        am.registerHandler(SpringCloudActionsContributor.STREAM_LOG_INSTANCE, condition, handler);
    }

    private void registerEnableRemoteDebuggingHandler(AzureActionManager am) {
        final BiPredicate<SpringCloudApp, AnActionEvent> condition = (r, e) -> r.getFormalStatus().isRunning();
        final BiConsumer<SpringCloudApp, AnActionEvent> handler = (c, e) -> SpringCloudAppEnableDebuggingAction.enableRemoteDebugging(c, e.getProject());
        am.registerHandler(SpringCloudActionsContributor.ENABLE_REMOTE_DEBUGGING, condition, handler);
    }

    private void registerDisableRemoteDebuggingHandler(AzureActionManager am) {
        final BiPredicate<SpringCloudApp, AnActionEvent> condition = (r, e) -> r.getFormalStatus().isRunning();
        final BiConsumer<SpringCloudApp, AnActionEvent> handler = (c, e) -> SpringCloudAppEnableDebuggingAction.disableRemoteDebugging(c, e.getProject());
        am.registerHandler(SpringCloudActionsContributor.DISABLE_REMOTE_DEBUGGING, condition, handler);
    }

    private void registerStartDebuggingHandler(AzureActionManager am) {
        final BiPredicate<SpringCloudAppInstance, AnActionEvent> condition = (r, e) -> r.getFormalStatus().isRunning();
        final BiConsumer<SpringCloudAppInstance, AnActionEvent> handler = (c, e) -> SpringCloudAppInstanceDebuggingAction.startDebugging(c, e.getProject());
        am.registerHandler(SpringCloudActionsContributor.REMOTE_DEBUGGING, condition, handler);
    }

    private void registerStartDebuggingAppHandler(AzureActionManager am) {
        final BiPredicate<SpringCloudApp, AnActionEvent> condition = (r, e) -> r.getFormalStatus().isRunning();
        final BiConsumer<SpringCloudApp, AnActionEvent> handler = (c, e) -> SpringCloudAppInstanceDebuggingAction.startDebuggingApp(c, e.getProject());
        am.registerHandler(SpringCloudActionsContributor.REMOTE_DEBUGGING_APP, condition, handler);
    }

    @Override
    public int getOrder() {
        return SpringCloudActionsContributor.INITIALIZE_ORDER + 1;
    }
}
