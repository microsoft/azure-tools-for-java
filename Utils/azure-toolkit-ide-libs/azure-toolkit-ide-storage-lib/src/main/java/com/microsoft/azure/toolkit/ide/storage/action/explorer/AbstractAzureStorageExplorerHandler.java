/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.storage.action.explorer;

import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.storage.StorageActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionInstance;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractAzureStorageExplorerHandler {

    private static final String STORAGE_EXPLORER_DOWNLOAD_URL = "https://go.microsoft.com/fwlink/?LinkId=723579";
    private static final String STORAGE_EXPLORER = "StorageExplorer";

    public void openResource(@Nonnull StorageAccount account) {
        // Get resource url
        final Charset charset = Charset.forName("UTF-8");
        String resourceUrl = "storageexplorer://v=1" +
            "&accountid=" + URLEncoder.encode(account.getId(), charset) +
            "&subscriptionid=" + URLEncoder.encode(account.getSubscriptionId(), charset) +
            "&source=AzureToolkitForIntelliJ";
        // try launch with uri
        boolean result = launchStorageExplorerWithUri(account, resourceUrl);
        if (!result) {
            // fall back to launch with command
            launchStorageExplorerThroughCommand(account, resourceUrl);
        }
    }

    public void openResource(@Nonnull final AbstractAzResource<?, StorageAccount, ?> storage) {
        // Get resource url
        final StorageAccount storageAccount = storage.getParent();
        final Charset charset = Charset.forName("UTF-8");
        String resourceUrl = "storageexplorer://v=1" +
            "&accountid=" + URLEncoder.encode(storageAccount.getId(), charset) +
            "&subscriptionid=" + URLEncoder.encode(storageAccount.getSubscriptionId(), charset) +
            "&source=AzureToolkitForIntelliJ" +
            "&resourcetype=" + URLEncoder.encode(storage.getModule().getName(), charset) +
            "&resourcename=" + URLEncoder.encode(storage.getName(), charset);
        // try launch with uri
        boolean result = launchStorageExplorerWithUri(storageAccount, resourceUrl);
        if (!result) {
            // fall back to launch with command
            launchStorageExplorerThroughCommand(storageAccount, resourceUrl);
        }
    }

    @AzureOperation(name = "boundary/storage.open_azure_storage_explorer")
    protected boolean launchStorageExplorerWithUri(@Nonnull final StorageAccount storageAccount, @Nonnull final String resourceUrl) {
        if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                final List<ProcessHandle> beforeLaunchProcesses = ProcessHandle.allProcesses().collect(Collectors.toList());
                Desktop.getDesktop().browse(URI.create(resourceUrl));
                final List<ProcessHandle> afterLaunchProcesses = ProcessHandle.allProcesses().collect(Collectors.toList());
                final Collection<ProcessHandle> newProcesses = CollectionUtils.removeAll(afterLaunchProcesses, beforeLaunchProcesses);
                return newProcesses.stream().map(ProcessHandle::info).map(ProcessHandle.Info::command)
                    .anyMatch(command -> StringUtils.containsAnyIgnoreCase(command.orElse(StringUtils.EMPTY), STORAGE_EXPLORER));
            } catch (IOException e) {
                log.info("failed to launch storage explorer from uri", e);
            }
        }
        return false;
    }

    protected void launchStorageExplorerThroughCommand(final StorageAccount storageAccount, final String resourceUrl) {
        try {
            // Get storage explorer path
            final String storageExplorerExecutable = getStorageExplorerExecutable();
            // Launch storage explorer with resource url
            if (StringUtils.isEmpty(storageExplorerExecutable) || !Files.exists(Path.of(storageExplorerExecutable))) {
                throw new RuntimeException("Cannot find Azure Storage Explorer.");
            }
            launchStorageExplorer(storageExplorerExecutable, resourceUrl);
        } catch (final RuntimeException e) {
            throw new AzureToolkitRuntimeException("Failed to launch Azure Storage Explorer.", e, (Object[]) getStorageNotFoundActions(storageAccount));
        }
    }

    protected String getStorageExplorerExecutable() {
        final String storageExplorerPath = Azure.az().config().getStorageExplorerPath();
        return StringUtils.isEmpty(storageExplorerPath) ? getStorageExplorerExecutableFromOS() : storageExplorerPath;
    }

    protected Action<?>[] getStorageNotFoundActions(@Nonnull final StorageAccount storageAccount) {
        final AzureActionManager am = AzureActionManager.getInstance();
        final Action<Void> openInAzureAction = new Action<>(Action.Id.<Void>of("user/storage.open_portal_storage_browser.account"))
            .withLabel("Open in Portal")
            .withIcon(AzureIcons.Action.PORTAL.getIconPath())
            .withIdParam(storageAccount.getId())
            .withHandler((ignore, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(storageAccount.getPortalUrl() + "/storagebrowser", e))
            .withAuthRequired(false);

        final Action<Void> downloadAction = new Action<>(Action.Id.<Void>of("user/storage.download_explorer"))
            .withLabel("Download")
            .withHandler((ignore, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(STORAGE_EXPLORER_DOWNLOAD_URL, e))
            .withAuthRequired(false);

        final Action<Void> configureAction = new Action<>(Action.Id.<Void>of("user/storage.config_explorer_path"))
            .withLabel("Configure")
            .withHandler((ignore,e) -> {
                final Action<Object> openSettingsAction = am.getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
                final Action<AzResource> openExplorerAction = am.getAction(StorageActionsContributor.OPEN_AZURE_STORAGE_EXPLORER);
                openSettingsAction.handleSync(null, null); // Open Azure Settings Panel sync
                if (StringUtils.isNotBlank(Azure.az().config().getStorageExplorerPath())) {
                    openExplorerAction.handle(storageAccount, e);
                }
            })
            .withAuthRequired(false);
        return storageAccount instanceof AzuriteStorageAccount ? new Action[]{downloadAction, configureAction} :
                new Action[]{openInAzureAction, downloadAction, configureAction};
    }

    protected abstract String getStorageExplorerExecutableFromOS();

    protected abstract void launchStorageExplorer(final String explorer, String storageUrl);
}
