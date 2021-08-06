/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.webapp.WebAppService;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.List;
import java.util.stream.Collectors;

public class WebAppComboBox extends AppServiceComboBox<WebAppComboBoxModel> {

    public WebAppComboBox(final Project project) {
        super(project);
    }

    @Override
    protected void createResource() {
        // todo: hide deployment part in creation dialog
        WebAppCreationDialog webAppCreationDialog = new WebAppCreationDialog(project);
        webAppCreationDialog.setDeploymentVisible(false);
        webAppCreationDialog.setOkActionListener(webAppConfig -> {
            final WebAppComboBoxModel newModel =
                new WebAppComboBoxModel(WebAppService.convertConfig2Settings(webAppConfig));
            newModel.setNewCreateResource(true);
            WebAppComboBox.this.addItem(newModel);
            WebAppComboBox.this.setSelectedItem(newModel);
            DefaultLoader.getIdeHelper().invokeLater(webAppCreationDialog::close);
        });
        webAppCreationDialog.show();
    }

    @NotNull
    @Override
    @AzureOperation(
        name = "webapp.list.detail|java|subscription|selected",
        type = AzureOperation.Type.SERVICE
    )
    protected List<WebAppComboBoxModel> loadAppServiceModels() {
        final List<IWebApp> webApps = Azure.az(AzureAppService.class).webapps(false);
        return webApps.stream().parallel()
                .filter(this::isJavaAppService)
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .map(WebAppComboBoxModel::new)
                .collect(Collectors.toList());
    }
}
