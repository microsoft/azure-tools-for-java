/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.keyvault;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.TerminalUtils;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.cli.AzureCliUtils;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.keyvault.CredentialVersion;
import com.microsoft.azure.toolkit.lib.keyvault.secret.SecretVersion;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KeyVaultCredentialActions {
    public static final Pattern NAME_PATTERN = Pattern.compile("^[0-9a-zA-Z-]{1,127}$");
    public static final String AZURE_CLI_INSTALL_URL = "https://learn.microsoft.com/en-us/cli/azure/install-azure-cli#install";
    public static final String NAME_LENGTH_MESSAGE = "Value must between 1 and 127 characters long.";
    public static final String NAME_VALIDATION_MESSAGE = "Value can only contain alphanumeric characters and dashes. " +
            "The value you provide may be copied globally for the purpose of running the service. " +
            "The value provided should not include personally identifiable or sensitive information.";

    public static AzureValidationInfo validateCredentialName(@Nonnull final AzureTextInput input) {
        final String name = input.getValue();
        if (Objects.isNull(name) || name.length() < 1 || name.length() > 127) {
            return AzureValidationInfo.error(NAME_LENGTH_MESSAGE, input);
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return AzureValidationInfo.error(NAME_VALIDATION_MESSAGE, input);
        }
        return AzureValidationInfo.ok(input);
    }

    public static void showCredential(@Nonnull final CredentialVersion resource, @Nullable final Project project) {
        ensureAzureCli(project);
        final String azureCliPath = Azure.az().config().getAzureCliPath();
        final String rawCommand = resource.getShowCredentialCommand();
        final String command = isAzureCliConfigured() ?
                StringUtils.replace(rawCommand, "az", azureCliPath, 1) : rawCommand;
        TerminalUtils.executeInTerminal(project, command);
    }

    public static void downloadCredential(@Nonnull final CredentialVersion resource, @Nullable final Project project) {
        ensureAzureCli(project);
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final FileChooserDescriptor fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            fileChooserDescriptor.setTitle("Choose Where to Save the Credential");
            final VirtualFile vf = FileChooser.chooseFile(fileChooserDescriptor, null, null);
            if (vf != null) {
                final Path path = Paths.get(vf.getPath(), getFileName(resource, Paths.get(vf.getPath())));
                final String azureCliPath = Azure.az().config().getAzureCliPath();
                final String rawCommand = resource.getDownloadCredentialCommand(path.toString());
                final String command = isAzureCliConfigured() ?
                        StringUtils.replace(rawCommand, "az", azureCliPath, 1) : rawCommand;
                TerminalUtils.executeInTerminal(project, command);
                final File file = path.toFile();
                AzureTaskManager.getInstance().runOnPooledThread(() -> showNotification(project, file));
            }
        });
    }

    private static String getFileName(@Nonnull final CredentialVersion resource, @Nonnull final Path path) {
        final String extension = resource instanceof SecretVersion ? "txt" : "pem";
        try {
            final String defaultName = resource.getCredential().getName() + "_" + resource.getName();
            final Set<String> existingFiles = FileUtils.listFiles(path.toFile(), new String[]{extension}, false).stream()
                    .map(f -> FilenameUtils.getBaseName(f.getName())).collect(Collectors.toSet());
            for (int i = 0; i < Integer.MAX_VALUE; i++) {
                final String name = i == 0 ? defaultName : String.format("%s_%d", defaultName, i);
                if (!existingFiles.contains(name)) {
                    return String.format("%s.%s", name, extension);
                }
            }
        } catch (final RuntimeException e) {
            // swallow exception to get name
        }
        return String.format("%s.%s", UUID.randomUUID().toString(), extension);
    }

    private static void showNotification(@Nonnull final Project project, @Nonnull final File file) {
        final File result = Mono.fromCallable(() -> file)
                .delayElement(Duration.ofSeconds(1))
                .subscribeOn(Schedulers.boundedElastic())
                .repeat(5)
                .takeUntil(f -> f.exists())
                .blockLast();
        if (Objects.nonNull(result) && result.exists()) {
            VirtualFileActions.notifyDownloadSuccess(file.getName(), file, project);
        }
    }

    private static void ensureAzureCli(@Nullable final Project project) {
        final boolean cliInstalled = AzureCliUtils.isAppropriateCliInstalled() || isAzureCliConfigured();
        if (!cliInstalled) {
            AzureTelemeter.info("keyvault.azure_cli_install_status", ImmutableMap.of("cli_installed", String.valueOf(false)));
            throw new AzureToolkitRuntimeException("Please install Azure CLI first.", getAzureCliNotExistsActions(project));
        }
        AzureTelemeter.info("keyvault.azure_cli_install_status", ImmutableMap.of("cli_installed", String.valueOf(true)));
    }

    private static boolean isAzureCliConfigured() {
        final String azureCliPath = Azure.az().config().getAzureCliPath();
        return StringUtils.isNotBlank(azureCliPath) && FileUtil.exists(azureCliPath);
    }

    private static Action<?>[] getAzureCliNotExistsActions(@Nonnull final Project project) {
        final Action<Object> settingAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
        final Action<String> learnAction = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL)
                .bind(AZURE_CLI_INSTALL_URL).withLabel("Learn More");
        return new Action[]{settingAction, learnAction};
    }
}
