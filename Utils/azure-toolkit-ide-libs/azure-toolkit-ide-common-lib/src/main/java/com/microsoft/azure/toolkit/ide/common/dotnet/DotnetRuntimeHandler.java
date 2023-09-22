/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.dotnet;

import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import static com.microsoft.azure.toolkit.lib.common.utils.CommandUtils.exec;

public class DotnetRuntimeHandler {
    public static final String SCRIPT_BASE_URL = "https://dot.net/v1/";
    public static final String SCRIPT_NAME = "dotnet-install." + (SystemUtils.IS_OS_WINDOWS ? "ps1" : "sh");
    public static final String SCRIPT_FULL_URL = SCRIPT_BASE_URL + SCRIPT_NAME;
    public static final String UNIX_INSTALL_COMMAND = "./dotnet-install.sh --runtime dotnet --version 6.0.9 --install-dir .";
    public static final String WINDOWS_INSTALL_RAW_COMMAND = "./dotnet-install.ps1 -Runtime dotnet -Version 6.0.9 -InstallDir .";
    public static final String WINDOWS_INSTALL_COMMAND = String.format("powershell.exe -NoProfile -ExecutionPolicy unrestricted -Command \"& { " +
            "[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12 ; & %s }\"",
        WINDOWS_INSTALL_RAW_COMMAND);
    public static final String INSTALL_COMMAND = SystemUtils.IS_OS_WINDOWS ? WINDOWS_INSTALL_COMMAND : UNIX_INSTALL_COMMAND;
    public static final String VERSION = "6.0.9";

    @Nullable
    private static String getDotnetVersion() {
        final String dotnetRuntimePath = Azure.az().config().getDotnetRuntimePath();
        return getDotnetVersion(dotnetRuntimePath);
    }

    @Nullable
    private static String getDotnetVersion(String dotnetRuntimePath) {
        try {
            final String command = SystemUtils.IS_OS_WINDOWS ? "powershell.exe ./dotnet --version" : "./dotnet --version";
            return exec(command, dotnetRuntimePath);
        } catch (final Throwable e) {
            // swallow exception to get version
            return null;
        }
    }

    public static boolean isDotnetRuntimeInstalled() {
        return isDotnetRuntimeInstalled(Azure.az().config().getDotnetRuntimePath());
    }

    public static boolean isDotnetRuntimeInstalled(@Nonnull final String path) {
        try {
            final String command = SystemUtils.IS_OS_WINDOWS ? "powershell.exe ./dotnet --list-runtimes" : "./dotnet --list-runtimes";
            return StringUtils.isNotBlank(exec(command, path));
        } catch (final Throwable e) {
            // swallow exception to get version
            return false;
        }
    }

    @Nullable
    public static String getDotnetRuntimePath() {
        final List<String> paths = CommandUtils.resolveCommandPath("dotnet");
        return CollectionUtils.isEmpty(paths) ? null : Paths.get(paths.get(0)).getParent().toString();
    }

    public static void installDotnet(final String path) {
        final Action<Object> openSettingsAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
        final File installScript = prepareDotnetInstallScript(path);
        try {
            if (!SystemUtils.IS_OS_WINDOWS) {
                // use ProcessBuilder to execute `dotnet-install.sh` to install dotnet runtime in macOS. `CommandUtils` is buggy
                // TODO: re implement `CommandUtils`.
                final File installPath = new File(path);
                final ProcessBuilder builder = new ProcessBuilder();
                builder.command("sh", "./dotnet-install.sh", "--runtime", "dotnet", "--version", "6.0.9", "--install-dir", ".");
                builder.directory(installPath);
                final Process process = builder.start();
                final int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new IOException("Failed to install dotnet runtime.");
                }
            } else {
                CommandUtils.exec(INSTALL_COMMAND, path);
            }
            Azure.az().config().setDotnetRuntimePath(path);
            AzureEventBus.emit("dotnet_runtime.updated");
            final String INSTALL_SUCCEED_MESSAGE = ".NET runtime is installed and configured successfully.";
            AzureMessager.getMessager().success(INSTALL_SUCCEED_MESSAGE, null, openSettingsAction, ResourceCommonActionsContributor.RESTART_IDE);
        } catch (final IOException | RuntimeException | InterruptedException e) {
            AzureMessager.getMessager().error(e, "Failed to install .NET Runtime, please download and configure the path manually",
                generateDownloadAction(), openSettingsAction);
        } finally {
            FileUtils.deleteQuietly(installScript);
        }
    }

    private static Action<?> generateDownloadAction() {
        return new Action<>(Action.Id.of("user/bicep.download_dotnet"))
            .withLabel("Download .NET Runtime")
            .withHandler(a -> AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://dotnet.microsoft.com/en-us/download"));
    }

    private static File prepareDotnetInstallScript(final String path) {
        try {
            final File dotnetInstall = Paths.get(path, SCRIPT_NAME).toFile();
            FileUtils.copyURLToFile(new URL(SCRIPT_FULL_URL), dotnetInstall);
            return dotnetInstall;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
