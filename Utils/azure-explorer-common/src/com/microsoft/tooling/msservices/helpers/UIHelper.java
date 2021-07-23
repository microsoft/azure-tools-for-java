/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.helpers;

import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.model.storage.StorageServiceTreeItem;
import com.microsoft.tooling.msservices.model.storage.Table;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public interface UIHelper {
    void showException(@NotNull String message,
            Throwable ex,
            @NotNull String title,
            boolean appendEx,
            boolean suggestDetail);

    void showError(@NotNull String message, @NotNull String title);

    default void showError(Component component, @NotNull String message, @NotNull String title) {
        JOptionPane.showMessageDialog(component, message, title, JOptionPane.ERROR_MESSAGE);
    }

    boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption);

    default boolean showConfirmation(@NotNull Component component, @NotNull String message, @NotNull String title, @NotNull String[] options,
                                     String defaultOption) {
        return showConfirmation(message, title, options, defaultOption);
    }

    void showInfo(@NotNull Node node, @NotNull String message);

    void showError(@NotNull Node node, @NotNull String message);

    void logError(String message, Throwable ex);

    File showFileChooser(String title);

    File showFileSaver(String title, String fileName);

    <T extends StorageServiceTreeItem> void openItem(
            Object projectObject, final StorageAccount storageAccount,
            final T item, String itemType, String itemName, String iconName);

    <T extends StorageServiceTreeItem> void openItem(
            Object projectObject, final ClientStorageAccount clientStorageAccount,
            final T item, String itemType, String itemName, String iconName);

    void openItem(@NotNull Object projectObject, @NotNull Object itemVirtualFile);

    void refreshQueue(@NotNull Object projectObject, @NotNull StorageAccount storageAccount, @NotNull Queue queue);

    void refreshBlobs(@NotNull Object projectObject, @NotNull String accountName, @NotNull BlobContainer container);

    void refreshTable(@NotNull Object projectObject, @NotNull StorageAccount storageAccount, @NotNull Table table);

    String promptForOpenSSLPath();

    void openRedisPropertyView(@NotNull RedisCacheNode node);

    void openRedisExplorer(@NotNull RedisCacheNode node);

    void openDeploymentPropertyView(@NotNull DeploymentNode node);

    void openResourceTemplateView(@NotNull DeploymentNode node, String template);

    void openInBrowser(String link);

    void openContainerRegistryPropertyView(@NotNull ContainerRegistryNode node);

    void openWebAppPropertyView(@NotNull WebAppNode node);

    default void openFunctionAppPropertyView(@NotNull FunctionAppNode node) {

    }

    default void openSpringCloudAppPropertyView(@NotNull SpringCloudAppNode node) {
    }

    void openDeploymentSlotPropertyView(@NotNull DeploymentSlotNode node);

    default void openMySQLPropertyView(@NotNull String id, @NotNull Object project) {

    }

    default void openSqlServerPropertyView(@NotNull String id, @NotNull Object project) {

    }

    @Nullable
    <T extends StorageServiceTreeItem> Object getOpenedFile(@NotNull Object projectObject,
            @NotNull String accountName,
            @NotNull T item);

    boolean isDarkTheme();

    default void closeSpringCloudAppPropertyView(@NotNull Object project, @NotNull String appId) {

    }

    default void showMessageDialog(Component component, String message, String title, Icon icon) {
        JOptionPane.showMessageDialog(component, message, title, JOptionPane.INFORMATION_MESSAGE, icon);
    }

    default int showConfirmDialog(Component component, String message, String title, String[] options, String defaultOption, Icon icon) {
        return JOptionPane.showOptionDialog(component,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                icon,
                options,
                defaultOption);
    }

    default boolean showYesNoDialog(Component component, String message, String title, Icon icon) {
        return JOptionPane.showConfirmDialog(
                component,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                icon) == JOptionPane.YES_OPTION;
    }

    default String showInputDialog(Component component, String message, String title, Icon icon) {
        return (String) JOptionPane.showInputDialog(component, message, title, JOptionPane.QUESTION_MESSAGE, icon, null, null);
    }

    default void showInfoNotification(String title, String message) {

    }

    default void showErrorNotification(String title, String message) {

    }

    default void showWarningNotification(String title, String message) {

    }

}
