/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig.slimui;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.webapp.AzureWebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.List;
import java.util.stream.Collectors;

public class WebAppComboBox extends AppServiceComboBox<WebAppConfig> {
    public WebAppComboBox(Project project) {
        super(project);
    }

    @Override
    protected void refreshItems() {
        Azure.az(AzureWebApp.class).refresh();
        super.refreshItems();
    }

    @Override
    protected List<WebAppConfig> loadAppServiceModels() {
        final List<WebApp> webApps = Azure.az(AzureWebApp.class).webApps();
        return webApps.stream().parallel()
            .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
            .map(webApp -> convertAppServiceToConfig(WebAppConfig::new, webApp))
            .collect(Collectors.toList());
    }

    @Override
    protected void createResource() {
        // todo: hide deployment part in creation dialog
        final WebAppCreationDialog webAppCreationDialog = new WebAppCreationDialog(project);
        webAppCreationDialog.setDeploymentVisible(false);
        webAppCreationDialog.setOkActionListener(webAppConfig -> {
            WebAppComboBox.this.addItem(webAppConfig);
            WebAppComboBox.this.setSelectedItem(webAppConfig);
            AzureTaskManager.getInstance().runLater(webAppCreationDialog::close);
        });
        webAppCreationDialog.show();
    }
}
