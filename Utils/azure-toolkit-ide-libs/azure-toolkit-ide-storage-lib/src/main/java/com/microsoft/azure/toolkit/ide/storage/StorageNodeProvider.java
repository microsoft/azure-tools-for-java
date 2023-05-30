/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.storage;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureModuleLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobContainer;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import com.microsoft.azure.toolkit.lib.storage.queue.Queue;
import com.microsoft.azure.toolkit.lib.storage.share.Share;
import com.microsoft.azure.toolkit.lib.storage.table.Table;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class StorageNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Storage Account";
    private static final String ICON = AzureIcons.StorageAccount.MODULE.getIconPath();
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureStorageAccount.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureStorageAccount || data instanceof StorageAccount;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureStorageAccount) {
            final AzureStorageAccount service = ((AzureStorageAccount) data);
            return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(StorageActionsContributor.SERVICE_ACTIONS)
                .addChildren(ignore -> service.accounts(true), (account, storageNode) -> this.createNode(account, storageNode, manager));
        } else if (data instanceof AzuriteStorageAccount) {
            final AzuriteStorageAccount account = (AzuriteStorageAccount) data;
            return new Node<>(account).view(new AzureResourceLabelView<>(account))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .actions(StorageActionsContributor.AZURITE_ACTIONS)
                .addChildren(s -> s.getSubModules().stream().filter(Objects::nonNull).collect(Collectors.toList()), (module, p) -> new Node<>(module)
                    .view(new AzureModuleLabelView<>(module, module.getResourceTypeName() + "s"))
                    .actions(StorageActionsContributor.STORAGE_MODULE_ACTIONS)
                    .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager))
                    .hasMoreChildren(AbstractAzResourceModule::hasMoreResources)
                    .loadMoreChildren(AbstractAzResourceModule::loadMoreResources));
        } else if (data instanceof StorageAccount) {
            final StorageAccount account = (StorageAccount) data;
            return new Node<>(account).view(new AzureResourceLabelView<>(account))
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .actions(StorageActionsContributor.ACCOUNT_ACTIONS)
                .addChildren(StorageAccount::getSubModules, (module, p) -> new Node<>(module)
                    .view(new AzureModuleLabelView<>(module, module.getResourceTypeName() + "s"))
                    .actions(StorageActionsContributor.STORAGE_MODULE_ACTIONS)
                    .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager))
                    .hasMoreChildren(AbstractAzResourceModule::hasMoreResources)
                    .loadMoreChildren(AbstractAzResourceModule::loadMoreResources));
        } else if (data instanceof BlobContainer) {
            final BlobContainer container = (BlobContainer) data;
            final AzureResourceLabelView<BlobContainer> view = new AzureResourceLabelView<>(container);
            final StringBuilder tips = new StringBuilder();
            Optional.ofNullable(container.getLastModified()).map(lm -> String.format("Date modified: %s", lm.format(DATE_TIME_FORMATTER))).ifPresent(tips::append);
            view.setTips(tips.toString());
            return new Node<>(container).view(view)
                .actions(StorageActionsContributor.CONTAINER_ACTIONS)
                .addChildren(c -> c.getSubFileModule().list(), (blob, p) -> this.createNode(blob, p, manager))
                .hasMoreChildren(c -> c.getSubFileModule().hasMoreResources())
                .loadMoreChildren(c -> c.getSubFileModule().loadMoreResources());
        } else if (data instanceof Share) {
            final Share share = (Share) data;
            final AzureResourceLabelView<Share> view = new AzureResourceLabelView<>(share);
            final StringBuilder tips = new StringBuilder();
            Optional.ofNullable(share.getLastModified()).map(lm -> String.format("Date modified: %s", lm.format(DATE_TIME_FORMATTER))).ifPresent(tips::append);
            view.setTips(tips.toString());
            return new Node<>(share).view(view)
                .actions(StorageActionsContributor.SHARE_ACTIONS)
                .addChildren(s -> s.getSubFileModule().list(), (file, p) -> this.createNode(file, p, manager))
                .hasMoreChildren(c -> c.getSubFileModule().hasMoreResources())
                .loadMoreChildren(c -> c.getSubFileModule().loadMoreResources());
        } else if (data instanceof Queue) {
            final Queue queue = (Queue) data;
            return new Node<>(queue)
                .actions(StorageActionsContributor.QUEUE_ACTIONS)
                .view(new AzureResourceLabelView<>(queue));
        } else if (data instanceof Table) {
            final Table table = (Table) data;
            return new Node<>(table)
                .actions(StorageActionsContributor.TABLE_ACTIONS)
                .view(new AzureResourceLabelView<>(table));
        } else if (data instanceof StorageFile) {
            final StorageFile file = (StorageFile) data;
            final AzureResourceLabelView<StorageFile> view = new AzureResourceLabelView<>(file, d -> "", StorageNodeProvider::getFileIcon);
            final Node<StorageFile> node = new Node<>(file).view(view);
            if (file.isDirectory()) {
                final StringBuilder tips = new StringBuilder();
                Optional.ofNullable(file.getCreationTime()).map(ct -> String.format("Date created: %s", ct.format(DATE_TIME_FORMATTER))).ifPresent(tips::append);
                view.setTips(tips.toString());
                node.actions(StorageActionsContributor.DIRECTORY_ACTIONS)
                    .addChildren(f -> f.getSubFileModule().list(), (f, p) -> this.createNode(f, p, manager))
                    .hasMoreChildren(c -> c.getSubFileModule().hasMoreResources())
                    .loadMoreChildren(c -> c.getSubFileModule().loadMoreResources());
            } else {
                final StringBuilder tips = new StringBuilder().append(String.format("Size: %s", FileUtils.byteCountToDisplaySize(file.getSize())));
                Optional.ofNullable(file.getLastModified()).map(lm -> String.format("Date modified: %s", lm.format(DATE_TIME_FORMATTER))).ifPresent(d -> tips.append(String.format(", %s", d)));
                view.setTips(tips.toString());
                node.actions(StorageActionsContributor.FILE_ACTIONS)
                    .doubleClickAction(StorageActionsContributor.OPEN_FILE);
            }
            return node;
        }
        return null;
    }

    private static AzureIcon getFileIcon(StorageFile file) {
        final String fileIconName = file.isDirectory() ? "folder" : FilenameUtils.getExtension(file.getName());
        return AzureIcon.builder().iconPath(FILE_EXTENSION_ICON_PREFIX + fileIconName).build();
    }
}
