package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.terminal.ui.TerminalWidget;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Map;

import static com.microsoft.azure.toolkit.intellij.common.TerminalUtils.getTerminalWidget;

public final class AzdUtils {

    private static final String AZURE_DEVELOPER_CLI = "azd";

    private AzdUtils() {
        // Utility class, no instantiation allowed
    }

    public static void logTelemetryEvent(String eventName) {
        Map<String, String> properties = Map.of(
                AzureTelemeter.OP_NAME, eventName,
                AzureTelemeter.OP_PARENT_ID, AZURE_DEVELOPER_CLI,
                AzureTelemeter.OPERATION_NAME, eventName, // what's the difference between OP_NAME and OPERATION_NAME?
                AzureTelemeter.SERVICE_NAME, AZURE_DEVELOPER_CLI
        );
        AzureTaskManager.getInstance().runLater(() -> {
            AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
        });
    }

    public static void executeInTerminal(Project project, String command) {
        executeInExistingTerminal(project, command, null, "azd");
    }

    public static void executeInExistingTerminal(@Nonnull Project project, @Nonnull String command, @Nullable Path workingDir, @Nullable String terminalTabTitle) {
        AzureTaskManager.getInstance().runLater(() -> {
            final TerminalWidget terminalWidget = getTerminalWidget(project, workingDir, terminalTabTitle);
            if (TerminalUtils.hasRunningCommands(terminalWidget)) {
                Messages.showErrorDialog(project, "Another command is already running. Please try again later.", "Error");
                return;
            }
            AzureTaskManager.getInstance().runInBackground(OperationBundle.description("boundary/common.execute_in_terminal.command", command), () -> {
                terminalWidget.requestFocus();
                terminalWidget.getTtyConnectorAccessor().executeWithTtyConnector((connector) -> {
                    terminalWidget.sendCommandToExecute(command);
                });
            });
        }, AzureTask.Modality.ANY);
    }
}
