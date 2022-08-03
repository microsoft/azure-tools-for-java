/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

public class FunctionAppComboBox extends AppServiceComboBox<FunctionAppConfig> {

    public FunctionAppComboBox(final Project project) {
        super(project);
    }

    @Override
    protected void refreshItems() {
        Azure.az(AzureFunctions.class).refresh();
        super.refreshItems();
    }

    @Override
    protected void createResource() {
        final FunctionAppCreationDialog functionAppCreationDialog = new FunctionAppCreationDialog(project);
        functionAppCreationDialog.setOkActionListener(functionAppConfig -> {
            FunctionAppComboBox.this.addItem(functionAppConfig);
            FunctionAppComboBox.this.setSelectedItem(functionAppConfig);
            AzureTaskManager.getInstance().runLater(functionAppCreationDialog::close);
        });
        functionAppCreationDialog.showAndGet();
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "function.list_java_apps",
        type = AzureOperation.Type.SERVICE
    )
    protected List<FunctionAppConfig> loadAppServiceModels() {
        return Azure.az(AzureFunctions.class).functionApps().parallelStream()
            .map(functionApp -> convertAppServiceToConfig(FunctionAppConfig::new, functionApp))
            .sorted((app1, app2) -> app1.getName().compareToIgnoreCase(app2.getName()))
            .collect(Collectors.toList());
    }
}
