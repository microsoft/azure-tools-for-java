/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.appservice.insights;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.function.runner.component.CreateApplicationInsightsDialog;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import org.apache.commons.collections.ListUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

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
    @AzureOperation(
        name = "ai.list.subscription",
        params = {"@subscription.subscriptionId()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends ApplicationInsightsConfig> loadItems() throws Exception {
        final List<ApplicationInsightsConfig> newItems =
            getItems().stream().filter(ApplicationInsightsConfig::isNewCreate).collect(Collectors.toList());
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
            AllIcons.General.Add, message("appService.insights.create.tooltip"), this::onCreateApplicationInsights);
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
