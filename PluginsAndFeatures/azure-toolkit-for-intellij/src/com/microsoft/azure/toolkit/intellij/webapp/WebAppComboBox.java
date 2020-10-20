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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.webapp.WebAppService;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.core.mvp.model.webapp.AzureWebAppMvpModel;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Subscription;

import javax.swing.*;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public class WebAppComboBox extends AzureComboBox<WebAppComboBoxModel> {

    private Project project;
    private Subscription subscription;

    public WebAppComboBox() {
        super(false);
        this.setRenderer(new WebAppCombineBoxRender(this));
    }

    // todo: optimize refreshing logic
    public synchronized void refreshItemsWithDefaultValue(@NotNull WebAppComboBoxModel defaultValue,
                                                          @NotNull BiPredicate<WebAppComboBoxModel, WebAppComboBoxModel> comparator) {
        unsubscribeSubscription(subscription);
        this.setLoading(true);
        this.removeAllItems();
        this.addItem(defaultValue);
        subscription = this.loadItemsAsync()
                           .subscribe(items -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                               this.removeAllItems();
                               this.setItems(items);
                               this.setLoading(false);
                               this.resetDefaultValue(defaultValue, comparator);
                           }), (e) -> {
                                   this.handleLoadingError(e);
                               });
    }

    private void resetDefaultValue(@NotNull WebAppComboBoxModel defaultValue, @NotNull BiPredicate<WebAppComboBoxModel,
                                   WebAppComboBoxModel> comparator) {
        final WebAppComboBoxModel model =
                (WebAppComboBoxModel) UIUtils.listComboBoxItems(this)
                                             .stream()
                                             .filter(item -> comparator.test((WebAppComboBoxModel) item, defaultValue))
                                             .findFirst().orElse(null);
        if (model != null) {
            this.setSelectedItem(model);
        } else if (defaultValue.isNewCreateResource()) {
            this.addItem(defaultValue);
            this.setSelectedItem(defaultValue);
        }
    }

    @Override
    protected void handleLoadingError(final Throwable e) {
        final Throwable rootCause = ExceptionUtils.getRootCause(e);
        if (rootCause instanceof InterruptedIOException || rootCause instanceof InterruptedException) {
            // Swallow interrupted exception caused by unsubscribe
            return;
        }
        this.setLoading(false);
        super.handleLoadingError(e);
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
                AllIcons.General.Add, "Create", this::createNewWebApp);
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof WebAppComboBoxModel) {
            final WebAppComboBoxModel selectedItem = (WebAppComboBoxModel) item;
            return selectedItem.isNewCreateResource() ?
                   String.format("(New) %s", selectedItem.getAppName()) : selectedItem.getAppName();
        } else {
            return Objects.toString(item, StringUtils.EMPTY);
        }
    }

    private void createNewWebApp() {
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
                                      String.format("(New) %s", webApp.getAppName()) : webApp.getAppName();
            final String os = webApp.getOs();
            final String runtime = webApp.getRuntime();
            final String resourceGroup = webApp.getResourceGroup();

            return String.format("<html><div>%s</div></div><small>Runtime: %s | Resource Group: %s</small></html>",
                    webAppName, runtime, resourceGroup);
        }
    }
}
