package com.microsoft.azure.toolkit.intellij.containerapps.streaminglog;

import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import com.microsoft.azure.toolkit.intellij.common.AppStreamingLogConsoleView;
import com.microsoft.azure.toolkit.intellij.common.StreamingLogsToolWindowManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import reactor.core.publisher.Flux;

import java.util.Objects;

public class ContainerAppStreamingLogManager {
    private static final ContainerAppStreamingLogManager instance = new ContainerAppStreamingLogManager();
    public static ContainerAppStreamingLogManager getInstance() {
        return instance;
    }

    public void showConsoleStreamingLog(Project project, ContainerApp app, String revisionName, String replicaName, String containerName) {
        AzureTaskManager.getInstance().runLater(() ->
                showStreamingLog(project, app, ContainerApp.LOG_TYPE_CONSOLE, revisionName, replicaName, containerName));
    }

    public void showSystemStreamingLog(Project project, ContainerApp app) {
        AzureTaskManager.getInstance().runLater(() ->
                showStreamingLog(project, app, ContainerApp.LOG_TYPE_SYSTEM, null, null, null));
    }

    private void showStreamingLog(Project project, ContainerApp app, String logType, String revisionName,
                                  String replicaName, String containerName) {
        final Content content = StreamingLogsToolWindowManager.getInstance().getToolWindowContent(project, app.getId());
        if (Objects.nonNull(content)) {
            StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(project, app.getName(), content);
            return;
        }

        final AzureString title = AzureString.fromString("open streaming logs");
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            try {
                final AppStreamingLogConsoleView consoleView = new AppStreamingLogConsoleView(project);
                final Flux<String> log = app.streamingLogs(logType, revisionName, replicaName, containerName, false, 100);
                consoleView.startStreamingLog(log);
                AzureTaskManager.getInstance().runLater(() ->
                        StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(project, app.getId(), app.getName(), consoleView)
                );
            } catch (final AzureToolkitRuntimeException e) {
                throw e;
            } catch (final Throwable e) {
                throw new AzureToolkitRuntimeException("failed to open streaming log", e);
            }
        }));
    }

}
