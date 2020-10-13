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

package com.microsoft.azure.toolkit.intellij.appservice.webapp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.intellij.appservice.component.input.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppService;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

public class ComboBoxWebApp extends AzureComboBox<WebAppComboBoxModel> {

    private Project project;

    public ComboBoxWebApp() {
        super();
        this.setRenderer(new WebAppCombineBoxRender(this));
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

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.Add, "Create new web app", this::createNewWebApp);
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof WebAppComboBoxModel) {
            final WebAppComboBoxModel selectedItem = (WebAppComboBoxModel) item;
            return selectedItem.isNewCreateResource()
                   ? String.format("%s (New Created)", selectedItem.getAppName())
                   : selectedItem.getAppName();
        } else {
            return item == null ? StringUtils.EMPTY : item.toString();
        }
    }

    private void createNewWebApp() {
        // todo: hide deployment part in creation dialog
        WebAppCreationDialog webAppCreationDialog = new WebAppCreationDialog(project);
        webAppCreationDialog.setOkActionListener(webAppConfig -> {
            final WebAppComboBoxModel newModel =
                    new WebAppComboBoxModel(WebAppService.convertConfig2Settings(webAppConfig));
            ComboBoxWebApp.this.addItem(newModel);
            ComboBoxWebApp.this.setSelectedItem(newModel);
            DefaultLoader.getIdeHelper().invokeLater(webAppCreationDialog::close);
        });
        webAppCreationDialog.show();
    }

    public class WebAppCombineBoxRender extends SimpleListCellRenderer {
        private final JComboBox comboBox;

        public WebAppCombineBoxRender(JComboBox comboBox) {
            this.comboBox = comboBox;
        }

        @Override
        public void customize(JList list, Object value, int index, boolean b, boolean b1) {
            if (value instanceof WebAppComboBoxModel) {
                final WebAppComboBoxModel webApp = (WebAppComboBoxModel) value;
                if (index >= 0) {
                    setText(getWebAppLabelText(webApp));
                } else {
                    setText(webApp.getAppName());
                }
            }
        }

        private String getWebAppLabelText(WebAppComboBoxModel webApp) {
            final String webAppName = webApp.isNewCreateResource() ?
                                      String.format("%s (New Created)", webApp.getAppName()) : webApp.getAppName();
            final String os = webApp.getOs();
            final String runtime = webApp.getRuntime();
            final String resourceGroup = webApp.getResourceGroup();

            return String.format("<html><div>%s</div></div><small>OS:%s Runtime: %s ResourceGroup:%s</small></html>",
                    webAppName, os, runtime, resourceGroup);
        }
    }
}
