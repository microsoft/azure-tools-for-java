/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.containerapps.ContainerAppsActionsContributor;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsManager;
import com.microsoft.azure.toolkit.intellij.containerapps.action.DeployImageAction;
import com.microsoft.azure.toolkit.intellij.containerapps.creation.CreateContainerAppAction;
import com.microsoft.azure.toolkit.intellij.containerapps.creation.CreateContainerAppsEnvironmentAction;
import com.microsoft.azure.toolkit.intellij.containerapps.streaminglog.ContainerSelectionDialog;
import com.microsoft.azure.toolkit.intellij.containerapps.streaminglog.StreamingToolwindowSelectionDialog;
import com.microsoft.azure.toolkit.intellij.containerapps.update.UpdateAppAction;
import com.microsoft.azure.toolkit.intellij.monitor.AzureMonitorManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.EnvironmentType;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.Azure.az;

public class IntelliJContainerAppsActionsContributor implements IActionsContributor {

    public static final Action.Id<VirtualFile> DEPLOY_IMAGE_TO_ACA = Action.Id.of("user/containerapps.deploy_image");

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<ContainerApp, AnActionEvent> serviceCondition = (r, e) -> r != null;
        am.<ContainerApp, AnActionEvent>registerHandler(ContainerAppsActionsContributor.UPDATE_IMAGE, UpdateAppAction::openUpdateDialog);
        am.<Tag, AnActionEvent>registerHandler(ContainerRegistryActionsContributor.DEPLOY_IMAGE_ACA, UpdateAppAction::openUpdateDialog);

