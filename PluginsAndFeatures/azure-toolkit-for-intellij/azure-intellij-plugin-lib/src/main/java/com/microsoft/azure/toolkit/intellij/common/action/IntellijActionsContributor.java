/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.action;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.actions.RevealFileAction;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerConfigurable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.FUSEventSource;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.intellij.common.properties.IntellijShowPropertiesViewAction;
import com.microsoft.azure.toolkit.intellij.common.streaminglog.StreamingLogsManager;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.StreamingLogSupport;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.INVOKE_COMMAND_IN_TERMINAL;

public class IntellijActionsContributor implements IActionsContributor {
    public static final Action.Id<Object> TRY_ULTIMATE = Action.Id.of("user/$database.try_ultimate");
    private static final String IDE_DOWNLOAD_URL = "https://www.jetbrains.com/idea/download/";
    public static final String ACTIONS_DEPLOY_TO_AZURE = "actions.common.deploy_to_azure";

    @Override
    public void registerHandlers(AzureActionManager am) {
        am.<String, AnActionEvent>registerHandler(INVOKE_COMMAND_IN_TERMINAL,
                (s, e) -> StringUtils.isNotBlank(s) && Objects.nonNull(e.getProject()),
                (s, e) -> TerminalUtils.executeInTerminal(Objects.requireNonNull(e.getProject()), s));
        am.registerHandler(ResourceCommonActionsContributor.OPEN_URL, Objects::nonNull, IntellijActionsContributor::browseUrl);
        am.<AzResource, AnActionEvent>registerHandler(ResourceCommonActionsContributor.SHOW_PROPERTIES,
            (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()),
            (s, e) -> IntellijShowPropertiesViewAction.showPropertyView(s, Objects.requireNonNull(e.getProject())));

        am.<StreamingLogSupport, AnActionEvent>registerHandler(StreamingLogSupport.OPEN_STREAMING_LOG,
                (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()),
                (s, e) -> StreamingLogsManager.getInstance().showStreamingLog(e.getProject(), s.getId(), s.getDisplayName(), s.streamingLogs(true)));

        final BiConsumer<Object, AnActionEvent> highlightResource = (r, e) -> {
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_EXPLORER).handle(null, e);
            AzureEventBus.emit("azure.explorer.select_resource", r);
        };
        am.registerHandler(ResourceCommonActionsContributor.SELECT_RESOURCE_IN_EXPLORER, (s, e) -> Objects.nonNull(s) && Objects.nonNull(e.getProject()), highlightResource);

        final AzureTaskManager tm = AzureTaskManager.getInstance();
        am.registerHandler(ResourceCommonActionsContributor.RESTART_IDE, (s, e) -> tm.runLater(() -> {
            final ApplicationEx app = ApplicationManagerEx.getApplicationEx();
            app.restart();
        }));

        am.registerHandler(ResourceCommonActionsContributor.SUPPRESS_ACTION, (id, e) -> IntellijAzureActionManager.suppress(id));

        am.registerHandler(ResourceCommonActionsContributor.ENABLE_PLUGIN, (id, e) -> AzureTaskManager.getInstance().runLater(() -> {
            PluginManager.getInstance().enablePlugin(PluginId.getId(id));
        }, AzureTask.Modality.ANY));
        am.registerHandler(ResourceCommonActionsContributor.ENABLE_PLUGIN_AND_RESTART, (id, e) -> AzureTaskManager.getInstance().runLater(() -> {
            PluginManager.getInstance().enablePlugin(PluginId.getId(id));
            PluginManagerConfigurable.showRestartDialog("Restart to Activate");
        }, AzureTask.Modality.ANY));
        am.registerHandler(ResourceCommonActionsContributor.SEARCH_INSTALLED_PLUGIN, (id, e) -> AzureTaskManager.getInstance().runLater(() -> {
            ShowSettingsUtil.getInstance().editConfigurable(((AnActionEvent) e).getProject(), new PluginManagerConfigurable(), it -> it.openInstalledTab(id));
        }, AzureTask.Modality.ANY));
        am.registerHandler(ResourceCommonActionsContributor.SEARCH_MARKETPLACE_PLUGIN, (id, e) -> AzureTaskManager.getInstance().runLater(() -> {
            ShowSettingsUtil.getInstance().editConfigurable(((AnActionEvent) e).getProject(), new PluginManagerConfigurable(), it -> it.openMarketplaceTab(id));
        }, AzureTask.Modality.ANY));
    }

    @AzureOperation(name = "boundary/$resource.open_url.url", params = {"u"})
    private static void browseUrl(String u) {
        BrowserUtil.browse(u);
    }

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(TRY_ULTIMATE)
            .withLabel("Try IntelliJ IDEA Ultimate")
            .withHandler((r, e) -> FUSEventSource.NOTIFICATION.openDownloadPageAndLog(((AnActionEvent) e).getProject(), IDE_DOWNLOAD_URL))
            .register(am);

        new Action<>(ResourceCommonActionsContributor.REVEAL_FILE)
            .withLabel(RevealFileAction.getActionName())
            .withHandler(VirtualFileActions::revealInExplorer)
            .register(am);

        new Action<>(ResourceCommonActionsContributor.OPEN_FILE)
            .withLabel("Open In Editor")
            .withHandler((file, e) -> AzureTaskManager.getInstance().runLater(() -> {
                final FileEditorManager fileEditorManager = FileEditorManager.getInstance(((AnActionEvent) e).getProject());
                final VirtualFile virtualFile = VfsUtil.findFileByIoFile(file, true);
                VirtualFileActions.openFileInEditor(virtualFile, (a) -> false, () -> {
                }, fileEditorManager);
            }))
            .register(am);
    }

    @Override
    public void registerGroups(final AzureActionManager am) {
        final ActionGroup deployToAzure = new ActionGroup(Arrays.asList(
            "Actions.DeployFunction",
            "Actions.DeploySpringCloud",
            "Actions.WebDeployAction"
        ), new Action.View("Deploy to Azure...", AzureIcons.Action.DEPLOY.getIconPath(), true, null));
        am.registerGroup(ACTIONS_DEPLOY_TO_AZURE, deployToAzure);
    }

    @Override
    public int getOrder() {
        return ResourceCommonActionsContributor.INITIALIZE_ORDER + 1; //after azure resource common actions registered
    }
}
