/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.appservice.insights;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.ide.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.CreateApplicationInsightsDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections.ListUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

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
        this.reloadItems();
    }

    @Override
    public void setValue(final ApplicationInsightsConfig insightsConfig) {
        if (insightsConfig != null && insightsConfig.isNewCreate() && !getItems().contains(insightsConfig)) {
            addItem(insightsConfig);
        }
        super.setValue(insightsConfig);
    }

    @Nonnull
    @Override
    @AzureOperation(
        name = "ai.list_ais.subscription",
        params = {"this.subscription.getId()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends ApplicationInsightsConfig> loadItems() throws Exception {
        final List<ApplicationInsightsConfig> newItems =
            getItems().stream().filter(ApplicationInsightsConfig::isNewCreate).collect(Collectors.toList());
        final List<ApplicationInsightsConfig> existingItems =
            subscription == null ? Collections.emptyList() :
                Azure.az(AzureApplicationInsights.class).applicationInsights(subscription.getId()).list().stream()
                    .map(instance -> new ApplicationInsightsConfig(instance.getName(), instance.getInstrumentationKey()))
                    .collect(Collectors.toList());
        return ListUtils.union(newItems, existingItems);
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("%s (%s)", message("appService.insights.create.tooltip"), KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::onCreateApplicationInsights);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
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
