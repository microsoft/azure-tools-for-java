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
package com.microsoft.azure.toolkit.intellij.appservice.insights;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.runner.functions.component.CreateApplicationInsightsDialog;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import org.apache.commons.collections.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApplicationInsightsComboBox extends AzureComboBox<ApplicationInsightsConfig> {

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

    @Override
    public void setValue(final ApplicationInsightsConfig insightsConfig) {
        if (insightsConfig != null && insightsConfig.isNewCreate() && !getItems().contains(insightsConfig)) {
            addItem(insightsConfig);
        }
        super.setValue(insightsConfig);
    }

    @NotNull
    @Override
    protected List<? extends ApplicationInsightsConfig> loadItems() throws Exception {
        final List<ApplicationInsightsConfig> newItems =
                getItems().stream().filter(item -> item.isNewCreate()).collect(Collectors.toList());
        final List<ApplicationInsightsConfig> existingItems =
                subscription == null ? Collections.EMPTY_LIST :
                AzureSDKManager.getInsightsResources(subscription.subscriptionId())
                               .stream()
                               .map(ApplicationInsightsConfig::new)
                               .collect(Collectors.toList());
        return ListUtils.union(newItems, existingItems);
    }

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
                AllIcons.General.Add, "Create new application insights instance", this::onCreateApplicationInsights);
    }

    @Override
    protected String getItemText(final Object item) {
        if (!(item instanceof ApplicationInsightsConfig)) {
            return EMPTY_ITEM;
        }
        final ApplicationInsightsConfig model = (ApplicationInsightsConfig) item;
        return ((ApplicationInsightsConfig) item).isNewCreate() ? String.format("(New) %s", model.getName()) : model.getName();
    }

    private void onCreateApplicationInsights() {
        final CreateApplicationInsightsDialog dialog = new CreateApplicationInsightsDialog();
        dialog.pack();
        if (dialog.showAndGet()) {
            ApplicationInsightsConfig config = ApplicationInsightsConfig.builder().newCreate(true).name(dialog.getApplicationInsightsName()).build();
            addItem(config);
            setSelectedItem(config);
        }
    }
}
