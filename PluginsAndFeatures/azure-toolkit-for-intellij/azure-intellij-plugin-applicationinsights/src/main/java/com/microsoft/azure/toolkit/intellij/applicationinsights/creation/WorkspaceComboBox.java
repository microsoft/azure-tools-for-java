/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.creation;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.workspace.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.applicationinsights.workspace.LogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class WorkspaceComboBox extends AzureComboBox<LogAnalyticsWorkspace> {
    private Subscription subscription;
    @Setter
    private Region region;
    @Setter
    private ResourceGroup resourceGroup;
    private final List<LogAnalyticsWorkspace> draftItems = new LinkedList<>();

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        // todo use cache manager
        this.loadItems();
    }

    @Nullable
    @Override
    protected LogAnalyticsWorkspace doGetDefaultValue() {
        return CacheManager.getUsageHistory(LogAnalyticsWorkspace.class)
                .peek(v -> (Objects.isNull(subscription) || Objects.equals(subscription, v.getSubscription()) &&
                        (Objects.isNull(region) || Objects.equals(region, v.getRegion()))));
    }

    @Override
    public void setValue(LogAnalyticsWorkspace val) {
        if (Objects.nonNull(val) && val.isDraftForCreating()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val);
    }

    @Override
    protected String getItemText(Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }
        final LogAnalyticsWorkspace workspace = (LogAnalyticsWorkspace) item;
        if (workspace.isDraftForCreating()) {
            return "(New) " + workspace.getName();
        }
        return workspace.getName();
    }

    @Nonnull
    @Override
    protected List<LogAnalyticsWorkspace> loadItems() {
        final List<LogAnalyticsWorkspace> workspaces = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            if (CollectionUtils.isNotEmpty(this.draftItems)) {
                workspaces.addAll(this.draftItems.stream()
                        .filter(p -> this.subscription.equals(p.getSubscription()))
                        .collect(Collectors.toList()));
            }
            final List<LogAnalyticsWorkspace> remoteWorkspaces = Azure.az(AzureLogAnalyticsWorkspace.class)
                    .logAnalyticsWorkspaces(subscription.getId()).list();
            workspaces.addAll(remoteWorkspaces);
            return workspaces.stream().sorted(Comparator.comparing(LogAnalyticsWorkspace::getName)).collect(Collectors.toList());
        }
        return workspaces;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureLogAnalyticsWorkspace.class)
                .logAnalyticsWorkspaces(s.getId()).refresh());
        super.refreshItems();
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        final List<ExtendableTextComponent.Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("%s (%s)", message("workspace.create.tooltip"), KeymapUtil.getKeystrokeText(keyStroke));
        final ExtendableTextComponent.Extension addEx = ExtendableTextComponent.Extension.create(AllIcons.General.Add, tooltip, this::showLoaAnalyticsWorkspaceCreationPopup);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void showLoaAnalyticsWorkspaceCreationPopup() {
        final WorkspaceCreationDialog dialog = new WorkspaceCreationDialog(this.subscription, this.resourceGroup, this.region);
        dialog.setOkActionListener((workspace) -> {
            workspace.setRegion(region);
            dialog.close();
            this.setValue(workspace);
        });
        dialog.show();
    }
}
