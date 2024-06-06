/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.storage;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceIconProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIconProvider;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.ConnectionStringStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class StorageNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Storage Account";
    private static final String ICON = AzureIcons.StorageAccount.MODULE.getIconPath();
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final AzureIcon.Modifier MODIFIER_CONNECTIONSTRING = new AzureIcon.Modifier("connectionstring", AzureIcon.ModifierLocation.BOTTOM_LEFT);
    public static final AzureIconProvider<IStorageAccount> STORAGE_ICON_PROVIDER =
        new AzureResourceIconProvider<IStorageAccount>()
            .withModifier((a) -> a instanceof ConnectionStringStorageAccount ? MODIFIER_CONNECTIONSTRING : null);

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureStorageAccount.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureStorageAccount || data instanceof StorageAccount || data instanceof ConnectionStringStorageAccount;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureStorageAccount) {
            return new AzServiceNode<>((AzureStorageAccount) data)
                .withIcon(ICON).withLabel(NAME)
                .withActions(StorageActionsContributor.SERVICE_ACTIONS)
                .addChildren(s -> s.accounts(true), (account, storageNode) -> this.createNode(account, storageNode, manager));
        } else if (data instanceof AzuriteStorageAccount) {
            return new AzResourceNode<>((AzuriteStorageAccount) data)
                .withDescription(AzuriteStorageAccount::getStatus)
                .withLabel("Storage Emulator (Azurite)")
                .withIcon(StorageNodeProvider::getAzuriteIcon)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(StorageActionsContributor.AZURITE_ACTIONS)
                .addChildren(s -> s.getSubModules().stream().filter(Objects::nonNull).collect(Collectors.toList()), (module, p) -> new AzModuleNode<>(module)
                    .withActions(StorageActionsContributor.STORAGE_MODULE_ACTIONS)
                    .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager)));
        } else if (data instanceof StorageAccount) {
            return new AzResourceNode<>((StorageAccount) data)
                .addInlineAction(ResourceCommonActionsContributor.PIN)
                .withActions(StorageActionsContributor.ACCOUNT_ACTIONS)
                .addChildren(StorageAccount::getSubModules, (module, p) -> new AzModuleNode<>(module)
                    .withActions(StorageActionsContributor.STORAGE_MODULE_ACTIONS)
                    .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager)));
        } else if (data instanceof ConnectionStringStorageAccount account) {
            return new AzResourceNode<>(account)
                .withIcon(STORAGE_ICON_PROVIDER::getIcon)
                .withActions(StorageActionsContributor.CONNECTION_STRING_ACCOUNT_ACTIONS)
                .addChildren(a -> account.getSubModules(), (module, p) -> new AzModuleNode<>(module)
                    .withActions(StorageActionsContributor.STORAGE_MODULE_ACTIONS)
                    .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager)));
        } else if (data instanceof BlobContainer) {
            return new AzResourceNode<>((BlobContainer) data)
                .withTips(c -> Optional.ofNullable(c.getLastModified()).map(lm -> String.format("Date modified: %s", lm.format(DATE_TIME_FORMATTER))).orElse(""))
                .withActions(StorageActionsContributor.CONTAINER_ACTIONS)
                .addChildren(c -> c.getSubFileModule().list(), (blob, p) -> this.createNode(blob, p, manager))
                .withMoreChildren(c -> c.getSubFileModule().hasMoreResources(), c -> c.getSubFileModule().loadMoreResources());
        } else if (data instanceof Share) {
            return new AzResourceNode<>((Share) data)
                .withTips(s -> Optional.ofNullable(s.getLastModified()).map(lm -> String.format("Date modified: %s", lm.format(DATE_TIME_FORMATTER))).orElse(""))
                .withActions(StorageActionsContributor.SHARE_ACTIONS)
                .addChildren(s -> s.getSubFileModule().list(), (file, p) -> this.createNode(file, p, manager))
                .withMoreChildren(c -> c.getSubFileModule().hasMoreResources(), c -> c.getSubFileModule().loadMoreResources());
        } else if (data instanceof Queue) {
            return new AzResourceNode<>((Queue) data)
                .withActions(StorageActionsContributor.QUEUE_ACTIONS);
        } else if (data instanceof Table) {
            return new AzResourceNode<>((Table) data)
                .withActions(StorageActionsContributor.TABLE_ACTIONS);
        } else if (data instanceof StorageFile) {
            final StorageFile file = (StorageFile) data;
            final Node<StorageFile> node = new AzResourceNode<>(file)
                .withIcon(StorageNodeProvider::getFileIcon)
                .withDescription(d -> "");
            if (file.isDirectory()) {
                node.withTips(f -> Optional.ofNullable(f.getCreationTime()).map(ct -> String.format("Date created: %s", ct.format(DATE_TIME_FORMATTER))).orElse(null))
                    .withActions(StorageActionsContributor.DIRECTORY_ACTIONS)
                    .addChildren(f -> f.getSubFileModule().list(), (f, p) -> this.createNode(f, p, manager))
                    .withMoreChildren(c -> c.getSubFileModule().hasMoreResources(), c -> c.getSubFileModule().loadMoreResources());
            } else {
                final StringBuilder tips = new StringBuilder().append(String.format("Size: %s", FileUtils.byteCountToDisplaySize(file.getSize())));
                Optional.ofNullable(file.getLastModified()).map(lm -> String.format("Date modified: %s", lm.format(DATE_TIME_FORMATTER))).ifPresent(d -> tips.append(String.format(", %s", d)));
                node.withTips(tips.toString())
                    .withActions(StorageActionsContributor.FILE_ACTIONS)
                    .onDoubleClicked(StorageActionsContributor.OPEN_FILE);
            }
            return node;
        }
        return null;
    }

    private static AzureIcon getAzuriteIcon(@Nonnull final AzuriteStorageAccount storageAccount) {
        final AzureIcon.Modifier modifier = AzureResourceIconProvider.getStatusModifier(storageAccount);
        return AzureIcon.builder().iconPath(AzureIcons.StorageAccount.AZURITE.getIconPath()).modifierList(Arrays.asList(modifier)).build();
    }

    private static AzureIcon getFileIcon(StorageFile file) {
        final String fileIconName = file.isDirectory() ? "folder" : FilenameUtils.getExtension(file.getName());
        return AzureIcon.builder().iconPath(FILE_EXTENSION_ICON_PREFIX + fileIconName).build();
    }
}