        am.registerHandler(ContainerAppsActionsContributor.CREATE_CONTAINER_APP,
                (ContainerAppsEnvironment r, AnActionEvent e) -> r.getFormalStatus().isConnected(),
                (ContainerAppsEnvironment r, AnActionEvent e) -> CreateContainerAppAction.create(e.getProject(), getContainerAppDefaultConfig(r, null)));
        am.registerHandler(ContainerAppsActionsContributor.GROUP_CREATE_CONTAINER_APP,
                (ResourceGroup r, AnActionEvent e) -> CreateContainerAppAction.create(e.getProject(), getContainerAppDefaultConfig(null, r)));
        am.registerHandler(ContainerAppsActionsContributor.SERVICE_CREATE_CONTAINER_APP,
                (Object r, AnActionEvent e) -> {
                    if (r instanceof ContainerAppsEnvironment env && env.getFormalStatus().isConnected()) {
                        CreateContainerAppAction.create(e.getProject(), getContainerAppDefaultConfig(env, null));
                    } else if (r instanceof ResourceGroup rg) {
                        CreateContainerAppAction.create(e.getProject(), getContainerAppDefaultConfig(null, rg));
                    } else {
                        CreateContainerAppAction.create(e.getProject(), getContainerAppDefaultConfig(null, null));
                    }
                });
        am.registerHandler(ContainerAppsActionsContributor.CREATE_CONTAINER_APPS_ENVIRONMENT,
                (AzureContainerApps r, AnActionEvent e) -> CreateContainerAppsEnvironmentAction.create(e.getProject(), getContainerAppsEnvironmentDefaultConfig(null)));
        am.registerHandler(ContainerAppsActionsContributor.GROUP_CREATE_CONTAINER_APPS_ENVIRONMENT,
                (ResourceGroup r, AnActionEvent e) -> CreateContainerAppsEnvironmentAction.create(e.getProject(), getContainerAppsEnvironmentDefaultConfig(r)));
        am.registerHandler(ContainerAppsActionsContributor.OPEN_LOGS_IN_MONITOR, (ContainerApp app, AnActionEvent e) -> {
            final LogAnalyticsWorkspace workspace = getWorkspace(app);
            Optional.ofNullable(e.getProject()).ifPresent(project -> AzureTaskManager.getInstance().runLater(() ->
                    AzureMonitorManager.getInstance().openMonitorWindow(e.getProject(), workspace, app.getId())));
        });
        am.registerHandler(ContainerAppsActionsContributor.START_SYSTEM_LOG_STREAMS, (ContainerApp app, AnActionEvent e) -> {
            final Flux<String> logs = app.streamingLogs(true, 20);
            AzureTaskManager.getInstance().runLater(() ->
                    StreamingLogsManager.getInstance().showStreamingLog(e.getProject(), app.getId(), app.getName(), logs));
        });
        am.registerHandler(ContainerAppsActionsContributor.START_CONSOLE_LOG_STREAMS, (ContainerApp app, AnActionEvent e) ->
                showConsoleStreamingLog(e.getProject(), app));
        am.registerHandler(ContainerAppsActionsContributor.START_ENV_LOG_STREAM,
                (ContainerAppsEnvironment appsEnvironment, AnActionEvent e) ->
                        !StreamingLogsManager.getInstance().isStreamingLogStarted(e.getProject(), appsEnvironment.getId()),
                (ContainerAppsEnvironment appsEnvironment, AnActionEvent e) -> {
                    final Flux<String> logs = appsEnvironment.streamingLogs(true, 20);
                    AzureTaskManager.getInstance().runLater(() ->
                            StreamingLogsManager.getInstance().showStreamingLog(e.getProject(), appsEnvironment.getId(), appsEnvironment.getName(), logs));
                });
        am.registerHandler(ContainerAppsActionsContributor.STOP_ENV_LOG_STREAM,
                (ContainerAppsEnvironment appsEnvironment, AnActionEvent e) ->
                        StreamingLogsManager.getInstance().isStreamingLogStarted(e.getProject(), appsEnvironment.getId()),
                (ContainerAppsEnvironment appsEnvironment, AnActionEvent e) ->
                        StreamingLogsManager.getInstance().closeStreamingLog(e.getProject(), appsEnvironment.getId()));
        am.registerHandler(ContainerAppsActionsContributor.STOP_APP_LOG_STREAMS,
                (ContainerApp app, AnActionEvent e) ->
                        StreamingLogsManager.getInstance().isStreamingLogStarted(e.getProject(), app.getId()),
                (ContainerApp app, AnActionEvent e) -> AzureTaskManager.getInstance().runLater(() -> {
                    final StreamingToolwindowSelectionDialog dialog = new StreamingToolwindowSelectionDialog(e.getProject(), app);
                    dialog.show();
                }));
    }

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(DEPLOY_IMAGE_TO_ACA)
                .withLabel("Deploy Image to Container App")
                .withIcon("/icons/ContainerAppDeploy.svg")
                .visibleWhen(s -> s instanceof VirtualFile)
                .withHandler((final VirtualFile file, final AnActionEvent event) ->
                        DeployImageAction.deployImageToAzureContainerApps(file, null, event))
                .register(am);
    }

    public static ContainerAppDraft.Config getContainerAppDefaultConfig(@Nullable final ContainerAppsEnvironment o, @Nullable final ResourceGroup resourceGroup) {
        final ContainerAppDraft.Config result = new ContainerAppDraft.Config();
        result.setName(Utils.generateRandomResourceName("aca", 32));
        final List<Subscription> subs = Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
        if (CollectionUtils.isEmpty(subs)) {
            throw new AzureToolkitRuntimeException("there are no subscription selected in your account");
        }
        final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
        final Subscription sub = Optional.ofNullable(historySub).orElseGet(() -> subs.get(0));
        result.setSubscription(sub);
        final ContainerAppsEnvironment cae = Optional.ofNullable(o).orElseGet(() -> CacheManager.getUsageHistory(ContainerAppsEnvironment.class)
                .peek(r -> r.getSubscriptionId().equals(sub.getId())));
        result.setEnvironment(cae);
        final ResourceGroup historyRg = CacheManager.getUsageHistory(ResourceGroup.class)
                .peek(r -> r.getSubscriptionId().equals(sub.getId()));
        final ResourceGroup rg = Optional.ofNullable(resourceGroup).orElseGet(() ->
                Optional.ofNullable(cae).map(ContainerAppsEnvironment::getResourceGroup).orElse(historyRg));
        result.setResourceGroup(rg);
        return result;
    }

    public static ContainerAppsEnvironmentDraft.Config getContainerAppsEnvironmentDefaultConfig(final ResourceGroup resourceGroup) {
        final ContainerAppsEnvironmentDraft.Config result = new ContainerAppsEnvironmentDraft.Config();
        result.setName(Utils.generateRandomResourceName("cae", 32));
        final List<Subscription> subs = Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
        if (CollectionUtils.isEmpty(subs)) {
            throw new AzureToolkitRuntimeException("there are no subscription selected in your account");
        }
        final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
        final Subscription sub = Optional.ofNullable(historySub).orElseGet(() -> subs.get(0));
        result.setSubscription(sub);
        final List<Region> regions = az(AzureAccount.class).listRegions(sub.getId());
        final Region historyRegion = CacheManager.getUsageHistory(Region.class).peek(regions::contains);
        result.setRegion(historyRegion);
        final ResourceGroup historyRg = CacheManager.getUsageHistory(ResourceGroup.class)
                .peek(r -> r.getSubscriptionId().equals(sub.getId()));
        result.setResourceGroup(ObjectUtils.firstNonNull(resourceGroup, historyRg));
        result.setEnvironmentType(EnvironmentType.WorkloadProfiles);
        return result;
    }

    private LogAnalyticsWorkspace getWorkspace(ContainerApp app) {
        final String workspaceConsumerId = Optional.ofNullable(app.getManagedEnvironment())
                .map(AbstractAzResource::getRemote)
                .map(remoteApp -> remoteApp.appLogsConfiguration().logAnalyticsConfiguration().customerId())
                .orElse(StringUtils.EMPTY);
        final LogAnalyticsWorkspace result = Azure.az(AzureLogAnalyticsWorkspace.class).logAnalyticsWorkspaces(app.getSubscriptionId()).list().stream()
                .filter(logAnalyticsWorkspace -> Objects.equals(logAnalyticsWorkspace.getCustomerId(), workspaceConsumerId))
                .findFirst().orElse(null);
        if (Objects.isNull(result)) {
            AzureMessager.getMessager().info(message("azure.monitor.info.workspaceNotFoundInACA", app.getName()));
        }
        return result;
    }

    public static void showConsoleStreamingLog(Project project, ContainerApp app) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ContainerSelectionDialog dialog = new ContainerSelectionDialog(project, app);
            dialog.show();
        });
    }

    @Override
    public int getOrder() {
        return ContainerAppsActionsContributor.INITIALIZE_ORDER + 1;
    }
}
