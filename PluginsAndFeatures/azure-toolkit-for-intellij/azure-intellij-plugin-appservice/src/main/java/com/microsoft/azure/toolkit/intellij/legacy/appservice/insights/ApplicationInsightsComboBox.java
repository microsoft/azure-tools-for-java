/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.appservice.insights;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.component.CreateApplicationInsightsDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.appservice.model.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Setter;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ApplicationInsightsComboBox extends AzureComboBox<ApplicationInsightsConfig> {
    private final List<ApplicationInsightsConfig> draftItems = new LinkedList<>();

    private Subscription subscription;
    @Setter
    private Region region;

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
    public void setValue(final ApplicationInsightsConfig val, Boolean fixed) {
        if (val != null && val.getCreateNewInstance()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val, fixed);
    }

    @Nonnull
    @Override
    @AzureOperation(name = "internal/ai.list_ais.subscription", params = {"this.subscription.getId()"})
    protected List<? extends ApplicationInsightsConfig> loadItems() {
        final List<ApplicationInsightsConfig> existingItems =
            subscription == null ? Collections.emptyList() :
                Azure.az(AzureApplicationInsights.class).applicationInsights(subscription.getId()).list().stream()
                    .map(instance -> ApplicationInsightsConfig.builder().name(instance.getName()).instrumentationKey(instance.getInstrumentationKey()).createNewInstance(false).build())
                    .collect(Collectors.toList());
        return ListUtils.union(this.draftItems, existingItems);
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(subscription).ifPresent(s -> Azure.az(AzureApplicationInsights.class).applicationInsights(subscription.getId()).refresh());
        super.refreshItems();
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
        return BooleanUtils.isTrue(model.getCreateNewInstance()) ? String.format("(New) %s", model.getName()) : model.getName();
    }

    private void onCreateApplicationInsights() {
        final CreateApplicationInsightsDialog dialog = new CreateApplicationInsightsDialog(subscription, region);
        dialog.pack();
        if (dialog.showAndGet()) {
            final ApplicationInsightsConfig config = ApplicationInsightsConfig.builder().createNewInstance(true).name(dialog.getApplicationInsightsName())
                    .workspaceConfig(dialog.getWorkspaceConfig()).build();
            this.setValue(config);
        }
    }
}
