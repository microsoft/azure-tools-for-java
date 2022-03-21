/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.file.AppServiceFileActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.intellij.appservice.actions.AppServiceFileAction;
import com.microsoft.azure.toolkit.intellij.appservice.actions.OpenAppServicePropertyViewAction;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.action.ProfileFlightRecordAction;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.action.SSHIntoWebAppAction;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.action.StartStreamingLogsAction;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.action.StopStreamingLogsAction;
import com.microsoft.azure.toolkit.intellij.legacy.docker.action.PushToContainerRegistryAction;
import com.microsoft.azure.toolkit.intellij.legacy.function.action.CreateFunctionAppAction;
import com.microsoft.azure.toolkit.intellij.legacy.function.action.DeployFunctionAppAction;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.CreateWebAppAction;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.action.DeployWebAppAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureFunction;
import com.microsoft.azure.toolkit.lib.appservice.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class AppServiceIntelliJActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER =
            Math.max(AppServiceActionsContributor.INITIALIZE_ORDER, ContainerRegistryActionsContributor.INITIALIZE_ORDER) + 1;
    private static final String UNABLE_TO_OPEN_EXPLORER = "Unable to open explorer";

    @Override
    public void registerActions(AzureActionManager am) {
        final BiConsumer<AppServiceFile, AnActionEvent> openFileHandler = (file, e) -> AzureTaskManager
                .getInstance().runLater(() -> new AppServiceFileAction().openAppServiceFile(file, e.getProject()));
        final ActionView.Builder openFileView = new ActionView.Builder("Open File", null)
                .title(s -> Optional.ofNullable(s)
                        .map(r -> AzureString.format("appservice|file.download", ((AppServiceFile) r).getName()))
                        .orElse(null))
                .enabled(s -> s instanceof AppServiceFile);
        final Action<AppServiceFile> openFileAction = new Action<>(openFileHandler, openFileView);
        openFileAction.setShortcuts(am.getIDEDefaultShortcuts().edit());
        am.registerAction(AppServiceFileActionsContributor.APP_SERVICE_FILE_VIEW, openFileAction);

        final BiConsumer<AppServiceFile, AnActionEvent> downloadFileHandler = (file, e) -> AzureTaskManager
                .getInstance().runLater(() -> new AppServiceFileAction().saveAppServiceFile(file, e.getProject(), null));
        final ActionView.Builder downloadFileView = new ActionView.Builder("Download", null)
                .title(s -> Optional.ofNullable(s)
                        .map(r -> AzureString.format("appservice|file.download", ((AppServiceFile) r).getName()))
                        .orElse(null))
                .enabled(s -> s instanceof AppServiceFile);
        final Action<AppServiceFile> downloadFileAction = new Action<>(downloadFileHandler, downloadFileView);
        downloadFileAction.setShortcuts("control alt D");
        am.registerAction(AppServiceFileActionsContributor.APP_SERVICE_FILE_DOWNLOAD, downloadFileAction);
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<IAppService<?>, AnActionEvent> isAppService = (r, e) -> r instanceof IAppService<?>;
        final BiConsumer<IAppService<?>, AnActionEvent> flightRecorderHandler = (c, e) ->
                AzureTaskManager.getInstance().runLater(() -> new ProfileFlightRecordAction(c, e.getProject()).execute());
        am.registerHandler(AppServiceActionsContributor.PROFILE_FLIGHT_RECORD, isAppService, flightRecorderHandler);

        final BiConsumer<IAppService<?>, AnActionEvent> startStreamingLogHandler = (c, e) ->
                AzureTaskManager.getInstance().runLater(() -> new StartStreamingLogsAction(c, e.getProject()).execute());
        am.registerHandler(AppServiceActionsContributor.START_STREAM_LOG, isAppService, startStreamingLogHandler);

        final BiConsumer<IAppService<?>, AnActionEvent> stopStreamingLogHandler = (c, e) ->
                AzureTaskManager.getInstance().runLater(() -> new StopStreamingLogsAction(c, e.getProject()).execute());
        am.registerHandler(AppServiceActionsContributor.STOP_STREAM_LOG, isAppService, stopStreamingLogHandler);

        final BiPredicate<IAppService<?>, AnActionEvent> isWebApp = (r, e) -> r instanceof WebApp;
        final BiConsumer<IAppService<?>, AnActionEvent> sshHandler = (c, e) ->
                AzureTaskManager.getInstance().runLater(() -> new SSHIntoWebAppAction((WebApp) c, e.getProject()).execute());
        am.registerHandler(AppServiceActionsContributor.SSH_INTO_WEBAPP, isAppService, sshHandler);

        final BiConsumer<IAzureBaseResource<?, ?>, AnActionEvent> deployWebAppHandler = (c, e) -> AzureTaskManager
                .getInstance().runLater(() -> new DeployWebAppAction((WebApp) c, e.getProject()).execute());
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, (r, e) -> r instanceof WebApp, deployWebAppHandler);

        final BiConsumer<Object, AnActionEvent> createWebAppHandler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> new CreateWebAppAction(e.getProject()).execute());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof AzureWebApp, createWebAppHandler);

        final BiConsumer<IAzureBaseResource<?, ?>, AnActionEvent> deployFunctionAppHandler = (c, e) -> AzureTaskManager
                .getInstance().runLater(() -> new DeployFunctionAppAction((FunctionApp) c, e.getProject()).execute());
        am.registerHandler(ResourceCommonActionsContributor.DEPLOY, (r, e) -> r instanceof FunctionApp, deployFunctionAppHandler);

        final BiConsumer<Object, AnActionEvent> createFunctionHandler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> new CreateFunctionAppAction(e.getProject()).execute());
        am.registerHandler(ResourceCommonActionsContributor.CREATE, (r, e) -> r instanceof AzureFunction, createFunctionHandler);

        final BiConsumer<AzResourceBase, AnActionEvent> showFunctionPropertyViewHandler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> new OpenAppServicePropertyViewAction().openFunctionAppPropertyView((FunctionApp) c, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, (r, e) -> r instanceof FunctionApp, showFunctionPropertyViewHandler);

        final BiConsumer<AzResourceBase, AnActionEvent> showWebAppPropertyViewHandler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> new OpenAppServicePropertyViewAction().openWebAppPropertyView((WebApp) c, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, (r, e) -> r instanceof WebApp, showWebAppPropertyViewHandler);

        final BiConsumer<AzResourceBase, AnActionEvent> showWebAppSlotPropertyViewHandler = (c, e) -> AzureTaskManager.getInstance()
                .runLater(() -> new OpenAppServicePropertyViewAction().openDeploymentSlotPropertyView((WebAppDeploymentSlot) c, e.getProject()));
        am.registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES, (r, e) -> r instanceof WebAppDeploymentSlot, showWebAppSlotPropertyViewHandler);

        final BiPredicate<FunctionEntity, AnActionEvent> triggerPredicate = (r, e) -> r instanceof FunctionEntity;
        final BiConsumer<FunctionEntity, AnActionEvent> triggerFunctionHandler = (entity, e) -> {
            final String functionId = Optional.ofNullable(entity.getFunctionAppId())
                    .orElseGet(() -> ResourceId.fromString(entity.getTriggerId()).parent().id());
            final FunctionApp functionApp = Azure.az(AzureFunction.class).get(functionId);
            final String triggerType = Optional.ofNullable(entity.getTrigger())
                    .map(functionTrigger -> functionTrigger.getProperty("type")).orElse(null);
            final Object request;
            if (StringUtils.equalsIgnoreCase(triggerType, "timertrigger")) {
                request = new Object();
            } else {
                final String input = AzureTaskManager.getInstance().runAndWaitAsObservable(new AzureTask<>(() -> Messages.showInputDialog(e.getProject(), "Please set the input value: ",
                        String.format("Trigger function %s", entity.getName()), null))).toBlocking().single();
                if (input == null) {
                    return;
                }
                request = new TriggerRequest(input);
            }
            functionApp.triggerFunction(entity.getName(), request);
        };
        am.registerHandler(FunctionAppActionsContributor.TRIGGER_FUNCTION, triggerPredicate, triggerFunctionHandler);

        // keep push docker image in app service library as form is shared between appservice/container repository but could not split into different project
        final BiPredicate<ContainerRegistry, AnActionEvent> pushImageCondition = (r, e) -> r instanceof ContainerRegistry;
        final BiConsumer<ContainerRegistry, AnActionEvent> pushImageHandler =
            (c, e) -> PushToContainerRegistryAction.execute(c, e.getProject());
        am.registerHandler(ContainerRegistryActionsContributor.PUSH_IMAGE, pushImageCondition, pushImageHandler);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }

    @RequiredArgsConstructor
    static class TriggerRequest {
        private final String input;
    }
}
