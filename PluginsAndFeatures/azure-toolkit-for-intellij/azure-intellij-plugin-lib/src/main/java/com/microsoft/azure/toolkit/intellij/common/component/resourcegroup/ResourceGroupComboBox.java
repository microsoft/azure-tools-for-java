/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component.resourcegroup;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.ui.components.fields.ExtendableTextComponent.Extension;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ResourceGroupComboBox extends AzureComboBox<ResourceGroup> {
    private Subscription subscription;
    private final List<ResourceGroup> draftItems = new ArrayList<>();

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return EMPTY_ITEM;
        }

        final ResourceGroup entity = (ResourceGroup) item;
        if (entity.isDraftForCreating()) {
            return "(New) " + entity.getName();
        }
        return entity.getName();
    }

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

    @Nonnull
    @Override
    @AzureOperation(
        name = "arm.list_resource_groups.subscription",
        params = {"this.subscription.getId()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends ResourceGroup> loadItems() {
        final List<ResourceGroup> groups = new ArrayList<>();
        if (Objects.nonNull(this.subscription)) {
            if (CollectionUtils.isNotEmpty(this.draftItems)) {
                groups.addAll(this.draftItems.stream()
                    .filter(i -> StringUtils.equalsIgnoreCase(this.subscription.getId(), i.getSubscriptionId()))
                    .collect(Collectors.toList()));
            }
            final String sid = subscription.getId();
            final List<ResourceGroup> remoteGroups = Azure.az(AzureResources.class).groups(sid).list().stream()
                .sorted(Comparator.comparing(ResourceGroup::getName)).collect(Collectors.toList());
            groups.addAll(remoteGroups);
        }
        return groups;
    }

    @Nonnull
    @Override
    protected List<Extension> getExtensions() {
        final List<Extension> extensions = super.getExtensions();
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, InputEvent.ALT_DOWN_MASK);
        final String tooltip = String.format("%s (%s)", AzureMessageBundle.message("common.resourceGroup.create.tooltip").toString(), KeymapUtil.getKeystrokeText(keyStroke));
        final Extension addEx = Extension.create(AllIcons.General.Add, tooltip, this::showResourceGroupCreationPopup);
        this.registerShortcut(keyStroke, addEx);
        extensions.add(addEx);
        return extensions;
    }

    private void showResourceGroupCreationPopup() {
        final ResourceGroupCreationDialog dialog = new ResourceGroupCreationDialog(this.subscription);
        dialog.setOkActionListener((group) -> {
            this.draftItems.add(0, group);
            dialog.close();
            final List<ResourceGroup> items = new ArrayList<>(this.getItems());
            items.add(0, group);
            this.setItems(items);
            this.setValue(group);
        });
        dialog.show();
    }
}
