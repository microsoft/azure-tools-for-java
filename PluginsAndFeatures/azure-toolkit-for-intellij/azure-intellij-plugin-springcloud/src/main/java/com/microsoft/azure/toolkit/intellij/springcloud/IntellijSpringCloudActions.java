/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActions;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActions;
import com.microsoft.azure.toolkit.intellij.springcloud.creation.CreateSpringCloudAppAction;
import com.microsoft.azure.toolkit.intellij.springcloud.deplolyment.DeploySpringCloudAppAction;
import com.microsoft.azure.toolkit.intellij.springcloud.streaminglog.SpringCloudStreamingLogAction;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class IntellijSpringCloudActions implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        this.registerCreateAppActionHandler(am);
        this.registerDeployAppActionHandler(am);
        this.registerShowPropertiesActionHandler(am);
        this.registerStreamLogActionHandler(am);
    }

    private void registerShowPropertiesActionHandler(AzureActionManager am) {
        final BiPredicate<IAzureResource<?>, AnActionEvent> condition = (r, e) -> r instanceof SpringCloudCluster;
        final BiConsumer<IAzureResource<?>, AnActionEvent> handler = (c, e) -> CreateSpringCloudAppAction.createApp((SpringCloudCluster) c, e.getProject());
        am.registerHandler(ResourceCommonActions.CREATE, condition, handler);
    }

    private void registerCreateAppActionHandler(AzureActionManager am) {
        final BiPredicate<IAzureResource<?>, AnActionEvent> condition = (r, e) -> r instanceof SpringCloudCluster;
        final BiConsumer<IAzureResource<?>, AnActionEvent> handler = (c, e) -> CreateSpringCloudAppAction.createApp((SpringCloudCluster) c, e.getProject());
        am.registerHandler(ResourceCommonActions.CREATE, condition, handler);
    }

    private void registerDeployAppActionHandler(AzureActionManager am) {
        final BiPredicate<IAzureResource<?>, AnActionEvent> condition = (r, e) -> r instanceof SpringCloudApp && Objects.nonNull(e.getProject());
        final BiConsumer<IAzureResource<?>, AnActionEvent> handler = (c, e) -> DeploySpringCloudAppAction.deploy((SpringCloudApp) c, e.getProject());
        am.registerHandler(ResourceCommonActions.DEPLOY, condition, handler);
    }

    private void registerStreamLogActionHandler(AzureActionManager am) {
        final BiPredicate<SpringCloudApp, AnActionEvent> condition = (r, e) -> true;
        final BiConsumer<SpringCloudApp, AnActionEvent> handler = (c, e) -> SpringCloudStreamingLogAction.startLogStreaming(c, e.getProject());
        am.registerHandler(SpringCloudActions.STREAM_LOG, condition, handler);
    }

    @Override
    public int getZOrder() {
        return IActionsContributor.super.getZOrder();
    }
}
