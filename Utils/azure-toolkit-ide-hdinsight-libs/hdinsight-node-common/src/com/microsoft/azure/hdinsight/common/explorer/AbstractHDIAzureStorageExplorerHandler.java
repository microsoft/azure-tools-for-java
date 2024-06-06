package com.microsoft.azure.hdinsight.common.explorer;

import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionInstance;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractHDIAzureStorageExplorerHandler {

    private static final String STORAGE_EXPLORER_DOWNLOAD_URL = "https://go.microsoft.com/fwlink/?LinkId=723579";
    private static final String STORAGE_EXPLORER = "StorageExplorer";
    public static final String ASE_DEEP_LINK = "storageexplorer:///";

    public void openResource(@Nonnull final IClusterDetail clusterDetail) {
        // Get resource url
        final String resourceUrl = clusterDetail instanceof ClusterDetail ? ("storageexplorer://v=1&accountid=" + ((ClusterDetail) clusterDetail).getId() + "&subscriptionid=" + clusterDetail.getSubscription().getId()) : ASE_DEEP_LINK;
        // try launch with uri
        boolean result = launchStorageExplorerWithUri(clusterDetail, resourceUrl);
        if (!result) {
            // fall back to launch with command
            launchStorageExplorerThroughCommand(clusterDetail, resourceUrl);
        }
    }

    protected boolean launchStorageExplorerWithUri(@Nonnull final IClusterDetail clusterDetail, @Nonnull final String resourceUrl) {
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

    protected void launchStorageExplorerThroughCommand(final IClusterDetail clusterDetail, final String resourceUrl) {
        try {
            // Get storage explorer path
            final String storageExplorerExecutable = getStorageExplorerExecutable();
            // Launch storage explorer with resource url
            if (StringUtils.isEmpty(storageExplorerExecutable) || !Files.exists(Path.of(storageExplorerExecutable))) {
                throw new AzureToolkitRuntimeException("Cannot find Azure Storage Explorer.");
            }
            launchStorageExplorer(storageExplorerExecutable, resourceUrl);
        } catch (final RuntimeException e) {
            throw new AzureToolkitRuntimeException("Failed to launch Azure Storage Explorer.", e, (Object[]) getStorageNotFoundActions(clusterDetail));
        }
    }

    protected String getStorageExplorerExecutable() {
        final String storageExplorerPath = Azure.az().config().getStorageExplorerPath();
        return StringUtils.isEmpty(storageExplorerPath) ? getStorageExplorerExecutableFromOS() : storageExplorerPath;
    }

    public String getPortalUrl(@Nonnull final IClusterDetail clusterDetail) {
        final String subscriptionId = clusterDetail.getSubscription().getId();
        final String id = ((ClusterDetail) clusterDetail).getId();
        final IAccount account = Azure.az(IAzureAccount.class).account();
        final Subscription subscription = account.getSubscription(subscriptionId);
        return String.format("%s/#@%s/resource%s", account.getPortalUrl(), subscription.getTenantId(), id);
    }

    protected Action<?>[] getStorageNotFoundActions(@Nonnull final IClusterDetail clusterDetail) {
        // Open in Azure Action
        final AzureActionManager am = AzureActionManager.getInstance();
        final Action.Id<IClusterDetail> OPEN = Action.Id.of("user/storage.open_portal_storage_browser.account");
        final Action<IClusterDetail> openInAzureAction = new Action<>(OPEN)
            .withLabel("Open in Portal")
            .withIcon(AzureIcons.Action.PORTAL.getIconPath())
            .withIdParam(IClusterDetail::getName)
            .enableWhen(s -> s instanceof IClusterDetail)
            .withHandler((ignore, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(getPortalUrl(clusterDetail) + "/storagebrowser", e))
            .withAuthRequired(false);

        final Action.Id<Void> DOWNLOAD = Action.Id.of("user/storage.download_explorer");
        final Action<Void> downloadAction = new Action<>(DOWNLOAD)
            .withLabel("Download")
            .withHandler((ignore, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(STORAGE_EXPLORER_DOWNLOAD_URL, e))
            .withAuthRequired(false);

        final Action.Id<Void> CONFIG = Action.Id.of("user/storage.config_explorer_path");
        final Action<Void> configureAction = new Action<>(CONFIG)
            .withLabel("Configure")
            .withHandler((ignore, e) -> {
                final Action<Object> openSettingsAction = am.getAction(ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS);
                final ActionInstance<Object> instance = openSettingsAction.instantiate(null, e);
                Objects.requireNonNull(instance).perform(); // Open Azure Settings Panel sync
                if (StringUtils.isNotBlank(Azure.az().config().getStorageExplorerPath())) {
                    openResource(clusterDetail);
                }
            })
            .withAuthRequired(false);
        return new Action[]{openInAzureAction, downloadAction, configureAction};
    }

    protected abstract String getStorageExplorerExecutableFromOS();

    protected abstract void launchStorageExplorer(final String explorer, String storageUrl);
}
