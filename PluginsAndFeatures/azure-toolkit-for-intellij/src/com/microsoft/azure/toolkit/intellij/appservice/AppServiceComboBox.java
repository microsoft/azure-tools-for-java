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

package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import rx.Subscription;

import javax.swing.*;
import java.io.InterruptedIOException;
import java.util.Objects;

import static com.microsoft.intellij.util.RxJavaUtils.unsubscribeSubscription;

public abstract class AppServiceComboBox<T extends AppServiceComboBoxModel> extends AzureComboBox<T> {

    protected Project project;
    protected Subscription subscription;

    public AppServiceComboBox(final Project project) {
        super(false);
        this.project = project;
        this.setRenderer(new AppCombineBoxRender(this));
    }

    // todo: optimize refreshing logic
    public synchronized void refreshItemsWithDefaultValue(@NotNull T defaultValue) {
        unsubscribeSubscription(subscription);
        this.setLoading(true);
        this.removeAllItems();
        this.addItem(defaultValue);
        subscription = this.loadItemsAsync()
                           .subscribe(items -> DefaultLoader.getIdeHelper().invokeLater(() -> {
                               items.forEach(this::addItem);
                               this.resetDefaultValue(defaultValue);
                               this.setLoading(false);
                           }), (e) -> {
                                   this.handleLoadingError(e);
                               });
    }

    private void resetDefaultValue(@NotNull T defaultValue) {
        final AppServiceComboBoxModel model = getItems()
                .stream()
                .filter(item -> AppServiceComboBoxModel.isSameApp(defaultValue, item) && item != defaultValue)
                .findFirst().orElse(null);
        if (model != null) {
            this.setSelectedItem(model);
            this.removeItem(defaultValue);
        } else if (defaultValue.isNewCreateResource()) {
            return;
        } else {
            this.setSelectedItem(null);
            this.removeItem(defaultValue);
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

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.Add, "Create", this::createResource);
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof AppServiceComboBoxModel) {
            final AppServiceComboBoxModel selectedItem = (AppServiceComboBoxModel) item;
            return selectedItem.isNewCreateResource() ?
                   String.format("(New) %s", selectedItem.getAppName()) : selectedItem.getAppName();
        } else {
            return Objects.toString(item, StringUtils.EMPTY);
        }
    }

    protected abstract void createResource();

    public class AppCombineBoxRender extends SimpleListCellRenderer {
        private final JComboBox comboBox;

        public AppCombineBoxRender(JComboBox comboBox) {
            this.comboBox = comboBox;
        }

        @Override
        public void customize(JList list, Object value, int index, boolean b, boolean b1) {
            if (value instanceof AppServiceComboBoxModel) {
                final AppServiceComboBoxModel app = (AppServiceComboBoxModel) value;
                if (index >= 0) {
                    setText(getAppServiceLabel(app));
                } else {
                    setText(app.getAppName());
                }
            }
        }

        private String getAppServiceLabel(AppServiceComboBoxModel appServiceModel) {
            final String appServiceName = appServiceModel.isNewCreateResource() ?
                                      String.format("(New) %s", appServiceModel.getAppName()) : appServiceModel.getAppName();
            final String runtime = appServiceModel.getRuntime();
            final String resourceGroup = appServiceModel.getResourceGroup();

            return String.format("<html><div>%s</div></div><small>Runtime: %s | Resource Group: %s</small></html>",
                    appServiceName, runtime, resourceGroup);
        }
    }
}
