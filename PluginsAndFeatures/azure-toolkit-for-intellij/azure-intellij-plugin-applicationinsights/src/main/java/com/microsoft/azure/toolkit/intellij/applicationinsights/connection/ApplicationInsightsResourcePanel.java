/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.connection;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ApplicationInsightsResourcePanel implements AzureFormJPanel<Resource<ApplicationInsight>> {
    private JPanel pnlRoot;
    private SubscriptionComboBox subscriptionComboBox;
    private AzureComboBox<ApplicationInsight> insightComboBox;

    public ApplicationInsightsResourcePanel() {
        this.init();
    }

    private void init() {
        this.insightComboBox.setRequired(true);
        this.insightComboBox.trackValidation();
        this.subscriptionComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                this.insightComboBox.reloadItems();
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                this.insightComboBox.clear();
            }
        });
    }

    @Override
    public void setValue(Resource<ApplicationInsight> accountResource) {
        ApplicationInsight account = accountResource.getData();
        Optional.ofNullable(account).ifPresent((a -> {
            this.subscriptionComboBox.setValue(new AzureComboBox.ItemReference<>(a.getSubscriptionId(), Subscription::getId));
            this.insightComboBox.setValue(new AzureComboBox.ItemReference<>(a.getName(), ApplicationInsight::getName));
        }));
    }

    @Nullable
    @Override
    public Resource<ApplicationInsight> getValue() {
        final ApplicationInsight account = this.insightComboBox.getValue();
        final AzureValidationInfo info = this.getValidationInfo(true);
        if (!info.isValid()) {
            return null;
        }
        return ApplicationInsightsResourceDefinition.INSTANCE.define(account);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(
                this.insightComboBox,
                this.subscriptionComboBox
        );
    }

    protected void createUIComponents() {
        final Supplier<List<? extends ApplicationInsight>> loader = () -> Optional
                .ofNullable(this.subscriptionComboBox)
                .map(AzureComboBox::getValue)
                .map(Subscription::getId)
                .map(id -> Azure.az(AzureApplicationInsights.class).applicationInsights(id).list()
                        .stream().sorted((first, second) -> StringUtils.compare(first.getName(), second.getName())).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        this.insightComboBox = new AzureComboBox<>(loader) {
            @Override
            protected String getItemText(Object item) {
                return Optional.ofNullable(item).map(i -> ((ApplicationInsight) i).getName()).orElse(StringUtils.EMPTY);
            }

            @Override
            protected void refreshItems() {
                Optional.ofNullable(ApplicationInsightsResourcePanel.this.subscriptionComboBox)
                    .map(AzureComboBox::getValue)
                    .map(Subscription::getId)
                    .ifPresent(id -> Azure.az(AzureApplicationInsights.class).applicationInsights(id).refresh());
                super.refreshItems();
            }
        };
    }

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }
}
