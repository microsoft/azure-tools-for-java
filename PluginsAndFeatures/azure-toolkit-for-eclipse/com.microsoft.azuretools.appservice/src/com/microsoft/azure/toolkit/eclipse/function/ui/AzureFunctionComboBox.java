/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.ui;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.toolkit.eclipse.appservice.component.AppServiceComboBox;
import com.microsoft.azure.toolkit.eclipse.functionapp.creation.CreateFunctionAppDialog;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azuretools.appservice.util.CommonUtils;

public class AzureFunctionComboBox extends AppServiceComboBox<FunctionAppConfig> {

    public AzureFunctionComboBox(Composite parent) {
        super(parent);
    }

    @Override
    protected void createResource() {
        Shell shell = this.getShell();
        CreateFunctionAppDialog createDialog = new CreateFunctionAppDialog(shell, FunctionAppConfig.getFunctionAppDefaultConfig());
        createDialog.setOkActionListener(config -> {
            createDialog.close();
            final List<FunctionAppConfig> items = this.getItems();
            items.add(0, config);
            this.setItems(items);
            this.setValue(config);
            AzureFunctionComboBox.this.fireValueChangedEvent(config);
        });
        createDialog.open();
    }

    @Override
    protected List<FunctionAppConfig> loadAppServiceModels() throws Exception {
        final List<FunctionApp> functionApps = Azure.az(AzureFunctions.class).functionApps();
        return functionApps.stream().parallel()
                .filter(AppServiceComboBox::isJavaAppService)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(t -> CommonUtils.toConfig(t))
                .collect(Collectors.toList());
    }

}
