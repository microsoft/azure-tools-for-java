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

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.runner.functions.component.CreateApplicationInsightsDialog;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApplicationInsightsComboBox extends AzureComboBox<ApplicationInsightsModel> {

    private Subscription subscription;

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @NotNull
    @Override
    protected List<? extends ApplicationInsightsModel> loadItems() throws Exception {
        return AzureSDKManager.getInsightsResources(subscription.subscriptionId())
                .stream()
                .map(ApplicationInsightsModel::new)
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.Add, "Create new application insights instance", this::onCreateApplicationInsights);
    }

    @Override
    protected String getItemText(final Object item) {
        if (!(item instanceof ApplicationInsightsModel)) {
            return EMPTY_ITEM;
        }
        final ApplicationInsightsModel model = (ApplicationInsightsModel) item;
        return ((ApplicationInsightsModel) item).isNewCreate() ? String.format("(New) %s", model.getName()) : model.getName();
    }

    private void onCreateApplicationInsights() {
        final CreateApplicationInsightsDialog dialog = new CreateApplicationInsightsDialog();
        dialog.pack();
        if (dialog.showAndGet()) {
            final ApplicationInsightsModel model = ApplicationInsightsModel.builder().name(dialog.getApplicationInsightsName()).build();
            setValue(model);
        }
    }
}
