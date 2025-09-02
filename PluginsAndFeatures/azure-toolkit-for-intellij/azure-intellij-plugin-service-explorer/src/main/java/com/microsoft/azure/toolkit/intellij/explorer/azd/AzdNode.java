package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.explorer.azd.AzdUtils.executeInTerminal;

public final class AzdNode extends Node<String> {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private static final String WIN_REFRESH_PATH_COMMAND = "$env:PATH = [System.Environment]::GetEnvironmentVariable(\"PATH\",\"Machine\") + \";\" + [System.Environment]::GetEnvironmentVariable(\"PATH\",\"User\"); ";
    private static final String LINUX_REFRESH_PATH_COMMAND = "source ~/.bashrc";
    private static final String MAC_REFRESH_PATH_COMMAND = "source ~/.zshrc";

    private static final String WIN_AZD_INSTALL_COMMAND = "winget install microsoft.azd";
    private static final String LINUX_AZD_INSTALL_COMMAND = "set -o pipefail && curl -fsSL https://aka.ms/install-azd.sh | bash";
    private static final String BREW_PATH = getBrewPath();
    private static final String MAC_AZD_INSTALL_COMMAND = "source ~/.zshrc && " + BREW_PATH + " tap azure/azd && " + BREW_PATH + " install azd";

    private final Project project;

    public AzdNode(Project project) {
        super("Azure Developer (Preview)");
        this.project = project;
        withIcon(AzureIcons.Common.SERVICES);
        initializeNode();
    }

    public void initializeNode() {
        if (isAzdInstalled()) {
            AzdUtils.logTelemetryEvent("azd-installed");
            showAzdActions();
        } else {
            showNotInstalled();
        }
    }

    private void showNotInstalled() {
        AzdUtils.logTelemetryEvent("azd-not-installed");
        withDescription("Install azd");
        onClicked(e -> {
            final String command;
            if (SystemUtils.IS_OS_WINDOWS) {
                command = WIN_AZD_INSTALL_COMMAND;
            } else if (SystemUtils.IS_OS_LINUX) {
                command = LINUX_AZD_INSTALL_COMMAND;
            } else {
                command = MAC_AZD_INSTALL_COMMAND;
            }

            final ConfirmAndRunDialog installDialog = new ConfirmAndRunDialog(project, "Install azd");
            installDialog.setLabel("Do you want to install Azure Developer CLI (azd)?");
            installDialog.setOkButtonText("Install");
            installDialog.setEventName("install");
            installDialog.setOnOkAction(() -> {
                installAzd(command);
            });
            installDialog.show();
        });
    }

    private void installAzd(String command) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Install azd", false) {
            @Override
            public void run(com.intellij.openapi.progress.ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText(getTitle() + "...");
                final String output = runAsBackgroundTask(command, error -> {
                    indicator.setText("Installation of azd failed.");
                    NotificationGroupManager.getInstance()
                            .getNotificationGroup("Azure Developer")
                            .createNotification("Installation of azd failed", "Install azd manually and <b>restart IDE</b>.<br>" + error, NotificationType.ERROR)
                            .addAction(new NotificationAction("Install azd manually") {
                                @Override
                                public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                                    BrowserUtil.browse("https://aka.ms/azd/install");
                                }
                            })
                            .notify(project);
                    indicator.stop();
                });

