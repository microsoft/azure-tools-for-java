/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.appservice.file;

import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.file.AppServiceFileService;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule;

import javax.swing.*;
import java.util.Objects;

public class AppServiceUserFilesRootNode extends AzureRefreshableNode {
    private static final String MODULE_ID = WebAppModule.class.getName();
    private static final String MODULE_NAME = "Files";
    private static final String ROOT_PATH = "/site/wwwroot";

    protected final String subscriptionId;
    protected final WebAppBase app;
    private AppServiceFileService fileService;

    public AppServiceUserFilesRootNode(final Node parent, final String subscriptionId, final WebAppBase app) {
        this(MODULE_NAME, parent, subscriptionId, app);
    }

    public AppServiceUserFilesRootNode(final String name, final Node parent, final String subscriptionId, final WebAppBase app) {
        super(MODULE_ID, name, parent, null);
        this.subscriptionId = subscriptionId;
        this.app = app;
    }

    @Override
    public void removeNode(final String sid, final String name, Node node) {
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        final AppServiceFileService service = this.getFileService();
        service.getFilesInDirectory(getRootPath()).stream()
               .map(file -> new AppServiceFileNode(file, this, service))
               .forEach(this::addChildNode);
    }

    @NotNull
    protected String getRootPath() {
        return ROOT_PATH;
    }

    public AppServiceFileService getFileService() {
        if (Objects.isNull(this.fileService)) {
            this.fileService = AppServiceFileService.forApp(app);
        }
        return this.fileService;
    }

    @Override
    public @Nullable Icon getIcon() {
        return DefaultLoader.getIdeHelper().getFileTypeIcon("/", true);
    }
}
