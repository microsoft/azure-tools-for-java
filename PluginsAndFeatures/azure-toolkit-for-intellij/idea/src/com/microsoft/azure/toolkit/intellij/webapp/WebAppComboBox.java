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

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceComboBox;
import com.microsoft.azure.toolkit.lib.webapp.WebAppService;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.utils.WebAppUtils;
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
    protected List<WebAppComboBoxModel> loadItems() throws Exception {
        final List<ResourceEx<WebApp>> webApps = AzureWebAppMvpModel.getInstance().listAllWebApps(false);
        return webApps.stream()
                      .filter(resource -> WebAppUtils.isJavaWebApp(resource.getResource()))
                      .sorted((a, b) -> a.getResource().name().compareToIgnoreCase(b.getResource().name()))
                      .map(webAppResourceEx -> new WebAppComboBoxModel(webAppResourceEx))
                      .collect(Collectors.toList());
    }
}
