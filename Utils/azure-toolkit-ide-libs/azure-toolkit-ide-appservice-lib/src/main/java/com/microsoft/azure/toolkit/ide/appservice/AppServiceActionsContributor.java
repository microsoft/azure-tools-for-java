/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class AppServiceActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> START_STREAM_LOG = AppServiceAppBase.START_STREAM_LOG;
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> STOP_STREAM_LOG = Action.Id.of("user/$appservice.close_log_stream.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> OPEN_LOGS_IN_MONITOR = Action.Id.of("user/$appservice.open_azure_monitor.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> REFRESH_DEPLOYMENT_SLOTS = Action.Id.of("user/$appservice.refresh_deployments.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> OPEN_IN_BROWSER = AppServiceAppBase.OPEN_IN_BROWSER;
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> SSH_INTO_WEBAPP = Action.Id.of("user/webapp.connect_ssh.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> PROFILE_FLIGHT_RECORD = Action.Id.of("user/webapp.profile_flight_recorder.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> COPY_FULL_APP_SETTINGS = Action.Id.of("user/$appservice.copy_app_settings.app");
    public static final Action.Id<AppServiceAppBase<?, ?, ?>> UPDATE_IMAGE = Action.Id.of("user/$appservice.update_image.app");
    public static final Action.Id<Map.Entry<String, String>> COPY_APP_SETTING = Action.Id.of("user/$appservice.copy_app_setting");
    public static final Action.Id<Map.Entry<String, String>> COPY_APP_SETTING_KEY = Action.Id.of("user/$appservice.copy_app_setting_key.key");
    public static final String APP_SETTINGS_ACTIONS = "actions.appservice.app_settings";

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(OPEN_IN_BROWSER)
            .withLabel("Open In Browser")
            .withIcon(AzureIcons.Action.PORTAL.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((s, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://" + s.getHostName(), e))
            .register(am);

        new Action<>(START_STREAM_LOG)
            .withLabel("Start Streaming Logs")
            .withIcon(AzureIcons.Action.LOG.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase && !(s instanceof FunctionApp app && (app.isContainerHostingFunctionApp() || app.isFlexConsumptionApp())))
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);

        new Action<>(STOP_STREAM_LOG)
            .withLabel("Stop Streaming Logs")
            .withIcon(AzureIcons.Action.LOG.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase && !(s instanceof FunctionApp app && (app.isContainerHostingFunctionApp() || app.isFlexConsumptionApp())))
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);

        new Action<>(PROFILE_FLIGHT_RECORD)
            .withLabel("Profile Flight Recorder")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);

        new Action<>(SSH_INTO_WEBAPP)
            .withLabel("SSH into Web App")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);

        new Action<>(REFRESH_DEPLOYMENT_SLOTS)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .withHandler(app -> AzureEventBus.emit("appservice.slot.refresh", app))
            .withShortcut(am.getIDEDefaultShortcuts().refresh())
            .register(am);

        new Action<>(OPEN_LOGS_IN_MONITOR)
            .withLabel("Open Logs with Azure Monitor")
            .withIcon(AzureIcons.Common.AZURE_MONITOR.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AppServiceAppBase)
            .visibleWhen(s -> s instanceof AppServiceAppBase && !(s instanceof FunctionApp app && (app.isContainerHostingFunctionApp() || app.isFlexConsumptionApp())))
            .register(am);

        new Action<>(COPY_FULL_APP_SETTINGS)
            .withLabel("Copy All")
            .withIdParam(AppServiceAppBase::getName)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withHandler((app, e) -> {
                final Map<String, String> variables = Optional.ofNullable(app.getAppSettings()).orElse(Collections.emptyMap());
                final String str = variables.entrySet().stream().map(v -> String.format("%s=%s", v.getKey(), v.getValue())).collect(Collectors.joining(System.lineSeparator()));
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.COPY_STRING).handle(str, e);
                AzureMessager.getMessager().success(AzureString.format("App settings of app (%s) has been copied into clipboard.", app.getName()));
            })
            .withAuthRequired(false)
            .register(am);

        new Action<>(UPDATE_IMAGE)
                .withLabel("Update Image")
                .withIdParam(AbstractAzResource::getName)
                .withIcon(AzureIcons.Action.UPLOAD.getIconPath())
                .withAuthRequired(true)
                .visibleWhen(s -> s instanceof AppServiceAppBase<?,?,?> app && !app.getFormalStatus().isReading() &&
                        Optional.ofNullable(app.getRuntime()).map(Runtime::isDocker).orElse(false))
                .enableWhen(s -> s.getFormalStatus().isConnected())
                .register(am);

        new Action<>(COPY_APP_SETTING)
                .withLabel("Copy")
                .withIcon(AzureIcons.Action.COPY.getIconPath())
                .withHandler((entry, e) -> {
                    final String str = String.format("%s=%s", entry.getKey(), entry.getValue());
                    AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.COPY_STRING).handle(str, e);
                    AzureMessager.getMessager().success(AzureString.format("App setting (%s) has been copied into clipboard.", entry.getKey()));
                })
                .withAuthRequired(false)
                .register(am);

        new Action<>(COPY_APP_SETTING_KEY)
                .withLabel("Copy Key")
                .withIdParam(Map.Entry::getKey)
                .withIcon(AzureIcons.Action.COPY.getIconPath())
                .withHandler((entry, e) -> {
                    AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.COPY_STRING).handle(entry.getKey(), e);
                    AzureMessager.getMessager().success(AzureString.format("App setting key (%s) been copied into clipboard.", entry.getKey()));
                })
                .withAuthRequired(false)
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        am.registerGroup(APP_SETTINGS_ACTIONS, new ActionGroup(AppServiceActionsContributor.COPY_FULL_APP_SETTINGS));
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AzResource, Object> startCondition = (r, e) -> r instanceof AppServiceAppBase<?, ?, ?> &&
            StringUtils.equals(r.getStatus(), AzResource.Status.STOPPED);
        final BiConsumer<AzResource, Object> startHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).start();
        am.registerHandler(ResourceCommonActionsContributor.START, startCondition, startHandler);

        final BiPredicate<AzResource, Object> stopCondition = (r, e) -> r instanceof AppServiceAppBase<?, ?, ?> &&
            StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<AzResource, Object> stopHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).stop();
        am.registerHandler(ResourceCommonActionsContributor.STOP, stopCondition, stopHandler);

        final BiPredicate<AzResource, Object> restartCondition = (r, e) -> r instanceof AppServiceAppBase<?, ?, ?> &&
            StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<AzResource, Object> restartHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).restart();
        am.registerHandler(ResourceCommonActionsContributor.RESTART, restartCondition, restartHandler);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
