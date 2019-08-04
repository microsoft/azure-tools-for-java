/**
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.tooling.msservices.helpers;

import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.model.storage.*;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.functionapp.FunctionAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.container.ContainerRegistryNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import java.io.File;

public interface UIHelper {
    void showException(@NotNull String message,
            Throwable ex,
            @NotNull String title,
            boolean appendEx,
            boolean suggestDetail);

    void showError(@NotNull String message, @NotNull String title);

    boolean showConfirmation(@NotNull String message, @NotNull String title, @NotNull String[] options, String defaultOption);

    void showInfo(@NotNull Node node, @NotNull String message);

    void showError(@NotNull Node node, @NotNull String message);

    void logError(String message, Throwable ex);

    File showFileChooser(String title);

    File showFileSaver(String title, String fileName);

    <T extends StorageServiceTreeItem> void openItem(Object projectObject, final StorageAccount storageAccount, final T item, String itemType, String itemName, String iconName);

    <T extends StorageServiceTreeItem> void openItem(Object projectObject, final ClientStorageAccount clientStorageAccount, final T item, String itemType, String itemName, String iconName);

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

    void openDeploymentSlotPropertyView(@NotNull DeploymentSlotNode node);

    void openFunctionAppProperties(@NotNull FunctionAppNode node);

    @Nullable
    <T extends StorageServiceTreeItem> Object getOpenedFile(@NotNull Object projectObject,
            @NotNull String accountName,
            @NotNull T item);

    boolean isDarkTheme();
}