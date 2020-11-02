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

package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBox;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public class FunctionAppComboBox extends AppServiceComboBox<FunctionAppComboBoxModel> {

    public FunctionAppComboBox(final Project project) {
        super(project);
    }

    @Override
    protected void createResource() {
        FunctionAppCreationDialog functionAppCreationDialog = new FunctionAppCreationDialog(project);
        functionAppCreationDialog.setOkActionListener(functionAppConfig -> {
            FunctionAppComboBoxModel newModel = new FunctionAppComboBoxModel(functionAppConfig);
            newModel.setNewCreateResource(true);
            FunctionAppComboBox.this.addItem(newModel);
            FunctionAppComboBox.this.setSelectedItem(newModel);
            DefaultLoader.getIdeHelper().invokeLater(functionAppCreationDialog::close);
        });
        functionAppCreationDialog.showAndGet();
    }

    @NotNull
    @Override
    protected List<? extends FunctionAppComboBoxModel> loadItems() throws Exception {
        final List<ResourceEx<FunctionApp>> functions = AzureFunctionMvpModel.getInstance().listAllFunctions(false);
        return functions.stream()
                        .filter(resource -> WebAppUtils.isJavaWebApp(resource.getResource()))
                        .sorted((a, b) -> StringUtils.compareIgnoreCase(a.getResource().name(), b.getResource().name()))
                        .map(function -> new FunctionAppComboBoxModel(function))
                        .collect(Collectors.toList());
    }
}