                if (output != null) {
                    AzureEventBus.emit("azd.installed");
                    NotificationGroupManager.getInstance()
                            .getNotificationGroup("Azure Developer")
                            .createNotification("Installation of azd is successful.", NotificationType.INFORMATION)
                            .notify(project);

                    if (SystemUtils.IS_OS_WINDOWS) {
                        executeInTerminal(project, WIN_REFRESH_PATH_COMMAND);
                    } else if (SystemUtils.IS_OS_LINUX) {
                        executeInTerminal(project, LINUX_REFRESH_PATH_COMMAND);
                    } else {
                        executeInTerminal(project, MAC_REFRESH_PATH_COMMAND);
                    }
                    indicator.stop();
                }
            }
        });
    }

    public void showAzdActions() {
        AzdUtils.logTelemetryEvent("azd-show-actions");
        addChild(getCreateFromTemplatesNode());
        addChild(getInitializeFromSourceNode());
        addChild(getProvisionResourcesNode());
        addChild(getDeployToAzureNode());
        addChild(getProvisionAndDeployToAzureNode());
    }

    @Override
    public synchronized void refreshView() {
        super.refreshView();
        refreshChildrenLater(false);
    }

    private Node<String> getProvisionAndDeployToAzureNode() {
        return new Node<>("Provision and Deploy")
                .withIcon(AzureIcons.Action.START)
                .onClicked(e -> {
                    new ConfirmAndRunDialog(project, "Provision and deploy")
                            .setLabel("Do you want to provision and deploy to Azure?")
                            .setOnOkAction(() -> executeInTerminal(project, "azd up"))
                            .setEventName("provision-and-deploy")
                            .show();
                });
    }

    private Node<String> getDeployToAzureNode() {
        return new Node<>("Deploy to Azure")
                .withIcon(AzureIcons.Action.DEPLOY)
                .onClicked(e -> new ConfirmAndRunDialog(project, "Deploy to Azure")
                        .setLabel("Do you want to start deployment to Azure?")
                        .setOnOkAction(() -> executeInTerminal(project, "azd deploy"))
                        .setEventName("deploy-to-azure")
                        .show());
    }

    private Node<String> getProvisionResourcesNode() {
        return new Node<>("Provision resources")
                .withIcon(AzureIcons.Action.EXPORT)
                .onClicked(e -> new ConfirmAndRunDialog(project, "Provision Resources")
                        .setLabel("Do you want to provision Azure resources?")
                        .setOnOkAction(() -> executeInTerminal(project, "azd provision"))
                        .setEventName("provision-resources")
                        .show());
    }

    private Node<String> getInitializeFromSourceNode() {
        return new Node<>("Initialize from source")
                .withIcon(AzureIcons.Action.EDIT)
                .onClicked(e -> new ConfirmAndRunDialog(project, "Initialize from source")
                        .setLabel("Do you want to initialize using existing code?")
                        .setOnOkAction(() -> executeInTerminal(project, "azd init"))
                        .setEventName("initialize-from-source")
                        .show());
    }

    private Node<String> getCreateFromTemplatesNode() {
        return new Node<>("Create from templates")
                .withIcon(AzureIcons.Common.CREATE)
                .onClicked(e -> {
                    AzdUtils.logTelemetryEvent("azd-show-templates");
                    new AzdTemplatesDialog(project).show();
                });
    }

    public static boolean isAzdInstalled() {
        return runAsBackgroundTask("azd version -o json", null) != null;
    }

    private static String runAsBackgroundTask(String command, Consumer<String> onError) {
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder();
            // Detect OS and set the appropriate command
            final String os = System.getProperty("os.name").toLowerCase();
            if (SystemUtils.IS_OS_WINDOWS) {
                processBuilder.command("cmd", "/c", command); // Windows
            } else if (SystemUtils.IS_OS_MAC) {
                processBuilder.command("zsh", "-c", command); // Mac
            } else {
                processBuilder.command("bash", "-c", command); // Linux
            }

            final Process process = processBuilder.start();

            // Read the command output
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                final String output = reader.lines().collect(Collectors.joining("<br>"));
                final int exitCode = process.waitFor();
                if (exitCode != 0) {
                    try (final BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        final String error = errorReader.lines().collect(Collectors.joining("<br>"));
                        if (onError != null) {
                            onError.accept(error);
                        }
                    }
                    return null;
                }
                return output;
            }
        } catch (final Exception e) {
            return null;
        }
    }

    private static String getBrewPath() {
        // Try common brew locations
        final String[] possiblePaths = {
                "/opt/homebrew/bin/brew",  // Apple Silicon
                "/usr/local/bin/brew"      // Intel Mac
        };

        for (final String path : possiblePaths) {
            if (new File(path).exists()) {
                return path;
            }
        }

        // Fallback to PATH lookup
        return "brew";
    }
}
